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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.neo4j.graphdb.Result;
import org.neo4j.jdbc.Neo4jResultSet;
import org.neo4j.jdbc.bolt.data.StatementData;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltNeo4jStatementIT {

	@ClassRule public static Neo4jBoltRule neo4j = new Neo4jBoltRule();

	@Rule public ExpectedException expectedEx = ExpectedException.none();

	/*------------------------------*/
	/*          executeQuery        */
	/*------------------------------*/
	@Test public void executeQueryShouldExecuteAndReturnCorrectData() throws SQLException {
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE);
		Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl() + "?noSsl");
		Statement statement = connection.createStatement();
		ResultSet rs = statement.executeQuery(StatementData.STATEMENT_MATCH_ALL_STRING);

		assertTrue(rs.next());
		assertEquals("test", rs.getString(1));
		assertFalse(rs.next());
		connection.close();
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_REV);
	}

	@Test public void executeQueryShouldExecuteAndReturnCorrectDataOnAutoCommitFalseStatement() throws SQLException {
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE);
		Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl() + "?noSsl");
		Statement statement = connection.createStatement();
		connection.setAutoCommit(false);

		ResultSet rs = statement.executeQuery(StatementData.STATEMENT_MATCH_ALL_STRING);

		connection.commit();
		assertTrue(rs.next());
		assertEquals("test", rs.getString(1));
		assertFalse(rs.next());
		connection.close();
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_REV);
	}

	@Test public void executeQueryShouldExecuteAndReturnCorrectDataOnAutoCommitFalseStatementAndCreatedWithParams() throws SQLException {
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE);
		Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl() + "?noSsl");
		Statement statement = connection.createStatement(Neo4jResultSet.TYPE_FORWARD_ONLY, Neo4jResultSet.CONCUR_READ_ONLY);
		connection.setAutoCommit(false);

		ResultSet rs = statement.executeQuery(StatementData.STATEMENT_MATCH_ALL_STRING);

		connection.commit();
		assertTrue(rs.next());
		assertEquals("test", rs.getString(1));
		assertFalse(rs.next());
		connection.close();
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_REV);
	}

	/*------------------------------*/
	/*         executeUpdate        */
	/*------------------------------*/
	@Test public void executeUpdateShouldExecuteAndReturnCorrectData() throws SQLException {
		Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl() + "?noSsl");
		Statement statement = connection.createStatement();
		int lines = statement.executeUpdate(StatementData.STATEMENT_CREATE);
		assertEquals(1, lines);

		lines = statement.executeUpdate(StatementData.STATEMENT_CREATE);
		assertEquals(1, lines);

		lines = statement.executeUpdate(StatementData.STATEMENT_CREATE_REV);
		assertEquals(2, lines);

		connection.close();
	}

	/*------------------------------*/
	/*            execute           */
	/*------------------------------*/
	@Test public void executeShouldExecuteAndReturnFalse() throws SQLException {
		Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl() + "?noSsl");
		Statement statement = connection.createStatement();
		boolean result = statement.execute(StatementData.STATEMENT_CREATE);
		assertFalse(result);

		result = statement.execute(StatementData.STATEMENT_CREATE_REV);
		assertFalse(result);

		connection.close();
	}

	@Test public void executeShouldExecuteAndReturnTrue() throws SQLException {
		Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl() + "?noSsl");
		Statement statement = connection.createStatement();
		boolean result = statement.execute(StatementData.STATEMENT_MATCH_ALL);
		assertTrue(result);

		connection.close();
	}

	@Test public void executeBadCypherQueryOnAutoCommitShouldReturnAnSQLException() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("Invalid input");

		Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl() + "?noSsl");

		Statement statement = connection.createStatement();
		try {
			statement.execute("AZERTYUIOP");
		}
		finally {
			connection.close();
		}
	}

	@Test public void executeBadCypherQueryWithoutAutoCommitShouldReturnAnSQLException() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("Invalid input");

		Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl() + "?noSsl");
		connection.setAutoCommit(false);

		Statement statement = connection.createStatement();
		try {
			statement.execute("AZERTYUIOP");
		}
		finally {
			connection.close();
		}
	}

	/*------------------------------*/
	/*         executeBatch         */
	/*------------------------------*/
	@Test public void executeBatchShouldWork() throws SQLException {
		Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl() + "?noSsl");
		Statement statement = connection.createStatement();
		connection.setAutoCommit(true);
		statement.addBatch(StatementData.STATEMENT_CREATE);
		statement.addBatch(StatementData.STATEMENT_CREATE);
		statement.addBatch(StatementData.STATEMENT_CREATE);

		int[] result = statement.executeBatch();

		assertArrayEquals(new int[]{1, 1, 1}, result);

		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_REV);

		connection.close();
	}

	@Test public void executeBatchShouldWorkWhenError() throws SQLException {
		Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl() + "?noSsl");
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

		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_REV);
		connection.close();
	}

	@Test public void executeBatchShouldWorkWithTransaction() throws SQLException {
		Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl() + "?noSsl");
		Statement statement = connection.createStatement();
		connection.setAutoCommit(false);
		statement.addBatch(StatementData.STATEMENT_CREATE);
		statement.addBatch(StatementData.STATEMENT_CREATE);
		statement.addBatch(StatementData.STATEMENT_CREATE);

		Result res = neo4j.getGraphDatabase().execute(StatementData.STATEMENT_COUNT_NODES);
		while(res.hasNext()){
			assertEquals(0L, res.next().get("total"));
		}

		int[] result = statement.executeBatch();

		assertArrayEquals(new int[]{1, 1, 1}, result);

		connection.commit();

		res = neo4j.getGraphDatabase().execute(StatementData.STATEMENT_COUNT_NODES);
		while(res.hasNext()){
			assertEquals(3L, res.next().get("total"));
		}

		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_REV);

		connection.close();
	}

	/*------------------------------*/
	/*             close            */
	/*------------------------------*/
	@Test public void closeShouldNotCloseTransaction() throws SQLException {
		try (Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl() + "?noSsl")) {
			connection.setAutoCommit(false);

			Statement statement = connection.createStatement();
			statement.execute("RETURN true AS result");
			statement.close();

			assertTrue(((BoltNeo4jConnection) connection).getTransaction().isOpen());
		}
	}

}
