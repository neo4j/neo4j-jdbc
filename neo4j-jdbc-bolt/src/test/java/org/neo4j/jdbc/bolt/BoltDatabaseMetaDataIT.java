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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.Rule;
import org.junit.Test;

/**
 * DatabaseMetaData IT Tests class
 */
public class BoltDatabaseMetaDataIT {

	@Rule public Neo4jBoltRule neo4j = new Neo4jBoltRule();

	@Test public void getDatabaseVersionShouldBeOK() throws SQLException, NoSuchFieldException, IllegalAccessException {
		Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl() + "?noSsl","user","password");

		assertNotNull(connection.getMetaData().getDatabaseProductVersion());
		assertNotEquals(-1, connection.getMetaData().getDatabaseMajorVersion());
		assertNotEquals(-1, connection.getMetaData().getDatabaseMajorVersion());
		assertEquals("user", connection.getMetaData().getUserName());

		connection.close();
	}

	@Test public void getDatabaseLabelsShouldBeOK() throws SQLException, NoSuchFieldException, IllegalAccessException {
		Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl() + "?noSsl","user","password");

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

		connection.close();
	}

	@Test public void classShouldWorkIfTransactionIsAlreadyOpened() throws SQLException {
		Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl() + "?noSsl","user","password");
		connection.setAutoCommit(false);
		connection.getMetaData();

		connection.close();
	}
}
