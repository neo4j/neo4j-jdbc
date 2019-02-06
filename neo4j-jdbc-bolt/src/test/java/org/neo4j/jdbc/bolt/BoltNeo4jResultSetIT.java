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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.neo4j.jdbc.bolt.data.StatementData;

import java.sql.*;

import static org.junit.Assert.*;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltNeo4jResultSetIT {

	@Rule public Neo4jBoltRule neo4j = new Neo4jBoltRule();

	@Rule public ExpectedException expectedEx = ExpectedException.none();

	/*------------------------------*/
	/*          flattening          */
	/*------------------------------*/

	@Test public void flatteningNumberWorking() throws SQLException {
		neo4j.getGraphDatabase().execute("CREATE (:User {name:\"name\"})");
		neo4j.getGraphDatabase().execute("CREATE (:User {surname:\"surname\"})");

		Connection conn = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl() + "?nossl,flatten=1");
		Statement stmt = conn.createStatement();

		ResultSet rs = stmt.executeQuery("MATCH (u:User) RETURN u;");
		assertEquals(4, rs.getMetaData().getColumnCount());
		assertTrue(rs.next());

		assertEquals(4, rs.findColumn("u.name"));
		assertEquals("name", rs.getString("u.name"));

		conn.close();
	}

	@Test public void flatteningNumberWorkingMoreRows() throws SQLException {
		neo4j.getGraphDatabase().execute("CREATE (:User {name:\"name\"})");
		neo4j.getGraphDatabase().execute("CREATE (:User {surname:\"surname\"})");

		Connection conn = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl() + "?nossl,flatten=2");
		Statement stmt = conn.createStatement();

		ResultSet rs = stmt.executeQuery("MATCH (u:User) RETURN u;");
		assertEquals(5, rs.getMetaData().getColumnCount());
		assertTrue(rs.next());

		assertEquals(4, rs.findColumn("u.name"));
		assertEquals("name", rs.getString("u.name"));

		assertTrue(rs.next());
		assertEquals(5, rs.findColumn("u.surname"));
		assertEquals("surname", rs.getString("u.surname"));

		conn.close();
	}

	@Test public void flatteningNumberWorkingAllRows() throws SQLException {
		neo4j.getGraphDatabase().execute("CREATE (:User {name:\"name\"})");
		neo4j.getGraphDatabase().execute("CREATE (:User {surname:\"surname\"})");

		Connection conn = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl() + "?nossl,flatten=-1");
		Statement stmt = conn.createStatement();

		ResultSet rs = stmt.executeQuery("MATCH (u:User) RETURN u;");
		assertEquals(5, rs.getMetaData().getColumnCount());
		assertTrue(rs.next());

		assertEquals(4, rs.findColumn("u.name"));
		assertEquals("name", rs.getString("u.name"));

		assertTrue(rs.next());
		assertEquals(5, rs.findColumn("u.surname"));
		assertEquals("surname", rs.getString("u.surname"));

		conn.close();
	}

	@Test public void findColumnShouldWorkWithFlattening() throws SQLException {
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE);

		Connection con = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl() + "?nossl,flatten=1");
		Statement stmt = con.createStatement();
		ResultSet rs = stmt.executeQuery(StatementData.STATEMENT_MATCH_NODES);

		assertEquals(4, rs.findColumn("n.name"));

		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_REV);

		con.close();
	}

	@Test public void shouldGetRowReturnValidNumbers() throws SQLException {
		neo4j.getGraphDatabase().execute("unwind range(1,5) as x create (:User{number:x})");

		Connection con = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl() + "?nossl");
		Statement stmt = con.createStatement();
		ResultSet rs = stmt.executeQuery("match (u:User) return u.number as number order by number asc");

		while (rs.next()) {
			assertEquals(rs.getRow(), rs.getInt("number"));

		}
		con.close();
	}

	@Test public void shouldNotHaveNext() throws SQLException {
		neo4j.getGraphDatabase().execute("unwind range(1,5) as x create (:User{number:x})");

		Connection con = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl() + "?nossl");
		Statement stmt = con.createStatement();
		ResultSet rs = stmt.executeQuery("MATCH (x:XXX) RETURN x LIMIT 1");

		assertFalse(rs.next());

		rs.close();

		con.close();
	}

	@Test public void shouldManageBacktick() throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl() + "?nossl,flatten=-1");
		Statement stmt = conn.createStatement();
		stmt.executeUpdate("create (p:`Person Ext`{name: 'Andrea', age: 80, `foo bar`: 'foo bar', foo_bar: 123, `@`: '@'})" +
				"-[:`KNOWS WHO`{since: 1933, `bar foo`:'bar foo'}]->" +
				"(p1:Person{name: 'Michael', age: 80, `foo bar`: 'foo bar'})");

		ResultSet rs = stmt.executeQuery("MATCH (p:Person) RETURN p;");
		assertEquals(6, rs.getMetaData().getColumnCount());
		assertTrue(rs.next());
		assertEquals("Michael", rs.getString("p.name"));
		assertEquals(80L, rs.getLong("p.age"));
		assertEquals("foo bar", rs.getString("p.`foo bar`"));
		assertArrayEquals(new String[] { "Person" }, (String[]) rs.getArray("p.labels").getArray());
		assertFalse(rs.next());

		rs = stmt.executeQuery("MATCH (p:`Person Ext`) RETURN p;");
		assertEquals(8, rs.getMetaData().getColumnCount());
		assertTrue(rs.next());
		assertEquals("Andrea", rs.getString("p.name"));
		assertEquals(80L, rs.getLong("p.age"));
		assertEquals("foo bar", rs.getString("p.`foo bar`"));
		assertEquals(123L, rs.getLong("p.foo_bar"));
		assertEquals("@", rs.getString("p.`@`"));
		assertArrayEquals(new String[] { "Person Ext" }, (String[]) rs.getArray("p.labels").getArray());
		assertFalse(rs.next());


		rs = stmt.executeQuery("MATCH (p:`Person Ext`)-[k:`KNOWS WHO`]->(p1:Person) RETURN k;");
		assertEquals(5, rs.getMetaData().getColumnCount());
		assertTrue(rs.next());
		assertEquals(1933L, rs.getLong("k.since"));
		assertEquals("bar foo", rs.getString("k.`bar foo`"));
		assertEquals("KNOWS WHO", rs.getString("k.type"));
		assertFalse(rs.next());

		conn.close();
	}
}
