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
 * Created on 23/02/16
 */
package org.neo4j.jdbc.bolt;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.neo4j.driver.internal.NetworkSession;
import org.neo4j.driver.internal.logging.DevNullLogging;
import org.neo4j.driver.internal.spi.ConnectionProvider;
import org.neo4j.driver.v1.AccessMode;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.jdbc.Neo4jDriver;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.sql.Connection;
import java.sql.SQLException;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.*;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
@RunWith(PowerMockRunner.class) @PrepareForTest(GraphDatabase.class) public class BoltNeo4jDriverTest {

	@Rule public ExpectedException expectedEx = ExpectedException.none();

	private static org.neo4j.driver.v1.Driver mockedDriver;

	@BeforeClass public static void initialize() {
		mockedDriver = mock(org.neo4j.driver.v1.Driver.class);
		ConnectionProvider connectionProvider = mock(ConnectionProvider.class, RETURNS_MOCKS);
		Mockito.when(mockedDriver.session()).thenReturn(new NetworkSession(connectionProvider, AccessMode.READ,null, DevNullLogging.DEV_NULL_LOGGING));
	}

	/*------------------------------*/
	/*           connect            */
	/*------------------------------*/
	//WARNING!! NOT COMPLETE TEST!! Needs tests for parameters

	@Test public void shouldConnectCreateConnection() throws SQLException {
		PowerMockito.mockStatic(GraphDatabase.class);
		Mockito.when(GraphDatabase.driver(Mockito.eq("bolt://test"), Mockito.eq(AuthTokens.none()),Mockito.any(Config.class))).thenReturn(mockedDriver);

		Neo4jDriver driver = new BoltNeo4jDriver();
		Connection connection = driver.connect("jdbc:neo4j:bolt://test", null);
		assertNotNull(connection);
	}

	@Test public void shouldConnectReturnNullIfUrlNotValid() throws SQLException {
		Neo4jDriver driver = new BoltNeo4jDriver();
		assertNull(driver.connect("jdbc:neo4j:http://localhost:7474", null));
		assertNull(driver.connect("bolt://localhost:7474", null));
		assertNull(driver.connect("jdbcbolt://localhost:7474", null));
		assertNull(driver.connect("jdbc:mysql://localhost:3306/sakila", null));
	}

	@Test public void shouldConnectThrowExceptionOnNullURL() throws SQLException {
		expectedEx.expect(SQLException.class);

		Neo4jDriver driver = new BoltNeo4jDriver();
		driver.connect(null, null);
	}

	@Test public void shouldConnectThrowExceptionOnConnectionFailed() throws SQLException {
		expectedEx.expect(SQLException.class);

		Neo4jDriver driver = new BoltNeo4jDriver();
		driver.connect("jdbc:neo4j:bolt://somehost:9999", null);
	}

	/*------------------------------*/
	/*          acceptsURL          */
	/*------------------------------*/
	@Test public void shouldAcceptURLOK() throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException, SQLException {
		Neo4jDriver driver = new BoltNeo4jDriver();
		assertTrue(driver.acceptsURL("jdbc:neo4j:bolt://localhost:7474"));
		assertTrue(driver.acceptsURL("jdbc:neo4j:bolt://192.168.0.1:7474"));
		assertTrue(driver.acceptsURL("jdbc:neo4j:bolt://localhost:8080"));
	}

	@Test public void shouldAcceptURLKO() throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException, SQLException {
		Neo4jDriver driver = new BoltNeo4jDriver();
		assertFalse(driver.acceptsURL("jdbc:neo4j:http://localhost:7474"));
		assertFalse(driver.acceptsURL("jdbc:file://192.168.0.1:7474"));
		assertFalse(driver.acceptsURL("bolt://localhost:7474"));
	}

	@Test public void shouldThrowException() throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException, SQLException {
		expectedEx.expect(SQLException.class);

		Neo4jDriver driver = new BoltNeo4jDriver();
		assertFalse(driver.acceptsURL(null));
	}
}
