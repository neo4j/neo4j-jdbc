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
 * Created on 08/03/16
 */
package org.neo4j.jdbc.bolt;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.jdbc.Neo4jResultSet;
import org.neo4j.jdbc.bolt.data.StatementData;
import org.neo4j.jdbc.bolt.utils.JdbcConnectionTestUtils;
import org.testcontainers.containers.Neo4jContainer;

import java.sql.Array;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltNeo4jStatementIT {

	@ClassRule
	public static final Neo4jContainer<?> neo4j = new Neo4jContainer<>("neo4j:4.3.0").withAdminPassword(null);

	@Rule public ExpectedException expectedEx = ExpectedException.none();

	static Connection connection;

	@Before
	public void setUp(){
		connection = JdbcConnectionTestUtils.verifyConnection(connection, neo4j);
	}

	@AfterClass
	public static void tearDown(){
		JdbcConnectionTestUtils.closeConnection(connection);
	}

	/*------------------------------*/
	/*          executeQuery        */
	/*------------------------------*/
	@Test public void executeQueryShouldExecuteAndReturnCorrectData() throws SQLException {
		JdbcConnectionTestUtils.executeTransactionally(neo4j, StatementData.STATEMENT_CREATE);
		Statement statement = connection.createStatement();
		ResultSet rs = statement.executeQuery(StatementData.STATEMENT_MATCH_ALL_STRING);

		assertTrue(rs.next());
		assertEquals("test", rs.getString(1));
		assertFalse(rs.next());
		JdbcConnectionTestUtils.executeTransactionally(neo4j, StatementData.STATEMENT_CREATE_REV);
	}

	@Test public void executeQueryShouldExecuteAndReturnCorrectDataOnAutoCommitFalseStatement() throws SQLException {
		JdbcConnectionTestUtils.executeTransactionally(neo4j, StatementData.STATEMENT_CREATE);
		Statement statement = connection.createStatement();
		connection.setAutoCommit(false);

		ResultSet rs = statement.executeQuery(StatementData.STATEMENT_MATCH_ALL_STRING);

		connection.commit();
		assertTrue(rs.next());
		assertEquals("test", rs.getString(1));
		assertFalse(rs.next());
		JdbcConnectionTestUtils.executeTransactionally(neo4j, StatementData.STATEMENT_CREATE_REV);
	}

	@Test public void executeQueryShouldExecuteAndReturnCorrectDataOnAutoCommitFalseStatementAndCreatedWithParams() throws SQLException {
		JdbcConnectionTestUtils.executeTransactionally(neo4j, StatementData.STATEMENT_CREATE);
		Statement statement = connection.createStatement(Neo4jResultSet.TYPE_FORWARD_ONLY, Neo4jResultSet.CONCUR_READ_ONLY);
		connection.setAutoCommit(false);

		ResultSet rs = statement.executeQuery(StatementData.STATEMENT_MATCH_ALL_STRING);

		connection.commit();
		assertTrue(rs.next());
		assertEquals("test", rs.getString(1));
		assertFalse(rs.next());
		JdbcConnectionTestUtils.executeTransactionally(neo4j, StatementData.STATEMENT_CREATE_REV);
	}

	/*------------------------------*/
	/*         executeUpdate        */
	/*------------------------------*/
	@Test public void executeUpdateShouldExecuteAndReturnCorrectData() throws SQLException {
		Statement statement = connection.createStatement();
		int lines = statement.executeUpdate(StatementData.STATEMENT_CREATE);
		assertEquals(1, lines);

		lines = statement.executeUpdate(StatementData.STATEMENT_CREATE);
		assertEquals(1, lines);

		lines = statement.executeUpdate(StatementData.STATEMENT_CREATE_REV);
		assertEquals(2, lines);
		statement.close();
	}

	/*------------------------------*/
	/*            execute           */
	/*------------------------------*/
	@Test public void executeShouldExecuteAndReturnFalse() throws SQLException {
		Statement statement = connection.createStatement();
		boolean result = statement.execute(StatementData.STATEMENT_CREATE);
		assertFalse(result);

		result = statement.execute(StatementData.STATEMENT_CREATE_REV);
		assertFalse(result);
		statement.close();

	}

	@Test public void executeShouldExecuteAndReturnTrue() throws SQLException {
		Statement statement = connection.createStatement();
		boolean result = statement.execute(StatementData.STATEMENT_MATCH_ALL);
		assertTrue(result);
		statement.close();

	}

	@Test public void executeBadCypherQueryOnAutoCommitShouldReturnAnSQLException() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("Invalid input");

		Statement statement = connection.createStatement();
		statement.execute("AZERTYUIOP");
		statement.close();

	}

	@Test public void executeBadCypherQueryWithoutAutoCommitShouldReturnAnSQLException() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("Invalid input");

		connection.setAutoCommit(false);

		Statement statement = connection.createStatement();
		try {
			statement.execute("AZERTYUIOP");
		} catch (Exception e) {
			statement.close();
			throw e;
		}
	}

	/*------------------------------*/
	/*         executeBatch         */
	/*------------------------------*/
	@Test public void executeBatchShouldWork() throws SQLException {
		Statement statement = connection.createStatement();
		connection.setAutoCommit(true);
		statement.addBatch(StatementData.STATEMENT_CREATE);
		statement.addBatch(StatementData.STATEMENT_CREATE);
		statement.addBatch(StatementData.STATEMENT_CREATE);

		int[] result = statement.executeBatch();

		assertArrayEquals(new int[]{1, 1, 1}, result);

		JdbcConnectionTestUtils.executeTransactionally(neo4j, StatementData.STATEMENT_CREATE_REV);
		statement.close();
	}

	@Test public void executeBatchShouldWorkWhenError() throws SQLException {
		connection.setAutoCommit(true);
		Statement statement = connection.createStatement();
		statement.addBatch(StatementData.STATEMENT_CREATE);
		statement.addBatch(StatementData.STATEMENT_CREATE);
		statement.addBatch("wrong query");
		statement.addBatch(StatementData.STATEMENT_CREATE);

		try {
			statement.executeBatch();
			fail();
		} catch (BatchUpdateException e){
			assertArrayEquals(new int[]{1, 1}, e.getUpdateCounts());
		}

		JdbcConnectionTestUtils.executeTransactionally(neo4j, StatementData.STATEMENT_CREATE_REV);
		statement.close();
	}

	@Test public void executeBatchShouldWorkWithTransaction() throws SQLException {
		Statement statement = connection.createStatement();
		connection.setAutoCommit(false);
		statement.addBatch(StatementData.STATEMENT_CREATE);
		statement.addBatch(StatementData.STATEMENT_CREATE);
		statement.addBatch(StatementData.STATEMENT_CREATE);

		try (org.neo4j.driver.Driver driver = GraphDatabase.driver(neo4j.getBoltUrl());
			 Session session = driver.session()) {
			Result res = session.run(StatementData.STATEMENT_COUNT_NODES);
			while (res.hasNext()){
				assertEquals(0L, res.next().get("total").asLong());
			}
		}

		int[] result = statement.executeBatch();

		assertArrayEquals(new int[]{1, 1, 1}, result);

		connection.commit();

		try (org.neo4j.driver.Driver driver = GraphDatabase.driver(neo4j.getBoltUrl());
			 Session session = driver.session()) {
			Result res = session.run(StatementData.STATEMENT_COUNT_NODES);
			while (res.hasNext()){
				assertEquals(3L, res.next().get("total").asLong());
			}
		}

		JdbcConnectionTestUtils.executeTransactionally(neo4j, StatementData.STATEMENT_CREATE_REV);
		statement.close();

	}

	/*------------------------------*/
	/*             close            */
	/*------------------------------*/
	@Test public void closeShouldNotCloseTransaction() throws SQLException {
		connection.setAutoCommit(false);

		Statement statement = connection.createStatement();
		statement.execute("RETURN true AS result");
		statement.close();

		assertTrue(((BoltNeo4jConnection) connection).getTransaction().isOpen());
	}

	/*
	Array
	 */

	@Test public void testGetEmptyArrayByIndex() throws SQLException {
		connection.setAutoCommit(true);

		Statement statement = connection.createStatement();
		statement.execute("RETURN [] AS result");

		ResultSet resultSet = statement.getResultSet();
		resultSet.next();
		Array array = resultSet.getArray(1);
		Object[] arrayResult = (Object[]) array.getArray();
		assertEquals(0, arrayResult.length);

		statement.close();
	}

	@Test public void testGetEmptyArrayByName() throws SQLException {
		connection.setAutoCommit(true);

		Statement statement = connection.createStatement();
		statement.execute("RETURN [] AS result");

		ResultSet resultSet = statement.getResultSet();
		resultSet.next();
		Array array = resultSet.getArray("result");
		Object[] arrayResult = (Object[]) array.getArray();
		assertEquals(0, arrayResult.length);

		statement.close();
	}

	@Test public void testGetOneArrayByIndex() throws SQLException {
		connection.setAutoCommit(true);

		Statement statement = connection.createStatement();
		statement.execute("RETURN [10] AS result");

		ResultSet resultSet = statement.getResultSet();
		resultSet.next();
		Array array = resultSet.getArray(1);
		Long[] arrayResult = (Long[]) array.getArray();
		assertEquals(new Long(10), arrayResult[0]);

		statement.close();
	}

	@Test public void testGetOneArrayByName() throws SQLException {
		connection.setAutoCommit(true);

		Statement statement = connection.createStatement();
		statement.execute("RETURN [10] AS result");

		ResultSet resultSet = statement.getResultSet();
		resultSet.next();
		Array array = resultSet.getArray("result");
		Long[] arrayResult = (Long[]) array.getArray();
		assertEquals(new Long(10), arrayResult[0]);

		statement.close();
	}

	@Test public void testCallProcedure() throws SQLException {
		connection.setAutoCommit(true);

		try (Statement statement = connection.createStatement()) {
			statement.execute("CALL db.schema.visualization");

			ResultSet resultSet = statement.getResultSet();
			resultSet.next();
			Array arrayNodes = resultSet.getArray("nodes");
			Object[] nodesArray = (Object[]) arrayNodes.getArray();
			assertEquals(0, nodesArray.length);
			Array arrayRels = resultSet.getArray("relationships");
			Object[] relsArray = (Object[]) arrayRels.getArray();
			assertEquals(0, relsArray.length);
		}
	}

	@Test public void testSimpleReturn() throws SQLException {
		connection.setAutoCommit(true);

		try (Statement statement = connection.createStatement()) {
			statement.execute("RETURN 1 AS id");

			ResultSet resultSet = statement.getResultSet();
			resultSet.next();
			final long id = resultSet.getLong("id");
			assertEquals(1L, id);
		}
	}

	@Test public void testUnwindNope() throws SQLException {
		connection.setAutoCommit(true);

		try (Statement statement = connection.createStatement()) {
			final boolean execute = statement.execute("UNWIND [] AS nope RETURN nope");
			assertTrue(execute);

			ResultSet resultSet = statement.getResultSet();
			resultSet.next();
			final Object nope = resultSet.getObject("nope");
			assertNull(nope);
		}
	}

	@Test public void testCreate() throws SQLException {
		connection.setAutoCommit(true);

		try (Statement statement = connection.createStatement()) {
			final boolean execute = statement.execute("CREATE (n:NodeTestToDelete)");
			assertFalse(execute);
			assertNull(statement.getResultSet());
		}
		JdbcConnectionTestUtils.executeTransactionally(neo4j, "MATCH (n:NodeTestToDelete) DELETE n");
	}

	@Test public void testExplain() throws SQLException {
		connection.setAutoCommit(true);

		try (Statement statement = connection.createStatement()) {
			final boolean execute = statement.execute("EXPLAIN CREATE (n:NodeTestToDelete)");
			assertFalse(execute);
			assertNull(statement.getResultSet());
		}
	}

	@Test public void testProfile() throws SQLException {
		connection.setAutoCommit(true);

		try (Statement statement = connection.createStatement()) {
			final boolean execute = statement.execute("PROFILE CREATE (n:NodeTestToDelete)");
			assertFalse(execute);
			assertNull(statement.getResultSet());
		}
		JdbcConnectionTestUtils.executeTransactionally(neo4j, "MATCH (n:NodeTestToDelete) DELETE n");
	}

}
