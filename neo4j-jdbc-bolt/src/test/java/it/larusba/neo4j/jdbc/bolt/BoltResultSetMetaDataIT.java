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
 * Created on 24/03/16
 */
package it.larusba.neo4j.jdbc.bolt;

import it.larusba.neo4j.jdbc.bolt.data.StatementData;
import org.junit.*;

import java.sql.*;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltResultSetMetaDataIT {
	@ClassRule public static Neo4jBoltRule neo4j = new Neo4jBoltRule();

	@BeforeClass public static void initialize() throws ClassNotFoundException, SQLException {
		Class.forName("it.larusba.neo4j.jdbc.bolt.BoltDriver");
	}

	@Before public void setUp() {
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_TWO_PROPERTIES);
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_TWO_PROPERTIES);
	}

	@After public void tearDown() {
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_TWO_PROPERTIES_REV);
	}

	/*------------------------------*/
	/*          flattening          */
	/*------------------------------*/

	@Test public void shouldAddVirtualColumnsOnNodeWithMultipleNodes() throws SQLException {
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_OTHER_TYPE_AND_RELATIONS);

		Connection con = DriverManager.getConnection("jdbc:" + neo4j.getBoltUrl());

		try (Statement stmt = con.createStatement()) {
			ResultSet rs = stmt.executeQuery(StatementData.STATEMENT_MATCH_NODES_MORE);
			ResultSetMetaData rsm = rs.getMetaData();

			int i = 1;

			assertEquals(9, rsm.getColumnCount());
			assertEquals("n", rsm.getColumnLabel(i++));
			assertEquals("n.id", rsm.getColumnLabel(i++));
			assertEquals("n.labels", rsm.getColumnLabel(i++));
			assertEquals("n.surname", rsm.getColumnLabel(i++));
			assertEquals("n.name", rsm.getColumnLabel(i++));
			assertEquals("s", rsm.getColumnLabel(i++));
			assertEquals("s.id", rsm.getColumnLabel(i++));
			assertEquals("s.labels", rsm.getColumnLabel(i++));
			assertEquals("s.status", rsm.getColumnLabel(i++));
		}
		con.close();

		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_OTHER_TYPE_AND_RELATIONS_REV);
	}

	@Test public void shouldAddVirtualColumnsOnNodeAndPreserveResultSet() throws SQLException {
		Connection con = DriverManager.getConnection("jdbc:" + neo4j.getBoltUrl());

		try (Statement stmt = con.createStatement()) {
			ResultSet rs = stmt.executeQuery(StatementData.STATEMENT_MATCH_NODES);
			ResultSetMetaData rsm = rs.getMetaData();

			int i = 1;

			assertEquals(5, rsm.getColumnCount());
			assertEquals("n", rsm.getColumnLabel(i++));
			assertEquals("n.id", rsm.getColumnLabel(i++));
			assertEquals("n.labels", rsm.getColumnLabel(i++));
			assertEquals("n.surname", rsm.getColumnLabel(i++));
			assertEquals("n.name", rsm.getColumnLabel(i++));

			assertTrue(rs.next());
			assertEquals("test", ((Map) rs.getObject(1)).get("name"));
		}
		con.close();
	}

	@Test public void shouldNotAddVirtualColumnsOnNodeIfNotOnlyNodes() throws SQLException {
		Connection con = DriverManager.getConnection("jdbc:" + neo4j.getBoltUrl());

		try (Statement stmt = con.createStatement()) {
			ResultSet rs = stmt.executeQuery(StatementData.STATEMENT_MATCH_MISC);
			ResultSetMetaData rsm = rs.getMetaData();

			assertEquals(2, rsm.getColumnCount());
			assertEquals("n", rsm.getColumnLabel(1));
			assertEquals("n.name", rsm.getColumnLabel(2));
		}
		con.close();
	}

	@Test public void shouldAddVirtualColumnsOnRelationsWithMultipleRelations() throws SQLException {
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_OTHER_TYPE_AND_RELATIONS);

		Connection con = DriverManager.getConnection("jdbc:" + neo4j.getBoltUrl());

		try (Statement stmt = con.createStatement()) {
			ResultSet rs = stmt.executeQuery(StatementData.STATEMENT_MATCH_RELATIONS);
			ResultSetMetaData rsm = rs.getMetaData();

			int i = 1;

			assertEquals(4, rsm.getColumnCount());
			assertEquals("r", rsm.getColumnLabel(i++));
			assertEquals("r.id", rsm.getColumnLabel(i++));
			assertEquals("r.type", rsm.getColumnLabel(i++));
			assertEquals("r.date", rsm.getColumnLabel(i++));
		}
		con.close();

		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_OTHER_TYPE_AND_RELATIONS_REV);
	}

	@Test public void shouldAddVirtualColumnsOnRelationsAndNodesWithMultiple() throws SQLException {
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_OTHER_TYPE_AND_RELATIONS);

		Connection con = DriverManager.getConnection("jdbc:" + neo4j.getBoltUrl());

		try (Statement stmt = con.createStatement()) {
			ResultSet rs = stmt.executeQuery(StatementData.STATEMENT_MATCH_NODES_RELATIONS);
			ResultSetMetaData rsm = rs.getMetaData();

			int i = 1;

			assertEquals(13, rsm.getColumnCount());
			assertEquals("n", rsm.getColumnLabel(i++));
			assertEquals("n.id", rsm.getColumnLabel(i++));
			assertEquals("n.labels", rsm.getColumnLabel(i++));
			assertEquals("n.surname", rsm.getColumnLabel(i++));
			assertEquals("n.name", rsm.getColumnLabel(i++));
			assertEquals("r", rsm.getColumnLabel(i++));
			assertEquals("r.id", rsm.getColumnLabel(i++));
			assertEquals("r.type", rsm.getColumnLabel(i++));
			assertEquals("r.date", rsm.getColumnLabel(i++));
			assertEquals("s", rsm.getColumnLabel(i++));
			assertEquals("s.id", rsm.getColumnLabel(i++));
			assertEquals("s.labels", rsm.getColumnLabel(i++));
			assertEquals("s.status", rsm.getColumnLabel(i++));
		}
		con.close();

		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_OTHER_TYPE_AND_RELATIONS_REV);
	}
}
