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

	/*------------------------------*/
	/*         executeUpdate        */
	/*------------------------------*/

	@Test public void executeUpdateShouldExecuteAndReturnCorrectData() throws SQLException {
		Connection connection = DriverManager.getConnection("jdbc:" + neo4j.httpURI().toString());
		Statement statement = connection.createStatement();
		int lines = statement.executeUpdate("CREATE (n:User {name:\"test\"})");
		assertEquals(1, lines);

		lines = statement.executeUpdate("CREATE (n:User {name:\"test\"})");
		assertEquals(1, lines);

		lines = statement.executeUpdate("MATCH (n:User {name:\"test\"}) DELETE n");
		assertEquals(2, lines);

		connection.close();
	}
}
