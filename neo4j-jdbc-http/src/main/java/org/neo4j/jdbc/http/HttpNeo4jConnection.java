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
import org.neo4j.jdbc.http.driver.CypherExecutor;
import org.neo4j.jdbc.http.driver.Neo4jResponse;
import org.neo4j.jdbc.http.driver.Neo4jStatement;
import org.neo4j.jdbc.impl.Neo4jConnectionImpl;
import org.neo4j.jdbc.utils.ExceptionBuilder;
import org.neo4j.jdbc.utils.Neo4jJdbcRuntimeException;
import org.neo4j.jdbc.utils.TimeLimitedCodeBlock;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class HttpNeo4jConnection extends Neo4jConnectionImpl {

	CypherExecutor executor;
	private boolean isClosed = false;

	/**
	 * Default constructor.
	 *
	 * @param host       Hostname of the Neo4j instance.
	 * @param port       HTTP port of the Neo4j instance.
	 * @param secure     Secure
	 * @param properties Properties of the url connection.
	 * @param url        Url
	 * @throws SQLException sqlexption
	 */
	public HttpNeo4jConnection(String host, Integer port, Boolean secure, Properties properties, String url) throws SQLException {
		super(properties, url, Neo4jResultSet.CLOSE_CURSORS_AT_COMMIT);
		this.executor = new CypherExecutor(host, port, secure, properties);
	}

	/**
	 * Execute a cypher query.
	 *
	 * @param queries    List of cypher queries
	 * @param parameters Parameter of the cypher queries (match by index)
	 * @param stats      Do we need to include stats ?
	 * @return ...
	 * @throws SQLException sqlexception
	 */
	public Neo4jResponse executeQueries(final List<String> queries, List<Map<String, Object>> parameters, Boolean stats) throws SQLException {
		checkClosed();

		if(queries.size() != parameters.size()) {
			throw new SQLException("Query and parameter list haven't the same cardinality");
		}

		List<Neo4jStatement> neo4jStatements = new ArrayList<>();
		for(int i=0;i<queries.size();i++) {
			String query = queries.get(i);
			Map<String, Object> params = parameters.get(i);

			checkReadOnly(query);
			neo4jStatements.add(new Neo4jStatement(query, params, stats));
		}

		return executor.executeQueries(neo4jStatements);
	}

	/**
	 * Execute a cypher query.
	 *
	 * @param query      Cypher query
	 * @param parameters Parameter of the cypher query
	 * @param stats      Do we need to include stats ?
	 * @return ...
	 * @throws SQLException sqlexception
	 */
	public Neo4jResponse executeQuery(final String query, Map<String, Object> parameters, Boolean stats) throws SQLException {
		checkClosed();
		checkReadOnly(query);
		return executor.executeQuery(new Neo4jStatement(query, parameters, stats));
	}

	@Override public Neo4jDatabaseMetaData getMetaData() throws SQLException {
		return new HttpNeo4jDatabaseMetaData(this);
	}

	/*------------------------------*/
	/*       Commit, rollback       */
	/*------------------------------*/

	@Override public void setAutoCommit(boolean autoCommit) throws SQLException {
		this.executor.setAutoCommit(autoCommit);
	}

	@Override public boolean getAutoCommit() throws SQLException {
		return this.executor.getAutoCommit();
	}

	@Override public void commit() throws SQLException {
		checkClosed();
		checkAutoCommit();
		executor.commit();
	}

	@Override public void rollback() throws SQLException {
		checkClosed();
		checkAutoCommit();
		executor.rollback();
	}

	/*------------------------------*/
	/*       Create Statement       */
	/*------------------------------*/

	@Override public java.sql.Statement createStatement() throws SQLException {
		this.checkClosed();
		return new HttpNeo4jStatement(this);
	}

	@Override public java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
		this.checkClosed();
		return new HttpNeo4jStatement(this);
	}

	@Override public java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
		this.checkClosed();
		return new HttpNeo4jStatement(this);
	}

	/*-------------------------------*/
	/*       Prepare Statement       */
	/*-------------------------------*/

	@Override public PreparedStatement prepareStatement(String cypher) throws SQLException {
		this.checkClosed();
		return new HttpNeo4jPreparedStatement(this, cypher);
	}

	@Override public PreparedStatement prepareStatement(String cypher, int resultSetType, int resultSetConcurrency) throws SQLException {
		this.checkClosed();
		return new HttpNeo4jPreparedStatement(this, cypher);
	}

	@Override public PreparedStatement prepareStatement(String cypher, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		this.checkClosed();
		return new HttpNeo4jPreparedStatement(this, cypher);
	}

	/*-------------------*/
	/*       Close       */
	/*-------------------*/

	@Override public boolean isClosed() throws SQLException {
		return isClosed;
	}

	@Override public void close() throws SQLException {
		if (!this.getAutoCommit() && executor.getOpenTransactionId() > 0) {
			executor.rollback();
		}
		executor.close();
		isClosed = true;
	}

	/*-------------------*/
	/*      isValid      */
	/*-------------------*/

	@Override public boolean isValid(int timeout) throws SQLException {
		if (timeout < 0) {
			throw new SQLException("Timeout can't be less than zero");
		}
		if (this.isClosed()) {
			return false;
		}

		Runnable r = new Runnable() {
			@Override public void run() {
				try {
					executor.executeQuery(new Neo4jStatement(FASTEST_STATEMENT, null, null));
				} catch (Exception e) {
					throw new Neo4jJdbcRuntimeException(e);
				}
			}
		};

		try {
			TimeLimitedCodeBlock.runWithTimeout(r, timeout, TimeUnit.SECONDS);
		}
		catch (Exception e) { // also timeout
			return false;
		}

		return true;
	}
}
