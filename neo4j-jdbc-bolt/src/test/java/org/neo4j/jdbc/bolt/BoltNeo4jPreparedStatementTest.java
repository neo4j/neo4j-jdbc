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
 * Created on 23/03/16
 */
package org.neo4j.jdbc.bolt;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.neo4j.driver.Result;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.summary.SummaryCounters;
import org.neo4j.jdbc.bolt.data.StatementData;
import org.neo4j.jdbc.bolt.impl.BoltNeo4jConnectionImpl;
import org.neo4j.jdbc.bolt.utils.Mocker;
import org.neo4j.jdbc.impl.ListArray;

import java.sql.BatchUpdateException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.sql.Types.ARRAY;
import static java.sql.Types.BLOB;
import static java.sql.Types.CLOB;
import static java.sql.Types.DATALINK;
import static java.sql.Types.INTEGER;
import static java.sql.Types.JAVA_OBJECT;
import static java.sql.Types.LONGNVARCHAR;
import static java.sql.Types.NCHAR;
import static java.sql.Types.NCLOB;
import static java.sql.Types.NULL;
import static java.sql.Types.NVARCHAR;
import static java.sql.Types.REF;
import static java.sql.Types.ROWID;
import static java.sql.Types.SQLXML;
import static java.sql.Types.STRUCT;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.neo4j.jdbc.bolt.utils.Mocker.mockConnectionClosed;
import static org.neo4j.jdbc.bolt.utils.Mocker.mockOpenConnection;
import static org.neo4j.jdbc.bolt.utils.Mocker.mockOpenConnectionWithResult;
import static org.neo4j.jdbc.bolt.utils.Mocker.mockResultWithUpdateCount;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltNeo4jPreparedStatementTest {

	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	private BoltNeo4jPreparedStatement preparedStatementOneParam;
	private BoltNeo4jPreparedStatement preparedStatementTwoParams;

	private BoltNeo4jResultSet expectedResultSet;
	private ResultSetFactory resultSetFactory;

	@Before public void interceptBoltResultSetConstructor() throws SQLException {
		expectedResultSet = mock(BoltNeo4jResultSet.class);
		resultSetFactory = (statement, iterator, params) -> expectedResultSet;
		preparedStatementOneParam = new BoltNeo4jPreparedStatement(mockOpenConnection(), resultSetFactory, "RETURN $1");
		preparedStatementTwoParams = new BoltNeo4jPreparedStatement(mockOpenConnection(), resultSetFactory, "RETURN $1+$2");
	}

	/*------------------------------*/
	/*             close            */
	/*------------------------------*/
	@Test public void closeShouldCloseExistingResultSet() throws Exception {
		Result mockedDriverResult = mock(Result.class);
		BoltNeo4jConnectionImpl connection = mockOpenConnectionWithResult(mockedDriverResult);
		BoltNeo4jPreparedStatement prStatement = new BoltNeo4jPreparedStatement(connection, resultSetFactory, "");
		final ResultSet resultSet = prStatement.executeQuery();

		prStatement.close();

		assertSame(resultSet, expectedResultSet);
		verify(expectedResultSet, times(1)).close();
	}

	@Test public void closeShouldNotCallCloseOnAnyResultSet() throws Exception {
		PreparedStatement prStatement = BoltNeo4jPreparedStatement.newInstance(mockOpenConnectionWithResult(null), "");

		prStatement.close();

		verify(expectedResultSet, never()).close();
	}

	@Test public void closeMultipleTimesIsNOOP() throws Exception {
		Result mockedDriverResult = mock(Result.class);
		BoltNeo4jConnectionImpl connection = mockOpenConnectionWithResult(mockedDriverResult);
		BoltNeo4jPreparedStatement prStatement = new BoltNeo4jPreparedStatement(connection, resultSetFactory, "");;
		final ResultSet resultSet = prStatement.executeQuery();

		prStatement.close();
		prStatement.close();
		prStatement.close();

		assertSame(resultSet, expectedResultSet);
		verify(resultSet, times(1)).close();
	}

	/*------------------------------*/
	/*           isClosed           */
	/*------------------------------*/
	@Test public void isClosedShouldReturnFalseWhenCreated() throws SQLException {
		BoltNeo4jConnectionImpl connection = mockOpenConnection();
		PreparedStatement prStatement = new BoltNeo4jPreparedStatement(connection, resultSetFactory, "");

		assertFalse(prStatement.isClosed());
	}

	/*------------------------------*/
	/*            setInt            */
	/*------------------------------*/

	@Test public void setIntShouldInsertTheCorrectIntegerValue() throws SQLException {
		this.preparedStatementOneParam.setInt(1, 10);
		assertEquals(10, this.preparedStatementOneParam.getParameters().get("1"));

		this.preparedStatementTwoParams.setInt(2, 125);
		assertEquals(125, this.preparedStatementTwoParams.getParameters().get("2"));
	}

	@Test public void setIntShouldOverrideOldValue() throws SQLException {
		this.preparedStatementOneParam.setInt(1, 10);
		assertEquals(10, this.preparedStatementOneParam.getParameters().get("1"));
		this.preparedStatementOneParam.setInt(1, 99);

		assertEquals(99, this.preparedStatementOneParam.getParameters().get("1"));
	}

	@Test public void setIntShouldThrowExceptionIfIndexDoesNotCorrespondToParameterMarker() throws SQLException {
		expectedEx.expect(SQLException.class);

		this.preparedStatementOneParam.setInt(2, 10);
	}

	@Test public void setIntShouldThrowExceptionIfIndexDoesNotCorrespondToParameterMarkerTwoParams() throws SQLException {
		expectedEx.expect(SQLException.class);

		this.preparedStatementTwoParams.setInt(3, 10);
	}

	@Test public void setIntShouldThrowExceptionIfClosedPreparedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		this.preparedStatementOneParam.close();
		this.preparedStatementOneParam.setInt(1, 10);
	}

	/*------------------------------*/
	/*            setLong           */
	/*------------------------------*/

	@Test public void setLongShouldInsertTheCorrectLongValue() throws SQLException {
		this.preparedStatementOneParam.setLong(1, 10L);
		assertEquals(10L, preparedStatementOneParam.getParameters().get("1"));

		this.preparedStatementTwoParams.setLong(2, 125L);
		assertEquals(125L, preparedStatementTwoParams.getParameters().get("2"));
	}

	@Test public void setLongShouldOverrideOldValue() throws SQLException {
		this.preparedStatementOneParam.setLong(1, 10L);
		assertEquals(10L, preparedStatementOneParam.getParameters().get("1"));
		this.preparedStatementOneParam.setLong(1, 99L);

		assertEquals(99L, preparedStatementOneParam.getParameters().get("1"));
	}

	@Test public void setLongShouldThrowExceptionIfIndexDoesNotCorrespondToParameterMarker() throws SQLException {
		expectedEx.expect(SQLException.class);

		this.preparedStatementOneParam.setLong(2, 10L);
	}

	@Test public void setLongShouldThrowExceptionIfClosedPreparedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		this.preparedStatementOneParam.close();
		this.preparedStatementOneParam.setLong(1, 10L);
	}

	/*------------------------------*/
	/*           setFloat           */
	/*------------------------------*/

	@Test public void setFloatShouldInsertTheCorrectFloatValue() throws SQLException {
		this.preparedStatementOneParam.setFloat(1, 10.5F);
		assertEquals(10.5F, preparedStatementOneParam.getParameters().get("1"));

		this.preparedStatementTwoParams.setFloat(2, 125.5F);
		assertEquals(125.5F, preparedStatementTwoParams.getParameters().get("2"));
	}

	@Test public void setFloatShouldOverrideOldValue() throws SQLException {
		this.preparedStatementOneParam.setFloat(1, 10.5F);
		assertEquals(10.5F, preparedStatementOneParam.getParameters().get("1"));

		this.preparedStatementOneParam.setFloat(1, 55.5F);
		assertEquals(55.5F, preparedStatementOneParam.getParameters().get("1"));
	}

	@Test public void setFloatShouldThrowExceptionIfIndexDoesNotCorrespondToParameterMarker() throws SQLException {
		expectedEx.expect(SQLException.class);

		this.preparedStatementOneParam.setFloat(99, 10.5F);
	}

	@Test public void setFloatShouldThrowExceptionIfClosedPreparedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		this.preparedStatementOneParam.close();
		this.preparedStatementOneParam.setFloat(1, 10.5F);
	}

	/*------------------------------*/
	/*           setDouble          */
	/*------------------------------*/

	@Test public void setDoubleShouldInsertTheCorrectDoubleValue() throws SQLException {
		this.preparedStatementOneParam.setDouble(1, 10.5);
		assertEquals(10.5, preparedStatementOneParam.getParameters().get("1"));

		this.preparedStatementTwoParams.setDouble(2, 125.5);
		assertEquals(125.5, preparedStatementTwoParams.getParameters().get("2"));
	}

	@Test public void setDoubleShouldOverrideOldValue() throws SQLException {
		this.preparedStatementOneParam.setDouble(1, 10.5);
		assertEquals(10.5, preparedStatementOneParam.getParameters().get("1"));
		this.preparedStatementOneParam.setDouble(1, 55.5);

		assertEquals(55.5, preparedStatementOneParam.getParameters().get("1"));
	}

	@Test public void setDoubleShouldThrowExceptionIfIndexDoesNotCorrespondToParameterMarker() throws SQLException {
		expectedEx.expect(SQLException.class);

		this.preparedStatementOneParam.setDouble(99, 10.5);
	}

	@Test public void setDoubleShouldThrowExceptionIfClosedPreparedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		this.preparedStatementOneParam.close();
		this.preparedStatementOneParam.setDouble(1, 10.5);
	}

	/*------------------------------*/
	/*           setShort           */
	/*------------------------------*/

	@Test public void setShortShouldInsertTheCorrectShortValue() throws SQLException {
		this.preparedStatementOneParam.setShort(1, (short) 10);
		assertEquals((short) 10, preparedStatementOneParam.getParameters().get("1"));

		this.preparedStatementTwoParams.setShort(2, (short) 125);
		assertEquals((short) 125, preparedStatementTwoParams.getParameters().get("2"));
	}

	@Test public void setShortShouldOverrideOldValue() throws SQLException {
		this.preparedStatementOneParam.setShort(1, (short) 10);
		assertEquals((short) 10, preparedStatementOneParam.getParameters().get("1"));
		this.preparedStatementOneParam.setShort(1, (short) 20);

		assertEquals((short) 20, preparedStatementOneParam.getParameters().get("1"));
	}

	@Test public void setShortShouldThrowExceptionIfIndexDoesNotCorrespondToParameterMarker() throws SQLException {
		expectedEx.expect(SQLException.class);

		this.preparedStatementOneParam.setShort(99, (short) 10);
	}

	@Test public void setShortShouldThrowExceptionIfClosedPreparedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		this.preparedStatementOneParam.close();
		this.preparedStatementOneParam.setShort(1, (short) 10);
	}

	/*------------------------------*/
	/*           setString          */
	/*------------------------------*/

	@Test public void setStringShouldInsertTheCorrectStringValue() throws SQLException {
		this.preparedStatementOneParam.setString(1, "string");
		assertEquals("string", preparedStatementOneParam.getParameters().get("1"));

		this.preparedStatementTwoParams.setString(2, "text");
		assertEquals("text", preparedStatementTwoParams.getParameters().get("2"));
	}

	@Test public void setStringShouldOverrideOldValue() throws SQLException {
		this.preparedStatementOneParam.setString(1, "string");
		assertEquals("string", preparedStatementOneParam.getParameters().get("1"));
		this.preparedStatementOneParam.setString(1, "otherString");

		assertEquals("otherString", preparedStatementOneParam.getParameters().get("1"));
	}

	@Test public void setStringShouldThrowExceptionIfIndexDoesNotCorrespondToParameterMarker() throws SQLException {
		expectedEx.expect(SQLException.class);

		this.preparedStatementOneParam.setString(99, "string");
	}

	@Test public void setStringShouldThrowExceptionIfClosedPreparedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		this.preparedStatementOneParam.close();
		this.preparedStatementOneParam.setString(1, "string");
	}

	/*------------------------------*/
	/*            setNull           */
	/*------------------------------*/

	@Test public void setNullShouldInsertTheCorrectNullValue() throws SQLException {
		this.preparedStatementOneParam.setNull(1, NULL);
		assertNull(preparedStatementOneParam.getParameters().get("1"));

		this.preparedStatementTwoParams.setNull(2, NULL);
		assertNull(preparedStatementTwoParams.getParameters().get("2"));
	}

	@Test public void setNullShouldThrowExceptionIfIndexDoesNotCorrespondToParameterMarker() throws SQLException {
		expectedEx.expect(SQLException.class);

		this.preparedStatementOneParam.setNull(99, NULL);
	}

	@Test public void setNullShouldThrowExceptionIfClosedPreparedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		this.preparedStatementOneParam.close();
		this.preparedStatementOneParam.setNull(1, NULL);
	}

	@Test public void setNullShouldThrowExceptionNotSupportedOnTypeArray() throws SQLException {
		expectedEx.expect(SQLFeatureNotSupportedException.class);
		this.preparedStatementOneParam.setNull(1, ARRAY);
	}

	@Test public void setNullShouldThrowExceptionNotSupportedOnTypeBlob() throws SQLException {
		expectedEx.expect(SQLFeatureNotSupportedException.class);
		this.preparedStatementOneParam.setNull(1, BLOB);
	}

	@Test public void setNullShouldThrowExceptionNotSupportedOnTypeClob() throws SQLException {
		expectedEx.expect(SQLFeatureNotSupportedException.class);
		this.preparedStatementOneParam.setNull(1, CLOB);
	}

	@Test public void setNullShouldThrowExceptionNotSupportedOnTypeDatalink() throws SQLException {
		expectedEx.expect(SQLFeatureNotSupportedException.class);
		this.preparedStatementOneParam.setNull(1, DATALINK);
	}

	@Test public void setNullShouldThrowExceptionNotSupportedOnTypeJavaObject() throws SQLException {
		expectedEx.expect(SQLFeatureNotSupportedException.class);
		this.preparedStatementOneParam.setNull(1, JAVA_OBJECT);
	}

	@Test public void setNullShouldThrowExceptionNotSupportedOnTypeNChar() throws SQLException {
		expectedEx.expect(SQLFeatureNotSupportedException.class);
		this.preparedStatementOneParam.setNull(1, NCHAR);
	}

	@Test public void setNullShouldThrowExceptionNotSupportedOnTypeNClob() throws SQLException {
		expectedEx.expect(SQLFeatureNotSupportedException.class);
		this.preparedStatementOneParam.setNull(1, NCLOB);
	}

	@Test public void setNullShouldThrowExceptionNotSupportedOnTypeNCVarchar() throws SQLException {
		expectedEx.expect(SQLFeatureNotSupportedException.class);
		this.preparedStatementOneParam.setNull(1, NVARCHAR);
	}

	@Test public void setNullShouldThrowExceptionNotSupportedOnTypeLongNVarchar() throws SQLException {
		expectedEx.expect(SQLFeatureNotSupportedException.class);
		this.preparedStatementOneParam.setNull(1, LONGNVARCHAR);
	}

	@Test public void setNullShouldThrowExceptionNotSupportedOnRef() throws SQLException {
		expectedEx.expect(SQLFeatureNotSupportedException.class);
		this.preparedStatementOneParam.setNull(1, REF);
	}

	@Test public void setNullShouldThrowExceptionNotSupportedOnRowId() throws SQLException {
		expectedEx.expect(SQLFeatureNotSupportedException.class);
		this.preparedStatementOneParam.setNull(1, ROWID);
	}

	@Test public void setNullShouldThrowExceptionNotSupportedOnSQLXML() throws SQLException {
		expectedEx.expect(SQLFeatureNotSupportedException.class);
		this.preparedStatementOneParam.setNull(1, SQLXML);
	}

	@Test public void setNullShouldThrowExceptionNotSupportedOnStruct() throws SQLException {
		expectedEx.expect(SQLFeatureNotSupportedException.class);
		this.preparedStatementOneParam.setNull(1, STRUCT);
	}

	/*------------------------------*/
	/*        clearParameters       */
	/*------------------------------*/

	@Test public void clearParametersShouldDeleteAllParameters() throws SQLException {
		Map<String, Object> value = preparedStatementOneParam.getParameters();
		value.put("1", "string");
		assertEquals(1, value.size());

		this.preparedStatementOneParam.clearParameters();
		assertEquals(0, value.size());
	}

	@Test public void clearParametersShouldThrowExceptionIfStatementClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		this.preparedStatementOneParam.close();

		this.preparedStatementOneParam.clearParameters();
	}

	/*------------------------------*/
	/*          setBoolean          */
	/*------------------------------*/

	@Test public void setBooleanShouldInsertTheCorrectBooleanValue() throws SQLException {
		this.preparedStatementOneParam.setBoolean(1, true);
		assertEquals(true, preparedStatementOneParam.getParameters().get("1"));

		this.preparedStatementTwoParams.setBoolean(2, false);
		assertEquals(false, preparedStatementTwoParams.getParameters().get("2"));
	}

	@Test public void setBooleanShouldOverrideOldValue() throws SQLException {
		this.preparedStatementOneParam.setBoolean(1, true);
		assertEquals(true, preparedStatementOneParam.getParameters().get("1"));
		this.preparedStatementOneParam.setBoolean(1, false);

		assertEquals(false, preparedStatementOneParam.getParameters().get("1"));
	}

	@Test public void setBooleanShouldThrowExceptionIfIndexDoesNotCorrespondToParameterMarker() throws SQLException {
		expectedEx.expect(SQLException.class);

		this.preparedStatementOneParam.setBoolean(99, true);
	}

	@Test public void setBooleanShouldThrowExceptionIfClosedPreparedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		this.preparedStatementOneParam.close();
		this.preparedStatementOneParam.setBoolean(1, true);
	}


	/*------------------------------*/
	/*          setObject           */
	/*------------------------------*/

	@Test public void setObjectShouldInsertTheCorrectObjectValue() throws SQLException {
		Object obj = new HashMap<>();
		this.preparedStatementOneParam.setObject(1, obj);
		assertEquals(obj, preparedStatementOneParam.getParameters().get("1"));

		this.preparedStatementTwoParams.setObject(2, obj);
		assertEquals(obj, preparedStatementTwoParams.getParameters().get("2"));
	}

	@Test public void setObjectShouldOverrideOldValue() throws SQLException {
		Object obj = new HashMap<>();
		this.preparedStatementOneParam.setObject(1, obj);
		assertEquals(obj, preparedStatementOneParam.getParameters().get("1"));

		Object newObj = new ArrayList<>();
		;
		this.preparedStatementOneParam.setObject(1, newObj);
		assertEquals(newObj, preparedStatementOneParam.getParameters().get("1"));
	}

	@Test public void setObjectShouldThrowExceptionIfIndexDoesNotCorrespondToParameterMarker() throws SQLException {
		expectedEx.expect(SQLException.class);

		this.preparedStatementOneParam.setObject(99, new HashMap<>());
	}

	@Test public void setObjectShouldThrowExceptionIfClosedPreparedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		this.preparedStatementOneParam.close();
		this.preparedStatementOneParam.setObject(1, new HashMap<>());
	}

	@Test public void setObjectShouldThrowExceptionIfObjectIsNotSupported() throws SQLException {
		expectedEx.expect(SQLException.class);

		this.preparedStatementOneParam.setObject(1, new Object());
	}

	/*------------------------------*/
	/*     getParameterMetaData     */
	/*------------------------------*/

	@Test public void getParameterMetaDataShouldReturnANewParameterMetaData() throws Exception {
		BoltNeo4jParameterMetaData parameterMetaData = (BoltNeo4jParameterMetaData) preparedStatementOneParam.getParameterMetaData();

		assertSame(preparedStatementOneParam, parameterMetaData.getPreparedStatement());
	}

	@Test public void getParameterMetaDataShouldThrowExceptionIfCalledOnClosedStatement() throws Exception {
		expectedEx.expect(SQLException.class);

		this.preparedStatementOneParam.close();
		this.preparedStatementOneParam.getParameterMetaData();
	}

	/*------------------------------*/
	/*    getResultSetConcurrency   */
	/*------------------------------*/
	@Test public void getResultSetConcurrencyShouldThrowExceptionWhenCalledOnClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		PreparedStatement statement = BoltNeo4jPreparedStatement.newInstance(mockConnectionClosed(), "");
		statement.close();
		statement.getResultSetConcurrency();
	}

	/*------------------------------*/
	/*    getResultSetHoldability   */
	/*------------------------------*/
	@Test public void getResultSetHoldabilityShouldThrowExceptionWhenCalledOnClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		PreparedStatement statement = BoltNeo4jPreparedStatement.newInstance(mockConnectionClosed(), "");
		statement.close();
		statement.getResultSetHoldability();
	}

	/*------------------------------*/
	/*       getResultSetType       */
	/*------------------------------*/
	@Test public void getResultSetTypeShouldThrowExceptionWhenCalledOnClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		PreparedStatement statement = BoltNeo4jPreparedStatement.newInstance(mockConnectionClosed(), "");
		statement.close();
		statement.getResultSetType();
	}

	/*------------------------------*/
	/*          executeQuery        */
	/*------------------------------*/
	@Test public void executeQueryShouldThrowExceptionWhenClosedConnection() throws SQLException {
		expectedEx.expect(SQLException.class);

		PreparedStatement statement = BoltNeo4jPreparedStatement.newInstance(mockConnectionClosed(), "", 0, 0, 0);
		statement.executeQuery();
	}

	@Test public void executeQueryShouldReturnCorrectResultSetStructureConnectionNotAutocommit() throws Exception {
		int[] parameters = {ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT};
		AtomicBoolean called = new AtomicBoolean(false);
		PreparedStatement statement = new BoltNeo4jPreparedStatement(mockOpenConnectionWithResult(null), (stmt, iterator, params) -> {
			assertNotNull(stmt);
			assertNull(iterator);
			assertArrayEquals(parameters, params);
			assertTrue(called.compareAndSet(false, true));
			return expectedResultSet;
		}, "RETURN 42", parameters);
		statement.executeQuery();

		assertTrue(called.get());
	}

	@Test public void executeQueryShouldThrowExceptionOnClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		PreparedStatement statement = BoltNeo4jPreparedStatement.newInstance(mockOpenConnection(), StatementData.STATEMENT_MATCH_ALL);
		statement.close();

		statement.executeQuery();
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

		PreparedStatement statement = BoltNeo4jPreparedStatement
				.newInstance(mockOpenConnectionWithResult(mockResult), StatementData.STATEMENT_CREATE_TWO_PROPERTIES_PARAMETRIC,
						ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
		statement.setString(1, "test");
		statement.setString(2, "test2");
		statement.executeUpdate();
	}

	@Test public void executeUpdateShouldThrowExceptionOnClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		PreparedStatement statement = BoltNeo4jPreparedStatement.newInstance(mockOpenConnection(), StatementData.STATEMENT_MATCH_ALL);
		statement.close();

		statement.executeUpdate();
	}

	@Test public void executeUpdateShouldThrowExceptionOnClosedConnection() throws SQLException {
		expectedEx.expect(SQLException.class);

		PreparedStatement statement = BoltNeo4jPreparedStatement.newInstance(mockConnectionClosed(), "", 0, 0, 0);
		statement.executeUpdate();
	}

	/*------------------------------*/
	/*            execute           */
	/*------------------------------*/
	@Test public void executeShouldRunQuery() throws SQLException {
		BoltNeo4jPreparedStatement statement = mock(BoltNeo4jPreparedStatement.class);
		when(statement.executeQuery()).thenCallRealMethod();

		statement.execute();
	}

	@Test public void executeShouldRunUpdate() throws SQLException {
		Result mockResult = mock(Result.class);
		ResultSummary mockSummary = mock(ResultSummary.class);
		SummaryCounters mockSummaryCounters = mock(SummaryCounters.class);

		when(mockResult.consume()).thenReturn(mockSummary);
		when(mockSummary.counters()).thenReturn(mockSummaryCounters);
		when(mockSummaryCounters.nodesCreated()).thenReturn(1);
		when(mockSummaryCounters.nodesDeleted()).thenReturn(0);

		PreparedStatement statement = BoltNeo4jPreparedStatement
				.newInstance(mockOpenConnectionWithResult(mockResult), StatementData.STATEMENT_CREATE_TWO_PROPERTIES_PARAMETRIC,
						ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
		statement.setString(1, "test");
		statement.setString(2, "test2");
		statement.execute();
	}

	@Test public void executeShouldThrowExceptionOnQueryOnClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		PreparedStatement statement = BoltNeo4jPreparedStatement.newInstance(mockOpenConnection(), StatementData.STATEMENT_MATCH_ALL);
		statement.close();

		statement.execute();
	}

	@Test public void executeShouldThrowExceptionOnUpdateOnClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		PreparedStatement statement = BoltNeo4jPreparedStatement.newInstance(mockOpenConnection(), StatementData.STATEMENT_CREATE);
		statement.close();

		statement.execute();
	}

	@Test public void executeShouldThrowExceptionOnQueryOnClosedConnection() throws SQLException {
		expectedEx.expect(SQLException.class);

		PreparedStatement statement = BoltNeo4jPreparedStatement.newInstance(mockConnectionClosed(), "", 0, 0, 0);
		when(statement.executeQuery()).thenCallRealMethod();
		statement.execute();
	}

	@Test public void executeShouldThrowExceptionOnUpdateOnClosedConnection() throws SQLException {
		expectedEx.expect(SQLException.class);

		PreparedStatement statement = BoltNeo4jPreparedStatement.newInstance(mockConnectionClosed(), "", 0, 0, 0);
		when(statement.executeUpdate()).thenCallRealMethod();
		statement.execute();
	}

	/*------------------------------*/
	/*        getUpdateCount        */
	/*------------------------------*/
	@Test public void getUpdateCountShouldReturnOne() throws SQLException {
		Result result = mockResultWithUpdateCount(1);
		BoltNeo4jPreparedStatement statement = new BoltNeo4jPreparedStatement(mockOpenConnectionWithResult(result), resultSetFactory, "RETURN 42");
		statement.executeUpdate();

		assertEquals(1, statement.getUpdateCount());
	}

	@Test public void getUpdateCountShouldReturnMinusOne() throws SQLException {
		ResultSetFactory nullRsFactory = (stmt, iterator, params) -> null;
		BoltNeo4jPreparedStatement statement = new BoltNeo4jPreparedStatement(mockOpenConnection(), nullRsFactory, "RETURN 42");

		assertEquals(-1, statement.getUpdateCount());
	}

	@Test public void getUpdateCountShouldThrowExceptionOnClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		PreparedStatement statement = BoltNeo4jPreparedStatement.newInstance(mockOpenConnection(), StatementData.STATEMENT_MATCH_ALL);
		statement.close();

		statement.getUpdateCount();
	}

	/*------------------------------*/
	/*         getResultSet         */
	/*------------------------------*/
	@Test public void getResultSetShouldNotReturnNull() throws SQLException {
		Result result = mock(Result.class, RETURNS_DEEP_STUBS);
		BoltNeo4jPreparedStatement statement = new BoltNeo4jPreparedStatement(mockOpenConnectionWithResult(result), resultSetFactory, "RETURN 42");
		statement.executeQuery();

		assertNotNull(statement.getResultSet());
	}

	@Test public void getResultSetShouldReturnNull() throws SQLException {
		BoltNeo4jStatement statement = mock(BoltNeo4jStatement.class);
		when(statement.isClosed()).thenReturn(false);
		when(statement.getResultSet()).thenCallRealMethod();

		assertNull(statement.getResultSet());
	}

	@Test public void getResultSetShouldThrowExceptionOnClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		PreparedStatement statement = BoltNeo4jPreparedStatement.newInstance(mockOpenConnection(), StatementData.STATEMENT_MATCH_ALL);
		statement.close();

		statement.getResultSet();
	}

	/*------------------------------*/
	/*           addBatch           */
	/*------------------------------*/

	@Test public void addBatchShouldAddParametersToStack() throws SQLException {
		BoltNeo4jPreparedStatement stmt = new BoltNeo4jPreparedStatement(mockOpenConnection(), resultSetFactory, "RETURN $1");

		stmt.setInt(1, 1);
		stmt.addBatch();
		stmt.setInt(1, 2);
		stmt.addBatch();

		assertEquals(Arrays.asList(new HashMap<String, Object>() {
			{
				this.put("1", 1);
			}
		}, new HashMap<String, Object>() {
			{
				this.put("1", 2);
			}
		}), stmt.getBatchParameters());
	}

	@Test public void addBatchShouldClearParameters() throws SQLException {
		BoltNeo4jPreparedStatement stmt = new BoltNeo4jPreparedStatement(mockOpenConnection(), resultSetFactory, "RETURN $1");
		stmt.setInt(1, 1);

		stmt.addBatch();

		assertEquals(Collections.EMPTY_MAP, stmt.getParameters());
	}

	@Test public void addBatchShouldThrowExceptionIfClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		PreparedStatement stmt = BoltNeo4jPreparedStatement.newInstance(Mocker.mockOpenConnection(), "?");
		stmt.setInt(1, 1);
		stmt.close();
		stmt.addBatch();
	}

	/*------------------------------*/
	/*          clearBatch          */
	/*------------------------------*/

	@Test public void clearBatchShouldWork() throws SQLException {
		BoltNeo4jPreparedStatement stmt = new BoltNeo4jPreparedStatement(mockOpenConnection(), resultSetFactory, "RETURN $1");
		stmt.setInt(1, 1);

		stmt.clearBatch();

		assertEquals(Collections.EMPTY_LIST, stmt.getBatchParameters());
	}

	@Test public void clearBatchShouldThrowExceptionIfClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		PreparedStatement stmt = BoltNeo4jPreparedStatement.newInstance(Mocker.mockOpenConnection(), "?");
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

		PreparedStatement stmt = BoltNeo4jPreparedStatement.newInstance(connection, "MATCH n WHERE id(n) = ? SET n.property=1");
		stmt.setInt(1, 1);
		stmt.addBatch();
		stmt.setInt(1, 2);
		stmt.addBatch();

		assertArrayEquals(new int[] { 1, 1 }, stmt.executeBatch());
	}

	@Test public void executeBatchShouldThrowExceptionOnError() throws SQLException {
		PreparedStatement stmt = BoltNeo4jPreparedStatement.newInstance(Mocker.mockOpenConnection(), "?");
		stmt.setInt(1, 1);
		stmt.addBatch();
		stmt.setInt(1, 2);
		stmt.addBatch();

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

		PreparedStatement stmt = BoltNeo4jPreparedStatement.newInstance(Mocker.mockConnectionClosed(), "?");
		stmt.setInt(1, 1);
		stmt.addBatch();
		stmt.setInt(1, 2);
		stmt.addBatch();
		stmt.close();

		stmt.executeBatch();
	}

	/*------------------------------*/
	/*         getConnection        */
	/*------------------------------*/

	@Test public void getConnectionShouldWork() throws SQLException {
		PreparedStatement stmt = BoltNeo4jPreparedStatement.newInstance(Mocker.mockOpenConnection(), "?");

		assertNotNull(stmt.getConnection());
		assertEquals(Mocker.mockOpenConnection().getClass(), stmt.getConnection().getClass());
	}

	@Test public void getConnectionShouldThrowExceptionOnClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		PreparedStatement stmt = BoltNeo4jPreparedStatement.newInstance(Mocker.mockConnectionClosed(), "?");
		stmt.close();

		stmt.getConnection();
	}

	/*------------------------------*/
	/*           setArray           */
	/*------------------------------*/

	@Test public void setArrayShouldInsertTheCorrectArray() throws SQLException {
		ListArray array = new ListArray(Arrays.asList(1L,2L,4L), INTEGER);
		this.preparedStatementOneParam.setArray(1, array);

		assertArrayEquals(new Long[]{1L,2L,4L}, (Long[]) preparedStatementOneParam.getParameters().get("1"));
	}
}
