/**
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
package it.neo4j.jdbc.bolt;

import it.neo4j.jdbc.ResultSet;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.neo4j.driver.internal.InternalSession;
import org.neo4j.driver.internal.logging.DevNullLogger;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.Transaction;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltConnectionTest {

	@Rule public ExpectedException expectedEx = ExpectedException.none();

	private Session mockSessionOpen() {
		Session session = mock(Session.class);
		when(session.isOpen()).thenReturn(true);
		Transaction transaction = mock(Transaction.class);
		when(session.beginTransaction()).thenReturn(transaction);
		return session;
	}

	private Session mockSessionClosed() {
		return mock(Session.class);
	}

	/*------------------------------*/
	/*           isClosed           */
	/*------------------------------*/
	@Test public void isClosedShouldReturnFalse() throws SQLException {
		Connection connection = new BoltConnection(mockSessionOpen());
		assertFalse(connection.isClosed());
	}

	@Test public void isClosedShouldReturnTrue() throws SQLException {
		Connection connection = new BoltConnection(mockSessionClosed());
		connection.close();
		assertTrue(connection.isClosed());
	}

	/*------------------------------*/
	/*             close            */
	/*------------------------------*/
	@Test public void closeShouldCloseConnection() throws SQLException {
		Session session = mock(Session.class);
		when(session.isOpen()).thenReturn(true).thenReturn(false);
		Connection connection = new BoltConnection(session);
		connection.close();
		verify(session, times(1)).close();
		assertTrue(connection.isClosed());
	}

	@Test public void closeShouldNotThrowExceptionWhenClosingAClosedConnection() throws SQLException {
		Session session = mockSessionClosed();
		Connection connection = new BoltConnection(session);
		connection.close();
		verify(session, never()).close();
		assertTrue(connection.isClosed());
	}

	@Test public void closeShouldThrowExceptionWhenDatabaseAccessErrorOccurred() throws SQLException {
		expectedEx.expect(SQLException.class);

		Session session = new InternalSession(null, new DevNullLogger());
		Connection connection = new BoltConnection(session);
		connection.close();
	}

	/*------------------------------*/
	/*          isReadOnly          */
	/*------------------------------*/
	@Test public void isReadOnlyShouldReturnFalse() throws SQLException {
		Connection connection = new BoltConnection(mockSessionOpen());
		assertFalse(connection.isReadOnly());
	}

	@Test public void isReadOnlyShouldReturnTrue() throws SQLException {
		Connection connection = new BoltConnection(mockSessionOpen());
		connection.setReadOnly(true);
		assertTrue(connection.isReadOnly());
	}

	@Test public void isReadOnlyShouldThrowExceptionWhenCalledOnAClosedConnection() throws SQLException {
		expectedEx.expect(SQLException.class);

		Connection connection = new BoltConnection(mockSessionClosed());
		connection.close();
		connection.isReadOnly();
	}

	/*------------------------------*/
	/*         setReadOnly          */
	/*------------------------------*/
	@Test public void setReadOnlyShouldSetReadOnlyTrue() throws SQLException {
		Connection connection = new BoltConnection(mockSessionOpen());
		assertFalse(connection.isReadOnly());
		connection.setReadOnly(true);
		assertTrue(connection.isReadOnly());
	}

	@Test public void setReadOnlyShouldSetReadOnlyFalse() throws SQLException {
		Connection connection = new BoltConnection(mockSessionOpen());
		assertFalse(connection.isReadOnly());
		connection.setReadOnly(false);
		assertFalse(connection.isReadOnly());
	}

	@Test public void setReadOnlyShouldSetReadOnlyFalseAfterSetItTrue() throws SQLException {
		Connection connection = new BoltConnection(mockSessionOpen());
		assertFalse(connection.isReadOnly());
		connection.setReadOnly(true);
		assertTrue(connection.isReadOnly());
		connection.setReadOnly(false);
		assertFalse(connection.isReadOnly());
	}

	@Test public void setReadOnlyShouldThrowExceptionIfCalledOnAClosedConnection() throws SQLException {
		expectedEx.expect(SQLException.class);

		Connection connection = new BoltConnection(mockSessionClosed());
		connection.close();
		connection.setReadOnly(true);
	}

	/*------------------------------*/
	/*        createStatement       */
	/*------------------------------*/

	//this test depends on BoltStatement implementation
	@Ignore @Test public void createStatementNoParamsShouldReturnNewStatement() throws SQLException {
		Connection connection = new BoltConnection(mockSessionOpen());
		Statement statement = connection.createStatement();

		assertTrue(statement instanceof BoltStatement);
		assertEquals(connection.getHoldability(), statement.getResultSetHoldability());
		assertEquals(ResultSet.CONCUR_READ_ONLY, statement.getResultSetConcurrency());
		assertEquals(ResultSet.TYPE_FORWARD_ONLY, statement.getResultSetType());
	}

	@Test public void createStatementNoParamsShouldThrowExceptionOnClosedConnection() throws SQLException {
		expectedEx.expect(SQLException.class);
		Connection connection = new BoltConnection(mockSessionClosed());
		connection.close();
		connection.createStatement();
	}

	//this test depends on BoltStatement implementation
	@Ignore @Test public void createStatementTwoParamsShouldReturnNewStatement() throws SQLException {
		Connection connection = new BoltConnection(mockSessionOpen());
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
		Connection connection = new BoltConnection(mockSessionClosed());
		connection.close();
		connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
	}

	//Must be managed only the types specified in the createStatementTwoParamsShouldReturnNewStatement test
	//This test doesn't cover all integers different from supported ones
	@Test public void createStatementTwoParamsShouldThrowExceptionOnWrongParams() throws SQLException {
		Connection connection = new BoltConnection(mockSessionOpen());

		int[] types = { ResultSet.TYPE_FORWARD_ONLY, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.TYPE_SCROLL_SENSITIVE };
		int[] concurrencies = { ResultSet.CONCUR_UPDATABLE, ResultSet.CONCUR_READ_ONLY };

		for (int type : types) {
			try {
				connection.createStatement(concurrencies[0], type);
				fail();
			} catch (SQLFeatureNotSupportedException e) {
				//Expected Exception
			} catch (Exception e) {
				fail();
			}
		}

		for (int concurrency : concurrencies) {
			try {
				connection.createStatement(concurrency, types[0]);
				fail();
			} catch (SQLFeatureNotSupportedException e) {
				//Expected Exception
			} catch (Exception e) {
				fail();
			}
		}
	}

	//this test depends on BoltStatement implementation
	@Ignore @Test public void createStatementThreeParamsShouldReturnNewStatement() throws SQLException {
		Connection connection = new BoltConnection(mockSessionOpen());
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
		Connection connection = new BoltConnection(mockSessionClosed());
		connection.close();
		connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
	}

	//Must be managed only the types specified in the createStatementThreeParamsShouldReturnNewStatement test
	//This test doesn't cover all integers different from supported ones
	@Test public void createStatementThreeParamsShouldThrowExceptionOnWrongParams() throws SQLException {
		Connection connection = new BoltConnection(mockSessionOpen());

		int[] types = { ResultSet.TYPE_FORWARD_ONLY, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.TYPE_SCROLL_SENSITIVE };
		int[] concurrencies = { ResultSet.CONCUR_UPDATABLE, ResultSet.CONCUR_READ_ONLY };
		int[] holdabilities = { ResultSet.HOLD_CURSORS_OVER_COMMIT, ResultSet.CLOSE_CURSORS_AT_COMMIT };

		for (int type : types) {
			try {
				connection.createStatement(concurrencies[0], holdabilities[0], type);
				fail();
			} catch (SQLFeatureNotSupportedException e) {
				//Expected Exception
			} catch (Exception e) {
				fail();
			}
		}

		for (int concurrency : concurrencies) {
			try {
				connection.createStatement(concurrency, holdabilities[0], types[0]);
				fail();
			} catch (SQLFeatureNotSupportedException e) {
				//Expected Exception
			} catch (Exception e) {
				fail();
			}
		}

		for (int holdability : holdabilities) {
			try {
				connection.createStatement(concurrencies[0], holdability, types[0]);
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

		Session session = new InternalSession(null, new DevNullLogger());
		Connection connection = new BoltConnection(session);
		connection.setAutoCommit(true);
	}

	@Test public void setAutoCommitShouldSetWhatIsPassed() throws SQLException {
		Connection connection = new BoltConnection(mockSessionOpen());

		connection.setAutoCommit(false);
		assertFalse(connection.getAutoCommit());
		connection.setAutoCommit(true);
		assertTrue(connection.getAutoCommit());
	}

	@Test public void setAutoCommitShouldCommit() throws SQLException {
		BoltConnection connection = new BoltConnection(mockSessionOpen());

		connection.setAutoCommit(true);
		verify(connection.getSession(), times(0)).beginTransaction();

		connection.setAutoCommit(false);
		verify(connection.getSession(), times(1)).beginTransaction();

		connection.setAutoCommit(false);
		verify(connection.getSession(), times(1)).beginTransaction();

		connection.setAutoCommit(true);
		verify(connection.getSession(), times(2)).beginTransaction();

		connection.setAutoCommit(true);
		verify(connection.getSession(), times(2)).beginTransaction();

		connection.setAutoCommit(false);
		verify(connection.getSession(), times(3)).beginTransaction();
	}

	/*------------------------------*/
	/*         getAutoCommit        */
	/*------------------------------*/

	@Test public void getAutoCommitShouldThrowExceptionIfConnectionIsClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		Connection connection = new BoltConnection(mockSessionClosed());

		connection.close();
		connection.getAutoCommit();
	}

	@Test public void getAutoCommitShouldReturnTrueByDefault() throws SQLException {
		Connection connection = new BoltConnection(mockSessionOpen());

		assertTrue(connection.getAutoCommit());
	}

	@Test public void getAutoCommitShouldReturnFalse() throws SQLException {
		Connection connection = new BoltConnection(mockSessionOpen());

		connection.setAutoCommit(false);
		assertFalse(connection.getAutoCommit());
	}

	@Test public void getAutoCommitShouldReturnTrue() throws SQLException {
		Connection connection = new BoltConnection(mockSessionOpen());

		connection.setAutoCommit(false);
		connection.setAutoCommit(true);
		assertTrue(connection.getAutoCommit());
	}

	/*------------------------------*/
	/*            commit            */
	/*------------------------------*/

	@Test public void commitShouldThrowExceptionIfConnectionIsClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		Connection connection = new BoltConnection(mockSessionClosed());

		connection.close();
		connection.commit();
	}

	@Test public void commitShouldThrowExceptionIfInAutoCommitIsTrue() throws SQLException {
		expectedEx.expect(SQLException.class);

		Connection connection = new BoltConnection(mockSessionOpen());

		connection.setAutoCommit(true);
		connection.commit();
	}

	/*------------------------------*/
	/*           rollback           */
	/*------------------------------*/

	@Test public void rollbackShouldThrowExceptionIfConnectionIsClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		Connection connection = new BoltConnection(mockSessionClosed());

		connection.close();
		connection.rollback();
	}

	@Test public void rollbackShouldThrowExceptionIfInAutoCommitIsTrue() throws SQLException {
		expectedEx.expect(SQLException.class);

		Connection connection = new BoltConnection(mockSessionOpen());

		connection.setAutoCommit(true);
		connection.rollback();
	}

	/*------------------------------*/
	/*   getTransactionIsolation    */
	/*------------------------------*/

	@Test public void getTransactionIsolationShouldThrowExceptionIfConnectionIsClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		Connection connection = new BoltConnection(mockSessionClosed());

		connection.close();
		connection.getTransactionIsolation();
	}

	@Test public void getTransactionIsolationShouldReturnTransactionReadCommitted() throws SQLException {
		Connection connection = new BoltConnection(mockSessionOpen());

		assertEquals(Connection.TRANSACTION_READ_COMMITTED, connection.getTransactionIsolation());
	}
}
