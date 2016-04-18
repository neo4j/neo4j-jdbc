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
 * Created on 15/4/2016
 */
package it.larusba.neo4j.jdbc.http;

import it.larusba.neo4j.jdbc.Loggable;
import it.larusba.neo4j.jdbc.ParameterMetaData;
import it.larusba.neo4j.jdbc.PreparedStatement;
import it.larusba.neo4j.jdbc.ResultSetMetaData;
import it.larusba.neo4j.jdbc.http.driver.Neo4jResponse;
import it.larusba.neo4j.jdbc.utils.PreparedStatementBuilder;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.HashMap;

import static java.sql.Types.*;

public class HttpPreparedStatement extends PreparedStatement implements Loggable {

	private HttpConnection          connection;
	private ResultSet               resultSet;
	private String                  cypher;
	private HashMap<String, Object> parameters;
	private int                     parametersNumber;
	private boolean loggable = false;

	/**
	 * Default constructor.
	 *
	 * @param httpConnection The Neo4j http connection.
	 * @param cypher         The prepared cypher query
	 */
	public HttpPreparedStatement(HttpConnection httpConnection, String cypher) {
		super();
		this.connection = httpConnection;
		this.cypher = PreparedStatementBuilder.replacePlaceholders(cypher);
		this.parametersNumber = PreparedStatementBuilder.placeholdersCount(cypher);
		this.parameters = new HashMap<>(this.parametersNumber);
	}

	/**
	 * Check if this statement is closed or not.
	 *
	 * @throws SQLException
	 */
	private void checkClosed() throws SQLException {
		if (this.isClosed()) {
			throw new SQLException("Statement already closed");
		}
	}

	private void checkParamsNumber(int parameterIndex) throws SQLException {
		if (parameterIndex > this.parametersNumber) {
			throw new SQLException("ParameterIndex does not correspond to a parameter marker in the SQL statement");
		}
	}

	private void insertParameter(int index, Object o) {
		this.parameters.put(new Integer(index).toString(), o);
	}

	@Override public ResultSet executeQuery() throws SQLException {
		checkClosed();
		if (connection.isClosed()) {
			throw new SQLException("Connection already closed");
		}

		Neo4jResponse response = connection.executeQuery(cypher, parameters, Boolean.FALSE);
		this.resultSet = new HttpResultSet(response.results.get(0));
		return resultSet;
	}

	@Override public int executeUpdate() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public void setNull(int parameterIndex, int sqlType) throws SQLException {
		this.checkClosed();
		this.checkParamsNumber(parameterIndex);
		//@formatter:off
        if(	sqlType == ARRAY ||
                sqlType == BLOB ||
                sqlType == CLOB ||
                sqlType == DATALINK ||
                sqlType == JAVA_OBJECT ||
                sqlType == NCHAR ||
                sqlType == NCLOB ||
                sqlType == NVARCHAR ||
                sqlType == LONGNVARCHAR ||
                sqlType == REF ||
                sqlType == ROWID ||
                sqlType == SQLXML ||
                sqlType == STRUCT){
            //@formatter:on
			throw new SQLFeatureNotSupportedException("The Type you specified is not supported");
		}
		this.insertParameter(parameterIndex, null);
	}

	@Override public void setBoolean(int parameterIndex, boolean x) throws SQLException {
		this.checkClosed();
		this.checkParamsNumber(parameterIndex);
		this.insertParameter(parameterIndex, x);
	}

	@Override public void setShort(int parameterIndex, short x) throws SQLException {
		this.checkClosed();
		this.checkParamsNumber(parameterIndex);
		this.insertParameter(parameterIndex, x);
	}

	@Override public void setInt(int parameterIndex, int x) throws SQLException {
		this.checkClosed();
		this.checkParamsNumber(parameterIndex);
		this.insertParameter(parameterIndex, x);
	}

	@Override public void setLong(int parameterIndex, long x) throws SQLException {
		this.checkClosed();
		this.checkParamsNumber(parameterIndex);
		this.insertParameter(parameterIndex, x);
	}

	@Override public void setFloat(int parameterIndex, float x) throws SQLException {
		this.checkClosed();
		this.checkParamsNumber(parameterIndex);
		this.insertParameter(parameterIndex, x);
	}

	@Override public void setDouble(int parameterIndex, double x) throws SQLException {
		this.checkClosed();
		this.checkParamsNumber(parameterIndex);
		this.insertParameter(parameterIndex, x);
	}

	@Override public void setString(int parameterIndex, String x) throws SQLException {
		this.checkClosed();
		this.checkParamsNumber(parameterIndex);
		this.insertParameter(parameterIndex, x);
	}

	@Override public void clearParameters() throws SQLException {
		this.checkClosed();
		this.parameters.clear();
	}

	@Override public boolean execute() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public ResultSetMetaData getMetaData() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public ParameterMetaData getParameterMetaData() throws SQLException {
		this.checkClosed();
		ParameterMetaData pmd = new HttpParameterMetaData(this);
		return pmd;
	}

	@Override public void close() throws SQLException {
		if (resultSet != null) {
			resultSet.close();
		}
		connection = null;
		resultSet = null;
	}

	@Override public ResultSet getResultSet() throws SQLException {
		checkClosed();
		return this.resultSet;
	}

	@Override public int getUpdateCount() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public int getResultSetConcurrency() throws SQLException {
		return ResultSet.CONCUR_READ_ONLY;
	}

	@Override public int getResultSetType() throws SQLException {
		return ResultSet.TYPE_FORWARD_ONLY;
	}

	@Override public Connection getConnection() throws SQLException {
		return this.connection;
	}

	@Override public int getResultSetHoldability() throws SQLException {
		return ResultSet.CLOSE_CURSORS_AT_COMMIT;
	}

	@Override public boolean isClosed() throws SQLException {
		return connection == null;
	}

	@Override public boolean isLoggable() {
		return loggable;
	}

	@Override public void setLoggable(boolean loggable) {
		this.loggable = loggable;
	}
}
