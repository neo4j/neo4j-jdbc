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
 * Created on 08/03/16
 */
package org.neo4j.jdbc.bolt;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.neo4j.harness.junit.rule.Neo4jRule;
import org.neo4j.jdbc.bolt.data.StatementData;
import org.neo4j.jdbc.bolt.utils.JdbcConnectionTestUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltNeo4jResultSetIT {

	@ClassRule
	public static Neo4jRule neo4j = new Neo4jRule();

	@Rule public ExpectedException expectedEx = ExpectedException.none();

	@Before
	public void setUp() throws SQLException {
		JdbcConnectionTestUtils.clearDatabase(neo4j);
	}

	/*------------------------------*/
	/*          flattening          */
	/*------------------------------*/

	@Test public void flatteningNumberWorking() throws SQLException {
		neo4j.defaultDatabaseService().executeTransactionally("CREATE (:User {name:\"name\"})");
		neo4j.defaultDatabaseService().executeTransactionally("CREATE (:User {surname:\"surname\"})");

		Connection conn = JdbcConnectionTestUtils.getConnection(neo4j,",flatten=1");
		Statement stmt = conn.createStatement();

		ResultSet rs = stmt.executeQuery("MATCH (u:User) RETURN u ORDER BY ID(u) ASC;");
		assertEquals(4, rs.getMetaData().getColumnCount());
		assertTrue(rs.next());

		assertEquals(4, rs.findColumn("u.name"));
		assertEquals("name", rs.getString("u.name"));

		JdbcConnectionTestUtils.closeConnection(conn, stmt, rs);
	}

	@Test public void flatteningNumberWorkingMoreRows() throws SQLException {
		neo4j.defaultDatabaseService().executeTransactionally("CREATE (:User {name:\"name\"})");
		neo4j.defaultDatabaseService().executeTransactionally("CREATE (:User {surname:\"surname\"})");

		Connection conn = JdbcConnectionTestUtils.getConnection(neo4j,",flatten=2");
		Statement stmt = conn.createStatement();

		ResultSet rs = stmt.executeQuery("MATCH (u:User) RETURN u ORDER BY ID(u) ASC;");
		assertEquals(5, rs.getMetaData().getColumnCount());
		assertTrue(rs.next());

		assertEquals(4, rs.findColumn("u.name"));
		assertEquals("name", rs.getString("u.name"));

		assertTrue(rs.next());
		assertEquals(5, rs.findColumn("u.surname"));
		assertEquals("surname", rs.getString("u.surname"));

		JdbcConnectionTestUtils.closeConnection(conn, stmt, rs);
	}

	@Test public void flatteningNumberWorkingAllRows() throws SQLException {
		neo4j.defaultDatabaseService().executeTransactionally("CREATE (:User {name:\"name\"})");
		neo4j.defaultDatabaseService().executeTransactionally("CREATE (:User {surname:\"surname\"})");

		Connection conn = JdbcConnectionTestUtils.getConnection(neo4j,",flatten=-1");
		Statement stmt = conn.createStatement();

		ResultSet rs = stmt.executeQuery("MATCH (u:User) RETURN u ORDER BY ID(u) ASC;");
		assertEquals(5, rs.getMetaData().getColumnCount());
		assertTrue(rs.next());

		assertEquals(4, rs.findColumn("u.name"));
		assertEquals("name", rs.getString("u.name"));

		assertTrue(rs.next());
		assertEquals(5, rs.findColumn("u.surname"));
		assertEquals("surname", rs.getString("u.surname"));

		JdbcConnectionTestUtils.closeConnection(conn, stmt, rs);
	}

	@Test public void findColumnShouldWorkWithFlattening() throws SQLException {
		neo4j.defaultDatabaseService().executeTransactionally(StatementData.STATEMENT_CREATE);

		Connection con = JdbcConnectionTestUtils.getConnection(neo4j,",flatten=1");
		Statement stmt = con.createStatement();
		ResultSet rs = stmt.executeQuery(StatementData.STATEMENT_MATCH_NODES);

		assertEquals(4, rs.findColumn("n.name"));

		neo4j.defaultDatabaseService().executeTransactionally(StatementData.STATEMENT_CREATE_REV);

		JdbcConnectionTestUtils.closeConnection(con, stmt, rs);
	}

	@Test public void shouldGetRowReturnValidNumbers() throws SQLException {
		neo4j.defaultDatabaseService().executeTransactionally("unwind range(1,5) as x create (:User{number:x})");

		Connection con = JdbcConnectionTestUtils.getConnection(neo4j);
		Statement stmt = con.createStatement();
		ResultSet rs = stmt.executeQuery("match (u:User) return u.number as number order by number asc");

		while (rs.next()) {
			assertEquals(rs.getRow(), rs.getInt("number"));

		}
		JdbcConnectionTestUtils.closeConnection(con, stmt, rs);
	}

	@Test public void shouldHasntNext() throws SQLException {
		neo4j.defaultDatabaseService().executeTransactionally("unwind range(1,5) as x create (:User{number:x})");

		Connection con = JdbcConnectionTestUtils.getConnection(neo4j);
		Statement stmt = con.createStatement();
		ResultSet rs = stmt.executeQuery("MATCH (x:XXX) RETURN x LIMIT 1");

		assertFalse(rs.next());

		JdbcConnectionTestUtils.closeConnection(con, stmt, rs);
	}

	@Test public void shouldGetRowReturnStringFromNumber() throws SQLException {
		neo4j.defaultDatabaseService().executeTransactionally("CREATE (:Test {intn: 1, floatn: 1.123})");

		Connection con = JdbcConnectionTestUtils.getConnection(neo4j);
		Statement stmt = con.createStatement();
		ResultSet rs = stmt.executeQuery("MATCH (x:Test) RETURN x.intn as i , x.floatn as f");

		rs.next();

		assertEquals(1, rs.getInt("i"));
		assertEquals(1.123, rs.getDouble("f"),0.0001);

		assertEquals("1", rs.getString("i"));
		assertEquals("1.123", rs.getString("f"));

		JdbcConnectionTestUtils.closeConnection(con, stmt, rs);
	}

	@Test public void shouldGetRowReturnStringFromNode() throws SQLException {
		neo4j.defaultDatabaseService().executeTransactionally("CREATE (:Test {intn: 1, floatn: 1.123})");

		Connection con = JdbcConnectionTestUtils.getConnection(neo4j);
		Statement stmt = con.createStatement();
		ResultSet rs = stmt.executeQuery("MATCH (x:Test) RETURN x");

		rs.next();
		//checks the content parts of the json string because the order can change and the id is always different
		String json = rs.getString("x");
		assertTrue(json.startsWith("{"));
		assertTrue(json.endsWith("}"));
		assertTrue(json.contains("\"_labels\":[\"Test\"]"));
		assertTrue(json.contains("\"floatn\":1.123"));
		assertTrue(json.contains("\"intn\":1"));

		JdbcConnectionTestUtils.closeConnection(con, stmt, rs);
	}

	@Test public void shouldGetRowReturnNodeValues() throws SQLException {
		neo4j.defaultDatabaseService().executeTransactionally("CREATE (:Test {intn: 1, floatn: 1.123})");

		Connection con = JdbcConnectionTestUtils.getConnection(neo4j);
		Statement stmt = con.createStatement();
		ResultSet rs = stmt.executeQuery("MATCH (x:Test) RETURN x");

		rs.next();
		Map<String, Object> map = (Map<String, Object>) rs.getObject("x");

		assertEquals(1L, map.get("intn"));
		assertEquals(1.123,(Double) map.get("floatn"),0.0001);

		JdbcConnectionTestUtils.closeConnection(con, stmt, rs);
	}

	@Test public void shouldGetRowReturnStringFromRelationship() throws SQLException {
		neo4j.defaultDatabaseService().executeTransactionally("CREATE (a:Test)-[r:Rel {intn: 1, floatn: 1.123}]->(b:Test)");

		Connection con = JdbcConnectionTestUtils.getConnection(neo4j);
		Statement stmt = con.createStatement();
		ResultSet rs = stmt.executeQuery("MATCH (a:Test)-[r:Rel]->(b:Test) RETURN r");

		rs.next();
		//checks the content parts of the json string because the order can change and the id is always different
		String json = rs.getString("r");
		assertTrue(json.startsWith("{"));
		assertTrue(json.endsWith("}"));
		assertTrue(json.contains("\"_type\":\"Rel\""));
		assertTrue(json.contains("\"floatn\":1.123"));
		assertTrue(json.contains("\"intn\":1"));

		JdbcConnectionTestUtils.closeConnection(con, stmt, rs);
	}

	@Test public void shouldGetRowReturnRelatianValues() throws SQLException {
		neo4j.defaultDatabaseService().executeTransactionally("CREATE (a:Test)-[r:Rel {intn: 1, floatn: 1.123}]->(b:Test)");

		Connection con = JdbcConnectionTestUtils.getConnection(neo4j);
		Statement stmt = con.createStatement();
		ResultSet rs = stmt.executeQuery("MATCH (a:Test)-[r:Rel]->(b:Test) RETURN r");

		rs.next();
		Map<String, Object> map = (Map<String, Object>) rs.getObject("r");

		assertEquals(1L, map.get("intn"));
		assertEquals(1.123,(Double) map.get("floatn"),0.0001);

		JdbcConnectionTestUtils.closeConnection(con, stmt, rs);
	}

}
