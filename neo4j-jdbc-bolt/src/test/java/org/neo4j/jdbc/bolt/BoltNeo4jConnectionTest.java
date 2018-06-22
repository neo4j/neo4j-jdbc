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
 * Created on 18/02/16
 */
package org.neo4j.jdbc.bolt;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.neo4j.driver.internal.NetworkSession;
import org.neo4j.driver.internal.logging.DevNullLogging;
import org.neo4j.driver.v1.*;
import org.neo4j.jdbc.bolt.data.StatementData;
import org.neo4j.jdbc.bolt.impl.BoltNeo4jConnectionImpl;

import java.sql.*;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.neo4j.jdbc.bolt.utils.Mocker.*;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltNeo4jConnectionTest {

	@Rule public ExpectedException expectedEx = ExpectedException.none();

	private BoltNeo4jConnectionImpl openConnection;
	private BoltNeo4jConnectionImpl closedConnection;
	private BoltNeo4jConnectionImpl slowOpenConnection;
	private BoltNeo4jConnectionImpl exceptionOpenConnection;

	@Before public void setUp() {
		openConnection = new BoltNeo4jConnectionImpl(mockDriverOpen(), new Properties(), "");
		closedConnection = new BoltNeo4jConnectionImpl(mockDriverClosed(), new Properties(), "");
		slowOpenConnection = new BoltNeo4jConnectionImpl(mockDriverOpenSlow(), new Properties(), "");
		exceptionOpenConnection = new BoltNeo4jConnectionImpl(mockDriverException(), new Properties(), "");
	}

	/*------------------------------*/
	/*        setHoldability        */
	/*------------------------------*/

	@Test public void setHoldabilityShouldSetTheHoldability() throws SQLException {
		openConnection.setHoldability(ResultSet.HOLD_CURSORS_OVER_COMMIT);
		assertEquals(ResultSet.HOLD_CURSORS_OVER_COMMIT, openConnection.getHoldability());
		openConnection.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
		assertEquals(ResultSet.CLOSE_CURSORS_AT_COMMIT, openConnection.getHoldability());
	}

	@Test public void setHoldabilityShouldThrowExceptionIfHoldabilityIsNotSupported() throws SQLException {
		expectedEx.expect(SQLFeatureNotSupportedException.class);
		openConnection.setHoldability(999);
	}

	@Test public void setHoldabilityShouldThrowExceptionWhenCalledOnClosedConnection() throws SQLException {
		expectedEx.expect(SQLException.class);
		closedConnection.setHoldability(ResultSet.HOLD_CURSORS_OVER_COMMIT);
	}

	/*------------------------------*/
	/*        getHoldability        */
	/*------------------------------*/

	@Test public void getHoldabilityShouldThrowExceptionWhenCalledOnClosedConnection() throws SQLException {
		expectedEx.expect(SQLException.class);
		closedConnection.getHoldability();
	}

	/*------------------------------*/
	/*           isClosed           */
	/*------------------------------*/
	@Test public void isClosedShouldReturnFalse() throws SQLException {
		assertFalse(openConnection.isClosed());
	}

	@Test public void isClosedShouldReturnTrue() throws SQLException {
		assertTrue(closedConnection.isClosed());
	}

	/*------------------------------*/
	/*             close            */
	/*------------------------------*/
	@Test public void closeShouldCloseConnection() throws SQLException {
		Session session = mock(Session.class);
		when(session.isOpen()).thenReturn(true).thenReturn(false);
		org.neo4j.driver.v1.Driver driver = mock(org.neo4j.driver.v1.Driver.class);
		when(driver.session(any(AccessMode.class), anyString())).thenReturn(session);
		Connection connection = new BoltNeo4jConnectionImpl(driver, new Properties(), "");
		connection.close();
		verify(session, times(1)).close();
		assertTrue(connection.isClosed());
	}

	@Test public void closeShouldNotThrowExceptionWhenClosingAClosedConnection() throws SQLException {
		Session session = mockSessionClosed();
        org.neo4j.driver.v1.Driver driver = mock(org.neo4j.driver.v1.Driver.class);
		when(driver.session(any(AccessMode.class), anyString())).thenReturn(session);
		Connection connection = new BoltNeo4jConnectionImpl(driver, new Properties(), "");
		connection.close();
		verify(session, never()).close();
		assertTrue(connection.isClosed());
	}

	/*
	@Ignore
	@Test public void closeShouldThrowExceptionWhenDatabaseAccessErrorOccurred() throws SQLException {
		expectedEx.expect(SQLException.class);

		Connection boltConnection = Mockito.mock(BoltNeo4jConnectionImpl.class);
		doThrow(new IllegalStateException()).when(boltConnection).close();
		ConnectionProvider connectionProvider = Mockito.mock(ConnectionProvider.class);
		when(connectionProvider.acquireConnection(AccessMode.READ)).thenReturn(boltConnection);
		Session session = new NetworkSession(connectionProvider, AccessMode.READ, null, DevNullLogging.DEV_NULL_LOGGING);
		session.run("return 1");
		Connection connectionFlatten = new BoltNeo4jConnectionImpl(session);
		connectionFlatten.close();
	}
    */

	/*------------------------------*/
	/*          isReadOnly          */
	/*------------------------------*/
	@Test public void isReadOnlyShouldReturnFalse() throws SQLException {
		assertFalse(openConnection.isReadOnly());
	}

	@Test public void isReadOnlyShouldReturnTrue() throws SQLException {
		openConnection.setReadOnly(true);
		assertTrue(openConnection.isReadOnly());
	}

	@Test public void isReadOnlyShouldThrowExceptionWhenCalledOnAClosedConnection() throws SQLException {
		expectedEx.expect(SQLException.class);
		closedConnection.isReadOnly();
	}

	/*------------------------------*/
	/*         setReadOnly          */
	/*------------------------------*/
	@Test public void setReadOnlyShouldSetReadOnlyTrue() throws SQLException {
		assertFalse(openConnection.isReadOnly());
		openConnection.setReadOnly(true);
		assertTrue(openConnection.isReadOnly());
	}

	@Test public void setReadOnlyShouldSetReadOnlyFalse() throws SQLException {
		assertFalse(openConnection.isReadOnly());
		openConnection.setReadOnly(false);
		assertFalse(openConnection.isReadOnly());
	}

	@Test public void setReadOnlyShouldSetReadOnlyFalseAfterSetItTrue() throws SQLException {
		assertFalse(openConnection.isReadOnly());
		openConnection.setReadOnly(true);
		assertTrue(openConnection.isReadOnly());
		openConnection.setReadOnly(false);
		assertFalse(openConnection.isReadOnly());
	}

	@Test public void setReadOnlyShouldThrowExceptionIfCalledOnAClosedConnection() throws SQLException {
		expectedEx.expect(SQLException.class);
		closedConnection.setReadOnly(true);
	}

	/*------------------------------*/
	/*        createStatement       */
	/*------------------------------*/

	@Test public void createStatementNoParamsShouldReturnNewStatement() throws SQLException {
		Connection connection = new BoltNeo4jConnectionImpl(mockDriverOpen(), new Properties(), "");
		Statement statement = connection.createStatement();

		assertNotNull(statement);
		assertEquals(connection.getHoldability(), statement.getResultSetHoldability());
		assertEquals(ResultSet.CONCUR_READ_ONLY, statement.getResultSetConcurrency());
		assertEquals(ResultSet.TYPE_FORWARD_ONLY, statement.getResultSetType());
	}

	@Test public void createStatementNoParamsShouldThrowExceptionOnClosedConnection() throws SQLException {
		expectedEx.expect(SQLException.class);
		closedConnection.createStatement();
	}

	@Test public void createStatementTwoParamsShouldReturnNewStatement() throws SQLException {
		Connection connection = new BoltNeo4jConnectionImpl(mockDriverOpen(), new Properties(), "");
		int[] types = { ResultSet.TYPE_FORWARD_ONLY, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.TYPE_SCROLL_SENSITIVE };
		int[] concurrencies = { ResultSet.CONCUR_UPDATABLE, ResultSet.CONCUR_READ_ONLY };

		for (int type : types) {
			Statement statement = connection.createStatement(type, concurrencies[0]);
			assertEquals(type, statement.getResultSetType());
		}

		for (int concurrency : concurrencies) {
			Statement statement = connection.createStatement(types[0], concurrency);
			assertEquals(concurrency, statement.getResultSetConcurrency());
		}
	}

	@Test public void createStatementTwoParamsShouldThrowExceptionOnClosedConnection() throws SQLException {
		expectedEx.expect(SQLException.class);
		closedConnection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
	}

	//Must be managed only the types specified in the createStatementTwoParamsShouldReturnNewStatement test
	//This test doesn't cover all integers different from supported ones
	@Test public void createStatementTwoParamsShouldThrowExceptionOnWrongParams() throws SQLException {

		int[] types = { ResultSet.TYPE_FORWARD_ONLY, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.TYPE_SCROLL_SENSITIVE };
		int[] concurrencies = { ResultSet.CONCUR_UPDATABLE, ResultSet.CONCUR_READ_ONLY };

		for (int type : types) {
			try {
				openConnection.createStatement(concurrencies[0], type);
				fail();
			} catch (SQLFeatureNotSupportedException e) {
				//Expected Exception
			} catch (Exception e) {
				fail();
			}
		}

		for (int concurrency : concurrencies) {
			try {
				openConnection.createStatement(concurrency, types[0]);
				fail();
			} catch (SQLFeatureNotSupportedException e) {
				//Expected Exception
			} catch (Exception e) {
				fail();
			}
		}
	}

	@Test public void createStatementThreeParamsShouldReturnNewStatement() throws SQLException {
		Connection connection = new BoltNeo4jConnectionImpl(mockDriverOpen(), new Properties(), "");
		int[] types = { ResultSet.TYPE_FORWARD_ONLY, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.TYPE_SCROLL_SENSITIVE };
		int[] concurrencies = { ResultSet.CONCUR_UPDATABLE, ResultSet.CONCUR_READ_ONLY };
		int[] holdabilities = { ResultSet.HOLD_CURSORS_OVER_COMMIT, ResultSet.CLOSE_CURSORS_AT_COMMIT };

		for (int type : types) {
			Statement statement = connection.createStatement(type, concurrencies[0], holdabilities[0]);
			assertEquals(type, statement.getResultSetType());
		}

		for (int concurrency : concurrencies) {
			Statement statement = connection.createStatement(types[0], concurrency, holdabilities[0]);
			assertEquals(concurrency, statement.getResultSetConcurrency());
		}

		for (int holdability : holdabilities) {
			Statement statement = connection.createStatement(types[0], concurrencies[0], holdability);
			assertEquals(holdability, statement.getResultSetHoldability());
		}
	}

	@Test public void createStatementThreeParamsShouldThrowExceptionOnClosedConnection() throws SQLException {
		expectedEx.expect(SQLException.class);
		closedConnection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
	}

	//Must be managed only the types specified in the createStatementThreeParamsShouldReturnNewStatement test
	//This test doesn't cover all integers different from supported ones
	@Test public void createStatementThreeParamsShouldThrowExceptionOnWrongParams() throws SQLException {
		int[] types = { ResultSet.TYPE_FORWARD_ONLY, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.TYPE_SCROLL_SENSITIVE };
		int[] concurrencies = { ResultSet.CONCUR_UPDATABLE, ResultSet.CONCUR_READ_ONLY };
		int[] holdabilities = { ResultSet.HOLD_CURSORS_OVER_COMMIT, ResultSet.CLOSE_CURSORS_AT_COMMIT };

		for (int type : types) {
			try {
				openConnection.createStatement(concurrencies[0], holdabilities[0], type);
				fail();
			} catch (SQLFeatureNotSupportedException e) {
				//Expected Exception
			} catch (Exception e) {
				fail();
			}
		}

		for (int concurrency : concurrencies) {
			try {
				openConnection.createStatement(concurrency, holdabilities[0], types[0]);
				fail();
			} catch (SQLFeatureNotSupportedException e) {
				//Expected Exception
			} catch (Exception e) {
				fail();
			}
		}

		for (int holdability : holdabilities) {
			try {
				openConnection.createStatement(concurrencies[0], holdability, types[0]);
			} catch (SQLFeatureNotSupportedException e) {
				//Expected Exception
			} catch (Exception e) {
				fail();
			}
		}
	}

	@Ignore
	@Test public void createStatementWithWrongSyntax() throws SQLException {
		try (Connection connection = new BoltNeo4jConnectionImpl(mockDriverOpen(), new Properties(), "")) {
			try {
				boolean ret = connection.createStatement().execute("CREATE (n:Test {name:'TEST'}) SET n.p1 = 'A1', n.。p2 = 'A2';");
				System.out.println("execute ret: " + ret);
			} catch (Exception e) {
				e.printStackTrace();
				try {
					if (connection != null) {
						boolean ret = connection.createStatement().execute("CREATE (n:Test {name:'TEST'}) SET n.p1 = 'A1', n.p2 = 'A2';");
						System.out.println("execute ret: " + ret);
					}
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	/*------------------------------*/
	/*        prepareStatement       */
	/*------------------------------*/

	@Test public void prepareStatementNoParamsShouldReturnNewStatement() throws SQLException {
		Connection connection = new BoltNeo4jConnectionImpl(mockDriverOpen(), new Properties(), "");
		Statement statement = connection.prepareStatement(StatementData.STATEMENT_MATCH_ALL_STRING_PARAMETRIC);

		assertNotNull(statement);
		assertEquals(connection.getHoldability(), statement.getResultSetHoldability());
		assertEquals(ResultSet.CONCUR_READ_ONLY, statement.getResultSetConcurrency());
		assertEquals(ResultSet.TYPE_FORWARD_ONLY, statement.getResultSetType());
	}

	@Test public void prepareStatementNoParamsShouldThrowExceptionOnClosedConnection() throws SQLException {
		expectedEx.expect(SQLException.class);
		closedConnection.prepareStatement(StatementData.STATEMENT_MATCH_ALL_STRING_PARAMETRIC);
	}

	@Test public void prepareStatementTwoParamsShouldReturnNewStatement() throws SQLException {
		Connection connection = new BoltNeo4jConnectionImpl(mockDriverOpen(), new Properties(), "");
		int[] types = { ResultSet.TYPE_FORWARD_ONLY, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.TYPE_SCROLL_SENSITIVE };
		int[] concurrencies = { ResultSet.CONCUR_UPDATABLE, ResultSet.CONCUR_READ_ONLY };

		for (int type : types) {
			Statement statement = connection.prepareStatement(StatementData.STATEMENT_MATCH_ALL_STRING_PARAMETRIC, type, concurrencies[0]);
			assertEquals(type, statement.getResultSetType());
		}

		for (int concurrency : concurrencies) {
			Statement statement = connection.prepareStatement(StatementData.STATEMENT_MATCH_ALL_STRING_PARAMETRIC, types[0], concurrency);
			assertEquals(concurrency, statement.getResultSetConcurrency());
		}
	}

	@Test public void prepareStatementTwoParamsShouldThrowExceptionOnClosedConnection() throws SQLException {
		expectedEx.expect(SQLException.class);
		closedConnection.prepareStatement(StatementData.STATEMENT_MATCH_ALL_STRING_PARAMETRIC, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
	}

	//Must be managed only the types specified in the prepareStatementTwoParamsShouldReturnNewStatement test
	//This test doesn't cover all integers different from supported ones
	@Test public void prepareStatementTwoParamsShouldThrowExceptionOnWrongParams() throws SQLException {

		int[] types = { ResultSet.TYPE_FORWARD_ONLY, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.TYPE_SCROLL_SENSITIVE };
		int[] concurrencies = { ResultSet.CONCUR_UPDATABLE, ResultSet.CONCUR_READ_ONLY };

		for (int type : types) {
			try {
				openConnection.prepareStatement(StatementData.STATEMENT_MATCH_ALL_STRING_PARAMETRIC, concurrencies[0], type);
				fail();
			} catch (SQLFeatureNotSupportedException e) {
				//Expected Exception
			} catch (Exception e) {
				fail();
			}
		}

		for (int concurrency : concurrencies) {
			try {
				openConnection.prepareStatement(StatementData.STATEMENT_MATCH_ALL_STRING_PARAMETRIC, concurrency, types[0]);
				fail();
			} catch (SQLFeatureNotSupportedException e) {
				//Expected Exception
			} catch (Exception e) {
				fail();
			}
		}
	}

	@Test public void prepareStatementThreeParamsShouldReturnNewStatement() throws SQLException {
		Connection connection = new BoltNeo4jConnectionImpl(mockDriverOpen(), new Properties(), "");
		int[] types = { ResultSet.TYPE_FORWARD_ONLY, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.TYPE_SCROLL_SENSITIVE };
		int[] concurrencies = { ResultSet.CONCUR_UPDATABLE, ResultSet.CONCUR_READ_ONLY };
		int[] holdabilities = { ResultSet.HOLD_CURSORS_OVER_COMMIT, ResultSet.CLOSE_CURSORS_AT_COMMIT };

		for (int type : types) {
			Statement statement = connection.prepareStatement(StatementData.STATEMENT_MATCH_ALL_STRING_PARAMETRIC, type, concurrencies[0], holdabilities[0]);
			assertEquals(type, statement.getResultSetType());
		}

		for (int concurrency : concurrencies) {
			Statement statement = connection.prepareStatement(StatementData.STATEMENT_MATCH_ALL_STRING_PARAMETRIC, types[0], concurrency, holdabilities[0]);
			assertEquals(concurrency, statement.getResultSetConcurrency());
		}

		for (int holdability : holdabilities) {
			Statement statement = connection.prepareStatement(StatementData.STATEMENT_MATCH_ALL_STRING_PARAMETRIC, types[0], concurrencies[0], holdability);
			assertEquals(holdability, statement.getResultSetHoldability());
		}
	}

	@Test public void prepareStatementThreeParamsShouldThrowExceptionOnClosedConnection() throws SQLException {
		expectedEx.expect(SQLException.class);
		closedConnection.prepareStatement(StatementData.STATEMENT_MATCH_ALL_STRING_PARAMETRIC, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
				ResultSet.HOLD_CURSORS_OVER_COMMIT);
	}

	//Must be managed only the types specified in the prepareStatementThreeParamsShouldReturnNewStatement test
	//This test doesn't cover all integers different from supported ones
	@Test public void prepareStatementThreeParamsShouldThrowExceptionOnWrongParams() throws SQLException {
		int[] types = { ResultSet.TYPE_FORWARD_ONLY, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.TYPE_SCROLL_SENSITIVE };
		int[] concurrencies = { ResultSet.CONCUR_UPDATABLE, ResultSet.CONCUR_READ_ONLY };
		int[] holdabilities = { ResultSet.HOLD_CURSORS_OVER_COMMIT, ResultSet.CLOSE_CURSORS_AT_COMMIT };

		for (int type : types) {
			try {
				openConnection.prepareStatement(StatementData.STATEMENT_MATCH_ALL_STRING_PARAMETRIC, concurrencies[0], holdabilities[0], type);
				fail();
			} catch (SQLFeatureNotSupportedException e) {
				//Expected Exception
			} catch (Exception e) {
				fail();
			}
		}

		for (int concurrency : concurrencies) {
			try {
				openConnection.prepareStatement(StatementData.STATEMENT_MATCH_ALL_STRING_PARAMETRIC, concurrency, holdabilities[0], types[0]);
				fail();
			} catch (SQLFeatureNotSupportedException e) {
				//Expected Exception
			} catch (Exception e) {
				fail();
			}
		}

		for (int holdability : holdabilities) {
			try {
				openConnection.prepareStatement(StatementData.STATEMENT_MATCH_ALL_STRING_PARAMETRIC, concurrencies[0], holdability, types[0]);
			} catch (SQLFeatureNotSupportedException e) {
				//Expected Exception
			} catch (Exception e) {
				fail();
			}
		}
	}

	//TODO needs IT tests checking initialization succeeded

	/*------------------------------*/
	/*         setAutocommit        */
	/*------------------------------*/

	//Check what cases of exception can occur
	@Ignore @Test public void setAutoCommitShouldThrowExceptionOnDatabaseAccessErrorOccurred() throws SQLException {
		expectedEx.expect(SQLException.class);

		Session session = new NetworkSession(null, AccessMode.READ,null, DevNullLogging.DEV_NULL_LOGGING);
		org.neo4j.driver.v1.Driver driver = mock(org.neo4j.driver.v1.Driver.class);
		when(driver.session()).thenReturn(session);
		Connection connection = new BoltNeo4jConnectionImpl(driver, new Properties(), "");
		connection.setAutoCommit(true);
	}

	@Test public void setAutoCommitShouldSetWhatIsPassed() throws SQLException {
		openConnection.setAutoCommit(false);
		assertFalse(openConnection.getAutoCommit());
		openConnection.setAutoCommit(true);
		assertTrue(openConnection.getAutoCommit());
	}

	@Test public void setAutoCommitShouldCommit() throws SQLException {
		openConnection.setAutoCommit(true);
		openConnection.createStatement();
		verify(openConnection.getSession(), times(1)).beginTransaction();

		openConnection.setAutoCommit(false);
		openConnection.createStatement();
		verify(openConnection.getSession(), times(1)).beginTransaction();

		openConnection.setAutoCommit(false);
		openConnection.createStatement();
		verify(openConnection.getSession(), times(1)).beginTransaction();

		openConnection.setAutoCommit(true);
		openConnection.createStatement();
		verify(openConnection.getSession(), times(2)).beginTransaction();

		openConnection.setAutoCommit(true);
		openConnection.createStatement();
		verify(openConnection.getSession(), times(3)).beginTransaction();

		openConnection.setAutoCommit(false);
		openConnection.createStatement();
		verify(openConnection.getSession(), times(3)).beginTransaction();
	}

	/*------------------------------*/
	/*         getAutoCommit        */
	/*------------------------------*/

	@Test public void getAutoCommitShouldThrowExceptionIfConnectionIsClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		closedConnection.getAutoCommit();
	}

	@Test public void getAutoCommitShouldReturnTrueByDefault() throws SQLException {
		assertTrue(openConnection.getAutoCommit());
	}

	@Test public void getAutoCommitShouldReturnFalse() throws SQLException {
		openConnection.setAutoCommit(false);
		assertFalse(openConnection.getAutoCommit());
	}

	@Test public void getAutoCommitShouldReturnTrue() throws SQLException {
		openConnection.setAutoCommit(false);
		openConnection.setAutoCommit(true);
		assertTrue(openConnection.getAutoCommit());
	}

	/*------------------------------*/
	/*            commit            */
	/*------------------------------*/

	@Test public void commitShouldThrowExceptionIfConnectionIsClosed() throws SQLException {
		expectedEx.expect(SQLException.class);
		closedConnection.commit();
	}

	@Test public void commitShouldThrowExceptionIfInAutoCommitIsTrue() throws SQLException {
		expectedEx.expect(SQLException.class);
		openConnection.setAutoCommit(true);
		openConnection.commit();
	}

	/*------------------------------*/
	/*           rollback           */
	/*------------------------------*/

	@Test public void rollbackShouldThrowExceptionIfConnectionIsClosed() throws SQLException {
		expectedEx.expect(SQLException.class);
		closedConnection.rollback();
	}

	@Test public void rollbackShouldThrowExceptionIfInAutoCommitIsTrue() throws SQLException {
		expectedEx.expect(SQLException.class);
		openConnection.setAutoCommit(true);
		openConnection.rollback();
	}

	/*------------------------------*/
	/*   getTransactionIsolation    */
	/*------------------------------*/

	@Test public void getTransactionIsolationShouldThrowExceptionIfConnectionIsClosed() throws SQLException {
		expectedEx.expect(SQLException.class);
		closedConnection.getTransactionIsolation();
	}

	@Test public void getTransactionIsolationShouldReturnTransactionReadCommitted() throws SQLException {
		assertEquals(Connection.TRANSACTION_READ_COMMITTED, openConnection.getTransactionIsolation());
	}

	/*------------------------------*/
	/*         setCatalog           */
	/*------------------------------*/

	@Test public void setCatalogShouldSilentlyIgnoreTheRequest() throws SQLException {
		openConnection.setCatalog("catalog");
		assertNull(openConnection.getCatalog());
	}

	@Test public void setCatalogShouldThrowExceptionWhenConnectionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);
		closedConnection.setCatalog("catalog");
	}

	/*------------------------------*/
	/*         getCatalog           */
	/*------------------------------*/

	@Test public void getCatalogShouldReturnNull() throws SQLException {
		assertNull(openConnection.getCatalog());
	}

	@Test public void getCatalogShouldThrowExceptionWhenConnectionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);
		closedConnection.getCatalog();
	}

	/*-------------------------------*/
	/*            isValid            */
	/*-------------------------------*/
	@Test public void isValidShouldThrowExceptionWhenTimeoutSuppliedLessThanZero() throws SQLException {
		expectedEx.expect(SQLException.class);
		openConnection.isValid(-2);
	}

	@Test public void isValidShouldReturnFalseIfCalledOnClosedConnection() throws SQLException {
		assertFalse(closedConnection.isValid(0));
	}

	@Test(timeout=1500) public void isValidShouldReturnFalseIfTimeout() throws SQLException {
		assertFalse(slowOpenConnection.isValid(1));
	}

	@Test(timeout=5000) public void isValidShouldReturnTrueIfNotTimeout() throws SQLException {
		assertTrue(openConnection.isValid(4));
	}

	@Test public void isValidShouldReturnFalseIfSessionException() throws SQLException {
		assertFalse(exceptionOpenConnection.isValid(5));
		assertFalse(exceptionOpenConnection.isValid(0));
	}

	@Test public void isValidShouldReturnTrueWithoutATimeoutValue() throws SQLException {
		assertTrue(openConnection.isValid(0));
	}

}
