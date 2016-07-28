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

import org.neo4j.jdbc.bolt.data.StatementData;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.sql.*;

import static org.junit.Assert.*;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltConnectionIT {

	@Rule public Neo4jBoltRule neo4j = new Neo4jBoltRule();  // here we're firing up neo4j with bolt enabled

	private String NEO4J_JDBC_BOLT_URL;

	@Before public void setup() {
		NEO4J_JDBC_BOLT_URL = "jdbc:neo4j:" + neo4j.getBoltUrl();
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
		assertNotNull(connection.getMetaData());
		connection.close();
	}
}
