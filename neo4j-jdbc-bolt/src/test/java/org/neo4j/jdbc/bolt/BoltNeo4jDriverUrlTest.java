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

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.neo4j.jdbc.Neo4jDriver;
import org.neo4j.jdbc.bolt.utils.Mocker;

import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
@RunWith(Parameterized.class)
public class BoltNeo4jDriverUrlTest {

	@Rule public ExpectedException expectedEx = ExpectedException.none();

	@Parameterized.Parameter
	public String completeValidUrl;

	private static org.neo4j.driver.Driver mockedDriver;

	@Parameterized.Parameters
	public static Iterable<?> data() {
		return Arrays.asList("jdbc:neo4j:bolt://test", "jdbc:neo4j:bolt+s://test", "jdbc:neo4j:bolt+ssc://test");
	}

	@BeforeClass
	public static void initialize() {
		mockedDriver = Mocker.mockDriver();
	}

	@After
	public void prepare() {
	}

	private String getBoltUrl() {
		return completeValidUrl.replace("jdbc:neo4j:", "");
	}

	private String getPrefix() {
		return completeValidUrl.split("://")[0];
	}

	@Test public void shouldConnectCreateConnection() throws SQLException, URISyntaxException {
		Neo4jDriver driver = new BoltDriver((routingUris, config, authToken, info) -> mockedDriver);
		Connection connection = driver.connect(completeValidUrl, null);
		assertNotNull(connection);
	}

	@Test public void shouldAcceptURL() throws SQLException {
		Neo4jDriver driver = new BoltDriver((routingUris, config, authToken, info) -> mockedDriver);
		assertTrue(driver.acceptsURL(getPrefix() + "://localhost:7687"));
		assertTrue(driver.acceptsURL(getPrefix() + "://localhost:7687/"));
		assertTrue(driver.acceptsURL(getPrefix() + "://192.168.0.1:7687"));
		assertTrue(driver.acceptsURL(getPrefix() + "://192.168.0.1:7687/"));
		assertTrue(driver.acceptsURL(getPrefix() + "://localhost:7687?routing:policy=it"));
		assertTrue(driver.acceptsURL(getPrefix() + "://localhost:7687?routing:region=europe&country=it"));
		assertTrue(driver.acceptsURL(getPrefix() + "://localhost:7687/?routing:region=europe&country=it"));
		assertTrue(driver.acceptsURL(getPrefix() + "://localhost:7687?noSsl&routing:region=europe&country=it"));
		assertTrue(driver.acceptsURL(getPrefix() + "://localhost:7687/?noSsl&routing:region=europe&country=it"));
		assertTrue(driver.acceptsURL(getPrefix() + "://localhost:7687,localhost:7688,localhost:7689/?noSsl&routing:region=europe&country=it"));
		assertTrue(driver.acceptsURL(getPrefix() + "://localhost:7687/?noSsl&routing:region=europe&country=it&routing:servers=localhost:7688,localhost:7689"));
	}
}
