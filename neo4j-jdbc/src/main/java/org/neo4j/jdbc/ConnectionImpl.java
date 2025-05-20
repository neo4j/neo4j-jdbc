/*
 * Copyright (c) 2023-2025 "Neo4j,"
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

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.net.URI;
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
import java.time.Duration;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.neo4j.bolt.connection.AccessMode;
import org.neo4j.bolt.connection.BasicResponseHandler;
import org.neo4j.bolt.connection.BoltConnection;
import org.neo4j.bolt.connection.exception.BoltConnectionReadTimeoutException;
import org.neo4j.bolt.connection.exception.BoltFailureException;
import org.neo4j.bolt.connection.message.Messages;
import org.neo4j.jdbc.Neo4jException.GQLError;
import org.neo4j.jdbc.Neo4jTransaction.State;
import org.neo4j.jdbc.events.ConnectionListener;
import org.neo4j.jdbc.events.ConnectionListener.StatementClosedEvent;
import org.neo4j.jdbc.events.ConnectionListener.StatementCreatedEvent;
import org.neo4j.jdbc.events.ConnectionListener.TranslationCachedEvent;
import org.neo4j.jdbc.events.StatementListener;
import org.neo4j.jdbc.tracing.Neo4jTracer;
import org.neo4j.jdbc.translator.spi.Cache;
import org.neo4j.jdbc.translator.spi.Translator;
import org.neo4j.jdbc.values.Type;

import static org.neo4j.jdbc.Neo4jException.withCause;
import static org.neo4j.jdbc.Neo4jException.withInternal;
import static org.neo4j.jdbc.Neo4jException.withReason;

/**
 * A Neo4j specific implementation of {@link Connection}.
 * <p>
 * At present, this implementation is not expected to be thread-safe.
 *
 * @author Michael J. Simons
 * @since 6.0.0
 */
final class ConnectionImpl implements Neo4jConnection {

	// Adding the comment /*+ NEO4J FORCE_CYPHER */ to your Cypher statement will make the
	// JDBC driver opt-out from translating it to Cypher, even if the driver has been
	// configured for automatic translation.
	private static final Pattern PATTERN_ENFORCE_CYPHER = Pattern
		.compile("(['`\"])?[^'`\"]*/\\*\\+ NEO4J FORCE_CYPHER \\*/[^'`\"]*(['`\"])?");

	private static final Logger LOGGER = Logger.getLogger("org.neo4j.jdbc.connection");

	private static final int TRANSLATION_CACHE_SIZE = 128;

	private final URI databaseUrl;

	private final BoltConnection boltConnection;

	private final Lazy<BoltConnection, RuntimeException> boltConnectionForMetaData;

	private final Lazy<DatabaseMetaData, RuntimeException> databaseMetadData;

	private final Set<Reference<Statement>> trackedStatementReferences = new HashSet<>();

	private final ReferenceQueue<Statement> trackedStatementReferenceQueue = new ReferenceQueue<>();

	private final Lazy<List<Translator>, RuntimeException> translators;

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

	private int networkTimeout;

	private final Warnings warnings = new Warnings();

	private SQLException fatalException;

	private boolean closed;

	private final Cache<String, String> l2cache = Cache.getInstance(TRANSLATION_CACHE_SIZE);

	private final BookmarkManager bookmarkManager;

	private final Map<String, Object> transactionMetadata = new ConcurrentHashMap<>();

	private final int relationshipSampleSize;

	private final String databaseName;

	private final AtomicBoolean resetNeeded = new AtomicBoolean(false);

	/**
	 * Neo4j as of now has no session / server state to hold those, but we keep it around
	 * for future use.
	 */
	private final Map<String, String> clientInfo = new ConcurrentHashMap<>();

	/**
	 * Callback from the owning driver.
	 */
	private final Consumer<Boolean> onClose;

	private final Set<ConnectionListener> listeners = new HashSet<>();

