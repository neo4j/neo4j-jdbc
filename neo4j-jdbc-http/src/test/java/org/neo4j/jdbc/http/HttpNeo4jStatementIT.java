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

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class HttpNeo4jStatementIT extends Neo4jHttpITUtil {

	/*------------------------------*/
	/*          executeQuery        */
	/*------------------------------*/

	@Test public void executeQueryShouldExecuteAndReturnCorrectData() throws SQLException {
		Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getHttpUrl());
		Statement statement = connection.createStatement();
		ResultSet rs = statement.executeQuery("MATCH (m:Movie { title:\"The Matrix\"}) RETURN m.title");

		assertTrue(rs.next());
		assertEquals("The Matrix", rs.getString(1));
		assertFalse(rs.next());
		connection.close();
	}

	@Test public void executeQueryWithNullResponseValueShouldExecuteAndReturnCorrectData() throws SQLException {
		Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getHttpUrl());
		Statement statement = connection.createStatement();
		ResultSet rs = statement.executeQuery("MATCH (n:Person) RETURN n.title AS title");

		assertTrue(rs.next());
		assertNull(rs.getString("title"));
		assertTrue(rs.wasNull());

		connection.close();
	}

	@Test public void executeBadCypherQueryShouldReturnAnSQLException() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("SyntaxError");

		Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getHttpUrl());
		Statement statement = connection.createStatement();
		try {
			statement.execute("AZERTYUIOP");
		}
		finally {
			connection.close();
		}
	}

	@Test public void successfullyParsesUnregisteredProcedureCall() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("ProcedureNotFound");

		Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getHttpUrl());
		Statement statement = connection.createStatement();
		try {
			statement.execute("CALL apoc.trigger.add('HAS_VALUE_ON_REMOVE_FROM_INDEX', \"UNWIND {deletedRelationships} AS r MATCH (d:Decision)-[r:HAS_VALUE_ON]->(Characteristic) CALL apoc.index.removeRelationshipByName('HAS_VALUE_ON', r) RETURN count(*)\", {phase:'after'})");
		}
		finally {
			connection.close();
		}
	}

	/*------------------------------*/
	/*         executeUpdate        */
	/*------------------------------*/

	@Test public void executeUpdateShouldExecuteAndReturnCorrectData() throws SQLException {
		Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getHttpUrl());
		Statement statement = connection.createStatement();

		// Node insertion
		int lines = statement.executeUpdate("CREATE (n:User {name:\"test1\"})");
		assertEquals("Stats on node insertion (1) failed", 1, lines);
		lines = statement.executeUpdate("CREATE (n:User {name:\"test2\"})");
		assertEquals("Stats on node insertion (2) failed", 1, lines);

		// Relation insertion
		lines = statement.executeUpdate("MATCH (from:User {name:\"test1\"}), (to:User {name:\"test1\"}) CREATE (from)-[:TEST {name:\"test\"}]->(to)");
		assertEquals("Stats on relation insertion failed", 1, lines);

		// Deletion
		lines = statement.executeUpdate("MATCH (n:User) DETACH DELETE n");
		assertEquals("Stats on node deletion failed", 3, lines);

		connection.close();
	}

	/*------------------------------*/
	/*         executeBatch         */
	/*------------------------------*/
	@Test public void executeBatchShouldWork() throws SQLException {
		Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getHttpUrl());
		Statement statement = connection.createStatement();
		connection.setAutoCommit(true);
		statement.addBatch("CREATE (:TestExecuteBatchShouldWork {name:\"test1\"})");
		statement.addBatch("CREATE (:TestExecuteBatchShouldWork {name:\"test2\"})");
		statement.addBatch("CREATE (:TestExecuteBatchShouldWork {name:\"test3\"})");

		int[] result = statement.executeBatch();
		assertArrayEquals(new int[] { 1, 1, 1 }, result);

		connection.close();
	}

	@Test public void executeBatchShouldWorkWhenError() throws SQLException {
		Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getHttpUrl());
		connection.setAutoCommit(true);
		Statement statement = connection.createStatement();
		statement.addBatch("CREATE (:TestExecuteBatchShouldWorkWhenError {name:\"test1\"})");
		statement.addBatch("CREATE (:TestExecuteBatchShouldWorkWhenError {name:\"test2\"})");
		statement.addBatch("wrong query");
		statement.addBatch("CREATE (:TestExecuteBatchShouldWorkWhenError {name:\"test3\"})");

		try {
			statement.executeBatch();
			fail();
		} catch (BatchUpdateException e) {
			assertArrayEquals(new int[] { 1, 1 }, e.getUpdateCounts());
		}

		connection.close();
	}

	@Test public void executeBatchShouldWorkWithTransaction() throws SQLException {
		Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getHttpUrl());
		Statement statement = connection.createStatement();
		connection.setAutoCommit(false);
		statement.addBatch("CREATE (:TestExecuteBatchShouldWorkWithTransaction_" + secureMode.toString() + " {name:\"test1\"})");
		statement.addBatch("CREATE (:TestExecuteBatchShouldWorkWithTransaction_" + secureMode.toString() + "  {name:\"test2\"})");
		statement.addBatch("CREATE (:TestExecuteBatchShouldWorkWithTransaction_" + secureMode.toString() + "  {name:\"test3\"})");

		// Check the result
		int[] result = statement.executeBatch();
		assertArrayEquals(new int[] { 1, 1, 1 }, result);

		// Check if it's not yet saved into db
		try (org.neo4j.driver.Driver driver = GraphDatabase.driver(neo4j.getBoltUrl());
			 Session session = driver.session()) {
			final Result res = session.run("MATCH (n:TestExecuteBatchShouldWorkWithTransaction_" + secureMode.toString() + ") RETURN count(n) AS total");
			while (res.hasNext()) {
				assertEquals(0L, res.next().get("total").asLong());
			}
		}

		connection.commit();

		// Check if it's saved into db
		try (org.neo4j.driver.Driver driver = GraphDatabase.driver(neo4j.getBoltUrl());
			 Session session = driver.session()) {
			final Result res = session.run("MATCH (n:TestExecuteBatchShouldWorkWithTransaction_" + secureMode.toString() + ") RETURN count(n) AS total");
			while (res.hasNext()) {
				assertEquals(3L, res.next().get("total").asLong());
			}
		}


		connection.close();
	}

}
