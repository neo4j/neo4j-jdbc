/*
 *
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
 * Created on 23/4/2016
 *
 */

package org.neo4j.jdbc;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.neo4j.harness.junit.Neo4jRule;
import org.neo4j.jdbc.bolt.BoltNeo4jConnection;
import org.neo4j.jdbc.http.HttpNeo4jConnection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class DriverTestIT {

	@Rule public ExpectedException expectedEx = ExpectedException.none();

	@ClassRule public static Neo4jRule neo4j = new Neo4jRule();

	@Test public void shouldReturnAHttpConnection() throws SQLException {
		Driver driver = new Driver();
		Connection connection = driver.connect("jdbc:neo4j:http://localhost:7474", new Properties());
		Assert.assertTrue(connection instanceof HttpNeo4jConnection);
	}
	@Test public void shouldReturnAHttpConnection2() throws SQLException {
		Driver driver = new Driver();
		Connection connection = driver.connect("jdbc:neo4j:http:localhost:7474", new Properties());
		Assert.assertTrue(connection instanceof HttpNeo4jConnection);
	}

	@Test public void shouldReturnAHttpsConnection() throws SQLException {
		Driver driver = new Driver();
		Connection connection = driver.connect("jdbc:neo4j:https://localhost", new Properties());
		Assert.assertTrue(connection instanceof HttpNeo4jConnection);
	}

	@Test public void shouldReturnABoltConnection() throws Exception {
		Driver driver = new Driver();
		Properties prop = new Properties();
		prop.setProperty("user","user");
		prop.setProperty("password","password");
		Connection connection = driver.connect("jdbc:neo4j:" + neo4j.boltURI() + "/?nossl", prop);
		Assert.assertTrue(connection instanceof BoltNeo4jConnection);
	}

	@Test public void shouldReturnNullWithBadUrl() throws SQLException {
		Driver driver = new Driver();
		Connection connection = driver.connect("jdbc:mysql:localhost", new Properties());
		Assert.assertNull(connection);
	}

	@Test public void shouldReturnNullWithUrlThatContainsOnlyJDBC() throws SQLException {
		// The driver can't understand that url so  should returns a null connection
		Driver driver = new Driver();
		Connection connection = driver.connect("jdbc:", new Properties());
		Assert.assertNull(connection);
	}

	@Test public void shouldReturnNullWithEmptyUrl() throws SQLException {
		// The driver can't understand that url so  should returns a null connection
		Driver driver = new Driver();
		Connection connection = driver.connect("", new Properties());
		Assert.assertNull(connection);
	}

	@Test public void shouldCallTheNextDriverWhenNonNeo4jUrl() throws SQLException {
		Driver driver = new Driver();
		DriverManager.registerDriver(driver);
		java.sql.Driver mysqlDriver = mock(java.sql.Driver.class);
		Connection mockConnection = mock(Connection.class);
		when(mysqlDriver.connect(anyString(), any(Properties.class))).thenReturn(mockConnection);
		DriverManager.registerDriver(mysqlDriver);

		DriverManager.getConnection("jdbc:mysql:localhost");

		verify(mysqlDriver, times(1)).connect(anyString(), any(Properties.class));
	}

}
