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
 * Created on 19/02/16
 */
package it.larusba.neo4j.jdbc.bolt;

import it.larusba.neo4j.jdbc.Connection;
import it.larusba.neo4j.jdbc.bolt.data.StatementData;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.internal.util.reflection.Whitebox;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.summary.ResultSummary;
import org.neo4j.driver.v1.summary.SummaryCounters;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static it.larusba.neo4j.jdbc.bolt.utils.Mocker.*;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.verifyNew;
import static org.powermock.api.mockito.PowerMockito.whenNew;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
@RunWith(PowerMockRunner.class) @PrepareForTest({ BoltStatement.class, BoltResultSet.class }) public class BoltStatementTest {

	@Rule public ExpectedException expectedEx = ExpectedException.none();

	private BoltResultSet mockedRS;

	@Before public void interceptBoltResultSetConstructor() throws Exception {
		mockedRS = mock(BoltResultSet.class);
		doNothing().when(mockedRS).close();
		whenNew(BoltResultSet.class).withAnyArguments().thenReturn(mockedRS);
	}

	/*------------------------------*/
	/*             close            */
	/*------------------------------*/
	@Test public void closeShouldCloseExistingResultSet() throws Exception {

		Statement statement = new BoltStatement(mockConnectionOpenWithTransactionThatReturns(null));
		statement.executeQuery(StatementData.STATEMENT_MATCH_ALL);
		statement.close();

		verify(mockedRS, times(1)).close();

	}

	@Test public void closeShouldNotCallCloseOnAnyResultSet() throws Exception {

		Statement statement = new BoltStatement(mockConnectionOpenWithTransactionThatReturns(null));
		statement.close();

		verify(mockedRS, never()).close();
	}

	@Test public void closeMultipleTimesIsNOOP() throws Exception {

		Statement statement = new BoltStatement(mockConnectionOpenWithTransactionThatReturns(null));
		statement.executeQuery(StatementData.STATEMENT_MATCH_ALL);
		statement.close();
		statement.close();
		statement.close();

		verify(mockedRS, times(1)).close();
	}

	@Test public void closeShouldCloseTheTransactionNotCommitting() throws Exception {

		Transaction mockTransaction = mock(Transaction.class);

		BoltConnection mockConnection = mockConnectionOpen();
		when(mockConnection.getTransaction()).thenReturn(mockTransaction);

		Statement statement = new BoltStatement(mockConnection);

		statement.close();

		verify(mockTransaction, times(1)).failure();
		verify(mockTransaction, times(1)).close();
	}

	/*------------------------------*/
	/*           isClosed           */
	/*------------------------------*/
	@Test public void isClosedShouldReturnFalseWhenCreated() throws SQLException {
		Statement statement = new BoltStatement(mockConnectionOpen());
		assertFalse(statement.isClosed());
	}

	/*------------------------------*/
	/*          executeQuery        */
	/*------------------------------*/

	@Test public void executeQueryShouldRun() throws SQLException {
		StatementResult mockResult = mock(StatementResult.class);

		Statement statement = new BoltStatement(mockConnectionOpenWithTransactionThatReturns(mockResult), ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
		statement.executeQuery(StatementData.STATEMENT_CREATE);
	}

	@Test public void executeQueryShouldThrowExceptionWhenClosedConnection() throws SQLException {
		expectedEx.expect(SQLException.class);

		Statement statement = new BoltStatement(mockConnectionClosed(), 0, 0, 0);
		statement.executeQuery(StatementData.STATEMENT_MATCH_ALL);
	}

	@Test public void executeQueryShouldReturnCorrectResultSetStructureConnectionNotAutocommit() throws Exception {
		BoltConnection mockConnection = mockConnectionOpenWithTransactionThatReturns(null);

		Statement statement = new BoltStatement(mockConnection, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
		statement.executeQuery(StatementData.STATEMENT_MATCH_ALL);

		verifyNew(BoltResultSet.class).withArguments(null, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
	}

	@Test public void executeQueryShouldThrowExceptionOnClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		BoltStatement statement = mock(BoltStatement.class);
		when(statement.isClosed()).thenReturn(true);
		when(statement.executeQuery(anyString())).thenCallRealMethod();

		statement.executeQuery(StatementData.STATEMENT_MATCH_ALL);
	}

	@Test public void executeQueryShouldThrowExceptionOnPreparedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Connection connection = new BoltConnection(mockSessionOpen());
		Statement statement = connection.prepareStatement("");
		statement.executeQuery(StatementData.STATEMENT_MATCH_ALL);
	}

	@Ignore @Test public void executeQueryShouldThrowExceptionOnCallableStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Connection connection = new BoltConnection(mockSessionOpen());
		Statement statement = connection.prepareCall("");
		statement.executeQuery(StatementData.STATEMENT_MATCH_ALL);
	}

