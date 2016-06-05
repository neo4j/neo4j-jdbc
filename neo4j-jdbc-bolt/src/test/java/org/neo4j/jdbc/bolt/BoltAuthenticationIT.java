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
 * Created on 23/03/16
 */
package org.neo4j.jdbc.bolt;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltAuthenticationIT {

	@Rule public Neo4jBoltRule neo4j = new Neo4jBoltRule(true);  // here we're firing up neo4j with bolt enabled

	private String NEO4J_JDBC_BOLT_URL;

	@Before public void setup() {
		NEO4J_JDBC_BOLT_URL = "jdbc:" + neo4j.getBoltUrl();
	}

	@Test public void shouldAuthenticate() throws SQLException {
		boolean result = false;
		Connection con = DriverManager.getConnection(NEO4J_JDBC_BOLT_URL + ",user=neo4j,password=neo4j");
		assertNotNull(con);
		try (Statement stmt = con.createStatement()) {
			stmt.executeQuery("MATCH (n:User) RETURN n.name");
		} catch (SQLException e) {
			result = e.getMessage().contains("The credentials you provided were valid, but must be changed before you can use this instance.");
		}
		con.close();
		assertTrue(result);
	}

	@Test public void shouldNotAuthenticateBecauseOfABadUserAndPassword() throws SQLException {
		boolean result = false;
		Connection con = DriverManager.getConnection(NEO4J_JDBC_BOLT_URL + ",user=teapot,password=teapot");
		assertNotNull(con);
		try (Statement stmt = con.createStatement()) {
			stmt.executeQuery("MATCH (n:User) RETURN n.name");
		} catch (SQLException e) {
			result = e.getMessage().contains("The client is unauthorized due to authentication failure.");
		}
		con.close();
		assertTrue(result);
	}

	@Test public void shouldNotAuthenticateBecauseOfABadUser() throws SQLException {
		boolean result = false;
		Connection con = DriverManager.getConnection(NEO4J_JDBC_BOLT_URL + ",user=teapot,password=neo4j");
		assertNotNull(con);
		try (Statement stmt = con.createStatement()) {
			stmt.executeQuery("MATCH (n:User) RETURN n.name");
		} catch (SQLException e) {
			result = e.getMessage().contains("The client is unauthorized due to authentication failure.");
		}
		con.close();
		assertTrue(result);
	}

	@Test public void shouldNotAuthenticateBecauseOfABadPassword() throws SQLException {
		boolean result = false;
		Connection con = DriverManager.getConnection(NEO4J_JDBC_BOLT_URL + ",user=neo4j,password=teapot");
		assertNotNull(con);
		try (Statement stmt = con.createStatement()) {
			stmt.executeQuery("MATCH (n:User) RETURN n.name");
		} catch (SQLException e) {
			result = e.getMessage().contains("The client is unauthorized due to authentication failure.");
		}
		con.close();
		assertTrue(result);
	}

	@Test public void shouldNotAuthenticateBecauseNoUserAndPasswordAreProvided() throws SQLException {
		boolean result = false;
		Connection con = DriverManager.getConnection(NEO4J_JDBC_BOLT_URL);
		assertNotNull(con);
		try (Statement stmt = con.createStatement()) {
			stmt.executeQuery("MATCH (n:User) RETURN n.name");
		} catch (SQLException e) {
			result = e.getMessage().contains("Authentication token must contain: 'scheme : basic'");
		}
		con.close();
		assertTrue(result);
	}

	@Test public void shouldNotAuthenticateBecauseNoPasswordIsProvided() throws SQLException {
		boolean result = false;
		Connection con = DriverManager.getConnection(NEO4J_JDBC_BOLT_URL + ",user=neo4j");
		assertNotNull(con);
		try (Statement stmt = con.createStatement()) {
			stmt.executeQuery("MATCH (n:User) RETURN n.name");
		} catch (SQLException e) {
			result = e.getMessage().contains("Authentication token must contain: 'scheme : basic'");
		}
		con.close();
		assertTrue(result);
	}

	@Test public void shouldNotAuthenticateBecauseNoUserIsProvided() throws SQLException {
		boolean result = false;
		Connection con = DriverManager.getConnection(NEO4J_JDBC_BOLT_URL + ",password=neo4j");
		assertNotNull(con);
		try (Statement stmt = con.createStatement()) {
			stmt.executeQuery("MATCH (n:User) RETURN n.name");
		} catch (SQLException e) {
			result = e.getMessage().contains("Authentication token must contain: 'scheme : basic'");
		}
		con.close();
		assertTrue(result);
	}
}
