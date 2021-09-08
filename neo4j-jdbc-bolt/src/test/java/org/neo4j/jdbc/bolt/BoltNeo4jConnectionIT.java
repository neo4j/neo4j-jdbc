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
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.jdbc.bolt.data.StatementData;
import org.neo4j.jdbc.bolt.impl.BoltNeo4jDriverImpl;
import org.neo4j.jdbc.bolt.utils.JdbcConnectionTestUtils;
import org.testcontainers.containers.Neo4jContainer;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.neo4j.jdbc.bolt.utils.Neo4jContainerUtils.createNeo4jContainer;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltNeo4jConnectionIT {

	@ClassRule
	public static final Neo4jContainer<?> neo4j = createNeo4jContainer();
	@Rule public ExpectedException expectedEx = ExpectedException.none();

	Connection writer;
	Connection reader;

	@Before
	public void setUp() throws SQLException {
		JdbcConnectionTestUtils.clearDatabase(neo4j);
		writer = JdbcConnectionTestUtils.verifyConnection(writer,neo4j);
		reader = JdbcConnectionTestUtils.verifyConnection(reader,neo4j);
	}

	@After
	public void tearDown() throws SQLException {
		JdbcConnectionTestUtils.closeConnection(writer);
		JdbcConnectionTestUtils.closeConnection(reader);
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
		JdbcConnectionTestUtils.closeResultSet(rs);

		//Set autocommit to false
		writer.setAutoCommit(false);
		writerStmt.executeQuery(StatementData.STATEMENT_CREATE);
		rs = readerStmt.executeQuery(StatementData.STATEMENT_COUNT_NODES);
		//Expect not to find new node
		assertTrue(rs.next());
		assertEquals(1, rs.getInt(1));
		JdbcConnectionTestUtils.closeResultSet(rs);

		writer.commit();
		rs = readerStmt.executeQuery(StatementData.STATEMENT_COUNT_NODES);
		//Expect to find 2 nodes
		assertTrue(rs.next());
		assertEquals(2, rs.getInt(1));
		JdbcConnectionTestUtils.closeResultSet(rs);

		//Set autocommit to true again
		writer.setAutoCommit(true);
		writerStmt.executeQuery(StatementData.STATEMENT_CREATE);
		rs = readerStmt.executeQuery(StatementData.STATEMENT_COUNT_NODES);
		//Expect to find 3 nodes
		assertTrue(rs.next());
		assertEquals(3, rs.getInt(1));

		JdbcConnectionTestUtils.closeResultSet(rs);
		JdbcConnectionTestUtils.closeStatement(readerStmt);
		JdbcConnectionTestUtils.closeStatement(writerStmt);

		JdbcConnectionTestUtils.executeTransactionally(neo4j, StatementData.STATEMENT_CREATE_REV);
	}

	@Test public void rollbackShouldWorkFine() throws SQLException {
		writer.setAutoCommit(false);
		// Creating a node with a transaction
		Statement stmt = writer.createStatement();
		ResultSet rs = stmt.executeQuery("CREATE (:RollbackShouldWorkFine{result:\"ok\"})");
		JdbcConnectionTestUtils.closeResultSet(rs);

		Statement stmtRead = reader.createStatement();
		rs = stmtRead.executeQuery("MATCH (n:RollbackShouldWorkFine) RETURN n.result");
		assertFalse(rs.next());
		JdbcConnectionTestUtils.closeResultSet(rs);

		writer.rollback();
		rs = stmtRead.executeQuery("MATCH (n:RollbackShouldWorkFine) RETURN n.result");
		assertFalse(rs.next());
		assertTrue(true);

		JdbcConnectionTestUtils.closeResultSet(rs);
		JdbcConnectionTestUtils.closeStatement(stmt);
		JdbcConnectionTestUtils.closeStatement(stmtRead);
	}

	@Test public void autoCommitShouldWorkFine() throws SQLException {

		// Creating a node
		Statement writeStatement = writer.createStatement();
		ResultSet rs = writeStatement.executeQuery("CREATE (:Person)");
		JdbcConnectionTestUtils.closeResultSet(rs);

		Statement readStatement = reader.createStatement();
		rs = readStatement.executeQuery("MATCH (n) RETURN n");

		assertTrue(rs.next());
		assertNotNull(rs.getObject(1));
		assertFalse(rs.next());

		JdbcConnectionTestUtils.closeResultSet(rs);
		JdbcConnectionTestUtils.closeStatement(readStatement);
		JdbcConnectionTestUtils.closeStatement(writeStatement);
	}

	@Test public void moreStatementsFromOneConnection() throws SQLException {
		writer.setAutoCommit(false);

		Statement statOne = writer.createStatement();
		Statement statTwo = writer.createStatement();

		//TODO use executeUpdate
		ResultSet rs = statOne.executeQuery("CREATE (:User {name:\"username\"})");
		JdbcConnectionTestUtils.closeResultSet(rs);

		rs =statTwo.executeQuery("CREATE (:Company {name:\"companyname\"})");
		JdbcConnectionTestUtils.closeResultSet(rs);

		Statement statReader = reader.createStatement();
		rs = statReader.executeQuery("MATCH (n) RETURN n.name AS name ORDER BY name desc");

		assertFalse(rs.next());

		writer.commit();

		JdbcConnectionTestUtils.closeResultSet(rs);

		rs = statReader.executeQuery("MATCH (n) RETURN n.name AS name ORDER BY name desc");

		assertTrue(rs.next());
		assertEquals("username", rs.getString(1));
		assertTrue(rs.next());
		assertEquals("companyname", rs.getString(1));
		assertFalse(rs.next());

		JdbcConnectionTestUtils.closeResultSet(rs);
		JdbcConnectionTestUtils.closeStatement(statOne);
		JdbcConnectionTestUtils.closeStatement(statTwo);
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
		JdbcConnectionTestUtils.closeConnection(connection, null, resultSet);
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
	@Test public void shouldManageAutocommitParameter() throws SQLException, URISyntaxException {
		try (Connection connection = JdbcConnectionTestUtils.getConnection(neo4j)) {
			assertTrue("default is true", connection.getAutoCommit());
		}
		try (Connection connection = JdbcConnectionTestUtils.getConnection(neo4j, "&autocommit=false")) {
			assertFalse("we defined false but it is not", connection.getAutoCommit());
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

    @Test
    public void closesDriverWhenConnectivityTestFails() {
        CustomTestBoltDriver testJdbcDriver = new CustomTestBoltDriver();

        assertConnectionFails(testJdbcDriver, "jdbc:neo4j:bolt://example.com");
        assertBoltDriverIsClosed(testJdbcDriver.getDriver());
    }

    private void assertConnectionFails(CustomTestBoltDriver testJdbcDriver, String uri) {
        try {
            testJdbcDriver.connect(uri, new Properties());
            Assert.fail("Connection should fail");
        } catch (Exception e) {
            assertThat(e, instanceOf(SQLException.class));
            assertThat(e.getMessage(), containsString("Unable to connect to example.com:7687"));
        }
    }

    private void assertBoltDriverIsClosed(Driver driver) {
        try (Session ignore = driver.session()) {
            Assert.fail("Driver should be closed");
        } catch (Exception e) {
            assertThat(e, instanceOf(IllegalStateException.class));
            assertEquals("This driver instance has already been closed", e.getMessage());
        }
    }

    static class CustomTestBoltDriver extends BoltNeo4jDriverImpl {

        private Driver driver;

        protected CustomTestBoltDriver() {
            super("bolt");
        }

        @Override
        protected Driver getDriver(List<URI> routingUris, Config config, AuthToken authToken, Properties info) throws URISyntaxException {
            driver = GraphDatabase.driver(routingUris.get(0), authToken, config);
            return driver;
        }

        @Override
        protected Properties getRoutingContext(String url, Properties properties) {
            return new Properties();
        }

        @Override
        protected String addRoutingPolicy(String url, Properties properties) {
            return url;
        }

        @Override
        protected List<URI> buildRoutingUris(String boltUrl, Properties properties) throws URISyntaxException {
            return Collections.singletonList(new URI(boltUrl));
        }

        public Driver getDriver() {
            return driver;
        }
    }
}
