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

import org.neo4j.jdbc.bolt.BoltConnection;
import org.neo4j.jdbc.http.HttpConnection;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.neo4j.harness.junit.Neo4jRule;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DriverTestIT {

	@Rule public ExpectedException expectedEx = ExpectedException.none();

	@ClassRule public static Neo4jRule neo4j = new Neo4jRule();

	@Test public void shouldReturnAHttpConnection() throws SQLException {
		BaseDriver driver = new Driver();
		Connection connection = driver.connect("jdbc:neo4j:http://localhost:7474", new Properties());
		Assert.assertTrue(connection instanceof HttpConnection);
	}
	@Test public void shouldReturnAHttpConnection2() throws SQLException {
		BaseDriver driver = new Driver();
		Connection connection = driver.connect("jdbc:neo4j:http:localhost:7474", new Properties());
		Assert.assertTrue(connection instanceof HttpConnection);
	}

	@Test public void shouldReturnAHttpsConnection() throws SQLException {
		BaseDriver driver = new Driver();
		Connection connection = driver.connect("jdbc:neo4j:https://localhost", new Properties());
		Assert.assertTrue(connection instanceof HttpConnection);
	}

	@Test public void shouldReturnABoltConnection() throws Exception {
		BaseDriver driver = new Driver();
		Connection connection = driver.connect("jdbc:neo4j:" + neo4j.boltURI() + "/?noSsl", new Properties());
		Assert.assertTrue(connection instanceof BoltConnection);
	}

	@Test public void shouldReturnAnExceptionWithBadUrl() throws SQLException {
		expectedEx.expect(SQLException.class);

		BaseDriver driver = new Driver();
		driver.connect("jdbc:mysql:localhost", new Properties());
	}

	@Test public void shouldReturnAnExceptionWithUrlThatContainsOnlyJDBC() throws SQLException {
		expectedEx.expect(SQLException.class);

		BaseDriver driver = new Driver();
		driver.connect("jdbc:", new Properties());
	}

	@Test public void shouldReturnAnExceptionWithEmptyUrl() throws SQLException {
		expectedEx.expect(SQLException.class);

		BaseDriver driver = new Driver();
		driver.connect("", new Properties());
	}
}
