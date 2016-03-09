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
 * Created on 03/02/16
 */
package it.neo4j.jdbc;

import java.sql.*;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public abstract class Connection implements java.sql.Connection {

	@Override public abstract Statement createStatement() throws SQLException;

	@Override public PreparedStatement prepareStatement(String sql) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public java.sql.CallableStatement prepareCall(String sql) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public String nativeSQL(String sql) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public abstract void setAutoCommit(boolean autoCommit) throws SQLException;

	@Override public abstract boolean getAutoCommit() throws SQLException;

	@Override abstract public void commit() throws SQLException;

	@Override abstract public void rollback() throws SQLException;

	@Override public abstract void close() throws SQLException;

	@Override public abstract boolean isClosed() throws SQLException;

	@Override public DatabaseMetaData getMetaData() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public abstract void setReadOnly(boolean readOnly) throws SQLException;

	@Override public abstract boolean isReadOnly() throws SQLException;

	@Override public void setCatalog(String catalog) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public String getCatalog() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public void setTransactionIsolation(int level) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override abstract public int getTransactionIsolation() throws SQLException;

	@Override public SQLWarning getWarnings() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public void clearWarnings() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public abstract Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException;

	@Override public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public java.sql.CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public Map<String, Class<?>> getTypeMap() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public void setHoldability(int holdability) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public int getHoldability() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public Savepoint setSavepoint() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public Savepoint setSavepoint(String name) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public void rollback(Savepoint savepoint) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public void releaseSavepoint(Savepoint savepoint) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public abstract Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException;

	@Override public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public Clob createClob() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public Blob createBlob() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public NClob createNClob() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public SQLXML createSQLXML() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean isValid(int timeout) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public void setClientInfo(String name, String value) throws SQLClientInfoException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public void setClientInfo(Properties properties) throws SQLClientInfoException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public String getClientInfo(String name) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public Properties getClientInfo() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public void setSchema(String schema) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public String getSchema() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public void abort(Executor executor) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public int getNetworkTimeout() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public <T> T unwrap(Class<T> iface) throws SQLException {
		return Wrapper.unwrap(iface, this);
	}

	@Override public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return Wrapper.isWrapperFor(iface, this.getClass());
	}
}
