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
package org.neo4j.jdbc.http.driver;

import org.neo4j.jdbc.http.test.Neo4jHttpIT;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.junit.Assert.*;

public class CypherExecutorIT extends Neo4jHttpIT {

	private CypherExecutor executor;

	@Before public void before() throws IOException, SQLException {
		String host = neo4j.httpsURI().getHost();
		Integer port = neo4j.httpsURI().getPort();
		Properties properties = new Properties();
		properties.put("userAgent", "Unit Test");
		this.executor = new CypherExecutor(host, port, false, properties);
	}

	@Test public void executeQueryShouldSucceed() throws Exception {
		List<Neo4jStatement> queries = getRandomNeo4jStatementFromCSV("data/queries.csv", -1).get("object");
		Neo4jResponse response = executor.executeQueries(queries);

		assertEquals(queries.size(), response.results.size());
		assertFalse(response.hasErrors());
	}

	@Test public void executeQueryThenRollbackShouldSucceed() throws SQLException {
		executor.setAutoCommit(Boolean.FALSE);
		String randomLabel = "Test" + UUID.randomUUID();
		executor.executeQuery(new Neo4jStatement("CREATE (:`" + randomLabel + "`)", null, Boolean.TRUE));
		executor.rollback();

		executor.setAutoCommit(Boolean.TRUE);
		Neo4jResponse response = executor.executeQuery(new Neo4jStatement("MATCH (n:`" + randomLabel + "`) RETURN n", null, Boolean.FALSE));
		assertEquals(0, response.results.get(0).rows.size());
	}

	@Test public void executeMultipleQueryThenCommitShouldSucceed() throws SQLException {
		executor.setAutoCommit(Boolean.FALSE);
		String randomLabel = "Test" + UUID.randomUUID();
		executor.executeQuery(new Neo4jStatement("CREATE (:`" + randomLabel + "` {value:1})", null, Boolean.FALSE));
		executor.executeQuery(new Neo4jStatement("CREATE (:`" + randomLabel + "` {value:2})", null, Boolean.FALSE));
		executor.commit();

		executor.setAutoCommit(Boolean.TRUE);
		Neo4jResponse response = executor.executeQuery(new Neo4jStatement("MATCH (n:`" + randomLabel + "`) RETURN n", null, Boolean.FALSE));
		assertEquals(2, response.results.get(0).rows.size());
	}

	@Test public void rollbackClosedTransactionShouldFail() throws SQLException {
		expectedEx.expect(SQLException.class);

		executor.setAutoCommit(Boolean.FALSE);
		String randomLabel = "Test" + UUID.randomUUID();
		executor.executeQuery(new Neo4jStatement("CREATE (:`" + randomLabel + "` {value:1})", null, Boolean.FALSE));
		executor.executeQuery(new Neo4jStatement("CREATE (:`" + randomLabel + "` {value:2})", null, Boolean.FALSE));
		executor.commit();

		executor.rollback();
	}

	@Test public void rollbackOnAutocommitShouldFail() throws SQLException {
		expectedEx.expect(SQLException.class);

		String randomLabel = "Test" + UUID.randomUUID();
		executor.executeQuery(new Neo4jStatement("CREATE (:`" + randomLabel + "` {value:1})", null, Boolean.FALSE));
		executor.rollback();
	}

	@Test public void commitOnAutocommitShouldFail() throws SQLException {
		expectedEx.expect(SQLException.class);

		String randomLabel = "Test" + UUID.randomUUID();
		executor.executeQuery(new Neo4jStatement("CREATE (:`" + randomLabel + "` {value:1})", null, Boolean.FALSE));
		executor.commit();
	}

	@Test public void transactionToAutoCommitModeShouldCommitCurrentTransaction() throws SQLException {
		executor.setAutoCommit(Boolean.FALSE);
		String randomLabel = "Test" + UUID.randomUUID();
		executor.executeQuery(new Neo4jStatement("CREATE (:`" + randomLabel + "` {value:1})", null, Boolean.FALSE));
		executor.executeQuery(new Neo4jStatement("CREATE (:`" + randomLabel + "` {value:2})", null, Boolean.FALSE));

		executor.setAutoCommit(Boolean.TRUE);
		Neo4jResponse response = executor.executeQuery(new Neo4jStatement("MATCH (n:`" + randomLabel + "`) RETURN n", null, Boolean.FALSE));
		assertEquals(2, response.results.get(0).rows.size());
	}

	@Test public void getServerVersionShouldSucceed() throws SQLException {
		assertNotEquals("Unknown", executor.getServerVersion());
	}

	@Test public void executeInvalidQueryShouldNotOpenTransaction() throws Exception {
		executor.setAutoCommit(Boolean.FALSE);

		Neo4jResponse response = executor.executeQuery(new Neo4jStatement("invalid", null, Boolean.FALSE));

		assertTrue(response.hasErrors());
		assertEquals(Integer.valueOf(-1), executor.getOpenTransactionId());
	}

	@Test public void executeValidThenInvalidQueryShouldRollbackTransaction() throws Exception {
		executor.setAutoCommit(Boolean.FALSE);
		Neo4jResponse response = executor.executeQuery(new Neo4jStatement("MATCH (n) RETURN count(n)", null, Boolean.FALSE));

		assertFalse(response.hasErrors());
		assertTrue(executor.getOpenTransactionId() > 0);

		response = executor.executeQuery(new Neo4jStatement("invalid", null, Boolean.FALSE));

		assertTrue(response.hasErrors());
		assertEquals(Integer.valueOf(-1), executor.getOpenTransactionId());
	}

	@After public void after() throws SQLException {
		executor.close();
	}

}
