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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.neo4j.jdbc.bolt.data.StatementData;
import org.neo4j.jdbc.bolt.utils.JdbcConnectionTestUtils;

import java.sql.*;

import static org.junit.Assert.*;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltNeo4jConnectionIT {

	@Rule public Neo4jBoltRule neo4j = new Neo4jBoltRule();  // here we're firing up neo4j with bolt enabled
	@Rule public ExpectedException expectedEx = ExpectedException.none();

	Connection writer;
	Connection reader;
	boolean defaultAutoCommit;

	@Before
	public void setUp() throws SQLException {
		writer = JdbcConnectionTestUtils.verifyConnection(writer,neo4j);
		reader = JdbcConnectionTestUtils.verifyConnection(reader,neo4j);
	}

	@Test public void commitShouldWorkFine() throws SQLException {
		writer.setAutoCommit(false);

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

	}

	@Test public void setAutoCommitShouldCommitFromFalseToTrue() throws SQLException {
		writer.setAutoCommit(false);

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

	}

	@Test public void setAutoCommitShouldWorkAfterMultipleChanges() throws SQLException {

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

		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_REV);
	}

	@Test public void rollbackShouldWorkFine() throws SQLException {
		writer.setAutoCommit(false);
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

	}

	@Test public void autoCommitShouldWorkFine() throws SQLException {

		// Creating a node
		Statement writeStatement = writer.createStatement();
		writeStatement.executeQuery("CREATE (:Person)");
		Statement readStatement = reader.createStatement();
		ResultSet rs = readStatement.executeQuery("MATCH (n) RETURN n");
		assertTrue(rs.next());
		assertNotNull(rs.getObject(1));
		assertFalse(rs.next());
	}

	@Test public void moreStatementsFromOneConnection() throws SQLException {
		writer.setAutoCommit(false);

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

	}

	@Test public void shouldRollbackAnEmptyTransaction() throws SQLException {
		// Connect (autoCommit = false)
		try (Connection connection = JdbcConnectionTestUtils.getConnection(neo4j)) {
			connection.setAutoCommit(false);

			connection.rollback();
		}
	}

	/*------------------------------*/
	/*         getMetaData          */
	/*------------------------------*/

	@Test public void getMetaDataShouldWork() throws SQLException {
		Connection connection = JdbcConnectionTestUtils.getConnection(neo4j);
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

		try (Connection connection = JdbcConnectionTestUtils.getConnection(neo4j)) {
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

	@Ignore
    @Test
	public void multipleRunShouldNotFail() throws Exception {

		for (int i = 0; i < 1000; i++) {
			try (Connection connection = JdbcConnectionTestUtils.getConnection(neo4j)) {
				try (Statement statement = connection.createStatement()) {
					try (ResultSet resultSet = statement.executeQuery("match (n) return count(n) as countOfNodes")) {
						if (resultSet.next()) {
                            resultSet.getObject("countOfNodes");
						}
					}
				}
			}
		}
	}
}