	ConnectionImpl(URI databaseUrl, Supplier<BoltConnection> boltConnectionSupplier,
			Supplier<List<Translator>> translators, boolean enableSQLTranslation, boolean enableTranslationCaching,
			boolean rewriteBatchedStatements, boolean rewritePlaceholders, BookmarkManager bookmarkManager,
			Map<String, Object> transactionMetadata, int relationshipSampleSize, String databaseName,
			Consumer<Boolean> onClose) {
		Objects.requireNonNull(boltConnectionSupplier);

		this.databaseUrl = Objects.requireNonNull(databaseUrl);
		this.boltConnection = boltConnectionSupplier.get();
		this.boltConnectionForMetaData = Lazy.of(boltConnectionSupplier);
		this.translators = Lazy.of(translators);
		this.enableSqlTranslation = enableSQLTranslation;
		this.enableTranslationCaching = enableTranslationCaching;
		this.rewriteBatchedStatements = rewriteBatchedStatements;
		this.rewritePlaceholders = rewritePlaceholders;
		this.bookmarkManager = Objects.requireNonNull(bookmarkManager);
		this.transactionMetadata.putAll(Objects.requireNonNullElseGet(transactionMetadata, Map::of));
		this.relationshipSampleSize = relationshipSampleSize;
		this.databaseName = Objects.requireNonNull(databaseName);
		this.databaseMetadData = Lazy.of((Supplier<DatabaseMetaData>) () -> {
			var views = this.translators.resolve().stream().flatMap(t -> t.getViews().stream()).toList();
			return new DatabaseMetadataImpl(this, this.enableSqlTranslation, this.relationshipSampleSize, views);
		});
		this.onClose = Objects.requireNonNullElse(onClose, aborted -> {
		});
	}

	void notifyStatementListeners(Class<? extends Statement> type) {
		var event = new StatementClosedEvent(this.databaseUrl, type);
		Events.notify(this.listeners, listener -> listener.onStatementClosed(event));
	}

	UnaryOperator<String> getTranslator(Consumer<SQLWarning> warningConsumer) throws SQLException {
		return getTranslator(false, warningConsumer);
	}

	UnaryOperator<String> getTranslator(boolean force, Consumer<SQLWarning> warningConsumer) throws SQLException {

		List<Translator> resolvedTranslators;
		if (!(this.enableSqlTranslation || force)) {
			resolvedTranslators = List.of((statement, optionalDatabaseMetaData) -> statement);
		}
		else {
			resolvedTranslators = this.translators.resolve();
		}

		if (resolvedTranslators.isEmpty()) {
			throw Neo4jDriver.noTranslatorsAvailableException();
		}

		var metaData = this.getMetaData();
		var sqlTranslator = new TranslatorChain(resolvedTranslators, metaData, warningConsumer);

		if (this.enableTranslationCaching) {
			return sql -> {
				synchronized (ConnectionImpl.this) {
					if (this.l2cache.containsKey(sql)) {
						return this.l2cache.get(sql);
					}
					else {
						var translation = sqlTranslator.apply(sql);
						this.l2cache.put(sql, translation);
						var event = new TranslationCachedEvent(this.l2cache.size());
						Events.notify(this.listeners, listener -> listener.onTranslationCached(event));
						return translation;
					}
				}
			};
		}
		return sqlTranslator;
	}

	@SuppressWarnings("MagicConstant") // On purpose
	@Override
	public Statement createStatement() throws SQLException {
		return this.createStatement(ResultSetImpl.SUPPORTED_TYPE, ResultSetImpl.SUPPORTED_CONCURRENCY,
				ResultSetImpl.SUPPORTED_HOLDABILITY);
	}

	@SuppressWarnings({ "MagicConstant", "SqlSourceToSinkFlow" }) // On purpose
	@Override
	public PreparedStatement prepareStatement(String sql) throws SQLException {
		return prepareStatement(sql, ResultSetImpl.SUPPORTED_TYPE, ResultSetImpl.SUPPORTED_CONCURRENCY,
				ResultSetImpl.SUPPORTED_HOLDABILITY);
	}

	@SuppressWarnings("SqlSourceToSinkFlow") // O'Really?
	@Override
	public CallableStatement prepareCall(String sql) throws SQLException {
		return prepareCall(sql, ResultSetImpl.SUPPORTED_TYPE, ResultSetImpl.SUPPORTED_CONCURRENCY,
				ResultSetImpl.SUPPORTED_HOLDABILITY);
	}

