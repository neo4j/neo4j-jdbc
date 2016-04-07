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
 * Created on 25/03/16
 */
package it.larusba.neo4j.jdbc.bolt;

import it.larusba.neo4j.jdbc.bolt.data.StatementData;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.sql.*;

import static org.junit.Assert.*;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltPreparedStatementIT {

	@ClassRule public static Neo4jBoltRule neo4j = new Neo4jBoltRule();

	@BeforeClass public static void initialize() throws ClassNotFoundException, SQLException {
		Class.forName("it.larusba.neo4j.jdbc.bolt.BoltDriver");
	}

	/*------------------------------*/
	/*          executeQuery        */
	/*------------------------------*/

	@Test public void executeQueryShouldExecuteAndReturnCorrectData() throws SQLException {
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_TWO_PROPERTIES);
		Connection connection = DriverManager.getConnection("jdbc:" + neo4j.getBoltUrl());
		PreparedStatement statement = connection.prepareStatement(StatementData.STATEMENT_MATCH_ALL_STRING_PARAMETRIC);
		statement.setString(1, "test");
		ResultSet rs = statement.executeQuery();

		assertTrue(rs.next());
		assertEquals("testAgain", rs.getString(1));
		assertFalse(rs.next());
		connection.close();
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_TWO_PROPERTIES_REV);
	}

	/*------------------------------*/
	/*         executeUpdate        */
	/*------------------------------*/
	@Test public void executeUpdateShouldExecuteAndReturnCorrectData() throws SQLException {
		Connection connection = DriverManager.getConnection("jdbc:" + neo4j.getBoltUrl());
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

		connection.close();
	}

	/*------------------------------*/
	/*            execute           */
	/*------------------------------*/
	@Test public void executeShouldExecuteAndReturnTrue() throws SQLException {
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_TWO_PROPERTIES);
		Connection connection = DriverManager.getConnection("jdbc:" + neo4j.getBoltUrl());
		PreparedStatement statement = connection.prepareStatement(StatementData.STATEMENT_MATCH_ALL_STRING_PARAMETRIC);
		statement.setString(1, "test");
		boolean result = statement.execute();
		assertTrue(result);

		connection.close();
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_TWO_PROPERTIES_REV);
	}

	@Test public void executeShouldExecuteAndReturnFalse() throws SQLException {
		Connection connection = DriverManager.getConnection("jdbc:" + neo4j.getBoltUrl());
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

		connection.close();
	}
}

