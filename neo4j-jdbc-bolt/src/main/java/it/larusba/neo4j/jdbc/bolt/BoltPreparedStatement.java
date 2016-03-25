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
 * Created on 23/03/16
 */
package it.larusba.neo4j.jdbc.bolt;

import it.larusba.neo4j.jdbc.ParameterMetaData;
import it.larusba.neo4j.jdbc.PreparedStatement;
import it.larusba.neo4j.jdbc.utils.PreparedStatementBuilder;
import org.neo4j.driver.v1.Transaction;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.HashMap;

import static java.sql.Types.*;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltPreparedStatement extends PreparedStatement implements Loggable {

	private BoltConnection connection;
	private Transaction    transaction;
	private ResultSet      currentResultSet;
	private boolean        closed;

	private boolean loggable = false;

	private String                  statement;
	private HashMap<String, Object> parameters;
	int parametersNumber;

	public BoltPreparedStatement(BoltConnection connection, String rawStatement) {
		this.connection = connection;
		this.transaction = connection.getTransaction();
		this.currentResultSet = null;
		this.closed = false;

		this.statement = PreparedStatementBuilder.replacePlaceholders(rawStatement);
		this.parametersNumber = PreparedStatementBuilder.placeholdersCount(rawStatement);
		this.parameters = new HashMap<>(this.parametersNumber);
	}

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

	@Override public ParameterMetaData getParameterMetaData() throws SQLException {
		this.checkClosed();
		ParameterMetaData pmd = new BoltParameterMetaData(this);
		return pmd;
	}

	@Override public void close() throws SQLException {
		if (this.closed) {
			return;
		}
		if (this.currentResultSet != null) {
			this.currentResultSet.close();
		}
		if (this.transaction != null) {
			this.transaction.failure();
			this.transaction.close();
		}
		this.closed = true;
	}

	@Override public boolean isClosed() throws SQLException {
		return this.closed;
	}

	@Override public boolean isLoggable() {
		return this.loggable;
	}

	@Override public void setLoggable(boolean loggable) {
		this.loggable = loggable;
	}
}
