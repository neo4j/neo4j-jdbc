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
package org.neo4j.driver.jdbc;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.neo4j.driver.jdbc.internal.bolt.BoltConnection;
import org.neo4j.driver.jdbc.translator.spi.SqlTranslator;

/**
 * A Neo4j specific implementation of {@link Connection}.
 *
 * @author Michael J. Simons
 * @since 1.0.0
 */
final class ConnectionImpl implements Neo4jConnection {

	private final BoltConnection boltConnection;

	private final Lazy<SqlTranslator> sqlTranslator;

	private boolean autoCommit = true;

	private final boolean automaticSqlTranslation;

	private boolean readOnly;

	private Statement statement;

	private boolean transactionOpen;

	private boolean closed;

	ConnectionImpl(BoltConnection boltConnection) {
		this(boltConnection, () -> {
			throw new UnsupportedOperationException("No SQL translator available");
		}, false);
	}

	ConnectionImpl(BoltConnection boltConnection, Supplier<SqlTranslator> sqlTranslator,
			boolean automaticSqlTranslation) {
		this.boltConnection = Objects.requireNonNull(boltConnection);
		this.sqlTranslator = Lazy.of(sqlTranslator);
		this.automaticSqlTranslation = automaticSqlTranslation;
	}

	UnaryOperator<String> getSqlProcessor() {
		return this.automaticSqlTranslation ? this.sqlTranslator.resolve()::translate : null;
	}

	UnaryOperator<Integer> getIndexProcessor() {
		return this.automaticSqlTranslation ? idx -> idx - 1 : null;
	}

	@Override
	public Statement createStatement() throws SQLException {
		if (this.closed) {
			throw new SQLException("The connection is closed");
		}
		if (this.statement != null) {
			this.statement.close();
		}
		this.transactionOpen = true;
		this.statement = new StatementImpl(this, this.boltConnection, this.autoCommit, getSqlProcessor());
		return this.statement;
	}

	@Override
	public PreparedStatement prepareStatement(String sql) throws SQLException {
		if (this.closed) {
			throw new SQLException("The connection is closed");
		}
		if (this.statement != null) {
			this.statement.close();
		}
		this.transactionOpen = true;
		var statement = new PreparedStatementImpl(this, this.boltConnection, this.autoCommit, getSqlProcessor(),
				getIndexProcessor(), sql);
		this.statement = statement;
		return statement;
	}

	@Override
	public CallableStatement prepareCall(String sql) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String nativeSQL(String sql) {
		return this.sqlTranslator.resolve().translate(sql);
	}

	@Override
	public void setAutoCommit(boolean autoCommit) throws SQLException {
		this.autoCommit = autoCommit;
	}

	@Override
	public boolean getAutoCommit() throws SQLException {
		return this.autoCommit;
	}

	@Override
	public void commit() throws SQLException {
		if (this.autoCommit) {
			return;
		}
		if (this.statement != null) {
			this.statement.close();
		}
		if (this.transactionOpen) {
			this.boltConnection.commit().toCompletableFuture().join();
			this.transactionOpen = false;
		}
	}

	@Override
	public void rollback() throws SQLException {
		if (this.autoCommit) {
			return;
		}
		if (this.statement != null) {
			this.statement.close();
		}
		if (this.transactionOpen) {
			this.boltConnection.rollback().toCompletableFuture().join();
			this.transactionOpen = false;
		}
	}

	@Override
	public void close() throws SQLException {
		this.boltConnection.close().toCompletableFuture().join();
		this.closed = true;
	}

	@Override
	public boolean isClosed() throws SQLException {
		return this.closed;
	}

	@Override
	public DatabaseMetaData getMetaData() {
		return new DatabaseMetadataImpl(this.boltConnection, this.automaticSqlTranslation);
	}

	@Override
	public void setReadOnly(boolean readOnly) throws SQLException {
		this.readOnly = readOnly;
	}

	@Override
	public boolean isReadOnly() throws SQLException {
		return this.readOnly;
	}

	@Override
	public void setCatalog(String catalog) throws SQLException {
		// cannot have catalog.
	}

	@Override
	public String getCatalog() throws SQLException {
		return null;
	}

	@Override
	public void setTransactionIsolation(int level) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getTransactionIsolation() {
		return Connection.TRANSACTION_READ_COMMITTED;
	}

	@Override
	public SQLWarning getWarnings() {
		return null;
	}

	@Override
	public void clearWarnings() {

	}

	@Override
	public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
			throws SQLException {
		if (resultSetType != ResultSet.TYPE_FORWARD_ONLY) {
			throw new UnsupportedOperationException("Unsupported result set type: " + resultSetType);
		}
		if (resultSetConcurrency != ResultSet.CONCUR_READ_ONLY) {
			throw new UnsupportedOperationException("Unsupported result set concurrency: " + resultSetConcurrency);
		}
		return prepareStatement(sql);
	}

	@Override
	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
		throw new SQLFeatureNotSupportedException();
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
		return ResultSet.CLOSE_CURSORS_AT_COMMIT;
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
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException {
		throw new SQLFeatureNotSupportedException();
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
	public boolean isValid(int timeout) {
		return true;
	}

	@Override
	public void setClientInfo(String name, String value) {
		// Do nothing for now but don't break
	}

	@Override
	public void setClientInfo(Properties properties) {
		// Do nothing for now but don't break
	}

	@Override
	public String getClientInfo(String name) {
		return null; // Do nothing for now but don't break
	}

	@Override
	public Properties getClientInfo() {
		return new Properties(); // Do nothing for now but don't break
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
		// not supported
	}

	@Override
	public String getSchema() throws SQLException {
		return "public";
	}

	@Override
	public void abort(Executor executor) throws SQLException {
		throw new UnsupportedOperationException();
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
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		throw new UnsupportedOperationException();
	}

}
