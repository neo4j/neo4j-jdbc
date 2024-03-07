/*
 * Copyright (c) 2023-2024 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.jdbc;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.ClientInfoStatus;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.neo4j.jdbc.internal.bolt.AccessMode;
import org.neo4j.jdbc.internal.bolt.BoltConnection;
import org.neo4j.jdbc.internal.bolt.TransactionType;
import org.neo4j.jdbc.internal.bolt.exception.MessageIgnoredException;
import org.neo4j.jdbc.internal.bolt.exception.Neo4jException;
import org.neo4j.jdbc.translator.spi.Cache;
import org.neo4j.jdbc.translator.spi.Translator;

/**
 * A Neo4j specific implementation of {@link Connection}.
 * <p>
 * At present, this implementation is not expected to be thread-safe.
 *
 * @author Michael J. Simons
 * @since 6.0.0
 */
final class ConnectionImpl implements Neo4jConnection {

	private static final int TRANSLATION_CACHE_SIZE = 128;

	private final BoltConnection boltConnection;

	private final Lazy<List<Translator>> translators;

	private final boolean enableSqlTranslation;

	private final boolean enableTranslationCaching;

	/**
	 * A flag if the {@link Neo4jPreparedStatement prepared statement} should rewrite
	 * batches into UNWIND statements.
	 */
	private final boolean rewriteBatchedStatements;

	private final boolean rewritePlaceholders;

	private Neo4jTransaction transaction;

	private boolean autoCommit = true;

	private boolean readOnly;

	private final Warnings warnings = new Warnings();

	private SQLException fatalException;

	private boolean closed;

	private final Cache<String, String> l2cache = Cache.getInstance(TRANSLATION_CACHE_SIZE);

	ConnectionImpl(BoltConnection boltConnection, Supplier<List<Translator>> translators, boolean enableSQLTranslation,
			boolean enableTranslationCaching, boolean rewriteBatchedStatements, boolean rewritePlaceholders) {
		this.boltConnection = Objects.requireNonNull(boltConnection);
		this.translators = Lazy.of(translators);
		this.enableSqlTranslation = enableSQLTranslation;
		this.enableTranslationCaching = enableTranslationCaching;
		this.rewriteBatchedStatements = rewriteBatchedStatements;
		this.rewritePlaceholders = rewritePlaceholders;
	}

	UnaryOperator<String> getTranslator(Consumer<SQLWarning> warningConsumer) throws SQLException {
		return getTranslator(false, warningConsumer);
	}

	UnaryOperator<String> getTranslator(boolean force, Consumer<SQLWarning> warningConsumer) throws SQLException {
		if (!(this.enableSqlTranslation || force)) {
			return UnaryOperator.identity();
		}

		var translators = this.translators.resolve();
		if (translators.isEmpty()) {
			throw Neo4jDriver.noTranslatorsAvailableException();
		}

		var metaData = this.getMetaData();
		var sqlTranslator = new TranslatorChain(translators, metaData, warningConsumer);

		if (this.enableTranslationCaching) {
			return sql -> {
				synchronized (ConnectionImpl.this) {
					if (this.l2cache.containsKey(sql)) {
						return this.l2cache.get(sql);
					}
					else {
						var translation = sqlTranslator.apply(sql);
						this.l2cache.put(sql, translation);
						return translation;
					}
				}
			};
		}
		return sqlTranslator;
	}

	@Override
	public Statement createStatement() throws SQLException {
		return this.createStatement(ResultSetImpl.SUPPORTED_TYPE, ResultSetImpl.SUPPORTED_CONCURRENCY,
				ResultSetImpl.SUPPORTED_HOLDABILITY);
	}

	@Override
	public PreparedStatement prepareStatement(String sql) throws SQLException {
		return prepareStatement(sql, ResultSetImpl.SUPPORTED_TYPE, ResultSetImpl.SUPPORTED_CONCURRENCY,
				ResultSetImpl.SUPPORTED_HOLDABILITY);
	}

	@Override
	public CallableStatement prepareCall(String sql) throws SQLException {
		return prepareCall(sql, ResultSetImpl.SUPPORTED_TYPE, ResultSetImpl.SUPPORTED_CONCURRENCY,
				ResultSetImpl.SUPPORTED_HOLDABILITY);
	}

	@Override
	public String nativeSQL(String sql) throws SQLException {
		assertIsOpen();
		return getTranslator(true, this.warnings).apply(sql);
	}

