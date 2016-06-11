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
 * Created on 24/03/16
 */
package org.neo4j.jdbc.bolt;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.neo4j.jdbc.bolt.data.StatementData;

import java.sql.*;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltResultSetMetaDataIT {
	@ClassRule public static Neo4jBoltRule neo4j = new Neo4jBoltRule();

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

		Connection con = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl());

		try (Statement stmt = con.createStatement()) {
			ResultSet rs = stmt.executeQuery(StatementData.STATEMENT_MATCH_NODES_MORE);

			assertEquals(stmt, rs.getStatement());
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
		Connection con = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl());

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
		Connection con = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl());

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

		Connection con = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl());

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

		Connection con = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl());

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

	@Test public void getColumnTypeShouldSucceed() throws SQLException {
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE);

		Connection con = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl());

		try (Statement stmt = con.createStatement()) {
			ResultSet rs = stmt.executeQuery("MATCH (n) return 'a',1,1.0,[1,2,3],{a:1},null,n,n.name");
			while (rs.next()) {
				ResultSetMetaData rsm = rs.getMetaData();

				assertEquals(Types.VARCHAR, rsm.getColumnType(1));
				assertEquals("STRING", rsm.getColumnTypeName(1));
				assertEquals(String.class.getName(), rsm.getColumnClassName(1));

				assertEquals(Types.INTEGER, rsm.getColumnType(2));
				assertEquals("INTEGER", rsm.getColumnTypeName(2));
				assertEquals(Long.class.getName(), rsm.getColumnClassName(2));

				assertEquals(Types.NUMERIC, rsm.getColumnType(3));
				assertEquals("FLOAT", rsm.getColumnTypeName(3));
				assertEquals(Double.class.getName(), rsm.getColumnClassName(3));

				assertEquals(Types.ARRAY, rsm.getColumnType(4));
				assertEquals("LIST OF ANY?", rsm.getColumnTypeName(4));
				assertEquals(List.class.getName(), rsm.getColumnClassName(4));

				assertEquals(Types.JAVA_OBJECT, rsm.getColumnType(5));
				assertEquals("MAP", rsm.getColumnTypeName(5));
				assertEquals(Map.class.getName(), rsm.getColumnClassName(5));
				// If null, it's the default column class, ie string
				assertEquals(Types.VARCHAR, rsm.getColumnType(6));
				assertEquals("STRING", rsm.getColumnTypeName(6));
				assertEquals(String.class.getName(), rsm.getColumnClassName(6));

				assertEquals(Types.JAVA_OBJECT, rsm.getColumnType(7));
				assertEquals("NODE", rsm.getColumnTypeName(7));
				assertEquals(Object.class.getName(), rsm.getColumnClassName(7));

				assertEquals(Types.VARCHAR, rsm.getColumnType(8));
				assertEquals("STRING", rsm.getColumnTypeName(8));
				assertEquals(String.class.getName(), rsm.getColumnClassName(8));
			}
		}
	}
}