	@Ignore @Test public void executeQueryShouldThrowExceptionOnTimeoutExceeded() throws SQLException {
		expectedEx.expect(SQLException.class);

		Transaction transaction = mock(Transaction.class);

		given(transaction.run(anyString())).willAnswer(invocation -> {
			Thread.sleep(1500);
			return null;
		});

		Session session = mock(Session.class);
		given(session.beginTransaction()).willReturn(transaction);
		given(session.isOpen()).willReturn(true);

		Statement statement = new BoltStatement(new BoltConnection(session), 0, 0, 0);

		statement.setQueryTimeout(1);
		statement.executeQuery(StatementData.STATEMENT_CREATE);

		fail();
	}

	/*------------------------------*/
	/*         executeUpdate        */
	/*------------------------------*/

	@Test public void executeUpdateShouldRun() throws SQLException {
		StatementResult mockResult = mock(StatementResult.class);
		ResultSummary mockSummary = mock(ResultSummary.class);
		SummaryCounters mockSummaryCounters = mock(SummaryCounters.class);

		when(mockResult.consume()).thenReturn(mockSummary);
		when(mockSummary.counters()).thenReturn(mockSummaryCounters);
		when(mockSummaryCounters.nodesCreated()).thenReturn(1);
		when(mockSummaryCounters.nodesDeleted()).thenReturn(0);

		Statement statement = new BoltStatement(mockConnectionOpenWithTransactionThatReturns(mockResult), ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
		statement.executeUpdate(StatementData.STATEMENT_CREATE);
	}

	@Test public void executeUpdateShouldThrowExceptionOnClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Connection connection = new BoltConnection(mockSessionOpen());
		Statement statement = connection.createStatement();
		statement.close();
		statement.executeUpdate(StatementData.STATEMENT_CREATE);
	}

