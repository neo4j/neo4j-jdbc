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
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.neo4j.jdbc.bolt.data.StatementData;
import org.neo4j.jdbc.bolt.utils.JdbcConnectionTestUtils;
import org.testcontainers.containers.Neo4jContainer;

import java.sql.Array;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.neo4j.driver.internal.types.InternalTypeSystem.TYPE_SYSTEM;
import static org.neo4j.jdbc.bolt.utils.ContainerUtils.neo4jImageCoordinates;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltNeo4jResultSetMetaDataIT {

	static Connection connectionFlatten;

	@ClassRule
	public static final Neo4jContainer<?> neo4j = new Neo4jContainer<>(neo4jImageCoordinates()).withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes").withAdminPassword(null);

	@Before public void setUp() {
		connectionFlatten = JdbcConnectionTestUtils.verifyConnection(connectionFlatten, neo4j,",flatten=1");
	}

	@After public void tearDown() {
		JdbcConnectionTestUtils.clearDatabase(neo4j);
	}

	@AfterClass
	public static void tearDownConnection(){
		JdbcConnectionTestUtils.closeConnection(connectionFlatten);
	}

	/*------------------------------*/
	/*          flattening          */
	/*------------------------------*/

	@Test public void shouldAddVirtualColumnsOnNodeWithMultipleNodes() throws SQLException {
		JdbcConnectionTestUtils.executeTransactionally(neo4j, StatementData.STATEMENT_CREATE_TWO_PROPERTIES);
		JdbcConnectionTestUtils.executeTransactionally(neo4j, StatementData.STATEMENT_CREATE_OTHER_TYPE_AND_RELATIONS);

		try (Statement stmt = connectionFlatten.createStatement()) {
			ResultSet rs = stmt.executeQuery(StatementData.STATEMENT_MATCH_NODES_MORE);

			assertEquals(stmt, rs.getStatement());
			ResultSetMetaData metadata = rs.getMetaData();

			List<String> labels = IntStream.range(1, metadata.getColumnCount() + 1)
					.boxed()
					.map(propagatingException(metadata::getColumnLabel))
					.collect(Collectors.toList());

			assertThat(labels, equalTo(asList(
					"n", "n.id", "n.labels", "n.name", "n.surname",
					"s", "s.id", "s.labels", "s.status"
			)));

		}

	}

	@Test public void shouldAddVirtualColumnsOnNodeAndPreserveResultSet() throws SQLException {
		JdbcConnectionTestUtils.executeTransactionally(neo4j, StatementData.STATEMENT_CREATE_TWO_PROPERTIES);

		try (Statement stmt = connectionFlatten.createStatement()) {
			ResultSet rs = stmt.executeQuery(StatementData.STATEMENT_MATCH_NODES);
			ResultSetMetaData metadata = rs.getMetaData();

			List<String> labels = IntStream.range(1, metadata.getColumnCount() + 1)
					.boxed()
					.map(propagatingException(metadata::getColumnLabel))
					.collect(Collectors.toList());

			assertThat(labels, equalTo(asList("n", "n.id", "n.labels", "n.name", "n.surname")));

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
		JdbcConnectionTestUtils.executeTransactionally(neo4j, StatementData.STATEMENT_CREATE_TWO_PROPERTIES);
		JdbcConnectionTestUtils.executeTransactionally(neo4j, StatementData.STATEMENT_CREATE_OTHER_TYPE_AND_RELATIONS);

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
		JdbcConnectionTestUtils.executeTransactionally(neo4j, StatementData.STATEMENT_CREATE_TWO_PROPERTIES);
		JdbcConnectionTestUtils.executeTransactionally(neo4j, StatementData.STATEMENT_CREATE_OTHER_TYPE_AND_RELATIONS);

		try (Statement stmt = connectionFlatten.createStatement()) {
			ResultSet rs = stmt.executeQuery(StatementData.STATEMENT_MATCH_NODES_RELATIONS);
			ResultSetMetaData metadata = rs.getMetaData();

			List<String> labels = IntStream.range(1, metadata.getColumnCount() + 1)
					.boxed()
					.map(propagatingException(metadata::getColumnLabel))
					.collect(Collectors.toList());

			assertThat(labels, equalTo(asList(
					"n", "n.id", "n.labels", "n.name", "n.surname",
					"r", "r.id", "r.type", "r.date",
					"s", "s.id", "s.labels", "s.status"
			)));
		}

	}

	@Test public void getColumnTypeShouldSucceed() throws SQLException {
		JdbcConnectionTestUtils.executeTransactionally(neo4j, StatementData.STATEMENT_CREATE);

		Connection con = JdbcConnectionTestUtils.getConnection(neo4j);

		try (Statement stmt = con.createStatement()) {
			ResultSet rs = stmt.executeQuery("MATCH (n) return 'a',1,1.0,[1,2,3],{a:1},null,n,n.name");
			while (rs.next()) {
				ResultSetMetaData rsm = rs.getMetaData();

				assertEquals(Types.VARCHAR, rsm.getColumnType(1));
				assertEquals(TYPE_SYSTEM.STRING().name(), rsm.getColumnTypeName(1));
				assertEquals(String.class.getName(), rsm.getColumnClassName(1));

				assertEquals(Types.INTEGER, rsm.getColumnType(2));
				assertEquals(TYPE_SYSTEM.INTEGER().name(), rsm.getColumnTypeName(2));
				assertEquals(Long.class.getName(), rsm.getColumnClassName(2));

				assertEquals(Types.FLOAT, rsm.getColumnType(3));
				assertEquals(TYPE_SYSTEM.FLOAT().name(), rsm.getColumnTypeName(3));
				assertEquals(Double.class.getName(), rsm.getColumnClassName(3));

				assertEquals(Types.ARRAY, rsm.getColumnType(4));
				assertEquals(TYPE_SYSTEM.LIST().name(), rsm.getColumnTypeName(4));
				assertEquals(Array.class.getName(), rsm.getColumnClassName(4));

				assertEquals(Types.JAVA_OBJECT, rsm.getColumnType(5));
				assertEquals(TYPE_SYSTEM.MAP().name(), rsm.getColumnTypeName(5));
				assertEquals(Map.class.getName(), rsm.getColumnClassName(5));

				assertEquals(Types.NULL, rsm.getColumnType(6));
				assertEquals(TYPE_SYSTEM.NULL().name(), rsm.getColumnTypeName(6));
				assertEquals(null, rsm.getColumnClassName(6));

				assertEquals(Types.JAVA_OBJECT, rsm.getColumnType(7));
				assertEquals(TYPE_SYSTEM.NODE().name(), rsm.getColumnTypeName(7));
				assertEquals(Object.class.getName(), rsm.getColumnClassName(7));

				assertEquals(Types.VARCHAR, rsm.getColumnType(8));
				assertEquals(TYPE_SYSTEM.STRING().name(), rsm.getColumnTypeName(8));
				assertEquals(String.class.getName(), rsm.getColumnClassName(8));
			}
		}

		con.close();
	}

	@Test public void getColumnTypeNameShouldBeCorrectAfterFlattening() throws SQLException {
		JdbcConnectionTestUtils.executeTransactionally(neo4j, StatementData.STATEMENT_CREATE);
		JdbcConnectionTestUtils.executeTransactionally(neo4j, StatementData.STATEMENT_CREATE_OTHER_TYPE_AND_RELATIONS);

		try (Statement stmt = connectionFlatten.createStatement()) {
			ResultSet rs = stmt.executeQuery(StatementData.STATEMENT_MATCH_NODES_RELATIONS);
			rs.next();
			ResultSetMetaData metadata = rs.getMetaData();

			List<String> columnTypes = IntStream.range(1, metadata.getColumnCount() + 1)
					.boxed()
					.map(propagatingException(metadata::getColumnTypeName))
					.collect(Collectors.toList());

			assertThat(columnTypes, equalTo(Arrays.asList(
					TYPE_SYSTEM.NODE().name(),
					TYPE_SYSTEM.INTEGER().name(),
					TYPE_SYSTEM.LIST().name(),
					TYPE_SYSTEM.STRING().name(),
					TYPE_SYSTEM.RELATIONSHIP().name(),
					TYPE_SYSTEM.INTEGER().name(),
					TYPE_SYSTEM.STRING().name(),
					TYPE_SYSTEM.INTEGER().name(),
					TYPE_SYSTEM.NODE().name(),
					TYPE_SYSTEM.INTEGER().name(),
					TYPE_SYSTEM.LIST().name(),
					TYPE_SYSTEM.BOOLEAN().name()
			)));
		}

	}

	@Test public void getColumnClassNameShouldBeCorrectAfterFlattening() throws SQLException {
		JdbcConnectionTestUtils.executeTransactionally(neo4j, StatementData.STATEMENT_CREATE);
		JdbcConnectionTestUtils.executeTransactionally(neo4j, StatementData.STATEMENT_CREATE_OTHER_TYPE_AND_RELATIONS);

		try (Statement stmt = connectionFlatten.createStatement()) {
			ResultSet rs = stmt.executeQuery(StatementData.STATEMENT_MATCH_NODES_RELATIONS);
			rs.next();

			ResultSetMetaData metadata = rs.getMetaData();
			List<String> classNames = IntStream.range(1, metadata.getColumnCount() + 1)
					.boxed()
					.map(propagatingException(metadata::getColumnClassName))
					.collect(Collectors.toList());

			assertThat(classNames, equalTo(Arrays.asList(
					Object.class.getName(),
					Long.class.getName(),
					Array.class.getName(),
					String.class.getName(),
					Object.class.getName(),
					Long.class.getName(),
					String.class.getName(),
					Long.class.getName(),
					Object.class.getName(),
					Long.class.getName(),
					Array.class.getName(),
					Boolean.class.getName()
			)));
		}

	}

	@Test public void getColumnsTypeNameShouldWorkWithVariableNumberOfProperties() throws SQLException {

		JdbcConnectionTestUtils.executeTransactionally(neo4j, StatementData.STATEMENT_CREATE);

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

	private static <T> Function<Integer, T> propagatingException(ThrowingFunction<Integer, T, SQLException> throwingFunc) {
		return (i) -> {
			try {
				return throwingFunc.apply(i);
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		};
	}
}

@FunctionalInterface
interface ThrowingFunction<T, R, E extends Exception> {
	R apply(T t) throws E;
}
