/**
 * Copyright (c) 2016 LARUS Business Automation [http://www.larus-ba.it]
 * <p>
 * This file is part of the "LARUS Integration Framework for Neo4j".
 * <p>
 * The "LARUS Integration Framework for Neo4j" is licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * Created on 27/12/17
 */
package org.neo4j.jdbc.impl;

import org.neo4j.jdbc.Neo4jArray;
import org.neo4j.jdbc.Neo4jConnection;
import org.neo4j.jdbc.Neo4jResultSet;
import org.neo4j.jdbc.utils.ExceptionBuilder;

import java.sql.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * @author Gianmarco Laggia @ Larus B.A.
 * @since 3.2.0
 */
public abstract class Neo4jConnectionImpl implements Neo4jConnection {
	/**
	 * JDBC Url used for this connection
	 */
	private final String url;

	/**
	 * JDBC driver properties
	 */
	private final Properties properties;

    /**
     * Client info properties
     */
    private Properties clientInfo;

    /**
	 * Is the connection is in readonly mode ?
	 */
	private boolean readOnly = false;

	/**
	 * Holdability of the connection
	 */
	private int holdability;

	protected static final String FASTEST_STATEMENT = "RETURN 1";

	/**
	 * Default constructor with properties.
	 *
	 * @param properties driver properties
	 * @param url connection url
	 * @param defaultHoldability connection holdability
	 */
	protected Neo4jConnectionImpl(Properties properties, String url, int defaultHoldability) {
		this.url = url;
		this.properties = properties;
		this.holdability = defaultHoldability;
	}

	public static boolean hasDebug(Properties properties) {
		return "true".equalsIgnoreCase(properties.getProperty("debug", "false"));
	}

	/**
	 * Get the connection url.
	 *
	 * @return String the connection url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Get the properties for this connection.
	 *
	 * @return Properties the properties for this connection
	 */
	public Properties getProperties() {
		return properties;
	}

	/**
	 * Get the user of this connection.
	 *
	 * @return String
	 */
	public String getUserName() {
		return properties.getProperty("user");
	}

	/*---------------------------------------*/
	/*       Some useful check method        */
	/*---------------------------------------*/

	/**
	 * Check if this connection is closed or not.
	 * If it's closed, then we throw a SQLException, otherwise we do nothing.
	 * @throws SQLException sqlexception
	 */
	protected void checkClosed() throws SQLException {
		if (this.isClosed()) {
			throw new SQLException("Connection already closed");
		}
	}

	/**
	 * Method to check if we are into autocommit mode.
	 * If we do, then it throw an exception.
	 * This method is for using into commit and rollback method.
	 * @throws SQLException sqlexception
	 */
	protected void checkAutoCommit() throws SQLException {
		if (this.getAutoCommit()) {
			throw new SQLException("Cannot commit when in autocommit");
		}
	}

	/**
	 * Check if the holdability parameter conform to specification.
	 * If it doesn't, we throw an exception.
	 * {@link java.sql.Connection#setHoldability(int)}
	 *
	 * @param resultSetHoldability The holdability value to check
	 * @throws SQLException sqlexception
	 */
	protected void checkHoldabilityParams(int resultSetHoldability) throws SQLException {
		// @formatter:off
		if( resultSetHoldability != Neo4jResultSet.HOLD_CURSORS_OVER_COMMIT &&
			resultSetHoldability != Neo4jResultSet.CLOSE_CURSORS_AT_COMMIT
		){
			throw new SQLFeatureNotSupportedException();
		}
		// @formatter:on
	}

	/**
	 * Check if the concurrency parameter conform to specification.
	 * If it doesn't, we throw an exception.
	 *
	 * @param resultSetConcurrency The concurrency value to check
	 * @throws SQLException sqlexception
	 */
	protected void checkConcurrencyParams(int resultSetConcurrency) throws SQLException {
		// @formatter:off
		if( resultSetConcurrency != Neo4jResultSet.CONCUR_UPDATABLE &&
			resultSetConcurrency != Neo4jResultSet.CONCUR_READ_ONLY
		){
			throw new SQLFeatureNotSupportedException();
		}
		// @formatter:on
	}

	/**
	 * Check if the resultset type parameter conform to specification.
	 * If it doesn't, we throw an exception.
	 *
	 * @param resultSetType The concurrency value to check
	 * @throws SQLException sqlexception
	 */
	protected void checkTypeParams(int resultSetType) throws SQLException {
		// @formatter:off
		if( resultSetType != Neo4jResultSet.TYPE_FORWARD_ONLY &&
			resultSetType != Neo4jResultSet.TYPE_SCROLL_INSENSITIVE &&
			resultSetType != Neo4jResultSet.TYPE_SCROLL_SENSITIVE
		){
			throw new SQLFeatureNotSupportedException();
		}
		// @formatter:on
	}

	/**
	 * Check if the transaction isolation level parameter conform to specification.
	 * If it doesn't, we throw an exception.
	 *
	 * @param level The transaction isolation level value to check
	 * @throws SQLException sqlexception
	 */
	protected void checkTransactionIsolation(int level) throws SQLException {
		// @formatter:off
		int[] invalid = {
			TRANSACTION_NONE,
			TRANSACTION_READ_COMMITTED,
			TRANSACTION_READ_UNCOMMITTED,
			TRANSACTION_REPEATABLE_READ,
			TRANSACTION_SERIALIZABLE
		};

		if(!Arrays.asList(invalid).contains(level)){
			throw new SQLException();
		}
		// @formatter:on
	}