	@Test public void executeUpdateShouldThrowExceptionOnPreparedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Connection connection = new BoltConnection(mockSessionOpen());
		Statement statement = connection.prepareStatement("");
		statement.executeUpdate(StatementData.STATEMENT_CREATE);
	}

	@Ignore @Test public void executeUpdateShouldThrowExceptionOnCallableStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Connection connection = new BoltConnection(mockSessionOpen());
		Statement statement = connection.prepareCall(null);
		statement.executeUpdate(StatementData.STATEMENT_CREATE);
	}

	@Ignore @Test public void executeUpdateShouldThrowExceptionOnTimeoutExceeded() throws SQLException {
		expectedEx.expect(SQLException.class);

		Transaction transaction = mock(Transaction.class);

		given(transaction.run(anyString())).willAnswer(invocation -> {
			Thread.sleep(1500);
			return null;
		});

		Session session = mock(Session.class);
		given(session.beginTransaction()).willReturn(transaction);
		given(session.isOpen()).willReturn(true);

		Statement statement = new BoltStatement(new BoltConnection(session), 0, 0, 0);

		statement.setQueryTimeout(1);
		statement.executeUpdate(StatementData.STATEMENT_CREATE);

		fail();
	}

	/*------------------------------*/
	/*    getResultSetConcurrency   */
	/*------------------------------*/
	@Test public void getResultSetConcurrencyShouldThrowExceptionWhenCalledOnClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Statement statement = new BoltStatement(mockConnectionClosed());
		statement.close();
		statement.getResultSetConcurrency();
	}

	/*------------------------------*/
	/*    getResultSetHoldability   */
	/*------------------------------*/
	@Test public void getResultSetHoldabilityShouldThrowExceptionWhenCalledOnClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Statement statement = new BoltStatement(mockConnectionClosed());
		statement.close();
		statement.getResultSetHoldability();
	}

	/*------------------------------*/
	/*       getResultSetType       */
	/*------------------------------*/
	@Test public void getResultSetTypeShouldThrowExceptionWhenCalledOnClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Statement statement = new BoltStatement(mockConnectionClosed());
		statement.close();
		statement.getResultSetType();
	}

	/*------------------------------*/
	/*            execute           */
	/*------------------------------*/
	@Test public void executeShouldRunQuery() throws SQLException {
		StatementResult mockResult = mock(StatementResult.class);

		Statement statement = new BoltStatement(mockConnectionOpenWithTransactionThatReturns(mockResult), ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
		statement.execute(StatementData.STATEMENT_MATCH_ALL);
	}

	@Test public void executeShouldRunUpdate() throws SQLException {
		StatementResult mockResult = mock(StatementResult.class);
		ResultSummary mockSummary = mock(ResultSummary.class);
		SummaryCounters mockSummaryCounters = mock(SummaryCounters.class);

		when(mockResult.consume()).thenReturn(mockSummary);
		when(mockSummary.counters()).thenReturn(mockSummaryCounters);
		when(mockSummaryCounters.nodesCreated()).thenReturn(1);
		when(mockSummaryCounters.nodesDeleted()).thenReturn(0);

		Statement statement = new BoltStatement(mockConnectionOpenWithTransactionThatReturns(mockResult), ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
		statement.execute(StatementData.STATEMENT_CREATE);
	}

	@Test public void executeShouldThrowExceptionOnQueryWhenClosedConnection() throws SQLException {
		expectedEx.expect(SQLException.class);

		Statement statement = new BoltStatement(mockConnectionClosed(), 0, 0, 0);
		statement.execute(StatementData.STATEMENT_MATCH_ALL);
	}

	@Test public void executeShouldThrowExceptionOnUpdateWhenClosedConnection() throws SQLException {
		expectedEx.expect(SQLException.class);

		Statement statement = new BoltStatement(mockConnectionClosed(), 0, 0, 0);
		statement.execute(StatementData.STATEMENT_CREATE);
	}

	@Test public void executeShouldThrowExceptionOnQueryOnClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		BoltStatement statement = mock(BoltStatement.class);
		when(statement.isClosed()).thenReturn(true);
		when(statement.execute(anyString())).thenCallRealMethod();
		when(statement.executeQuery(anyString())).thenCallRealMethod();

		statement.execute(StatementData.STATEMENT_MATCH_ALL);
	}

	@Test public void executeShouldThrowExceptionOnUpdateOnClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		BoltStatement statement = mock(BoltStatement.class);
		when(statement.isClosed()).thenReturn(true);
		when(statement.execute(anyString())).thenCallRealMethod();
		when(statement.executeUpdate(anyString())).thenCallRealMethod();

		statement.execute(StatementData.STATEMENT_CREATE);
	}

	@Test public void executeShouldThrowExceptionOnQueryOnPreparedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Connection connection = new BoltConnection(mockSessionOpen());
		Statement statement = connection.prepareStatement("");
		when(statement.executeQuery(anyString())).thenCallRealMethod();

		statement.execute(StatementData.STATEMENT_MATCH_ALL);
	}

	@Test public void executeShouldThrowExceptionOnUpdateOnPreparedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Connection connection = new BoltConnection(mockSessionOpen());
		Statement statement = connection.prepareStatement("");
		when(statement.executeUpdate(anyString())).thenCallRealMethod();

		statement.execute(StatementData.STATEMENT_CREATE);
	}

	@Ignore @Test public void executeShouldThrowExceptionOnCallableStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Connection connection = new BoltConnection(mockSessionOpen());
		Statement statement = connection.prepareCall("");
		statement.execute(StatementData.STATEMENT_MATCH_ALL);
	}

	@Ignore @Test public void executeShouldThrowExceptionOnTimeoutExceeded() throws SQLException {
		expectedEx.expect(SQLException.class);

		Transaction transaction = mock(Transaction.class);

		given(transaction.run(anyString())).willAnswer(invocation -> {
			Thread.sleep(1500);
			return null;
		});

		Session session = mock(Session.class);
		given(session.beginTransaction()).willReturn(transaction);
		given(session.isOpen()).willReturn(true);

		Statement statement = new BoltStatement(new BoltConnection(session), 0, 0, 0);

		statement.setQueryTimeout(1);
		statement.execute(StatementData.STATEMENT_CREATE);

		fail();
	}

	/*------------------------------*/
	/*        getUpdateCount        */
	/*------------------------------*/
	@Test public void getUpdateCountShouldReturnOne() throws SQLException {
		BoltStatement statement = mock(BoltStatement.class);
		when(statement.isClosed()).thenReturn(false);
		when(statement.getUpdateCount()).thenCallRealMethod();
		Whitebox.setInternalState(statement, "currentResultSet", null);
		Whitebox.setInternalState(statement, "currentUpdateCount", 1);

		assertEquals(1, statement.getUpdateCount());
	}

	@Test public void getUpdateCountShouldReturnMinusOne() throws SQLException {
		BoltStatement statement = mock(BoltStatement.class);
		when(statement.isClosed()).thenReturn(false);
		when(statement.getUpdateCount()).thenCallRealMethod();
		Whitebox.setInternalState(statement, "currentResultSet", mock(BoltResultSet.class));

		assertEquals(-1, statement.getUpdateCount());
	}

	@Test public void getUpdateCountShouldThrowExceptionOnClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		BoltStatement statement = mock(BoltStatement.class);
		when(statement.isClosed()).thenReturn(true);
		when(statement.getUpdateCount()).thenCallRealMethod();

		statement.getUpdateCount();
	}

	/*------------------------------*/
	/*         getResultSet         */
	/*------------------------------*/
	@Test public void getResultSetShouldNotReturnNull() throws SQLException {
		BoltStatement statement = mock(BoltStatement.class);
		when(statement.isClosed()).thenReturn(false);
		when(statement.getResultSet()).thenCallRealMethod();
		Whitebox.setInternalState(statement, "currentResultSet", mock(BoltResultSet.class));
		Whitebox.setInternalState(statement, "currentUpdateCount", -1);

		assertTrue(statement.getResultSet() != null);
	}

	@Test public void getResultSetShouldReturnNull() throws SQLException {
		BoltStatement statement = mock(BoltStatement.class);
		when(statement.isClosed()).thenReturn(false);
		when(statement.getResultSet()).thenCallRealMethod();
		Whitebox.setInternalState(statement, "currentUpdateCount", 1);

		assertEquals(null, statement.getResultSet());
	}

	@Test public void getResultSetShouldThrowExceptionOnClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		BoltStatement statement = mock(BoltStatement.class);
		when(statement.isClosed()).thenReturn(true);
		when(statement.getResultSet()).thenCallRealMethod();

		statement.getResultSet();
	}

}
