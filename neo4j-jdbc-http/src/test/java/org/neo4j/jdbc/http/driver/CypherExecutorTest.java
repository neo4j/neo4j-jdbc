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
package org.neo4j.jdbc.http.driver;

import org.neo4j.jdbc.http.test.Neo4jHttpUnitTest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Random;

public class CypherExecutorTest extends Neo4jHttpUnitTest {

	private CypherExecutor executor;

	@Before public void before() throws IOException, SQLException {
		String host = "azertyuiop";
		Integer port = 1234;
		Properties properties = new Properties();
		this.executor = new CypherExecutor(host, port, properties);
	}

	@Test public void getTransactionIdShouldReturnCorrectId() {
		Random idGenerator = new Random();
		for (Integer i = 1; i < 100; i++) {
			Integer id = idGenerator.nextInt();
			Integer returnId = executor.getTransactionId(executor.transactionUrl + "/" + id);
			Assert.assertEquals(id, returnId);
		}
	}

	@Test public void getTransactionIdShouldReturnNegativeId() {
		List<String> urls = Arrays.asList("", "http://localhost1234:1234/db/data", executor.transactionUrl, executor.transactionUrl + "/commit");
		for (String url : urls) {
			Integer returnId = executor.getTransactionId(url);
			Assert.assertTrue(returnId < 0);
		}
	}

	@Test public void executeQueryOnDownServerShouldFail() throws Exception {
		expectedEx.expect(SQLException.class);

		List<Neo4jStatement> queries = getRandomNeo4jStatementFromCSV("data/queries.csv", 1).get("object");
		executor.executeQueries(queries);
	}

	@Test public void executeEmptyQueryShouldFail() throws Exception {
		expectedEx.expect(SQLException.class);
		executor.executeQuery(new Neo4jStatement("", null, null));
	}

	@After public void after() throws SQLException {
		executor.close();
	}

}
