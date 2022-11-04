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
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.neo4j.jdbc.bolt.BoltNeo4jConnection;
import org.neo4j.jdbc.http.HttpNeo4jConnection;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DriverTestIT {

	@Rule public ExpectedException expectedEx = ExpectedException.none();

	@ClassRule
	public static final Neo4jContainer<?> neo4j = new Neo4jContainer<>(neo4jImageCoordinates()).withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
			.waitingFor(new WaitAllStrategy() // no need to override this once https://github.com/testcontainers/testcontainers-java/issues/4454 is fixed
					.withStrategy(new LogMessageWaitStrategy().withRegEx(".*Bolt enabled on .*:7687\\.\n"))
					.withStrategy(new HttpWaitStrategy().forPort(7474).forStatusCodeMatching(response -> response == HTTP_OK)))
			.withAdminPassword(null);

	@Test public void shouldReturnAHttpConnection() throws SQLException {
		Driver driver = new Driver();
		Connection connection = driver.connect("jdbc:neo4j:" + neo4j.getHttpUrl(), new Properties());
		Assert.assertTrue(connection instanceof HttpNeo4jConnection);
	}
	@Test public void shouldReturnAHttpConnection2() throws SQLException {
		Driver driver = new Driver();
		Connection connection = driver.connect("jdbc:neo4j:" +  neo4j.getHttpUrl(), new Properties());
		Assert.assertTrue(connection instanceof HttpNeo4jConnection);
	}

	@Ignore
	@Test public void shouldReturnAHttpsConnection() throws SQLException {
		Driver driver = new Driver();
		Connection connection = driver.connect("jdbc:neo4j:" + neo4j.getHttpsUrl(), new Properties());
		Assert.assertTrue(connection instanceof HttpNeo4jConnection);
	}

	@Test public void shouldReturnABoltConnection() throws Exception {
		Driver driver = new Driver();
		Properties prop = new Properties();
		prop.setProperty("user","user");
		prop.setProperty("password","password");
		Connection connection = driver.connect("jdbc:neo4j:" + neo4j.getBoltUrl() + "/?nossl", prop);
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

	@Test public void shouldCallTheNextDriverWhenNonNeo4jUrl() throws Exception {
		Driver driver = new Driver();
		DriverManager.registerDriver(driver);
		java.sql.Driver mysqlDriver = mock(java.sql.Driver.class);
		Connection mockConnection = mock(Connection.class);
		when(mysqlDriver.connect(anyString(), any(Properties.class))).thenReturn(mockConnection);
		DriverManager.registerDriver(mysqlDriver);

		DriverManager.getConnection("jdbc:mysql:localhost");

		verify(mysqlDriver, times(1)).connect(anyString(), any(Properties.class));
	}

	// duplicate of neo4j-jdbc-bolt ContainerUtils
	private static String neo4jImageCoordinates() {
		String neo4jVersion = System.getenv("NEO4J_VERSION");
		if (neo4jVersion == null) neo4jVersion = "4.4";
		String enterpriseEdition = System.getenv("NEO4J_ENTERPRISE_EDITION");
		if (enterpriseEdition == null) enterpriseEdition = "false";
		return String.format("neo4j:%s%s", neo4jVersion, Boolean.parseBoolean(enterpriseEdition) ? "-enterprise": "");
	}
}
