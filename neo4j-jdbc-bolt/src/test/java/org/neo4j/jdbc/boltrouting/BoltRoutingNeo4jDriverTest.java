/*
 * Copyright (c) 2018 LARUS Business Automation [http://www.larus-ba.it]
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
 */
package org.neo4j.jdbc.boltrouting;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.jdbc.Neo4jDriver;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.sql.SQLException;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.*;

/**
 * @author AgileLARUS
 * @since 3.3.1
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({GraphDatabase.class, Config.TrustStrategy.class})
public class BoltRoutingNeo4jDriverTest {

	@Rule public ExpectedException expectedEx = ExpectedException.none();

	/*------------------------------*/
	/*          acceptsURL          */
	/*------------------------------*/
	@Test public void shouldAcceptURL() throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException, SQLException {
		Neo4jDriver driver = new BoltRoutingNeo4jDriver();
		assertTrue(driver.acceptsURL("jdbc:neo4j:bolt+routing://localhost:7687"));
		assertTrue(driver.acceptsURL("jdbc:neo4j:bolt+routing://localhost:7687/"));
		assertTrue(driver.acceptsURL("jdbc:neo4j:bolt+routing://192.168.0.1:7687"));
		assertTrue(driver.acceptsURL("jdbc:neo4j:bolt+routing://192.168.0.1:7687/"));
		assertTrue(driver.acceptsURL("jdbc:neo4j:bolt+routing://localhost:7687?region=europe&country=it"));
		assertTrue(driver.acceptsURL("jdbc:neo4j:bolt+routing://localhost:7687/?region=europe&country=it"));
		assertTrue(driver.acceptsURL("jdbc:neo4j:bolt+routing://localhost:7687?noSsl,region=europe&country=it"));
		assertTrue(driver.acceptsURL("jdbc:neo4j:bolt+routing://localhost:7687/?noSsl,region=europe&country=it"));
	}

	@Test public void shoulNotAcceptURL() throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException, SQLException {
		Neo4jDriver driver = new BoltRoutingNeo4jDriver();
		assertFalse(driver.acceptsURL("jdbc:neo4j:http://localhost:7687"));
		assertFalse(driver.acceptsURL("jdbc:file://192.168.0.1:7687"));
		assertFalse(driver.acceptsURL("bolt://localhost:7687"));
	}

	@Test public void shouldThrowException() throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException, SQLException {
		expectedEx.expect(SQLException.class);

		Neo4jDriver driver = new BoltRoutingNeo4jDriver();
		assertFalse(driver.acceptsURL(null));
	}
}
