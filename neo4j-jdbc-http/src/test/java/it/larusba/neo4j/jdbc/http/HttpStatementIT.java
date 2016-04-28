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

import java.sql.*;

import static org.junit.Assert.*;

public class HttpStatementIT extends Neo4jHttpIT {

	@BeforeClass public static void initialize() throws ClassNotFoundException, SQLException {
		Class.forName("it.larusba.neo4j.jdbc.http.HttpDriver");
	}

	/*------------------------------*/
	/*          executeQuery        */
    /*------------------------------*/

	@Test public void executeQueryShouldExecuteAndReturnCorrectData() throws SQLException {
		Connection connection = DriverManager.getConnection("jdbc:" + neo4j.httpURI().toString());
		Statement statement = connection.createStatement();
		ResultSet rs = statement.executeQuery("MATCH (m:Movie { title:\"The Matrix\"}) RETURN m.title");

		assertTrue(rs.next());
		assertEquals("The Matrix", rs.getString(1));
		assertFalse(rs.next());
		connection.close();
	}

	@Test public void executeQueryWithNullResponseValueShouldExecuteAndReturnCorrectData() throws SQLException {
		Connection connection = DriverManager.getConnection("jdbc:" + neo4j.httpURI().toString());
		Statement statement = connection.createStatement();
		ResultSet rs = statement.executeQuery("MATCH (n:Person) RETURN n.title AS title");

		assertTrue(rs.next());
		assertNull(rs.getString("title"));
		assertTrue(rs.wasNull());

		connection.close();
	}

	/*------------------------------*/
	/*         executeUpdate        */
	/*------------------------------*/

	@Test public void executeUpdateShouldExecuteAndReturnCorrectData() throws SQLException {
		Connection connection = DriverManager.getConnection("jdbc:" + neo4j.httpURI().toString());
		Statement statement = connection.createStatement();

		// Node insertion
		int lines = statement.executeUpdate("CREATE (n:User {name:\"test1\"})");
		assertEquals("Stats on node insertion (1) failed", 1, lines);
		lines = statement.executeUpdate("CREATE (n:User {name:\"test2\"})");
		assertEquals("Stats on node insertion (2) failed",1, lines);

		// Relation insertion
		lines = statement.executeUpdate("MATCH (from:User {name:\"test1\"}), (to:User {name:\"test1\"}) CREATE (from)-[:TEST {name:\"test\"}]->(to)");
		assertEquals("Stats on relation insertion failed",1, lines);

		// Deletion
		lines = statement.executeUpdate("MATCH (n:User) DETACH DELETE n");
		assertEquals("Stats on node deletion failed", 3, lines);

		connection.close();
	}

}