	/**
	 * Check if the auto generated keys parameter conform to specification.
	 * If it doesn't, we throw an exception.
	 *
	 * @param autoGeneratedKeys the auto generated keys value to check
	 * @throws SQLException sqlexception
	 */
	private void checkAutoGeneratedKeys(int autoGeneratedKeys) throws SQLException {
		// @formatter:off
		if( autoGeneratedKeys != Statement.RETURN_GENERATED_KEYS &&
				autoGeneratedKeys != Statement.NO_GENERATED_KEYS
				){
			throw new SQLException();
		}
		// @formatter:on
	}

	/*------------------------------------*/
	/*       Default implementation       */
	/*------------------------------------*/

	@Override public void setReadOnly(boolean readOnly) throws SQLException {
		this.checkClosed();
		this.doSetReadOnly(readOnly);
	}

	@Override public boolean isReadOnly() throws SQLException {
		this.checkClosed();
		return this.readOnly;
	}

    protected void doSetReadOnly(boolean readOnly) throws SQLException {
        this.readOnly = readOnly;
    }

    protected boolean getReadOnly() throws SQLException {
        return this.readOnly;
    }

	@Override public void setHoldability(int holdability) throws SQLException {
		this.checkClosed();
		this.checkHoldabilityParams(holdability);
		this.holdability = holdability;
	}

	@Override public int getHoldability() throws SQLException {
		this.checkClosed();
		return this.holdability;
	}

	/**
	 * Default implementation of setCatalog.
	 * Neo4j doesn't implement catalog feature, so we do nothing to avoid some tools exception.
	 */
	@Override public void setCatalog(String catalog) throws SQLException {
		this.checkClosed();
		return;
	}

	/**
	 * Default implementation of getCatalog.
	 * Neo4j doesn't implement catalog feature, so return <code>null</code> (@see {@link java.sql.Connection#getCatalog})
	 */
	@Override public String getCatalog() throws SQLException {
		this.checkClosed();
		return null;
	}

	/**
	 * Default implementation of getTransactionIsolation.
	 */
	@Override public int getTransactionIsolation() throws SQLException {
		this.checkClosed();
		return TRANSACTION_READ_COMMITTED;
	}

	/**
	 * Default implementation of setTransactionIsolation.
	 */
	@Override public void setTransactionIsolation(int level) throws SQLException {
		this.checkClosed();
		this.checkTransactionIsolation(level);
		if (level != TRANSACTION_READ_COMMITTED) {
			throw new SQLException("Unsupported isolation level");
		}
	}

	/**
	 * Default implementation of preparedStatement(String, int).
	 * We're just ignoring the autoGeneratedKeys param.
	 */
	@Override public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
		this.checkAutoGeneratedKeys(autoGeneratedKeys);
		return prepareStatement(sql);
	}

	/**
	 * Default implementation of nativeSQL.
	 * Here we should implement some hacks for JDBC tools if needed.
	 * This method must be used before running a query.
	 */
	@Override public String nativeSQL(String sql) throws SQLException {
		return sql;
	}

	@Override public <T> T unwrap(Class<T> iface) throws SQLException {
		return org.neo4j.jdbc.Wrapper.unwrap(iface, this);
	}

	@Override public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return org.neo4j.jdbc.Wrapper.isWrapperFor(iface, this.getClass());
	}

	@Override public SQLWarning getWarnings() throws SQLException {
		checkClosed();
		return null;
	}

	@Override public void clearWarnings() throws SQLException {
		checkClosed();
	}

	@Override public String getSchema() throws SQLException {
		checkClosed();
		return null;
	}

	/*---------------------------------*/
	/*       Not implemented yet       */
	/*---------------------------------*/

	@Override public java.sql.CallableStatement prepareCall(String sql) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public java.sql.CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public Map<String, Class<?>> getTypeMap() throws SQLException {
		return Collections.emptyMap();
	}

	@Override public void setTypeMap(Map<String, Class<?>> map) throws SQLException {} // do nothing

	@Override public Savepoint setSavepoint() throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public Savepoint setSavepoint(String name) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void rollback(Savepoint savepoint) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void releaseSavepoint(Savepoint savepoint) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public Clob createClob() throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public Blob createBlob() throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public NClob createNClob() throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public SQLXML createSQLXML() throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setClientInfo(String name, String value) throws SQLClientInfoException {
        if (name != null && value != null) {
            if (this.clientInfo == null) {
                this.clientInfo = new Properties();
            }
            this.clientInfo.setProperty(name, value);
        }
	}

	@Override public void setClientInfo(Properties properties) throws SQLClientInfoException {
		this.clientInfo = properties;
	}

	@Override public String getClientInfo(String name) throws SQLException {
		return (this.clientInfo != null) ? this.clientInfo.getProperty(name) : null;
	}

	@Override public Properties getClientInfo() throws SQLException {
		return this.clientInfo;
	}

	@Override public Neo4jArray createArrayOf(String typeName, Object[] elements) throws SQLException {
		return new ListArray(typeName, elements);
	}

	@Override public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setSchema(String schema) throws SQLException {} // do nothing

	@Override public void abort(Executor executor) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public int getNetworkTimeout() throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

}
