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
package it.larusba.neo4j.jdbc.http;

import it.larusba.neo4j.jdbc.http.test.Neo4jHttpIT;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.Result;

import java.sql.*;

import static org.junit.Assert.*;

public class HttpPreparedStatementIT extends Neo4jHttpIT {

	/*------------------------------*/
	/*          executeQuery        */
	/*------------------------------*/

	@Test public void executeQueryShouldExecuteAndReturnCorrectData() throws SQLException {
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
	/*          executeUpdate        */
	/*------------------------------*/

	@Test public void executeUpdateShouldExecuteAndReturnCorrectData() throws SQLException {
		Connection connection = DriverManager.getConnection(getJDBCUrl());
		PreparedStatement statement = connection.prepareStatement("CREATE (n:TestExecuteUpdateShouldExecuteAndReturnCorrectData {value:?})");
		statement.setString(1, "AZERTYUIOP");

		assertEquals(1, statement.executeUpdate());
		connection.close();
	}

	/*------------------------------*/
	/*         executeBatch         */
	/*------------------------------*/
	@Test public void executeBatchShouldWork() throws SQLException {
		Connection connection = DriverManager.getConnection("jdbc:" + neo4j.httpURI().toString());
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
		Connection connection = DriverManager.getConnection("jdbc:" + neo4j.httpURI().toString());
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
		Connection connection = DriverManager.getConnection("jdbc:" + neo4j.httpURI().toString());
		PreparedStatement statement = connection.prepareStatement("CREATE (:TestExecuteBatchShouldWorkWithTransaction { name:?, value:?})");
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

		Result res = neo4j.getGraphDatabaseService().execute("MATCH (n:TestExecuteBatchShouldWorkWithTransaction) RETURN count(n) AS total");
		while(res.hasNext()){
			assertEquals(0L, res.next().get("total"));
		}
		assertArrayEquals(new int[]{1, 1, 1}, result);

		connection.commit();
		res = neo4j.getGraphDatabaseService().execute("MATCH (n:TestExecuteBatchShouldWorkWithTransaction) RETURN count(n) AS total");
		while(res.hasNext()){
			assertEquals(3L, res.next().get("total"));
		}

		connection.close();
	}
}

