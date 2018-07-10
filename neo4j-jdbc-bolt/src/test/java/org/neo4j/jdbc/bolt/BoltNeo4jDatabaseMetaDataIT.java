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
 * Created on 17/4/2016
 *
 */
package org.neo4j.jdbc.bolt;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.jdbc.bolt.utils.JdbcConnectionTestUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.*;

/**
 * Neo4jDatabaseMetaData IT Tests class
 */
public class BoltNeo4jDatabaseMetaDataIT {

	@Rule public Neo4jBoltRule neo4j = new Neo4jBoltRule();

	Connection connection;

	@Before public void setUp(){
		connection = JdbcConnectionTestUtils.verifyConnection(connection,neo4j);
	}

	@After
	public void tearDown() throws SQLException {
		JdbcConnectionTestUtils.closeConnection(connection);
	}

	@Test public void getDatabaseVersionShouldBeOK() throws SQLException, NoSuchFieldException, IllegalAccessException {

		assertNotNull(connection.getMetaData().getDatabaseProductVersion());
		assertNotEquals(-1, connection.getMetaData().getDatabaseMajorVersion());
		assertNotEquals(-1, connection.getMetaData().getDatabaseMajorVersion());
		assertEquals("user", connection.getMetaData().getUserName());

	}

	@Test public void getDatabaseLabelsShouldBeOK() throws SQLException, NoSuchFieldException, IllegalAccessException {

		try (Statement statement = connection.createStatement()) {
			statement.execute("create (a:A {one:1, two:2})");
			statement.execute("create (b:B {three:3, four:4})");
		}
		
		ResultSet labels = connection.getMetaData().getTables(null, null, null, null);
		
		assertNotNull(labels);
		assertTrue(labels.next());
		assertEquals("A", labels.getString("TABLE_NAME"));
		assertTrue(labels.next());
		assertEquals("B", labels.getString("TABLE_NAME"));
		assertTrue(!labels.next());

	}

	@Test public void classShouldWorkIfTransactionIsAlreadyOpened() throws SQLException {
		connection.setAutoCommit(false);
		connection.getMetaData();
	}
}
