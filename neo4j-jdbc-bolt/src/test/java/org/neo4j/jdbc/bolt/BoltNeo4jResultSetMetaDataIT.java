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
import org.neo4j.driver.internal.types.InternalTypeSystem;
import org.neo4j.jdbc.bolt.data.StatementData;
import org.neo4j.jdbc.bolt.utils.JdbcConnectionTestUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltNeo4jResultSetMetaDataIT {

	//private final static String FLATTEN_URI = "flatten=1";

	Connection connectionFlatten;

	@ClassRule public static Neo4jBoltRule neo4j = new Neo4jBoltRule();


	@Before public void setUp() {
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_TWO_PROPERTIES_REV);
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_TWO_PROPERTIES);
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_TWO_PROPERTIES);
		connectionFlatten = JdbcConnectionTestUtils.verifyConnection(connectionFlatten, neo4j,",flatten=1");
	}

	@After public void tearDown() {
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CLEAR_DB);
	}

	/*------------------------------*/
	/*          flattening          */
	/*------------------------------*/

	@Test public void shouldAddVirtualColumnsOnNodeWithMultipleNodes() throws SQLException {
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_OTHER_TYPE_AND_RELATIONS);

		try (Statement stmt = connectionFlatten.createStatement()) {
			ResultSet rs = stmt.executeQuery(StatementData.STATEMENT_MATCH_NODES_MORE);

			assertEquals(stmt, rs.getStatement());
			ResultSetMetaData rsm = rs.getMetaData();

			assertEquals(9, rsm.getColumnCount());

			List<String> labels = new ArrayList<>();
			labels.add(rsm.getColumnLabel(1));
			labels.add(rsm.getColumnLabel(2));
			labels.add(rsm.getColumnLabel(3));
			labels.add(rsm.getColumnLabel(4));
			labels.add(rsm.getColumnLabel(5));
			labels.add(rsm.getColumnLabel(6));
			labels.add(rsm.getColumnLabel(7));
			labels.add(rsm.getColumnLabel(8));
			labels.add(rsm.getColumnLabel(9));

			assertTrue(labels.contains("n"));
			assertTrue(labels.contains("n.id"));
			assertTrue(labels.contains("n.labels"));
			assertTrue(labels.contains("n.surname"));
			assertTrue(labels.contains("n.name"));
			assertTrue(labels.contains("s"));
			assertTrue(labels.contains("s.id"));
			assertTrue(labels.contains("s.labels"));
			assertTrue(labels.contains("s.status"));

		}

	}

	@Test public void shouldAddVirtualColumnsOnNodeAndPreserveResultSet() throws SQLException {

		try (Statement stmt = connectionFlatten.createStatement()) {
			ResultSet rs = stmt.executeQuery(StatementData.STATEMENT_MATCH_NODES);
			ResultSetMetaData rsm = rs.getMetaData();

			assertEquals(5, rsm.getColumnCount());
			List<String> labels = new ArrayList<>();
			labels.add(rsm.getColumnLabel(1));
			labels.add(rsm.getColumnLabel(2));
			labels.add(rsm.getColumnLabel(3));
			labels.add(rsm.getColumnLabel(4));
			labels.add(rsm.getColumnLabel(5));

			assertTrue(labels.contains("n"));
			assertTrue(labels.contains("n.id"));
			assertTrue(labels.contains("n.labels"));
			assertTrue(labels.contains("n.surname"));
			assertTrue(labels.contains("n.name"));

			assertTrue(rs.next());
			assertEquals("test", ((Map) rs.getObject(1)).get("name"));
		}
	}

	@Test public void shouldNotAddVirtualColumnsOnNodeIfNotOnlyNodes() throws SQLException {

		try (Statement stmt = connectionFlatten.createStatement()) {
			ResultSet rs = stmt.executeQuery(StatementData.STATEMENT_MATCH_MISC);
			ResultSetMetaData rsm = rs.getMetaData();

			assertEquals(2, rsm.getColumnCount());
			assertEquals("n", rsm.getColumnLabel(1));
			assertEquals("n.name", rsm.getColumnLabel(2));
		}
	}

	@Test public void shouldAddVirtualColumnsOnRelationsWithMultipleRelations() throws SQLException {
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_OTHER_TYPE_AND_RELATIONS);

		try (Statement stmt = connectionFlatten.createStatement()) {
			ResultSet rs = stmt.executeQuery(StatementData.STATEMENT_MATCH_RELATIONS);
			ResultSetMetaData rsm = rs.getMetaData();

			int i = 1;

			assertEquals(4, rsm.getColumnCount());
			assertEquals("r", rsm.getColumnLabel(i++));
			assertEquals("r.id", rsm.getColumnLabel(i++));
			assertEquals("r.type", rsm.getColumnLabel(i++));
			assertEquals("r.date", rsm.getColumnLabel(i++));
		}

	}

	@Test public void shouldAddVirtualColumnsOnRelationsAndNodesWithMultiple() throws SQLException {
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_OTHER_TYPE_AND_RELATIONS);

		try (Statement stmt = connectionFlatten.createStatement()) {
			ResultSet rs = stmt.executeQuery(StatementData.STATEMENT_MATCH_NODES_RELATIONS);
			ResultSetMetaData rsm = rs.getMetaData();

			assertEquals(13, rsm.getColumnCount());

			List<String> labels = new ArrayList<>();
			labels.add(rsm.getColumnLabel(1));
			labels.add(rsm.getColumnLabel(2));
			labels.add(rsm.getColumnLabel(3));
			labels.add(rsm.getColumnLabel(4));
			labels.add(rsm.getColumnLabel(5));
			labels.add(rsm.getColumnLabel(6));
			labels.add(rsm.getColumnLabel(7));
			labels.add(rsm.getColumnLabel(8));
			labels.add(rsm.getColumnLabel(9));
			labels.add(rsm.getColumnLabel(10));
			labels.add(rsm.getColumnLabel(11));
			labels.add(rsm.getColumnLabel(12));
			labels.add(rsm.getColumnLabel(13));

			assertTrue(labels.contains("n"));
			assertTrue(labels.contains("n.id"));
			assertTrue(labels.contains("n.labels"));
			assertTrue(labels.contains("n.surname"));
			assertTrue(labels.contains("n.name"));
			assertTrue(labels.contains("s"));
			assertTrue(labels.contains("s.id"));
			assertTrue(labels.contains("s.labels"));
			assertTrue(labels.contains("s.status"));
			assertTrue(labels.contains("r"));
			assertTrue(labels.contains("r.id"));
			assertTrue(labels.contains("r.type"));
			assertTrue(labels.contains("r.date"));
		}

	}

	@Test public void getColumnTypeShouldSucceed() throws SQLException {
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE);

		Connection con = JdbcConnectionTestUtils.getConnection(neo4j);

		try (Statement stmt = con.createStatement()) {
			ResultSet rs = stmt.executeQuery("MATCH (n) return 'a',1,1.0,[1,2,3],{a:1},null,n,n.name");
			while (rs.next()) {
				ResultSetMetaData rsm = rs.getMetaData();

				assertEquals(Types.VARCHAR, rsm.getColumnType(1));
				assertEquals(InternalTypeSystem.TYPE_SYSTEM.STRING().name(), rsm.getColumnTypeName(1));
				assertEquals(String.class.getName(), rsm.getColumnClassName(1));

				assertEquals(Types.INTEGER, rsm.getColumnType(2));
				assertEquals(InternalTypeSystem.TYPE_SYSTEM.INTEGER().name(), rsm.getColumnTypeName(2));
				assertEquals(Long.class.getName(), rsm.getColumnClassName(2));

				assertEquals(Types.FLOAT, rsm.getColumnType(3));
				assertEquals(InternalTypeSystem.TYPE_SYSTEM.FLOAT().name(), rsm.getColumnTypeName(3));
				assertEquals(Double.class.getName(), rsm.getColumnClassName(3));

				assertEquals(Types.ARRAY, rsm.getColumnType(4));
				assertEquals(InternalTypeSystem.TYPE_SYSTEM.LIST().name(), rsm.getColumnTypeName(4));
				assertEquals(Array.class.getName(), rsm.getColumnClassName(4));

				assertEquals(Types.JAVA_OBJECT, rsm.getColumnType(5));
				assertEquals(InternalTypeSystem.TYPE_SYSTEM.MAP().name(), rsm.getColumnTypeName(5));
				assertEquals(Map.class.getName(), rsm.getColumnClassName(5));

				assertEquals(Types.NULL, rsm.getColumnType(6));
				assertEquals(InternalTypeSystem.TYPE_SYSTEM.NULL().name(), rsm.getColumnTypeName(6));
				assertEquals(null, rsm.getColumnClassName(6));

				assertEquals(Types.JAVA_OBJECT, rsm.getColumnType(7));
				assertEquals(InternalTypeSystem.TYPE_SYSTEM.NODE().name(), rsm.getColumnTypeName(7));
				assertEquals(Object.class.getName(), rsm.getColumnClassName(7));

				assertEquals(Types.VARCHAR, rsm.getColumnType(8));
				assertEquals(InternalTypeSystem.TYPE_SYSTEM.STRING().name(), rsm.getColumnTypeName(8));
				assertEquals(String.class.getName(), rsm.getColumnClassName(8));
			}
		}

		con.close();
	}

	@Test public void getColumnTypeNameShouldBeCorrectAfterFlattening() throws SQLException {
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE);
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_OTHER_TYPE_AND_RELATIONS);

		try (Statement stmt = connectionFlatten.createStatement()) {
			ResultSet rs = stmt.executeQuery(StatementData.STATEMENT_MATCH_NODES_RELATIONS);
			rs.next();

			ResultSetMetaData rsm = rs.getMetaData();

			assertEquals(InternalTypeSystem.TYPE_SYSTEM.NODE().name(), rsm.getColumnTypeName(1));
			assertEquals(InternalTypeSystem.TYPE_SYSTEM.INTEGER().name(), rsm.getColumnTypeName(2));
			assertEquals(InternalTypeSystem.TYPE_SYSTEM.LIST().name(), rsm.getColumnTypeName(3));
			assertEquals(InternalTypeSystem.TYPE_SYSTEM.STRING().name(), rsm.getColumnTypeName(4));
			assertEquals(InternalTypeSystem.TYPE_SYSTEM.STRING().name(), rsm.getColumnTypeName(5));
			assertEquals(InternalTypeSystem.TYPE_SYSTEM.RELATIONSHIP().name(), rsm.getColumnTypeName(6));
			assertEquals(InternalTypeSystem.TYPE_SYSTEM.INTEGER().name(), rsm.getColumnTypeName(7));
			assertEquals(InternalTypeSystem.TYPE_SYSTEM.STRING().name(), rsm.getColumnTypeName(8));
			assertEquals(InternalTypeSystem.TYPE_SYSTEM.INTEGER().name(), rsm.getColumnTypeName(9));
			assertEquals(InternalTypeSystem.TYPE_SYSTEM.NODE().name(), rsm.getColumnTypeName(10));
			assertEquals(InternalTypeSystem.TYPE_SYSTEM.INTEGER().name(), rsm.getColumnTypeName(11));
			assertEquals(InternalTypeSystem.TYPE_SYSTEM.LIST().name(), rsm.getColumnTypeName(12));
			assertEquals(InternalTypeSystem.TYPE_SYSTEM.BOOLEAN().name(), rsm.getColumnTypeName(13));
		}

	}

	@Test public void getColumnClassNameShouldBeCorrectAfterFlattening() throws SQLException {

		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE);
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_OTHER_TYPE_AND_RELATIONS);

		try (Statement stmt = connectionFlatten.createStatement()) {
			ResultSet rs = stmt.executeQuery(StatementData.STATEMENT_MATCH_NODES_RELATIONS);
			rs.next();

			ResultSetMetaData rsm = rs.getMetaData();

			assertEquals(Object.class.getName(), rsm.getColumnClassName(1));
			assertEquals(Long.class.getName(), rsm.getColumnClassName(2));
			assertEquals(Array.class.getName(), rsm.getColumnClassName(3));
			assertEquals(String.class.getName(), rsm.getColumnClassName(4));
			assertEquals(String.class.getName(), rsm.getColumnClassName(5));
			assertEquals(Object.class.getName(), rsm.getColumnClassName(6));
			assertEquals(Long.class.getName(), rsm.getColumnClassName(7));
			assertEquals(String.class.getName(), rsm.getColumnClassName(8));
			assertEquals(Long.class.getName(), rsm.getColumnClassName(9));
			assertEquals(Object.class.getName(), rsm.getColumnClassName(10));
			assertEquals(Long.class.getName(), rsm.getColumnClassName(11));
			assertEquals(Array.class.getName(), rsm.getColumnClassName(12));
			assertEquals(Boolean.class.getName(), rsm.getColumnClassName(13));
		}

	}

	@Test public void getColumnsTypeNameShouldWorkWithVariableNumberOfProperties() throws SQLException {

		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE);

		Connection con = JdbcConnectionTestUtils.getConnection(neo4j);

		try (Statement stmt = con.createStatement()) {
			ResultSet rs = stmt.executeQuery(StatementData.STATEMENT_MATCH_NODES);

			while(rs.next()) {
				ResultSetMetaData rsm = rs.getMetaData();
				assertEquals(1, rsm.getColumnCount());
				assertEquals(2000, rsm.getColumnType(1));
				assertEquals("NODE", rsm.getColumnTypeName(1));
			}
		}

		con.close();
	}
}
