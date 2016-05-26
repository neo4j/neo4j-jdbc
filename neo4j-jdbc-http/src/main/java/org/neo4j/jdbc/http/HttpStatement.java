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
package org.neo4j.jdbc.http;

import org.neo4j.jdbc.Loggable;
import org.neo4j.jdbc.Statement;
import org.neo4j.jdbc.http.driver.Neo4jResponse;

import java.sql.BatchUpdateException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpStatement extends Statement implements Loggable {

	private List<String> batchStatements;
	private boolean loggable = false;

	public HttpStatement(HttpConnection httpConnection) {
		super(httpConnection);
		batchStatements = new ArrayList<>();
	}

	@Override public ResultSet executeQuery(String cypher) throws SQLException {
		this.execute(cypher);
		return currentResultSet;
	}

	@Override public int executeUpdate(String cypher) throws SQLException {
		this.execute(cypher);
		return currentUpdateCount;
	}

	@Override public boolean execute(String cypher) throws SQLException {
		checkClosed();

		// execute the query
		Neo4jResponse response = ((HttpConnection) getConnection()).executeQuery(cypher, null, Boolean.TRUE);

		// Parse stats
		this.currentUpdateCount = 0;
		this.currentUpdateCount = ((HttpConnection) getConnection()).computeResultUpdateCount(response.results.get(0));

		// Parse response data
		this.currentResultSet = null;
		if (response.results.get(0) != null) {
			this.currentResultSet = new HttpResultSet(response.results.get(0));
		}

		return (this.currentResultSet != null);
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

	/*-------------------*/
	/*       Batch       */
	/*-------------------*/

	@Override public void addBatch(String sql) throws SQLException {
		this.checkClosed();
		this.batchStatements.add(sql);
	}

	@Override public void clearBatch() throws SQLException {
		this.checkClosed();
		this.batchStatements.clear();
	}

	@Override public int[] executeBatch() throws SQLException {
		this.checkClosed();

		// execute batch queries
		List<Map<String, Object>> parameters = new ArrayList<>();
		for (int i = 0; i < batchStatements.size(); i++) {
			parameters.add(new HashMap());
		}
		Neo4jResponse response = ((HttpConnection) getConnection()).executeQueries(batchStatements, parameters, Boolean.TRUE);

		// proceed the result
		int[] result = new int[response.results.size()];
		for (int i = 0; i < response.results.size(); i++) {
			result[i] = ((HttpConnection) getConnection()).computeResultUpdateCount(response.results.get(i));
		}

		// we check if there is some error into the response => batch exception
		if (response.errors != null && response.errors.size() > 0) {
			throw new BatchUpdateException(result, response.errors.get(0).getCause());
		}
		// if no exception and we don't have the same cardiniality between queries & result => batch exception
		if (response.results.size() != batchStatements.size()) {
			throw new BatchUpdateException("Result size doesn't match queries size", result);
		}

		return result;
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
