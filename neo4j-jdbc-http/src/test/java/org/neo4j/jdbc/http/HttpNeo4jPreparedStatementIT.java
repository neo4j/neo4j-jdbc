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

import org.junit.Test;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.jdbc.http.test.Neo4jHttpITUtil;
import org.neo4j.jdbc.impl.ListArray;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Map;

import static java.sql.Types.INTEGER;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class HttpNeo4jPreparedStatementIT extends Neo4jHttpITUtil {

	/*------------------------------*/
	/*          executeQuery        */
	/*------------------------------*/

	@Test public void executeQueryShouldRunAndReturnCorrectData() throws SQLException {
		Connection connection = DriverManager.getConnection(getJDBCUrl());
		PreparedStatement statement = connection.prepareStatement("MATCH (m:Movie) WHERE m.title= ? RETURN m.title");
		statement.setString(1, "The Matrix");
		ResultSet rs = statement.executeQuery();

		assertTrue(rs.next());
		assertEquals("The Matrix", rs.getString(1));
		assertFalse(rs.next());
		connection.close();
	}

	/*------------------------------*/
	/*          executeUpdate       */
	/*------------------------------*/

	@Test public void executeUpdateShouldRunAndReturnCorrectData() throws SQLException {
		Connection connection = DriverManager.getConnection(getJDBCUrl());
		PreparedStatement statement = connection.prepareStatement("CREATE (n:TestExecuteUpdateShouldExecuteAndReturnCorrectData {value:?})");
		statement.setString(1, "AZERTYUIOP");

		assertEquals(1, statement.executeUpdate());
		connection.close();
	}

	/*------------------------------*/
	/*          execute             */
	/*------------------------------*/
	
	@Test public void executeWithReadOnlyStatementShouldRunAndReturnCorrectData() throws SQLException {
		Connection connection = DriverManager.getConnection(getJDBCUrl());
		PreparedStatement statement = connection.prepareStatement("MATCH (m:Movie) WHERE m.title= ? RETURN m.title");
		statement.setString(1, "The Matrix");
		
		assertTrue(statement.execute());
		connection.close();
	}
	
	@Test public void executeWithUpdateStatementShouldRunAndReturnCorrectData() throws SQLException {
		Connection connection = DriverManager.getConnection(getJDBCUrl());
		PreparedStatement statement = connection.prepareStatement("CREATE (n:TestExecuteUpdateShouldExecuteAndReturnCorrectData {value:?})");
		statement.setString(1, "AZERTYUIOP");
		
		assertFalse(statement.execute());
		connection.close();
	}
	
	/*------------------------------*/
	/*         executeBatch         */
	/*------------------------------*/
	@Test public void executeBatchShouldWork() throws SQLException {
		Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getHttpUrl());
		PreparedStatement statement = connection.prepareStatement("CREATE (:TestExecuteBatchShouldWork { name:?, value:?})");
		connection.setAutoCommit(true);
		statement.setString(1, "test1");
		statement.setString(2, "test2");
		statement.addBatch();
		statement.setString(1, "test3");
		statement.setString(2, "test4");
		statement.addBatch();
		statement.setString(1, "test5");
		statement.setString(2, "test6");
		statement.addBatch();

		int[] result = statement.executeBatch();
		assertArrayEquals(new int[]{1, 1, 1}, result);

		connection.close();
	}

	@Test public void executeBatchShouldWorkWhenError() throws SQLException {
		Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getHttpUrl());
		connection.setAutoCommit(true);
		PreparedStatement statement = connection.prepareStatement("wrong cypher statement ?");
		statement.setString(1, "test1");
		statement.addBatch();
		statement.setString(1, "test3");
		statement.addBatch();
		statement.setString(1, "test5");
		statement.addBatch();

		try {
			statement.executeBatch();
			fail();
		} catch (BatchUpdateException e){
			assertArrayEquals(new int[0], e.getUpdateCounts());
		}

		connection.close();
	}

	@Test public void executeBatchShouldWorkWithTransaction() throws SQLException {
		Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getHttpUrl());
		PreparedStatement statement = connection.prepareStatement("CREATE (:TestExecuteBatchShouldWorkWithTransaction_" + secureMode.toString() + " { name:?, value:?})");
		connection.setAutoCommit(false);
		statement.setString(1, "test1");
		statement.setString(2, "test2");
		statement.addBatch();
		statement.setString(1, "test3");
		statement.setString(2, "test4");
		statement.addBatch();
		statement.setString(1, "test5");
		statement.setString(2, "test6");
		statement.addBatch();

		int[] result = statement.executeBatch();

		try (org.neo4j.driver.Driver driver = GraphDatabase.driver(neo4j.getBoltUrl());
			 Session session = driver.session()) {
			final Result res = session.run("MATCH (n:TestExecuteBatchShouldWorkWithTransaction_" + secureMode.toString() + ") RETURN count(n) AS total");
			while (res.hasNext()) {
				assertEquals(0L, res.next().get("total").asLong());
			}
		}

		assertArrayEquals(new int[]{1, 1, 1}, result);

		connection.commit();
		
		try (org.neo4j.driver.Driver driver = GraphDatabase.driver(neo4j.getBoltUrl());
			 Session session = driver.session()) {
			final Result res = session.run("MATCH (n:TestExecuteBatchShouldWorkWithTransaction_" + secureMode.toString() + ") RETURN count(n) AS total");
			while (res.hasNext()) {
				assertEquals(3L, res.next().get("total").asLong());
			}
		}

		connection.close();
	}

	@Test public void shouldInsertArrayType() throws SQLException {
		try (Connection connection = DriverManager.getConnection(getJDBCUrl())) {
			PreparedStatement statement = connection.prepareStatement("CREATE (:TestArrayType {name:?, props:?})");
			statement.setString(1, "Andrea Santurbano");
			statement.setArray(2, new ListArray(Arrays.asList(1L,2L,4L), INTEGER));
			assertEquals(1, statement.executeUpdate());
			statement.close();

			Statement search = connection.createStatement();
			ResultSet rs = search.executeQuery("MATCH (n:TestArrayType) return n");
			assertTrue(rs.next());
			Map<String, Object> map = (Map<String, Object>) rs.getObject("n");
			assertEquals("Andrea Santurbano", map.get("name"));
			assertEquals(Arrays.asList(1L,2L,4L), map.get("props"));
			search.close();
		}
	}
}

