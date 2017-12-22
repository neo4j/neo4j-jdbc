/*
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
import org.neo4j.jdbc.Neo4jStatement;
import org.neo4j.jdbc.http.driver.Neo4jResponse;

import java.sql.BatchUpdateException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpNeo4jStatement extends Neo4jStatement implements Loggable {

	public HttpNeo4jStatement(HttpNeo4jConnection httpConnection) {
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
		Neo4jResponse response = ((HttpNeo4jConnection) getConnection()).executeQuery(cypher, null, Boolean.TRUE);

		if (response.hasErrors()) {
			throw new SQLException(response.displayErrors());
		}
		
		// Parse stats
		this.currentUpdateCount = response.getFirstResult().getUpdateCount();

		// Parse response data
		boolean hasResultSets = response.hasResultSets();

		this.currentResultSet = hasResultSets ? new HttpNeo4jResultSet(this,response.getFirstResult()) : null;

		return hasResultSets;
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

	@Override public int[] executeBatch() throws SQLException {
		this.checkClosed();

		// execute batch queries
		List<Map<String, Object>> parameters = new ArrayList<>();
		for (int i = 0; i < batchStatements.size(); i++) {
			parameters.add(new HashMap<String, Object>());
		}
		Neo4jResponse response = ((HttpNeo4jConnection) getConnection()).executeQueries(batchStatements, parameters, Boolean.TRUE);

		// proceed the result
		int[] result = new int[response.getResults().size()];
		for (int i = 0; i < response.getResults().size(); i++) {
			result[i] = response.getResults().get(i).getUpdateCount();
		}

		// we check if there is some error into the response => batch exception
		if (response.getErrors() != null && response.getErrors().size() > 0) {
			throw new BatchUpdateException(result, response.getErrors().get(0).getCause());
		}
		// if no exception and we don't have the same cardiniality between queries & result => batch exception
		if (response.getResults().size() != batchStatements.size()) {
			throw new BatchUpdateException("Result size doesn't match queries size", result);
		}

		return result;
	}
}
