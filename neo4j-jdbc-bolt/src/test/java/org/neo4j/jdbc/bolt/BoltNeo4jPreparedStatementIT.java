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
 * Created on 25/03/16
 */
package org.neo4j.jdbc.bolt;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.harness.junit.rule.Neo4jRule;
import org.neo4j.jdbc.bolt.data.StatementData;
import org.neo4j.jdbc.bolt.utils.JdbcConnectionTestUtils;
import org.neo4j.jdbc.impl.ListArray;

import java.sql.*;
import java.util.Arrays;
import java.util.Map;

import static java.sql.Types.INTEGER;
import static org.junit.Assert.*;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltNeo4jPreparedStatementIT {

	@ClassRule public static Neo4jRule neo4j = new Neo4jRule();

	static Connection connection;

	@Before public void setUp(){
		connection = JdbcConnectionTestUtils.verifyConnection(connection, neo4j);
	}

	@AfterClass
	public static void tearDown(){
		JdbcConnectionTestUtils.closeConnection(connection);
	}

	/*------------------------------*/
	/*          executeQuery        */
	/*------------------------------*/

	@Before
	public void cleanDB(){
		neo4j.defaultDatabaseService().executeTransactionally(StatementData.STATEMENT_CLEAR_DB);
	}


	@Test public void executeQueryShouldExecuteAndReturnCorrectData() throws SQLException {
		neo4j.defaultDatabaseService().executeTransactionally(StatementData.STATEMENT_CREATE_TWO_PROPERTIES);
		PreparedStatement statement = connection.prepareStatement(StatementData.STATEMENT_MATCH_ALL_STRING_PARAMETRIC);
		statement.setString(1, "test");
		ResultSet rs = statement.executeQuery();
		
		assertTrue(rs.next());
		assertEquals("testAgain", rs.getString(1));
		assertFalse(rs.next());
		neo4j.defaultDatabaseService().executeTransactionally(StatementData.STATEMENT_CREATE_TWO_PROPERTIES_REV);
	}

	@Test public void executeQueryWithNamedParamShouldExecuteAndReturnCorrectData() throws SQLException {
		neo4j.defaultDatabaseService().executeTransactionally(StatementData.STATEMENT_CREATE_TWO_PROPERTIES);
		PreparedStatement statement = connection.prepareStatement(StatementData.STATEMENT_MATCH_ALL_STRING_PARAMETRIC_NAMED);
		statement.setString(1, "test");
		ResultSet rs = statement.executeQuery();

		assertTrue(rs.next());
		assertEquals("testAgain", rs.getString(1));
		assertFalse(rs.next());
		neo4j.defaultDatabaseService().executeTransactionally(StatementData.STATEMENT_CREATE_TWO_PROPERTIES_REV);
	}

	/*------------------------------*/
	/*         executeUpdate        */
	/*------------------------------*/
	@Test public void executeUpdateShouldExecuteAndReturnCorrectData() throws SQLException {
		PreparedStatement statement = connection.prepareStatement(StatementData.STATEMENT_CREATE_TWO_PROPERTIES_PARAMETRIC);
		statement.setString(1, "test1");
		statement.setString(2, "test2");
		int lines = statement.executeUpdate();
		assertEquals(1, lines);

		lines = statement.executeUpdate();
		assertEquals(1, lines);

		statement = connection.prepareStatement(StatementData.STATEMENT_CREATE_TWO_PROPERTIES_PARAMETRIC_REV);
		statement.setString(1, "test1");
		statement.setString(2, "test2");
		lines = statement.executeUpdate();
		assertEquals(2, lines);

	}

	/*------------------------------*/
	/*            execute           */
	/*------------------------------*/
	@Test public void executeShouldExecuteAndReturnTrue() throws SQLException {
		neo4j.defaultDatabaseService().executeTransactionally(StatementData.STATEMENT_CREATE_TWO_PROPERTIES);
		PreparedStatement statement = connection.prepareStatement(StatementData.STATEMENT_MATCH_ALL_STRING_PARAMETRIC);
		statement.setString(1, "test");
		boolean result = statement.execute();
		assertTrue(result);
		ResultSet resultSet = statement.getResultSet();
		assertTrue(resultSet.next());
		assertEquals("testAgain", resultSet.getString("n.surname"));
		assertFalse(resultSet.next());
		resultSet.close();
		statement.close();
	}

	@Test public void executeShouldExecuteAndReturnFalse() throws SQLException {
		PreparedStatement statement = connection.prepareStatement(StatementData.STATEMENT_CREATE_TWO_PROPERTIES_PARAMETRIC);
		statement.setString(1, "test1");
		statement.setString(2, "test2");

		boolean result = statement.execute();
		assertFalse(result);

		statement = connection.prepareStatement(StatementData.STATEMENT_CREATE_TWO_PROPERTIES_PARAMETRIC_REV);
		statement.setString(1, "test1");
		statement.setString(2, "test2");

		result = statement.execute();
		assertFalse(result);

	}

	/*------------------------------*/
	/*         executeBatch         */
	/*------------------------------*/
	@Test public void executeBatchShouldWork() throws SQLException {
		PreparedStatement statement = connection.prepareStatement(StatementData.STATEMENT_CREATE_TWO_PROPERTIES_PARAMETRIC);
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


	}

	@Test public void executeBatchShouldWorkWhenError() throws SQLException {
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


	}

	@Test public void executeBatchShouldWorkWithTransaction() throws SQLException {
		PreparedStatement statement = connection.prepareStatement(StatementData.STATEMENT_CREATE_TWO_PROPERTIES_PARAMETRIC);
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

		try (Transaction tx = neo4j.defaultDatabaseService().beginTx()) {
			Result res = tx.execute(StatementData.STATEMENT_COUNT_NODES);
			while(res.hasNext()){
				assertEquals(0L, res.next().get("total"));
			}
		}

		int[] result = statement.executeBatch();

		assertArrayEquals(new int[]{1, 1, 1}, result);

		connection.commit();

		try (Transaction tx = neo4j.defaultDatabaseService().beginTx()) {
			Result res = tx.execute(StatementData.STATEMENT_COUNT_NODES);
			while(res.hasNext()){
				assertEquals(3L, res.next().get("total"));
			}
		}

	}

	/*------------------------------*/
	/*            MoreResult        */
	/*------------------------------*/
	@Test public void testMoreResultWithOneResultSet() throws SQLException {
		neo4j.defaultDatabaseService().executeTransactionally(StatementData.STATEMENT_CREATE_TWO_PROPERTIES);
		PreparedStatement statement = connection.prepareStatement(StatementData.STATEMENT_MATCH_ALL_STRING_PARAMETRIC);
		statement.setString(1, "test");
		boolean result = statement.execute();

		assertTrue(statement.getMoreResults());

		ResultSet resultSet = statement.getResultSet();
		assertNotNull(resultSet);

		assertFalse(statement.getMoreResults());

		ResultSet resultSet2 = statement.getResultSet();
		assertNull(resultSet2);

		resultSet.close();
		statement.close();
	}

	@Test public void testMoreResultWithNoResultSet() throws SQLException {
		PreparedStatement statement = connection.prepareStatement(StatementData.STATEMENT_CREATE_TWO_PROPERTIES_PARAMETRIC);
		statement.setString(1, "testName");
		statement.setString(2, "testSurname");
		boolean result = statement.execute();

		assertFalse(statement.getMoreResults());

		ResultSet resultSet = statement.getResultSet();
		assertNull(resultSet);

		assertFalse(statement.getMoreResults());

		statement.close();
	}

	/*------------------------------*/
	/*            UpdateCount       */
	/*------------------------------*/
	@Test public void testUpdateCountWithCreate() throws SQLException {
		PreparedStatement statement = connection.prepareStatement(StatementData.STATEMENT_CREATE_TWO_PROPERTIES_PARAMETRIC);
		statement.setString(1, "test1");
		statement.setString(2, "test2");

		boolean result = statement.execute();
		assertFalse(result);
		assertEquals(1, statement.getUpdateCount());

		statement.close();
	}

	@Test public void testUpdateCountWithUpdate() throws SQLException {
		neo4j.defaultDatabaseService().executeTransactionally(StatementData.STATEMENT_CREATE_TWO_PROPERTIES);
		PreparedStatement statement = connection.prepareStatement(StatementData.STATEMENT_UPDATE_NODES_PARAM);
		statement.setString(1, "test");

		boolean result = statement.execute();
		assertFalse(result);
		assertEquals(1, statement.getUpdateCount());

		statement.close();
	}

	@Test public void testUpdateCountWithReturn() throws SQLException {
		neo4j.defaultDatabaseService().executeTransactionally(StatementData.STATEMENT_CREATE_TWO_PROPERTIES);
		PreparedStatement statement = connection.prepareStatement(StatementData.STATEMENT_MATCH_ALL_STRING_PARAMETRIC_NAMED);
		statement.setString(1, "test");

		boolean result = statement.execute();
		assertTrue(result);
		assertEquals(-1, statement.getUpdateCount());

		statement.close();
	}

	@Test public void shouldInsertArrayType() throws SQLException {
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

	@Test public void testMoreResultInvokedTwice() throws SQLException {
		neo4j.defaultDatabaseService().executeTransactionally(StatementData.STATEMENT_CREATE_TWO_PROPERTIES);
		PreparedStatement statement = connection.prepareStatement(StatementData.STATEMENT_MATCH_ALL_STRING_PARAMETRIC);
		statement.setString(1, "test");
		statement.execute();

		assertTrue(statement.getMoreResults());

		assertFalse(statement.getMoreResults());

		statement.close();
	}
}