	@Override
	public String nativeSQL(String sql) throws SQLException {
		LOGGER.log(Level.FINER, () -> "Translating `%s` into native SQL".formatted(sql));
		assertIsOpen();
		return getTranslator(true, this.warnings).apply(sql);
	}

	@Override
	public void setAutoCommit(boolean autoCommit) throws SQLException {
		LOGGER.log(Level.FINER, () -> "Setting auto commit to %s".formatted(autoCommit));
		assertIsOpen();
		if (this.autoCommit == autoCommit) {
			return;
		}

		if (this.transaction != null) {
			if (State.OPEN_FAILED == this.transaction.getState()) {
				throw new Neo4jException(withReason("The existing transaction must be rolled back explicitly"));
			}
			if (this.transaction.isRunnable()) {
				this.transaction.commit();
			}
		}

		this.autoCommit = autoCommit;
	}

	@Override
	public boolean getAutoCommit() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Getting auto commit");
		assertIsOpen();
		return this.autoCommit;
	}

	@Override
	public void commit() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Commiting");
		assertIsOpen();
		if (this.transaction == null || State.COMMITTED.equals(this.transaction.getState())) {
			LOGGER.log(Level.INFO, "There is no active transaction that can be committed, ignoring");
			return;
		}
		if (this.transaction.isAutoCommit()) {
			throw new Neo4jException(
					GQLError.$2DN01.withTemplatedMessage("Auto commit transaction may not be managed explicitly"));
		}
		this.transaction.commit();
		this.transaction = null;
	}

	@Override
	public void rollback() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Rolling back");
		assertIsOpen();
		if (this.transaction == null || !this.transaction.isRunnable()) {
			LOGGER.log(Level.INFO, "There is no active transaction that can be rolled back, ignoring");
			this.transaction = null;
			return;
		}
		if (this.transaction.isAutoCommit()) {
			throw new Neo4jException(
					GQLError.$40N01.withTemplatedMessage("Auto commit transaction may not be managed explicitly"));
		}
		this.transaction.rollback();
		this.transaction = null;
	}

	@Override
	public void close() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Closing");
		if (isClosed()) {
			return;
		}

		Exception exceptionDuringRollback = null;
		if (this.transaction != null && this.transaction.isRunnable()) {
			try {
				this.transaction.rollback();
			}
			catch (Exception ex) {
				exceptionDuringRollback = ex;
			}
		}

		try {
			closeBoltConnections();
		}
		catch (Exception ex) {
			if (ex instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			if (exceptionDuringRollback != null) {
				ex.addSuppressed(exceptionDuringRollback);
			}
			throw new Neo4jException(
					GQLError.$08000.causedBy(ex).withMessage("An error occurred while closing connection"));
		}
		finally {
			this.closed = true;
			this.onClose.accept(false);
		}
	}

	private void closeBoltConnections() throws InterruptedException, ExecutionException {
		this.boltConnection.close().toCompletableFuture().get();
		synchronized (this.boltConnectionForMetaData) {
			if (this.boltConnectionForMetaData.isResolved()) {
				this.boltConnectionForMetaData.resolve().close();
			}
		}
	}

	@Override
	public boolean isClosed() {
		LOGGER.log(Level.FINER, () -> "Getting closed state");
		return this.closed;
	}

	@Override
	public DatabaseMetaData getMetaData() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Getting metadata");
		assertIsOpen();
		return this.databaseMetadData.resolve().unwrap(Neo4jDatabaseMetaData.class).flush();
	}

	@Override
	public void setReadOnly(boolean readOnly) throws SQLException {
		LOGGER.log(Level.FINER, () -> "Setting read only to %s".formatted(readOnly));
		assertIsOpen();
		if (this.transaction != null && this.transaction.isOpen()) {
			throw new Neo4jException(
					withReason("Updating read only setting during an unfinished transaction is not permitted"));
		}
		this.readOnly = readOnly;
	}

	@Override
	public boolean isReadOnly() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Getting read only state");
		assertIsOpen();
		return this.readOnly;
	}

	@Override
	public void setCatalog(String catalog) throws SQLException {
		LOGGER.log(Level.FINER, () -> "Setting catalog to `%s`".formatted(catalog));
		assertIsOpen();
		if (this.databaseName == null || !this.databaseName.equalsIgnoreCase(catalog)) {
			throw new SQLFeatureNotSupportedException("Changing the catalog is not implemented");
		}
	}

	@Override
	public String getCatalog() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Getting catalog");
		assertIsOpen();
		return this.getDatabaseName();
	}

	@Override
	public void setTransactionIsolation(int level) throws SQLException {
		throw new SQLFeatureNotSupportedException("Setting transaction isolation level is not supported");
	}

	@Override
	public int getTransactionIsolation() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Getting transaction isolation");
		assertIsOpen();
		return Connection.TRANSACTION_READ_COMMITTED;
	}

	@Override
	public SQLWarning getWarnings() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Getting warnings");
		assertIsOpen();
		return this.warnings.get();
	}

	@Override
	public void clearWarnings() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Clearing warnings");
		assertIsOpen();
		this.warnings.clear();
	}

	@SuppressWarnings("MagicConstant")
	@Override
	public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
		return createStatement(resultSetType, resultSetConcurrency, ResultSetImpl.SUPPORTED_HOLDABILITY);
	}

	@SuppressWarnings({ "MagicConstant", "SqlSourceToSinkFlow" })
	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
			throws SQLException {
		return prepareStatement(sql, resultSetType, resultSetConcurrency, ResultSetImpl.SUPPORTED_HOLDABILITY);
	}

	private static void assertValidResultSetTypeAndConcurrency(int resultSetType, int resultSetConcurrency)
			throws SQLException {
		if (resultSetType != ResultSetImpl.SUPPORTED_TYPE) {
			throw new SQLFeatureNotSupportedException("Unsupported result set type: " + resultSetType);
		}
		if (resultSetConcurrency != ResultSetImpl.SUPPORTED_CONCURRENCY) {
			throw new SQLFeatureNotSupportedException("Unsupported result set concurrency: " + resultSetConcurrency);
		}
	}

	@SuppressWarnings("SqlSourceToSinkFlow")
	@Override
	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
		return prepareCall(sql, resultSetType, resultSetConcurrency, ResultSetImpl.SUPPORTED_HOLDABILITY);
	}

	@Override
	public Map<String, Class<?>> getTypeMap() {
		LOGGER.log(Level.FINER, () -> "Getting type map");
		return Map.of();
	}

	@Override
	public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
		LOGGER.log(Level.FINER, () -> "Setting type map");
		Neo4jConversions.assertTypeMap(map);
	}

	@Override
	public void setHoldability(int holdability) throws SQLException {
		LOGGER.log(Level.FINER, () -> "Setting holdability to %d".formatted(holdability));
		if (holdability != ResultSetImpl.SUPPORTED_HOLDABILITY) {
			throw new SQLFeatureNotSupportedException();
		}
	}

	@Override
	public int getHoldability() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Getting holdability");
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
		LOGGER.log(Level.FINER, () -> "Creating statement with with type %d, concurrency %d and holdability %d"
			.formatted(resultSetType, resultSetConcurrency, resultSetHoldability));
		assertIsOpen();
		assertValidResultSetTypeAndConcurrency(resultSetType, resultSetConcurrency);
		assertValidResultSetHoldability(resultSetHoldability);
		var localWarnings = new Warnings();
		return trackStatement(new StatementImpl(this, this::getTransaction, getTranslator(localWarnings), localWarnings,
				this::notifyStatementListeners));
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException {
		LOGGER.log(Level.FINER, () -> "Preparing statement `%s` with type %d, concurrency %d and holdability %d"
			.formatted(sql, resultSetType, resultSetConcurrency, resultSetHoldability));
		assertIsOpen();
		assertValidResultSetTypeAndConcurrency(resultSetType, resultSetConcurrency);
		assertValidResultSetHoldability(resultSetHoldability);
		var localWarnings = new Warnings();
		return trackStatement(
				new PreparedStatementImpl(this, this::getTransaction, getTranslator(localWarnings), localWarnings,
						this::notifyStatementListeners, this.rewritePlaceholders, this.rewriteBatchedStatements, sql));
	}

	@Override
	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException {
		LOGGER.log(Level.FINER, () -> "Preparing call `%s` with type %d, concurrency %d and holdability %d"
			.formatted(sql, resultSetType, resultSetConcurrency, resultSetHoldability));
		assertIsOpen();
		assertValidResultSetTypeAndConcurrency(resultSetType, resultSetConcurrency);
		assertValidResultSetHoldability(resultSetHoldability);
		return trackStatement(CallableStatementImpl.prepareCall(this, this::getTransaction,
				this::notifyStatementListeners, this.rewriteBatchedStatements, sql));
	}

	private static void assertValidResultSetHoldability(int resultSetHoldability) throws SQLException {
		if (resultSetHoldability != ResultSetImpl.SUPPORTED_HOLDABILITY) {
			throw new SQLFeatureNotSupportedException(
					"Unsupported result set holdability, result sets will always be closed when the underlying transaction is closed: "
							+ resultSetHoldability);
		}
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
		LOGGER.log(Level.FINER,
				() -> "Trying to prepare statement with auto generated keys set to %d".formatted(autoGeneratedKeys));
		if (autoGeneratedKeys != Statement.NO_GENERATED_KEYS) {
			throw new SQLFeatureNotSupportedException();
		}

		return prepareStatement(sql);
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
		LOGGER.log(Level.FINER, () -> "Checking validity with timeout %d".formatted(timeout));
		if (timeout < 0) {
			throw new Neo4jException(GQLError.$22N02.withTemplatedMessage("timeout", timeout));
		}
		if (this.closed || this.fatalException != null) {
			return false;
		}
		if (this.transaction != null && this.transaction.isRunnable()) {
			try {
				this.transaction.runAndDiscard("RETURN 1", Collections.emptyMap(), timeout, false);
				return true;
			}
			catch (SQLException ignored) {
				return false;
			}
		}

		try {
			var handler = new BasicResponseHandler();
			var future = this.boltConnection.writeAndFlush(handler, Messages.reset())
				.thenCompose(ignored -> handler.summaries())
				.toCompletableFuture();
			if (timeout > 0) {
				future.get(timeout, TimeUnit.SECONDS);
			}
			else {
				future.get();
			}
			return true;
		}
		catch (TimeoutException ignored) {
			return false;
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new Neo4jException(withInternal(ex, "The thread has been interrupted."));
		}
		catch (ExecutionException ex) {
			var cause = Optional.ofNullable(ex.getCause()).orElse(ex);
			if (!(cause instanceof BoltFailureException)) {
				this.fatalException = new Neo4jException(
						GQLError.$08000.withMessage("The connection is no longer valid"));
				handleFatalException(this.fatalException, new Neo4jException(withCause(cause)));
			}
			return false;
		}
	}

	@Override
	public void setClientInfo(String name, String value) throws SQLClientInfoException {
		LOGGER.log(Level.FINER, () -> "Setting client info `%s` to `%s`".formatted(name, value));
		if (this.closed) {
			throw new SQLClientInfoException("The connection is closed", Collections.emptyMap());
		}

		try {
			setClientInfo0(name, value);
		}
		catch (SQLWarning ex) {
			this.warnings.accept(ex);
		}
	}

	private void setClientInfo0(String name, String value) throws SQLWarning {
		if (name == null || name.isBlank()) {
			var throwable = new SQLClientInfoException(Map.of("", ClientInfoStatus.REASON_UNKNOWN));
			throw new SQLWarning("Client information without a name are not supported", throwable);
		}
		if (!DatabaseMetadataImpl.isSupportedClientInfoProperty(name)) {
			var throwable = new SQLClientInfoException(Map.of(name, ClientInfoStatus.REASON_UNKNOWN_PROPERTY));
			throw new SQLWarning("Unknown client info property `" + name + "`", throwable);
		}
		if (value == null || value.isBlank()) {
			var throwable = new SQLClientInfoException(Map.of("", ClientInfoStatus.REASON_VALUE_INVALID));
			throw new SQLWarning("Client information without a value are not supported", throwable);

		}
		this.clientInfo.put(name, value);
	}

	@Override
	public void setClientInfo(Properties properties) throws SQLClientInfoException {
		LOGGER.log(Level.FINER, () -> "Setting client info via properties");
		if (this.closed) {
			throw new SQLClientInfoException("The connection is closed", Collections.emptyMap());
		}
		Objects.requireNonNull(properties);
		var failedProperties = new HashMap<String, ClientInfoStatus>();
		// It is supposed to be an atomic operation
		synchronized (this) {
			for (String key : properties.stringPropertyNames()) {
				try {
					setClientInfo0(key, properties.getProperty(key));
				}
				catch (SQLWarning ex) {
					failedProperties.putAll(((SQLClientInfoException) ex.getCause()).getFailedProperties());
				}
			}
		}
		if (!failedProperties.isEmpty()) {
			var throwable = new SQLClientInfoException(Collections.unmodifiableMap(failedProperties));
			this.warnings.accept(new SQLWarning("There have been issues setting some properties", throwable));
		}
	}

	@Override
	public String getClientInfo(String name) throws SQLException {
		LOGGER.log(Level.FINER, () -> "Getting client info `%s`".formatted(name));
		assertIsOpen();
		return this.clientInfo.get(name);
	}

	@Override
	public Properties getClientInfo() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Getting client info");
		assertIsOpen();
		var result = new Properties();
		result.putAll(this.clientInfo);
		return result;
	}

	/**
	 * Please see {@link Connection#createArrayOf(String, Object[])} for the full
	 * documentation. The Neo4j implementation here as special requirements for the type
	 * name.
	 * @param typeName supported names are defined by Neo4j types {@link Type}.
	 * @param elements the elements that populate the returned object
	 * @return a new Array object
	 * @throws SQLException if a database error occurs, the JDBC type is not * appropriate
	 * for the typeName and the conversion is not supported, the typeName is null or this
	 * method is called on a closed connection
	 * @see Connection#createArrayOf(String, Object[])
	 */
	@Override
	public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
		return ArrayImpl.of(this, typeName, elements);
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
		LOGGER.log(Level.FINER, () -> "Getting schema");
		assertIsOpen();
		return "public";
	}

	@Override
	public void abort(Executor ignored) throws SQLException {
		LOGGER.log(Level.FINER, () -> "Trying to abort the current transaction");
		if (this.closed) {
			return;
		}
		if (this.fatalException != null) {
			this.closed = true;
			return;
		}
		this.fatalException = new Neo4jException(withReason("The connection has been explicitly aborted."));
		if (this.transaction != null && this.transaction.isRunnable()) {
			this.transaction.fail(this.fatalException);
		}
		try {
			closeBoltConnections();
		}
		catch (InterruptedException | ExecutionException ex) {
			if (ex instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			this.fatalException.addSuppressed(ex);
		}
		finally {
			this.closed = true;
			this.onClose.accept(true);
		}
	}

	@Override
	public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
		LOGGER.log(Level.FINER, () -> "Setting network timeout to %d milliseconds".formatted(milliseconds));
		assertIsOpen();
		if (milliseconds < 0) {
			throw new Neo4jException(GQLError.$22N02.withTemplatedMessage("network timeout", milliseconds));
		}
		this.networkTimeout = milliseconds;
		if (milliseconds == 0) {
			this.boltConnection.defaultReadTimeout()
				.ifPresent(defaultTimeout -> LOGGER.log(Level.FINE, String.format(
						"setNetworkTimeout has been called with 0, will use the Bolt server default of % d milliseconds.",
						defaultTimeout.toMillis())));
			setReadTimeout0(null);
		}
		else {
			setReadTimeout0(Duration.ofMillis(this.networkTimeout));
		}
	}

	private void setReadTimeout0(Duration duration) throws Neo4jException {
		var failureMessage = "Failed to set read timeout";
		try {
			this.boltConnection.setReadTimeout(duration).toCompletableFuture().get();
		}
		catch (ExecutionException ex) {
			throw new Neo4jException(withInternal(ex, failureMessage));
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new Neo4jException(withInternal(ex, failureMessage));
		}
	}

	@Override
	public int getNetworkTimeout() {
		LOGGER.log(Level.FINER, () -> "Getting network timeout");
		return this.networkTimeout;
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		LOGGER.log(Level.FINER,
				() -> "Unwrapping `%s` into `%s`".formatted(getClass().getCanonicalName(), iface.getCanonicalName()));
		if (iface.isAssignableFrom(getClass())) {
			return iface.cast(this);
		}
		else {
			throw new Neo4jException(withReason("This object does not implement the given interface"));
		}
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return iface.isAssignableFrom(getClass());
	}

	private void assertIsOpen() throws SQLException {
		if (this.closed) {
			throw new Neo4jException(GQLError.$08000.withMessage("The connection is closed"));
		}
	}

	/**
	 * Gets the current transactions or creates a new one.
	 * @param additionalTransactionMetadata any additional metadata that should be
	 * attached to the transaction
	 * @return a transaction
	 * @throws SQLException if there's already an open transaction and the connection is
	 * using auto commit mode
	 */
	Neo4jTransaction getTransaction(Map<String, Object> additionalTransactionMetadata) throws SQLException {
		assertIsOpen();
		if (this.fatalException != null) {
			throw this.fatalException;
		}
		if (this.transaction != null && this.transaction.isOpen()) {
			if (this.transaction.isAutoCommit()) {
				throw new SQLFeatureNotSupportedException("Only a single autocommit transaction is supported");
			}
			return this.transaction;
		}

		var combinedTransactionMetadata = getCombinedTransactionMetadata(additionalTransactionMetadata);
		this.transaction = new DefaultTransactionImpl(this.boltConnection, this.bookmarkManager,
				combinedTransactionMetadata, this::handleFatalException, this.resetNeeded.getAndSet(false),
				this.autoCommit, getAccessMode(), null, this.databaseName, state -> this.resetNeeded
					.compareAndSet(false, EnumSet.of(State.FAILED, State.OPEN_FAILED).contains(state)));
		return this.transaction;
	}

	/**
	 * Creates a new transaction that is not yet attached to this connection and might
	 * never will.
	 * @param additionalTransactionMetadata any additional metadata that should be
	 * attached to the transaction
	 * @return a transaction
	 * @throws SQLException if {@link #getApp()} fails to retrieve client info
	 */
	Neo4jTransaction newMetadataTransaction(Map<String, Object> additionalTransactionMetadata) throws SQLException {
		var combinedTransactionMetadata = getCombinedTransactionMetadata(additionalTransactionMetadata);
		return new DefaultTransactionImpl(this.boltConnectionForMetaData.resolve(), this.bookmarkManager,
				combinedTransactionMetadata, this::handleFatalException, false, this.autoCommit, getAccessMode(), null,
				this.databaseName, state -> {
				});
	}

	private Map<String, Object> getCombinedTransactionMetadata(Map<String, Object> additionalTransactionMetadata)
			throws SQLException {
		Map<String, Object> combinedTransactionMetadata = new HashMap<>(
				this.transactionMetadata.size() + additionalTransactionMetadata.size() + 1);
		combinedTransactionMetadata.putAll(this.transactionMetadata);
		combinedTransactionMetadata.putAll(additionalTransactionMetadata);
		if (!combinedTransactionMetadata.containsKey("app")) {
			combinedTransactionMetadata.put("app", this.getApp());
		}
		return combinedTransactionMetadata;
	}

	private AccessMode getAccessMode() {
		return this.readOnly ? AccessMode.READ : AccessMode.WRITE;
	}

	@Override
	public void flushTranslationCache() {
		LOGGER.log(Level.FINER, () -> "Flushing translation cache");
		synchronized (this) {
			this.l2cache.flush();
			this.translators.resolve().forEach(Translator::flushCache);
		}
	}

	@Override
	public String getDatabaseName() {
		LOGGER.log(Level.FINER, () -> "Getting database name");
		return this.databaseName;
	}

	@Override
	public void addListener(ConnectionListener connectionListener) {
		this.listeners.add(Objects.requireNonNull(connectionListener));
	}

	private <T extends StatementImpl> T trackStatement(T statement) {
		purgeClearedStatementReferences();

		this.trackedStatementReferences.add(new WeakReference<>(statement, this.trackedStatementReferenceQueue));

		if (!this.listeners.isEmpty()) {
			this.listeners.forEach(listener -> {
				if (listener instanceof StatementListener statementListener) {
					statement.addListener(statementListener);
				}
			});

			Class<? extends Statement> type = statement.getType();
			var statementCreatedEvent = new StatementCreatedEvent(this.databaseUrl, type, statement);
			Events.notify(this.listeners, listener -> listener.onStatementCreated(statementCreatedEvent));
		}

		return statement;
	}

	private void purgeClearedStatementReferences() {
		var reference = this.trackedStatementReferenceQueue.poll();
		while (reference != null) {
			this.trackedStatementReferences.remove(reference);
			reference = this.trackedStatementReferenceQueue.poll();
		}
	}

	private void handleFatalException(SQLException fatalSqlException, SQLException sqlException) {
		var cause = sqlException.getCause();
		if (cause instanceof BoltConnectionReadTimeoutException) {
			for (var reference : this.trackedStatementReferences) {
				var statement = reference.get();
				if (statement != null) {
					try {
						statement.close();
					}
					catch (Exception ex) {
						sqlException.addSuppressed(ex);
					}
				}
			}
			try {
				close();
			}
			catch (SQLException ex) {
				sqlException.addSuppressed(ex);
			}
		}
		else {
			this.fatalException = fatalSqlException;
		}
	}

	@Override
	public Neo4jConnection withMetadata(Map<String, Object> metadata) {
		LOGGER.log(Level.FINER, () -> "Adding new transaction metadata");
		if (metadata != null) {
			this.transactionMetadata.putAll(metadata);
		}
		return this;
	}

	/**
	 * A string suitable to be used as {@code app} value inside Neo4j transactional
	 * metadata.
	 * @return a string suitable to be used as {@code app} value inside Neo4j
	 * transactional metadata
	 */
	String getApp() throws SQLException {
		var applicationName = getClientInfo("ApplicationName");
		return String.format("%sJava/%s (%s %s %s) neo4j-jdbc/%s",
				(applicationName == null || applicationName.isBlank()) ? "" : applicationName.trim() + " ",
				Optional.ofNullable(System.getProperty("java.version"))
					.filter(Predicate.not(String::isBlank))
					.orElse("-"),
				Optional.ofNullable(System.getProperty("java.vm.vendor"))
					.filter(Predicate.not(String::isBlank))
					.orElse("-"),
				Optional.ofNullable(System.getProperty("java.vm.name"))
					.filter(Predicate.not(String::isBlank))
					.orElse("-"),
				Optional.ofNullable(System.getProperty("java.vm.version"))
					.filter(Predicate.not(String::isBlank))
					.orElse("-"),
				Optional.ofNullable(System.getProperty("neo4j.jdbc.version"))
					.filter(Predicate.not(String::isBlank))
					.orElseGet(ProductVersion::getValue));
	}

	@Override
	public URI getDatabaseURL() {
		return this.databaseUrl;
	}

	@Override
	public Neo4jConnection withTracer(Neo4jTracer tracer) {
		if (tracer == null
				|| this.listeners.stream().anyMatch(l -> l instanceof Tracing t && t.usingSameTracer(tracer))) {
			return this;
		}
		this.addListener(new Tracing(tracer, this));
		return this;
	}

	static boolean forceCypher(String sql) {
		var matcher = PATTERN_ENFORCE_CYPHER.matcher(sql);
		while (matcher.find()) {
			if (matcher.group(1) != null && matcher.group(1).equals(matcher.group(2))) {
				continue;
			}
			return true;
		}
		return false;
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

			Throwable lastException = null;
			String result = null;
			String in = statement;

			for (var translator : this.translators) {
				// Break out early if any of the translators indicates a final Cypher
				// statement
				if (forceCypher(in)) {
					result = in;
					break;
				}
				try {
					result = translator.translate(in, this.metaData);
					// Don't overwrite previous results if the intermediate is null
					if (result != null) {
						in = result;
					}
				}
				catch (IllegalArgumentException ex) {
					if (this.translators.size() == 1) {
						throw ex;
					}
					this.warningSink.accept(new SQLWarning(
							"Translator %s failed to translate `%s`".formatted(translator.getClass().getName(), in),
							ex));
					if (ex.getCause() != null) {
						lastException = ex.getCause();
					}
					else {
						lastException = ex;
					}
				}
			}

			if (result == null) {
				throw new IllegalStateException("No suitable translator for input `%s`".formatted(statement),
						lastException);
			}

			return result;
		}

	}

}
