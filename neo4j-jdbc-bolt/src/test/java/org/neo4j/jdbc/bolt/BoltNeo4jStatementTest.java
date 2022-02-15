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
 * Created on 19/02/16
 */
package org.neo4j.jdbc.bolt;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.summary.SummaryCounters;
import org.neo4j.jdbc.bolt.data.StatementData;
import org.neo4j.jdbc.bolt.impl.BoltNeo4jConnectionImpl;
import org.neo4j.jdbc.bolt.utils.Mocker;
import org.neo4j.jdbc.bolt.utils.ReflectionUtil;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.neo4j.jdbc.bolt.utils.Mocker.mockConnectionClosed;
import static org.neo4j.jdbc.bolt.utils.Mocker.mockDriverOpen;
import static org.neo4j.jdbc.bolt.utils.Mocker.mockOpenConnection;
import static org.neo4j.jdbc.bolt.utils.Mocker.mockOpenConnectionWithResult;
import static org.neo4j.jdbc.bolt.utils.Mocker.mockResultWithUpdateCount;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltNeo4jStatementTest {

	@Rule
	public ExpectedException expectedEx = ExpectedException.none();
	private BoltNeo4jResultSet expectedResultSet;
	private ResultSetFactory resultSetFactory;

	@Before public void interceptBoltResultSetConstructor() throws Exception {
		expectedResultSet = mock(BoltNeo4jResultSet.class);
		resultSetFactory = (debug, statement, iterator, params) -> expectedResultSet;
		BoltNeo4jResultSet mockedRS = mock(BoltNeo4jResultSet.class);
		doNothing().when(mockedRS).close();
	}

	/*------------------------------*/
	/*             close            */
	/*------------------------------*/
	@Test public void closeShouldCloseExistingResultSet() throws Exception {
		Result result = mock(Result.class);
		Statement statement = new BoltNeo4jStatement(mockOpenConnectionWithResult(result), resultSetFactory);
		final ResultSet resultSet = statement.executeQuery(StatementData.STATEMENT_MATCH_ALL);
		statement.close();

		assertSame(expectedResultSet, resultSet);
		verify(expectedResultSet, times(1)).close();
	}

	@Test public void closeMultipleTimesIsNOOP() throws Exception {
		Result mockedResult = mock(Result.class);
		Statement statement = new BoltNeo4jStatement(mockOpenConnectionWithResult(mockedResult), resultSetFactory);
		final ResultSet resultSet = statement.executeQuery(StatementData.STATEMENT_MATCH_ALL);

		statement.close();
		statement.close();
		statement.close();

		verify(resultSet, times(1)).close();
	}

	@Test public void closeShouldNotTouchTheTransaction() throws Exception {

		Transaction mockTransaction = mock(Transaction.class);

		BoltNeo4jConnectionImpl mockConnection = mockOpenConnection();
		when(mockConnection.getTransaction()).thenReturn(mockTransaction);

		Statement statement = BoltNeo4jStatement.newInstance(false, mockConnection);

		statement.close();

		verify(mockTransaction, never()).rollback();
		verify(mockTransaction, never()).commit();
		verify(mockTransaction, never()).close();
	}

	/*------------------------------*/
	/*           isClosed           */
	/*------------------------------*/

	@Test public void isClosedShouldReturnFalseWhenCreated() throws SQLException {
		Statement statement = BoltNeo4jStatement.newInstance(false, mockOpenConnection());
		assertFalse(statement.isClosed());
	}

	/*------------------------------*/
	/*          executeQuery        */
	/*------------------------------*/

	@Test public void executeQueryShouldRun() throws SQLException {
		Result mockResult = mock(Result.class);

		Statement statement = BoltNeo4jStatement
				.newInstance(false, mockOpenConnectionWithResult(mockResult), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
						ResultSet.HOLD_CURSORS_OVER_COMMIT);
		statement.executeQuery(StatementData.STATEMENT_CREATE);
	}

	@Test public void executeQueryShouldThrowExceptionWhenClosedConnection() throws SQLException {
		expectedEx.expect(SQLException.class);

		Statement statement = BoltNeo4jStatement.newInstance(false, mockConnectionClosed(), 0, 0, 0);
		statement.executeQuery(StatementData.STATEMENT_MATCH_ALL);
	}

	@Test public void executeQueryShouldReturnCorrectResultSetStructureConnectionNotAutocommit() throws Exception {
		BoltNeo4jConnectionImpl mockConnection = mockOpenConnectionWithResult(null);

		Statement statement = BoltNeo4jStatement
				.newInstance(false, mockConnection, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
		statement.executeQuery(StatementData.STATEMENT_MATCH_ALL);

//		verifyStatic(BoltNeo4jResultSet.class);
		BoltNeo4jResultSet.newInstance(false, statement, null, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
	}

	@Test public void executeQueryShouldThrowExceptionOnClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Statement statement = BoltNeo4jStatement.newInstance(false, mockOpenConnection());
		statement.close();

		statement.executeQuery(StatementData.STATEMENT_MATCH_ALL);
	}

	@Test public void executeQueryShouldThrowExceptionOnPreparedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Connection connection = new BoltNeo4jConnectionImpl(mockDriverOpen(), new Properties(), "");
		Statement statement = connection.prepareStatement("");
		statement.executeQuery(StatementData.STATEMENT_MATCH_ALL);
	}

	@Ignore @Test public void executeQueryShouldThrowExceptionOnCallableStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Connection connection = new BoltNeo4jConnectionImpl(mockDriverOpen(), new Properties(), "");
		Statement statement = connection.prepareCall("");
		statement.executeQuery(StatementData.STATEMENT_MATCH_ALL);
	}

	@Ignore @Test public void executeQueryShouldThrowExceptionOnTimeoutExceeded() throws SQLException {
		expectedEx.expect(SQLException.class);

		Statement statement = BoltNeo4jStatement.newInstance(false, new BoltNeo4jConnectionImpl(mockDriverOpen(), new Properties(), ""), 0, 0, 0);
		statement.setQueryTimeout(1);
		statement.executeQuery(StatementData.STATEMENT_CREATE);

		fail();
	}

	/*------------------------------*/
	/*         executeUpdate        */
	/*------------------------------*/

	@Test public void executeUpdateShouldRun() throws SQLException {
		Result mockResult = mock(Result.class);
		ResultSummary mockSummary = mock(ResultSummary.class);
		SummaryCounters mockSummaryCounters = mock(SummaryCounters.class);

		when(mockResult.consume()).thenReturn(mockSummary);
		when(mockSummary.counters()).thenReturn(mockSummaryCounters);
		when(mockSummaryCounters.nodesCreated()).thenReturn(1);
		when(mockSummaryCounters.nodesDeleted()).thenReturn(0);

		Statement statement = BoltNeo4jStatement
				.newInstance(false, mockOpenConnectionWithResult(mockResult), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
						ResultSet.HOLD_CURSORS_OVER_COMMIT);
		statement.executeUpdate(StatementData.STATEMENT_CREATE);
	}

	@Test public void executeUpdateShouldThrowExceptionOnClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Connection connection = new BoltNeo4jConnectionImpl(mockDriverOpen(), new Properties(), "");
		Statement statement = connection.createStatement();
		statement.close();
		statement.executeUpdate(StatementData.STATEMENT_CREATE);
	}

	@Test public void executeUpdateShouldThrowExceptionOnPreparedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Connection connection = new BoltNeo4jConnectionImpl(mockDriverOpen(), new Properties(), "");
		Statement statement = connection.prepareStatement("");
		statement.executeUpdate(StatementData.STATEMENT_CREATE);
	}

	@Ignore @Test public void executeUpdateShouldThrowExceptionOnCallableStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Connection connection = new BoltNeo4jConnectionImpl(mockDriverOpen(), new Properties(), "");
		Statement statement = connection.prepareCall(null);
		statement.executeUpdate(StatementData.STATEMENT_CREATE);
	}

	@Ignore @Test public void executeUpdateShouldThrowExceptionOnTimeoutExceeded() throws SQLException {
		expectedEx.expect(SQLException.class);

		Statement statement = BoltNeo4jStatement.newInstance(false, new BoltNeo4jConnectionImpl(mockDriverOpen(), new Properties(), ""), 0, 0, 0);
		statement.setQueryTimeout(1);
		statement.executeUpdate(StatementData.STATEMENT_CREATE);

		fail();
	}

	/*------------------------------*/
	/*    getResultSetConcurrency   */
	/*------------------------------*/
	@Test public void getResultSetConcurrencyShouldThrowExceptionWhenCalledOnClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Statement statement = BoltNeo4jStatement.newInstance(false, mockConnectionClosed());
		statement.close();
		statement.getResultSetConcurrency();
	}

	/*------------------------------*/
	/*    getResultSetHoldability   */
	/*------------------------------*/
	@Test public void getResultSetHoldabilityShouldThrowExceptionWhenCalledOnClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Statement statement = BoltNeo4jStatement.newInstance(false, mockConnectionClosed());
		statement.close();
		statement.getResultSetHoldability();
	}

	/*------------------------------*/
	/*       getResultSetType       */
	/*------------------------------*/
	@Test public void getResultSetTypeShouldThrowExceptionWhenCalledOnClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Statement statement = BoltNeo4jStatement.newInstance(false, mockConnectionClosed());
		statement.close();
		statement.getResultSetType();
	}

	/*------------------------------*/
	/*            execute           */
	/*------------------------------*/
	@Test public void executeShouldRunQuery() throws SQLException {
		Statement statement = new BoltNeo4jStatement(
				mockOpenConnectionWithResult(mockResultWithUpdateCount(1)),
				resultSetFactory,
				ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
		statement.execute(StatementData.STATEMENT_MATCH_ALL);
	}

	@Test public void executeShouldRunUpdate() throws SQLException {
		Result mockResult = mock(Result.class);
		ResultSummary mockSummary = mock(ResultSummary.class);
		SummaryCounters mockSummaryCounters = mock(SummaryCounters.class);

		when(mockResult.consume()).thenReturn(mockSummary);
		when(mockSummary.counters()).thenReturn(mockSummaryCounters);
		when(mockSummaryCounters.nodesCreated()).thenReturn(1);
		when(mockSummaryCounters.nodesDeleted()).thenReturn(0);

		Statement statement = BoltNeo4jStatement
				.newInstance(false, mockOpenConnectionWithResult(mockResult), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
						ResultSet.HOLD_CURSORS_OVER_COMMIT);
		statement.execute(StatementData.STATEMENT_CREATE);
	}

	@Test public void executeShouldThrowExceptionOnQueryWhenClosedConnection() throws SQLException {
		expectedEx.expect(SQLException.class);

		Statement statement = BoltNeo4jStatement.newInstance(false, mockConnectionClosed(), 0, 0, 0);
		statement.execute(StatementData.STATEMENT_MATCH_ALL);
	}

	@Test public void executeShouldThrowExceptionOnUpdateWhenClosedConnection() throws SQLException {
		expectedEx.expect(SQLException.class);

		Statement statement = BoltNeo4jStatement.newInstance(false, mockConnectionClosed(), 0, 0, 0);
		statement.execute(StatementData.STATEMENT_CREATE);
	}

	@Test public void executeShouldThrowExceptionOnQueryOnClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Statement statement = BoltNeo4jStatement.newInstance(false, mockOpenConnection());
		statement.close();

		statement.execute(StatementData.STATEMENT_MATCH_ALL);
	}

	@Test public void executeShouldThrowExceptionOnUpdateOnClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Statement statement = BoltNeo4jStatement.newInstance(false, mockOpenConnection());
		statement.close();

		statement.execute(StatementData.STATEMENT_CREATE);
	}

	@Test public void executeShouldThrowExceptionOnQueryOnPreparedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Connection connection = new BoltNeo4jConnectionImpl(mockDriverOpen(), new Properties(), "");
		Statement statement = connection.prepareStatement("");

		statement.execute(StatementData.STATEMENT_MATCH_ALL);
	}

	@Test public void executeShouldThrowExceptionOnUpdateOnPreparedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Connection connection = new BoltNeo4jConnectionImpl(mockDriverOpen(), new Properties(), "");
		Statement statement = connection.prepareStatement("");
		statement.executeUpdate(StatementData.STATEMENT_CREATE);
	}

	@Ignore @Test public void executeShouldThrowExceptionOnCallableStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Connection connection = new BoltNeo4jConnectionImpl(mockDriverOpen(), new Properties(), "");
		Statement statement = connection.prepareCall("");
		statement.execute(StatementData.STATEMENT_MATCH_ALL);
	}

	@Ignore @Test public void executeShouldThrowExceptionOnTimeoutExceeded() throws SQLException {
		expectedEx.expect(SQLException.class);

		Statement statement = BoltNeo4jStatement.newInstance(false, new BoltNeo4jConnectionImpl(mockDriverOpen(), new Properties(), ""), 0, 0, 0);
		statement.setQueryTimeout(1);
		statement.execute(StatementData.STATEMENT_CREATE);

		fail();
	}

	/*------------------------------*/
	/*        getUpdateCount        */
	/*------------------------------*/
	@Test public void getUpdateCountShouldReturnOne() throws SQLException {
		BoltNeo4jStatement statement = new BoltNeo4jStatement(
				mockOpenConnectionWithResult(mockResultWithUpdateCount(1)),
				resultSetFactory
		);
		statement.execute("RETURN 42");

		assertEquals(1, statement.getUpdateCount());
	}

	@Test public void getUpdateCountShouldReturnMinusOne() throws SQLException {
		BoltNeo4jStatement statement = new BoltNeo4jStatement(mockOpenConnection(), resultSetFactory);

		assertEquals(-1, statement.getUpdateCount());
	}

	@Test public void getUpdateCountShouldThrowExceptionOnClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Statement statement = BoltNeo4jStatement.newInstance(false, mockOpenConnection());
		statement.close();

		statement.getUpdateCount();
	}

	/*------------------------------*/
	/*         getResultSet         */
	/*------------------------------*/
	@Test public void getResultSetShouldNotReturnNull() throws SQLException {
		Result result = mock(Result.class);
		BoltNeo4jStatement statement = new BoltNeo4jStatement(mockOpenConnectionWithResult(result), resultSetFactory);
		statement.executeQuery("RETURN 42");

		assertNotNull(statement.getResultSet());
	}

	@Test public void getResultSetShouldReturnNull() throws SQLException {
		BoltNeo4jStatement statement = mock(BoltNeo4jStatement.class);
		when(statement.isClosed()).thenReturn(false);
		when(statement.getResultSet()).thenCallRealMethod();
//		Whitebox.setInternalState(statement, "currentUpdateCount", 1);

		assertEquals(null, statement.getResultSet());
	}

	@Test public void getResultSetShouldThrowExceptionOnClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Statement statement = BoltNeo4jStatement.newInstance(false, mockOpenConnection());
		statement.close();

		statement.getResultSet();
	}


	/*------------------------------*/
	/*           addBatch           */
	/*------------------------------*/

	@Test public void addBatchShouldAddStringToStack() throws SQLException {
		BoltNeo4jStatement statement = new BoltNeo4jStatement(mockOpenConnection(), resultSetFactory);

		statement.addBatch("MATCH n WHERE id(n) = 1 SET n.property=1");
		statement.addBatch("MATCH n WHERE id(n) = 2 SET n.property=2");

		assertEquals(Arrays.asList(
				"MATCH n WHERE id(n) = 1 SET n.property=1",
				"MATCH n WHERE id(n) = 2 SET n.property=2"), statement.getBatchStatements());
	}

	@Test public void addBatchShouldThrowExceptionIfClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Statement stmt = BoltNeo4jStatement.newInstance(false, Mocker.mockOpenConnection());
		stmt.close();
		stmt.addBatch("");
	}

	/*------------------------------*/
	/*          clearBatch          */
	/*------------------------------*/

	@Test public void clearBatchShouldWork() throws SQLException {
		BoltNeo4jStatement statement = mock(BoltNeo4jStatement.class);
		statement.addBatch("MATCH n WHERE id(n) = 1 SET n.property=1");
		statement.addBatch("MATCH n WHERE id(n) = 2 SET n.property=2");

		statement.clearBatch();

		assertEquals(Collections.EMPTY_LIST, statement.getBatchStatements());
	}

	@Test public void clearBatchShouldThrowExceptionIfClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Statement stmt = BoltNeo4jStatement.newInstance(false, Mocker.mockOpenConnection());
		stmt.close();
		stmt.clearBatch();
	}

	/*------------------------------*/
	/*         executeBatch         */
	/*------------------------------*/

	@Test public void executeBatchShouldWork() throws SQLException {
		Transaction transaction = mock(Transaction.class);
		BoltNeo4jConnectionImpl connection = mockOpenConnection();
		when(connection.getTransaction()).thenReturn(transaction);
		when(connection.getAutoCommit()).thenReturn(true);
		Result stmtResult = mock(Result.class);
		ResultSummary resultSummary = mock(ResultSummary.class);
		SummaryCounters summaryCounters = mock(SummaryCounters.class);

        when(transaction.run(anyString(), anyMap())).thenReturn(stmtResult);
		when(stmtResult.consume()).thenReturn(resultSummary);
		when(resultSummary.counters()).thenReturn(summaryCounters);
		when(summaryCounters.nodesCreated()).thenReturn(1);
		when(summaryCounters.nodesDeleted()).thenReturn(0);

		Statement stmt = BoltNeo4jStatement.newInstance(false, connection);
		String str1 = "MATCH n WHERE id(n) = 1 SET n.property=1";
		String str2 = "MATCH n WHERE id(n) = 2 SET n.property=2";
		stmt.addBatch(str1);
		stmt.addBatch(str2);

		assertArrayEquals(new int[] { 1, 1 }, stmt.executeBatch());
	}

	@Test public void executeBatchShouldThrowExceptionOnError() throws SQLException {
		Statement stmt = BoltNeo4jStatement.newInstance(false, Mocker.mockOpenConnection());
		stmt.addBatch("MATCH n WHERE id(n) = 1 SET n.property=1");
		stmt.addBatch("MATCH n WHERE id(n) = 2 SET n.property=2");

		class TxRunRuntimeException extends RuntimeException {}
		Transaction transaction = mock(Transaction.class);
		Mockito.when(transaction.run(anyString(), anyMap())).thenThrow(TxRunRuntimeException.class);
		BoltNeo4jConnection connection = (BoltNeo4jConnection) stmt.getConnection();
		Mockito.when(connection.getTransaction()).thenReturn(transaction);

		try {
			stmt.executeBatch();
			fail();
		} catch (BatchUpdateException e) {
			Throwable wrappedException = e.getCause();
			assertTrue("actual error is wrapped in SQLException", wrappedException instanceof SQLException);
			assertTrue("actual error comes from `transaction.run`", wrappedException.getCause() instanceof TxRunRuntimeException);
			assertArrayEquals(new int[0], e.getUpdateCounts());
		}
	}

	@Test public void executeBatchShouldThrowExceptionOnClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		Statement stmt = BoltNeo4jStatement.newInstance(false, Mocker.mockConnectionClosed());
		String str1 = "MATCH n WHERE id(n) = 1 SET n.property=1";
		String str2 = "MATCH n WHERE id(n) = 2 SET n.property=2";
		stmt.addBatch(str1);
		stmt.addBatch(str2);
		stmt.close();

		stmt.executeBatch();
	}

	/*------------------------------*/
	/*         getConnection        */
	/*------------------------------*/

	@Test public void getConnectionShouldWork() throws SQLException {
		Statement stmt = BoltNeo4jStatement.newInstance(false, Mocker.mockOpenConnection());

		assertNotNull(stmt.getConnection());
		assertEquals(Mocker.mockOpenConnection().getClass(), stmt.getConnection().getClass());
	}

	@Test public void getConnectionShouldThrowExceptionOnClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Statement stmt = BoltNeo4jStatement.newInstance(false, Mocker.mockConnectionClosed());
		stmt.close();

		stmt.getConnection();
	}

	@Test public void shouldCreateABrandNewSession() throws Exception {
		Properties props = new Properties();
		Connection connection = new BoltNeo4jConnectionImpl(mockDriverOpen(), props, "");
		PreparedStatement statement = connection.prepareStatement(StatementData.STATEMENT_MATCH_ALL);
		Session session = ReflectionUtil.getPrivateField(connection, "session", Session.class);
		statement.executeQuery();
		PreparedStatement otherStatement = connection.prepareStatement(StatementData.STATEMENT_MATCH_ALL);
		Session otherSession = ReflectionUtil.getPrivateField(connection, "session", Session.class);
		otherStatement.executeQuery();
		assertNotSame(session, otherSession);
	}
}
