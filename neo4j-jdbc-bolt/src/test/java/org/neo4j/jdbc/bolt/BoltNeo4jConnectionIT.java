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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.neo4j.jdbc.bolt.data.StatementData;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltNeo4jConnectionIT {

	@Rule public Neo4jBoltRule neo4j = new Neo4jBoltRule();  // here we're firing up neo4j with bolt enabled
	@Rule public ExpectedException expectedEx = ExpectedException.none();

	private String NEO4J_JDBC_BOLT_URL;

	@Before public void setup() {
		NEO4J_JDBC_BOLT_URL = "jdbc:neo4j:" + neo4j.getBoltUrl() + "?nossl";
	}

	@Test public void commitShouldWorkFine() throws SQLException {
		// Connect (autoCommit = false)
		Connection writer = DriverManager.getConnection(NEO4J_JDBC_BOLT_URL);
		writer.setAutoCommit(false);

		Connection reader = DriverManager.getConnection(NEO4J_JDBC_BOLT_URL);

		// Creating a node with a transaction
		try (Statement stmt = writer.createStatement()) {
			stmt.executeQuery("CREATE (:CommitShouldWorkFine{result:\"ok\"})");

			Statement stmtRead = reader.createStatement();
			ResultSet rs = stmtRead.executeQuery("MATCH (n:CommitShouldWorkFine) RETURN n.result");
			assertFalse(rs.next());

			writer.commit();
			rs = stmtRead.executeQuery("MATCH (n:CommitShouldWorkFine) RETURN n.result");
			assertTrue(rs.next());
			assertEquals("ok", rs.getString("n.result"));
			assertFalse(rs.next());
		}

		writer.close();
		reader.close();
	}

	@Test public void setAutoCommitShouldCommitFromFalseToTrue() throws SQLException {
		// Connect (autoCommit = false)
		Connection writer = DriverManager.getConnection(NEO4J_JDBC_BOLT_URL);
		writer.setAutoCommit(false);
		Connection reader = DriverManager.getConnection(NEO4J_JDBC_BOLT_URL);

		// Creating a node with a transaction
		try (Statement stmt = writer.createStatement()) {
			stmt.executeQuery("CREATE (:SetAutoCommitSwitch{result:\"ok\"})");

			Statement stmtRead = reader.createStatement();
			ResultSet rs = stmtRead.executeQuery("MATCH (n:SetAutoCommitSwitch) RETURN n.result");
			assertFalse(rs.next());

			writer.setAutoCommit(true);
			rs = stmtRead.executeQuery("MATCH (n:SetAutoCommitSwitch) RETURN n.result");
			assertTrue(rs.next());
			assertEquals("ok", rs.getString("n.result"));
			assertFalse(rs.next());
		}

		writer.close();
		reader.close();
	}

	@Test public void setAutoCommitShouldWorkAfterMultipleChanges() throws SQLException {
		Connection writer = DriverManager.getConnection(NEO4J_JDBC_BOLT_URL);
		Connection reader = DriverManager.getConnection(NEO4J_JDBC_BOLT_URL);

		Statement writerStmt = writer.createStatement();
		writerStmt.executeQuery(StatementData.STATEMENT_CREATE);
		Statement readerStmt = reader.createStatement();
		ResultSet rs = readerStmt.executeQuery(StatementData.STATEMENT_COUNT_NODES);
		//Expect to read data
		assertTrue(rs.next());
		assertEquals(1, rs.getInt(1));

		//Set autocommit to false
		writer.setAutoCommit(false);
		writerStmt.executeQuery(StatementData.STATEMENT_CREATE);
		rs = readerStmt.executeQuery(StatementData.STATEMENT_COUNT_NODES);
		//Expect not to find new node
		assertTrue(rs.next());
		assertEquals(1, rs.getInt(1));
		writer.commit();
		rs = readerStmt.executeQuery(StatementData.STATEMENT_COUNT_NODES);
		//Expect to find 2 nodes
		assertTrue(rs.next());
		assertEquals(2, rs.getInt(1));

		//Set autocommit to true again
		writer.setAutoCommit(true);
		writerStmt.executeQuery(StatementData.STATEMENT_CREATE);
		rs = readerStmt.executeQuery(StatementData.STATEMENT_COUNT_NODES);
		//Expect to find 3 nodes
		assertTrue(rs.next());
		assertEquals(3, rs.getInt(1));

		writer.close();
		reader.close();

		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_REV);
	}

	@Test public void rollbackShouldWorkFine() throws SQLException {
		// Connect (autoCommit = false)
		Connection writer = DriverManager.getConnection(NEO4J_JDBC_BOLT_URL);
		writer.setAutoCommit(false);
		Connection reader = DriverManager.getConnection(NEO4J_JDBC_BOLT_URL);
		// Creating a node with a transaction
		Statement stmt = writer.createStatement();
		stmt.executeQuery("CREATE (:RollbackShouldWorkFine{result:\"ok\"})");

		Statement stmtRead = reader.createStatement();
		ResultSet rs = stmtRead.executeQuery("MATCH (n:RollbackShouldWorkFine) RETURN n.result");
		assertFalse(rs.next());

		writer.rollback();
		rs = stmtRead.executeQuery("MATCH (n:RollbackShouldWorkFine) RETURN n.result");
		assertFalse(rs.next());
		assertTrue(true);

		writer.close();
		reader.close();
	}

	@Test public void autoCommitShouldWorkFine() throws SQLException {
		// Connect (autoCommit = true, by default)
		Connection writer = DriverManager.getConnection(NEO4J_JDBC_BOLT_URL);
		Connection reader = DriverManager.getConnection(NEO4J_JDBC_BOLT_URL);

		// Creating a node
		Statement writeStatement = writer.createStatement();
		writeStatement.executeQuery("CREATE (:Person)");
		Statement readStatement = reader.createStatement();
		ResultSet rs = readStatement.executeQuery("MATCH (n) RETURN n");
		assertTrue(rs.next());
		assertNotNull(rs.getObject(1));
		assertFalse(rs.next());

		writer.close();
		reader.close();
	}

	@Test public void moreStatementsFromOneConnection() throws SQLException {
		Connection writer = DriverManager.getConnection(NEO4J_JDBC_BOLT_URL);
		writer.setAutoCommit(false);
		Connection reader = DriverManager.getConnection(NEO4J_JDBC_BOLT_URL);

		Statement statOne = writer.createStatement();
		Statement statTwo = writer.createStatement();

		//TODO use executeUpdate
		statOne.executeQuery("CREATE (:User {name:\"username\"})");
		statTwo.executeQuery("CREATE (:Company {name:\"companyname\"})");

		Statement statReader = reader.createStatement();
		ResultSet rs = statReader.executeQuery("MATCH (n) RETURN n.name");

		assertFalse(rs.next());

		writer.commit();
		rs = statReader.executeQuery("MATCH (n) RETURN n.name");

		assertTrue(rs.next());
		assertEquals("username", rs.getString(1));
		assertTrue(rs.next());
		assertEquals("companyname", rs.getString(1));
		assertFalse(rs.next());

		writer.close();
		reader.close();
	}

	@Test public void shouldRollbackAnEmptyTransaction() throws SQLException {
		// Connect (autoCommit = false)
		try (Connection connection = DriverManager.getConnection(NEO4J_JDBC_BOLT_URL)) {
			connection.setAutoCommit(false);

			connection.rollback();
		}
	}

	/*------------------------------*/
	/*         getMetaData          */
	/*------------------------------*/

	@Test public void getMetaDataShouldWork() throws SQLException {
		Connection connection = DriverManager.getConnection(NEO4J_JDBC_BOLT_URL);
		DatabaseMetaData metaData = connection.getMetaData();
		assertNotNull(metaData);
		ResultSet resultSet = metaData.getColumns(null, null, null, null);
		while (resultSet.next()) {
			System.out.print(resultSet.getString(1) + " | ");
			System.out.print(resultSet.getString(2) + " | ");
			System.out.print(resultSet.getString(3) + " | ");
			System.out.print(resultSet.getString(4) + " | ");
			System.out.print(resultSet.getString(5) + " | ");
			System.out.println();
		}
		connection.close();
	}

	@Test public void killingQueryThreadExecutionShouldNotInvalidateTheConnection() throws SQLException {

		try (Connection connection = DriverManager.getConnection(NEO4J_JDBC_BOLT_URL)) {
			assertFalse(connection.isClosed());
			assertTrue(connection.isValid(0));
			
			Thread t = new Thread() {
				public void run() {
					try (Statement statement = connection.createStatement()) {
						statement.executeQuery("WITH ['Michael','Stefan','Alberto','Marco','Gianmarco','Benoit','Frank'] AS names FOREACH (r IN range(0,10000000) | CREATE (:User {id:r, name:names[r % size(names)]+' '+r}));");
					}
					catch (SQLException sqle) {
					}
				}
			};

			t.start();
			t.interrupt();
			while (t.isAlive()) {
			}

			assertFalse(connection.isClosed());
			assertTrue(connection.isValid(0));
			
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery("RETURN 1")) {
					assertTrue(resultSet.next());
					assertEquals(1, resultSet.getLong(1));
				}
			}
		}
	}
}
