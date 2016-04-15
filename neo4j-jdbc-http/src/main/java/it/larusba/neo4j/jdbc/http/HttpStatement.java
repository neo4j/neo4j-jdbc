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

import it.larusba.neo4j.jdbc.Statement;
import it.larusba.neo4j.jdbc.http.driver.Neo4jResponse;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class HttpStatement extends Statement implements Loggable {

	private HttpConnection connection;
	private ResultSet      resultSet;
	private boolean loggable = false;

	public HttpStatement(HttpConnection httpConnection) {
		this.connection = httpConnection;
	}

	@Override public ResultSet executeQuery(String cypher) throws SQLException {
		checkClosed();
		if (connection.isClosed()) {
			throw new SQLException("Connection already closed");
		}

		Neo4jResponse response = connection.executeQuery(cypher, null, null);
		this.resultSet = new HttpResultSet(response.results.get(0));
		return resultSet;
	}

	@Override public int executeUpdate(String cypher) throws SQLException {
		checkClosed();
		if (connection.isClosed()) {
			throw new SQLException("Connection already closed");
		}

		Neo4jResponse response = connection.executeQuery(cypher, null, Boolean.TRUE);
		Map<String, Object> stats = response.results.get(0).stats;
		int result = (int) stats.get("nodes_created");
		result += (int) stats.get("nodes_deleted");
		result += (int) stats.get("relationships_created");
		result += (int) stats.get("relationship_deleted");
		return result;
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

	@Override public void close() throws SQLException {
		if (resultSet != null) {
			resultSet.close();
		}
		connection = null;
		resultSet = null;
	}

	@Override public int getMaxRows() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public void setMaxRows(int max) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean execute(String sql) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public ResultSet getResultSet() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
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
