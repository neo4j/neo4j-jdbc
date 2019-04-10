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

import org.neo4j.jdbc.*;
import org.neo4j.jdbc.http.driver.Neo4jResponse;

import java.sql.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.function.Function;

public class HttpNeo4jPreparedStatement extends Neo4jPreparedStatement implements Loggable {

	/**
	 * Default constructor.
	 *
	 * @param httpConnection The Neo4j http connection.
	 * @param cypher         The prepared cypher query
	 */
	public HttpNeo4jPreparedStatement(HttpNeo4jConnection httpConnection, String cypher) {
		super(httpConnection, cypher);
	}

	@Override public ResultSet executeQuery() throws SQLException {
		checkClosed();
		this.execute();
		return currentResultSet;
	}

	@Override public int executeUpdate() throws SQLException {
		checkClosed();
		this.execute();
		return currentUpdateCount;
	}

	@Override public boolean execute() throws SQLException {
		checkClosed();

		// execute the statement
		Neo4jResponse response = ((HttpNeo4jConnection) getConnection()).executeQuery(this.statement, this.parameters, Boolean.TRUE);

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

	@Override public Neo4jResultSetMetaData getMetaData() throws SQLException {
		return new HttpNeo4jResultSetMetaData(((HttpNeo4jResultSet) this.currentResultSet).result);
	}

	@Override public Neo4jParameterMetaData getParameterMetaData() throws SQLException {
		this.checkClosed();
		return new HttpNeo4jParameterMetaData(this);
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

		List<String> queries = new ArrayList<>();
		for (int i = 0; i < batchParameters.size(); i++) {
			queries.add(this.statement);
		}
		// execute batch queries
		Neo4jResponse response = ((HttpNeo4jConnection) getConnection()).executeQueries(queries, batchParameters, Boolean.TRUE);

		// proceed the result
		int[] result = new int[response.getResults().size()];
		for (int i = 0; i < response.getResults().size(); i++) {
			result[i] = response.getResults().get(i).getUpdateCount();
		}

		// we check if there is some error into the response => batch exception
		if (response.getErrors() != null && response.getErrors().size() > 0) {
			throw new BatchUpdateException(result, response.getErrors().get(0).getCause());
		}
		// if no exception and we don't have the same cardinality between queries & result => batch exception
		if (response.getResults().size() != batchParameters.size()) {
			throw new BatchUpdateException("Result size doesn't match queries size", result);
		}

		return result;
	}

	/*-------------------*/
	/*   setParameter    */
	/*-------------------*/

	protected void setTemporal(int parameterIndex, long epoch, ZoneId zone, Function<ZonedDateTime, Temporal> extractTemporal) throws SQLException {
		checkClosed();
		checkParamsNumber(parameterIndex);

		ZonedDateTime zdt = Instant.ofEpochMilli(epoch).atZone(zone);

		insertParameter(parameterIndex, extractTemporal.apply(zdt));
	}

	@Override
	public void setDate(int parameterIndex, Date x) throws SQLException {
		setTemporal(parameterIndex, x.getTime(), ZoneId.systemDefault(), (zdt)-> zdt.toLocalDate());
	}

	@Override
	public void setArray(int parameterIndex, Array x) throws SQLException {
		checkClosed();
		insertParameter(parameterIndex, x.getArray());
	}
}
