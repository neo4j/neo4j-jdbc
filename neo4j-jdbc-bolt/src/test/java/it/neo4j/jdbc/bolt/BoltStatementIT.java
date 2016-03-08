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
 * Created on 08/03/16
 */
package it.neo4j.jdbc.bolt;

import it.neo4j.jdbc.bolt.data.StatementData;
import org.junit.*;

import java.sql.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltStatementIT {

	@ClassRule public static Neo4jBoltRule neo4j = new Neo4jBoltRule();

	@BeforeClass public static void initialize() throws ClassNotFoundException, SQLException {
		Class.forName("it.neo4j.jdbc.bolt.BoltDriver");
	}

	/*------------------------------*/
	/*          executeQuery        */
	/*------------------------------*/

	@Test public void executeQueryShouldExecuteAndReturnCorrectData() throws SQLException {
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE);
		Connection connection = DriverManager.getConnection("jdbc:" + neo4j.getBoltUrl());
		Statement statement = connection.createStatement();
		ResultSet rs = statement.executeQuery(StatementData.STATEMENT_MATCH_ALL_STRING);

		assertTrue(rs.next());
		assertEquals("test", rs.getString(1));
		assertFalse(rs.next());
		connection.close();
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_REV);
	}

	/*------------------------------*/
	/*         executeUpdate        */
	/*------------------------------*/

	@Ignore @Test public void executeUpdateShouldExecuteAndReturnCorrectData() throws SQLException {
		Connection connection = DriverManager.getConnection("jdbc:" + neo4j.getBoltUrl());
		Statement statement = connection.createStatement();
		int lines = statement.executeUpdate(StatementData.STATEMENT_CREATE);
		assertEquals(1, lines);

		lines = statement.executeUpdate(StatementData.STATEMENT_CREATE);
		assertEquals(1, lines);

		lines = statement.executeUpdate(StatementData.STATEMENT_CREATE_REV);
		assertEquals(2, lines);

		connection.close();
	}
}
