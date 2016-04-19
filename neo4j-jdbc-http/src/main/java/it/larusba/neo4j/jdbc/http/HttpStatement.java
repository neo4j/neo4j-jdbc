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
import it.larusba.neo4j.jdbc.Statement;
import it.larusba.neo4j.jdbc.http.driver.Neo4jResponse;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class HttpStatement extends Statement implements Loggable {

	private boolean loggable = false;

	public HttpStatement(HttpConnection httpConnection) {
		super(httpConnection);
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
