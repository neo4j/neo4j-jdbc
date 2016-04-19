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

import it.larusba.neo4j.jdbc.*;
import it.larusba.neo4j.jdbc.http.driver.Neo4jResponse;
import it.larusba.neo4j.jdbc.utils.PreparedStatementBuilder;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.HashMap;
import java.util.Map;

import static java.sql.Types.*;

public class HttpPreparedStatement extends PreparedStatement implements Loggable {

	private boolean loggable = false;

	/**
	 * Default constructor.
	 *
	 * @param httpConnection The Neo4j http connection.
	 * @param cypher         The prepared cypher query
	 */
	public HttpPreparedStatement(HttpConnection httpConnection, String cypher) {
		super(httpConnection, cypher);
	}

	@Override public ResultSet executeQuery() throws SQLException {
		checkClosed();
		if (connection.isClosed()) {
			throw new SQLException("Connection already closed");
		}

		Neo4jResponse response = ((HttpConnection) getConnection()).executeQuery(statement, parameters, Boolean.FALSE);
		this.currentResultSet = new HttpResultSet(response.results.get(0));
		return currentResultSet;
	}

	@Override public int executeUpdate() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}


	@Override public boolean execute() throws SQLException {
		checkClosed();

		// execute the query
		Neo4jResponse response = ((HttpConnection) getConnection()).executeQuery(this.statement, this.parameters, Boolean.TRUE);

		// Parse stats
		this.currentUpdateCount = 0;
		if (response.results.get(0) != null && response.results.get(0).stats != null) {
			Map<String, Object> stats = response.results.get(0).stats;
			int updated = (int) stats.get("nodes_created");
			updated += (int) stats.get("nodes_deleted");
			updated += (int) stats.get("relationships_created");
			updated += (int) stats.get("relationship_deleted");
			this.currentUpdateCount = updated;
		}

		// Parse response data
		this.currentResultSet = null;
		if (response.results.get(0) != null) {
			this.currentResultSet = new HttpResultSet(response.results.get(0));
		}

		return (this.currentResultSet != null);
	}

	@Override public ResultSetMetaData getMetaData() throws SQLException {
		return InstanceFactory.debug(HttpResultSetMetaData.class,
				new HttpResultSetMetaData(((HttpResultSet) this.currentResultSet).result),
				this.isLoggable());
	}

	@Override public ParameterMetaData getParameterMetaData() throws SQLException {
		this.checkClosed();
		ParameterMetaData pmd = new HttpParameterMetaData(this);
		return pmd;
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

	/*--------------------*/
	/*       Logger       */
	/*--------------------*/

	@Override public boolean isLoggable() {
		return loggable;
	}

	@Override public void setLoggable(boolean loggable) {
		this.loggable = loggable;
	}
}