	@Override
	public void setAutoCommit(boolean autoCommit) throws SQLException {
		assertIsOpen();
		if (this.autoCommit == autoCommit) {
			return;
		}

		if (this.transaction != null) {
			if (Neo4jTransaction.State.OPEN_FAILED == this.transaction.getState()) {
				throw new SQLException("The existing transaction must be rolled back explicitly.");
			}
			if (this.transaction.isRunnable()) {
				this.transaction.commit();
			}
		}

		this.autoCommit = autoCommit;
	}

	@Override
	public boolean getAutoCommit() throws SQLException {
		assertIsOpen();
		return this.autoCommit;
	}

	@Override
	public void commit() throws SQLException {
		assertIsOpen();
		if (this.transaction == null) {
			throw new SQLException("There is no transaction to commit.");
		}
		if (this.transaction.isAutoCommit()) {
			throw new SQLException("Auto commit transaction may not be managed explicitly.");
		}
		this.transaction.commit();
	}

	@Override
	public void rollback() throws SQLException {
		assertIsOpen();
		if (this.transaction == null) {
			throw new SQLException("There is no transaction to rollback.");
		}
		if (this.transaction.isAutoCommit()) {
			throw new SQLException("Auto commit transaction may not be managed explicitly.");
		}
		this.transaction.rollback();
	}

	@Override
	public void close() throws SQLException {
		if (isClosed()) {
			return;
		}

		Throwable rollbackThrowable = null;
		if (this.transaction != null && this.transaction.isRunnable()) {
			try {
				this.transaction.rollback();
			}
			catch (Throwable throwable) {
				rollbackThrowable = throwable;
			}
		}

		try {
			this.boltConnection.close().toCompletableFuture().get();
		}
		catch (Throwable throwable) {
			if (rollbackThrowable != null) {
				throwable.addSuppressed(rollbackThrowable);
			}
			throw new SQLException("An error occured while closing connection.", throwable);
		}
		finally {
			this.closed = true;
		}
	}

	@Override
	public boolean isClosed() {
		return this.closed;
	}

	@Override
	public DatabaseMetaData getMetaData() throws SQLException {
		assertIsOpen();
		return new DatabaseMetadataImpl(this, () -> getTransaction(false), this.enableSqlTranslation);
	}

	@Override
	public void setReadOnly(boolean readOnly) throws SQLException {
		assertIsOpen();
		if (this.transaction != null && this.transaction.isOpen()) {
			throw new SQLException("Updating read only setting during an unfinished transaction is not permitted.");
		}
		this.readOnly = readOnly;
	}

	@Override
	public boolean isReadOnly() throws SQLException {
		assertIsOpen();
		return this.readOnly;
	}

	@Override
	public void setCatalog(String catalog) throws SQLException {
		assertIsOpen();
		// cannot have catalog.
	}

	@Override
	public String getCatalog() throws SQLException {
		assertIsOpen();
		return null;
	}

	@Override
	public void setTransactionIsolation(int level) throws SQLException {
		throw new SQLException("Setting transaction isolation level is not supported.");
	}

	@Override
	public int getTransactionIsolation() throws SQLException {
		assertIsOpen();
		return Connection.TRANSACTION_READ_COMMITTED;
	}

	@Override
	public SQLWarning getWarnings() throws SQLException {
		assertIsOpen();
		return this.warnings.get();
	}

	@Override
	public void clearWarnings() throws SQLException {
		assertIsOpen();
		this.warnings.clear();
	}

	@Override
	public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
		return createStatement(resultSetType, resultSetConcurrency, ResultSetImpl.SUPPORTED_HOLDABILITY);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
			throws SQLException {
		return prepareStatement(sql, resultSetType, resultSetConcurrency, ResultSetImpl.SUPPORTED_HOLDABILITY);
	}

	private static void assertValidResultSetTypeAndConcurrency(int resultSetType, int resultSetConcurrency)
			throws SQLException {
		if (resultSetType != ResultSetImpl.SUPPORTED_TYPE) {
			throw new SQLException("Unsupported result set type: " + resultSetType);
		}
		if (resultSetConcurrency != ResultSetImpl.SUPPORTED_CONCURRENCY) {
			throw new SQLException("Unsupported result set concurrency: " + resultSetConcurrency);
		}
	}

