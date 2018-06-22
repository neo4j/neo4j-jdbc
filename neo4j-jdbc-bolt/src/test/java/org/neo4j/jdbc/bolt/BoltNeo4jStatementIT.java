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

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.neo4j.graphdb.Result;
import org.neo4j.jdbc.Neo4jResultSet;
import org.neo4j.jdbc.bolt.data.StatementData;
import org.neo4j.jdbc.bolt.utils.JdbcConnectionTestUtils;

import java.sql.*;

import static org.junit.Assert.*;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltNeo4jStatementIT {

	@ClassRule public static Neo4jBoltRule neo4j = new Neo4jBoltRule();

	@Rule public ExpectedException expectedEx = ExpectedException.none();

	Connection connection;

	@Before
	public void setUp(){
		connection = JdbcConnectionTestUtils.verifyConnection(connection, neo4j);
	}


	/*------------------------------*/
	/*          executeQuery        */
	/*------------------------------*/
	@Test public void executeQueryShouldExecuteAndReturnCorrectData() throws SQLException {
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE);
		Statement statement = connection.createStatement();
		ResultSet rs = statement.executeQuery(StatementData.STATEMENT_MATCH_ALL_STRING);

		assertTrue(rs.next());
		assertEquals("test", rs.getString(1));
		assertFalse(rs.next());
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_REV);
	}

	@Test public void executeQueryShouldExecuteAndReturnCorrectDataOnAutoCommitFalseStatement() throws SQLException {
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE);
		Statement statement = connection.createStatement();
		connection.setAutoCommit(false);

		ResultSet rs = statement.executeQuery(StatementData.STATEMENT_MATCH_ALL_STRING);

		connection.commit();
		assertTrue(rs.next());
		assertEquals("test", rs.getString(1));
		assertFalse(rs.next());
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_REV);
	}

	@Test public void executeQueryShouldExecuteAndReturnCorrectDataOnAutoCommitFalseStatementAndCreatedWithParams() throws SQLException {
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE);
		Statement statement = connection.createStatement(Neo4jResultSet.TYPE_FORWARD_ONLY, Neo4jResultSet.CONCUR_READ_ONLY);
		connection.setAutoCommit(false);

		ResultSet rs = statement.executeQuery(StatementData.STATEMENT_MATCH_ALL_STRING);

		connection.commit();
		assertTrue(rs.next());
		assertEquals("test", rs.getString(1));
		assertFalse(rs.next());
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_REV);
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

	}

	@Test public void executeShouldExecuteAndReturnTrue() throws SQLException {
		Statement statement = connection.createStatement();
		boolean result = statement.execute(StatementData.STATEMENT_MATCH_ALL);
		assertTrue(result);

	}

	@Test public void executeBadCypherQueryOnAutoCommitShouldReturnAnSQLException() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("Invalid input");

		Statement statement = connection.createStatement();
		statement.execute("AZERTYUIOP");

	}

	@Test public void executeBadCypherQueryWithoutAutoCommitShouldReturnAnSQLException() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("Invalid input");

		connection.setAutoCommit(false);

		Statement statement = connection.createStatement();
		statement.execute("AZERTYUIOP");
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

		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_REV);
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

		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_REV);
	}

	@Test public void executeBatchShouldWorkWithTransaction() throws SQLException {
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

}