	@Override
	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
		return prepareCall(sql, resultSetType, resultSetConcurrency, ResultSetImpl.SUPPORTED_HOLDABILITY);
	}

	@Override
	public Map<String, Class<?>> getTypeMap() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setHoldability(int holdability) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public int getHoldability() throws SQLException {
		assertIsOpen();
		return ResultSetImpl.SUPPORTED_HOLDABILITY;
	}

	@Override
	public Savepoint setSavepoint() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Savepoint setSavepoint(String name) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void rollback(Savepoint savepoint) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void releaseSavepoint(Savepoint savepoint) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		assertIsOpen();
		assertValidResultSetTypeAndConcurrency(resultSetType, resultSetConcurrency);
		assertValidResultSetHoldability(resultSetHoldability);
		var localWarnings = new Warnings();
		return new StatementImpl(this, this::getTransaction, getTranslator(localWarnings), localWarnings);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException {
		assertIsOpen();
		assertValidResultSetTypeAndConcurrency(resultSetType, resultSetConcurrency);
		assertValidResultSetHoldability(resultSetHoldability);
		var localWarnings = new Warnings();
		var translator = getTranslator(localWarnings);
		UnaryOperator<String> decoratedTranslator;
		if (this.rewritePlaceholders) {
			decoratedTranslator = (translator != null)
					? s -> PreparedStatementImpl.rewritePlaceholders(translator.apply(s))
					: PreparedStatementImpl::rewritePlaceholders;
		}
		else {
			decoratedTranslator = translator;
		}
		return new PreparedStatementImpl(this, this::getTransaction, decoratedTranslator, localWarnings,
				this.rewriteBatchedStatements, sql);
	}

	@Override
	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException {
		assertIsOpen();
		assertValidResultSetTypeAndConcurrency(resultSetType, resultSetConcurrency);
		assertValidResultSetHoldability(resultSetHoldability);
		return CallableStatementImpl.prepareCall(this, this::getTransaction, this.rewriteBatchedStatements, sql);
	}

	private static void assertValidResultSetHoldability(int resultSetHoldability) throws SQLException {
		if (resultSetHoldability != ResultSetImpl.SUPPORTED_HOLDABILITY) {
			throw new SQLException(
					"Unsupported result set holdability, result sets will always be closed when the underlying transaction is closed: "
							+ resultSetHoldability);
		}
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Clob createClob() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Blob createBlob() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public NClob createNClob() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public SQLXML createSQLXML() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public boolean isValid(int timeout) throws SQLException {
		if (timeout < 0) {
			throw new SQLException("Negative timeout is not supported.");
		}
		if (this.closed) {
			return false;
		}
		if (this.fatalException != null) {
			return false;
		}
		if (this.transaction != null && this.transaction.isRunnable()) {
			try {
				this.transaction.runAndDiscard("RETURN 1", Collections.emptyMap(), timeout, false);
			}
			catch (SQLException ignored) {
				return false;
			}
		}
		else {
			try {
				var future = this.boltConnection.reset(true).toCompletableFuture();
				if (timeout > 0) {
					future.get(timeout, TimeUnit.SECONDS);
				}
				else {
					future.get();
				}
			}
			catch (TimeoutException ignored) {
				return false;
			}
			catch (InterruptedException ex) {
				throw new SQLException("The thread has been interrupted.", ex);
			}
			catch (ExecutionException ex) {
				var cause = ex.getCause();
				if (!(cause instanceof Neo4jException) && !(cause instanceof MessageIgnoredException)) {
					this.fatalException = new SQLException("The connection is no longer valid.", ex);
				}
				return false;
			}
		}
		return true;
	}

	@Override
	public void setClientInfo(String name, String value) throws SQLClientInfoException {
		if (this.closed) {
			throw new SQLClientInfoException("The connection is closed", Collections.emptyMap());
		}
		Objects.requireNonNull(name);
		var reason = "Client info property is not supported.";
		var throwable = new SQLClientInfoException(Map.of(name, ClientInfoStatus.REASON_UNKNOWN_PROPERTY));
		this.warnings.accept(new SQLWarning(reason, throwable));
	}

	@Override
	public void setClientInfo(Properties properties) throws SQLClientInfoException {
		if (this.closed) {
			throw new SQLClientInfoException("The connection is closed", Collections.emptyMap());
		}
		Objects.requireNonNull(properties);
		var failedProperties = new HashMap<String, ClientInfoStatus>();
		for (var entry : properties.entrySet()) {
			var key = entry.getKey();
			if (key instanceof String keyString) {
				failedProperties.put(keyString, ClientInfoStatus.REASON_UNKNOWN_PROPERTY);
			}
		}
		if (!failedProperties.isEmpty()) {
			var reason = "Client info properties are not supported.";
			var throwable = new SQLClientInfoException(Collections.unmodifiableMap(failedProperties));
			this.warnings.accept(new SQLWarning(reason, throwable));
		}
	}

	@Override
	public String getClientInfo(String name) throws SQLException {
		assertIsOpen();
		return null;
	}

	@Override
	public Properties getClientInfo() throws SQLException {
		assertIsOpen();
		return new Properties();
	}

	@Override
	public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setSchema(String schema) throws SQLException {
		assertIsOpen();
	}

	@Override
	public String getSchema() throws SQLException {
		assertIsOpen();
		return "public";
	}

	@Override
	public void abort(Executor ignored) throws SQLException {
		if (this.closed) {
			return;
		}
		if (this.fatalException != null) {
			this.closed = true;
			return;
		}
		this.fatalException = new SQLException("The connection has been explicitly aborted.");
		if (this.transaction != null && this.transaction.isRunnable()) {
			this.transaction.fail(this.fatalException);
		}
		try {
			this.boltConnection.close().toCompletableFuture().get();
		}
		catch (InterruptedException | ExecutionException ex) {
			this.fatalException.addSuppressed(ex);
		}
		this.closed = true;
	}

	@Override
	public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public int getNetworkTimeout() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		if (iface.isAssignableFrom(getClass())) {
			return iface.cast(this);
		}
		else {
			throw new SQLException("This object does not implement the given interface.");
		}
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return iface.isAssignableFrom(getClass());
	}

	private void assertIsOpen() throws SQLException {
		if (this.closed) {
			throw new SQLException("The connection is closed.");
		}
	}

	Neo4jTransaction getTransaction() throws SQLException {
		return getTransaction(true);
	}

	Neo4jTransaction getTransaction(boolean autoCommitCheck) throws SQLException {
		assertIsOpen();
		if (this.fatalException != null) {
			throw this.fatalException;
		}
		if (this.transaction != null && this.transaction.isOpen()) {
			if (autoCommitCheck && this.transaction.isAutoCommit()) {
				throw new SQLException("Only a single autocommit transaction is supported.");
			}
		}
		else {
			var resetStage = (this.transaction != null
					&& Neo4jTransaction.State.FAILED.equals(this.transaction.getState()))
							? this.boltConnection.reset(false) : CompletableFuture.completedStage(null);
			var transactionType = this.autoCommit ? TransactionType.UNCONSTRAINED : TransactionType.DEFAULT;
			var beginStage = this.boltConnection.beginTransaction(Collections.emptySet(), getAccessMode(),
					transactionType, false);
			this.transaction = new DefaultTransactionImpl(this.boltConnection,
					exception -> this.fatalException = exception, resetStage.thenCompose(ignored -> beginStage),
					this.autoCommit);
		}
		return this.transaction;
	}

	private AccessMode getAccessMode() {
		return this.readOnly ? AccessMode.READ : AccessMode.WRITE;
	}

	@Override
	public void flushTranslationCache() {
		synchronized (this) {
			this.l2cache.flush();
			this.translators.resolve().forEach(Translator::flushCache);
		}
	}

	static class TranslatorChain implements UnaryOperator<String> {

		private final List<Translator> translators;

		private final DatabaseMetaData metaData;

		private final Consumer<SQLWarning> warningSink;

		TranslatorChain(List<Translator> translators, DatabaseMetaData metaData, Consumer<SQLWarning> warningSink) {
			this.translators = translators;
			this.metaData = metaData;
			this.warningSink = warningSink;
		}

		@Override
		public String apply(String statement) {

			if (this.translators.size() == 1) {
				return this.translators.get(0).translate(statement, this.metaData);
			}
			String result = null;
			for (var translator : this.translators) {
				var in = (result != null) ? result : statement;
				try {
					result = translator.translate(in, this.metaData);
				}
				catch (IllegalArgumentException ex) {
					this.warningSink.accept(new SQLWarning(
							"Translator %s failed to translate `%s`".formatted(translator.getClass().getName(), in),
							ex));
				}
			}

			if (result == null) {
				throw new IllegalStateException("No suitable translator for input `%s`".formatted(statement));
			}

			return result;
		}

	}

}
