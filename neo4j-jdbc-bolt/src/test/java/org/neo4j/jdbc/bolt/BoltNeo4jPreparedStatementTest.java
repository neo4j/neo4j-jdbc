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

import org.neo4j.jdbc.Neo4jPreparedStatement;
import org.neo4j.jdbc.bolt.data.StatementData;
import org.neo4j.jdbc.bolt.utils.Mocker;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.summary.ResultSummary;
import org.neo4j.driver.v1.summary.SummaryCounters;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.sql.BatchUpdateException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import static org.neo4j.jdbc.bolt.utils.Mocker.*;
import static java.sql.Types.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.verifyNew;
import static org.powermock.api.mockito.PowerMockito.whenNew;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
@RunWith(PowerMockRunner.class) @PrepareForTest({ BoltNeo4jPreparedStatement.class, BoltNeo4jResultSet.class }) public class BoltNeo4jPreparedStatementTest {

	@Rule public ExpectedException expectedEx = ExpectedException.none();

	private Neo4jPreparedStatement preparedStatementOneParam;
	private Neo4jPreparedStatement preparedStatementTwoParams;

	private BoltNeo4jResultSet mockedRS;

	@Before public void interceptBoltResultSetConstructor() throws Exception {
		this.preparedStatementOneParam = new BoltNeo4jPreparedStatement(mockConnectionOpenWithTransactionThatReturns(null), "MATCH n RETURN n WHERE n.name = ?");
		this.preparedStatementTwoParams = new BoltNeo4jPreparedStatement(mockConnectionOpenWithTransactionThatReturns(null),
				"MATCH n RETURN n WHERE n.name = ? AND n.surname = ?");

		this.mockedRS = mock(BoltNeo4jResultSet.class);
		doNothing().when(this.mockedRS).close();
		whenNew(BoltNeo4jResultSet.class).withArguments(anyObject(), anyObject()).thenReturn(this.mockedRS);
	}

	/*------------------------------*/
	/*             close            */
	/*------------------------------*/
	@Test public void closeShouldCloseExistingResultSet() throws Exception {
		Neo4jPreparedStatement prStatement = new BoltNeo4jPreparedStatement(mockConnectionOpenWithTransactionThatReturns(null), "");
		assertNotNull(prStatement.executeQuery());
		prStatement.close();

		verify(this.mockedRS, times(1)).close();
	}

	@Test public void closeShouldNotCallCloseOnAnyResultSet() throws Exception {
		Neo4jPreparedStatement prStatement = new BoltNeo4jPreparedStatement(mockConnectionOpenWithTransactionThatReturns(null), "");
		prStatement.close();

		verify(this.mockedRS, never()).close();
	}

	@Test public void closeMultipleTimesIsNOOP() throws Exception {
		Neo4jPreparedStatement prStatement = new BoltNeo4jPreparedStatement(mockConnectionOpenWithTransactionThatReturns(null), "");
		prStatement.executeQuery();
		prStatement.close();
		prStatement.close();
		prStatement.close();

		verify(this.mockedRS, times(1)).close();
	}

	/*------------------------------*/
	/*           isClosed           */
	/*------------------------------*/
	@Test public void isClosedShouldReturnFalseWhenCreated() throws SQLException {
		Neo4jPreparedStatement statement = new BoltNeo4jPreparedStatement(mockConnectionOpen(), "");

		assertFalse(statement.isClosed());
	}

	/*------------------------------*/
	/*            setInt            */
	/*------------------------------*/

	@Test public void setIntShouldInsertTheCorrectIntegerValue() throws SQLException {
		this.preparedStatementOneParam.setInt(1, 10);
		HashMap<String, Object> value = Whitebox.getInternalState(this.preparedStatementOneParam, "parameters");
		assertEquals(10, value.get("1"));

		this.preparedStatementTwoParams.setInt(2, 125);
		value = Whitebox.getInternalState(this.preparedStatementTwoParams, "parameters");
		assertEquals(125, value.get("2"));
	}

	@Test public void setIntShouldOverrideOldValue() throws SQLException {
		this.preparedStatementOneParam.setInt(1, 10);
		HashMap<String, Object> value = Whitebox.getInternalState(this.preparedStatementOneParam, "parameters");
		assertEquals(10, value.get("1"));

		this.preparedStatementOneParam.setInt(1, 99);
		value = Whitebox.getInternalState(this.preparedStatementOneParam, "parameters");
		assertEquals(99, value.get("1"));
	}

	@Test public void setIntShouldThrowExceptionIfIndexDoesNotCorrespondToParameterMarker() throws SQLException {
		expectedEx.expect(SQLException.class);

		this.preparedStatementOneParam.setInt(99, 10);
	}

	@Test public void setIntShouldThrowExceptionIfIndexDoesNotCorrespondToParameterMarkerTwoParams() throws SQLException {
		expectedEx.expect(SQLException.class);

		this.preparedStatementTwoParams.setInt(99, 10);
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
		HashMap<String, Object> value = Whitebox.getInternalState(this.preparedStatementOneParam, "parameters");
		assertEquals(10L, value.get("1"));

		this.preparedStatementTwoParams.setLong(2, 125L);
		value = Whitebox.getInternalState(this.preparedStatementTwoParams, "parameters");
		assertEquals(125L, value.get("2"));
	}

	@Test public void setLongShouldOverrideOldValue() throws SQLException {
		this.preparedStatementOneParam.setLong(1, 10L);
		HashMap<String, Object> value = Whitebox.getInternalState(this.preparedStatementOneParam, "parameters");
		assertEquals(10L, value.get("1"));

		this.preparedStatementOneParam.setLong(1, 99L);
		value = Whitebox.getInternalState(this.preparedStatementOneParam, "parameters");
		assertEquals(99L, value.get("1"));
	}

	@Test public void setLongShouldThrowExceptionIfIndexDoesNotCorrespondToParameterMarker() throws SQLException {
		expectedEx.expect(SQLException.class);

		this.preparedStatementOneParam.setLong(99, 10L);
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
		HashMap<String, Object> value = Whitebox.getInternalState(this.preparedStatementOneParam, "parameters");
		assertEquals(10.5F, value.get("1"));

		this.preparedStatementTwoParams.setFloat(2, 125.5F);
		value = Whitebox.getInternalState(this.preparedStatementTwoParams, "parameters");
		assertEquals(125.5F, value.get("2"));
	}

	@Test public void setFloatShouldOverrideOldValue() throws SQLException {
		this.preparedStatementOneParam.setFloat(1, 10.5F);
		HashMap<String, Object> value = Whitebox.getInternalState(this.preparedStatementOneParam, "parameters");
		assertEquals(10.5F, value.get("1"));

		this.preparedStatementOneParam.setFloat(1, 55.5F);
		value = Whitebox.getInternalState(this.preparedStatementOneParam, "parameters");
		assertEquals(55.5F, value.get("1"));
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
		HashMap<String, Object> value = Whitebox.getInternalState(this.preparedStatementOneParam, "parameters");
		assertEquals(10.5, value.get("1"));

		this.preparedStatementTwoParams.setDouble(2, 125.5);
		value = Whitebox.getInternalState(this.preparedStatementTwoParams, "parameters");
		assertEquals(125.5, value.get("2"));
	}

	@Test public void setDoubleShouldOverrideOldValue() throws SQLException {
		this.preparedStatementOneParam.setDouble(1, 10.5);
		HashMap<String, Object> value = Whitebox.getInternalState(this.preparedStatementOneParam, "parameters");
		assertEquals(10.5, value.get("1"));

		this.preparedStatementOneParam.setDouble(1, 55.5);
		value = Whitebox.getInternalState(this.preparedStatementOneParam, "parameters");
		assertEquals(55.5, value.get("1"));
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
		HashMap<String, Object> value = Whitebox.getInternalState(this.preparedStatementOneParam, "parameters");
		assertEquals((short) 10, value.get("1"));

		this.preparedStatementTwoParams.setShort(2, (short) 125);
		value = Whitebox.getInternalState(this.preparedStatementTwoParams, "parameters");
		assertEquals((short) 125, value.get("2"));
	}

	@Test public void setShortShouldOverrideOldValue() throws SQLException {
		this.preparedStatementOneParam.setShort(1, (short) 10);
		HashMap<String, Object> value = Whitebox.getInternalState(this.preparedStatementOneParam, "parameters");
		assertEquals((short) 10, value.get("1"));

		this.preparedStatementOneParam.setShort(1, (short) 20);
		value = Whitebox.getInternalState(this.preparedStatementOneParam, "parameters");
		assertEquals((short) 20, value.get("1"));
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
		HashMap<String, Object> value = Whitebox.getInternalState(this.preparedStatementOneParam, "parameters");
		assertEquals("string", value.get("1"));

		this.preparedStatementTwoParams.setString(2, "text");
		value = Whitebox.getInternalState(this.preparedStatementTwoParams, "parameters");
		assertEquals("text", value.get("2"));
	}

	@Test public void setStringShouldOverrideOldValue() throws SQLException {
		this.preparedStatementOneParam.setString(1, "string");
		HashMap<String, Object> value = Whitebox.getInternalState(this.preparedStatementOneParam, "parameters");
		assertEquals("string", value.get("1"));

		this.preparedStatementOneParam.setString(1, "otherString");
		value = Whitebox.getInternalState(this.preparedStatementOneParam, "parameters");
		assertEquals("otherString", value.get("1"));
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
		HashMap<String, Object> value = Whitebox.getInternalState(this.preparedStatementOneParam, "parameters");
		assertEquals(null, value.get("1"));

		this.preparedStatementTwoParams.setNull(2, NULL);
		value = Whitebox.getInternalState(this.preparedStatementTwoParams, "parameters");
		assertEquals(null, value.get("2"));
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
		HashMap<String, Object> value = Whitebox.getInternalState(this.preparedStatementOneParam, "parameters");

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
		HashMap<String, Object> value = Whitebox.getInternalState(this.preparedStatementOneParam, "parameters");
		assertEquals(true, value.get("1"));

		this.preparedStatementTwoParams.setBoolean(2, false);
		value = Whitebox.getInternalState(this.preparedStatementTwoParams, "parameters");
		assertEquals(false, value.get("2"));
	}

	@Test public void setBooleanShouldOverrideOldValue() throws SQLException {
		this.preparedStatementOneParam.setBoolean(1, true);
		HashMap<String, Object> value = Whitebox.getInternalState(this.preparedStatementOneParam, "parameters");
		assertEquals(true, value.get("1"));

		this.preparedStatementOneParam.setBoolean(1, false);
		value = Whitebox.getInternalState(this.preparedStatementOneParam, "parameters");
		assertEquals(false, value.get("1"));
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
		HashMap<String, Object> value = Whitebox.getInternalState(this.preparedStatementOneParam, "parameters");
		assertEquals(obj, value.get("1"));

		this.preparedStatementTwoParams.setObject(2, obj);
		value = Whitebox.getInternalState(this.preparedStatementTwoParams, "parameters");
		assertEquals(obj, value.get("2"));
	}

	@Test public void setObjectShouldOverrideOldValue() throws SQLException {
		Object obj = new HashMap<>();
		this.preparedStatementOneParam.setObject(1, obj);
		HashMap<String, Object> value = Whitebox.getInternalState(this.preparedStatementOneParam, "parameters");
		assertEquals(obj, value.get("1"));

		Object newObj = new ArrayList<>();;
		this.preparedStatementOneParam.setObject(1, newObj);
		value = Whitebox.getInternalState(this.preparedStatementOneParam, "parameters");
		assertEquals(newObj, value.get("1"));
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
		whenNew(BoltNeo4jParameterMetaData.class).withAnyArguments().thenReturn(null);

		this.preparedStatementOneParam.getParameterMetaData();

		verifyNew(BoltNeo4jParameterMetaData.class).withArguments(this.preparedStatementOneParam);

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

		Neo4jPreparedStatement statement = new BoltNeo4jPreparedStatement(mockConnectionClosed(), "");
		statement.close();
		statement.getResultSetConcurrency();
	}

	/*------------------------------*/
	/*    getResultSetHoldability   */
	/*------------------------------*/
	@Test public void getResultSetHoldabilityShouldThrowExceptionWhenCalledOnClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Neo4jPreparedStatement statement = new BoltNeo4jPreparedStatement(mockConnectionClosed(), "");
		statement.close();
		statement.getResultSetHoldability();
	}

	/*------------------------------*/
	/*       getResultSetType       */
	/*------------------------------*/
	@Test public void getResultSetTypeShouldThrowExceptionWhenCalledOnClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Neo4jPreparedStatement statement = new BoltNeo4jPreparedStatement(mockConnectionClosed(), "");
		statement.close();
		statement.getResultSetType();
	}

	/*------------------------------*/
	/*          executeQuery        */
	/*------------------------------*/
	@Test public void executeQueryShouldThrowExceptionWhenClosedConnection() throws SQLException {
		expectedEx.expect(SQLException.class);

		Neo4jPreparedStatement statement = new BoltNeo4jPreparedStatement(mockConnectionClosed(), "", 0, 0, 0);
		statement.executeQuery();
	}

	@Test public void executeQueryShouldReturnCorrectResultSetStructureConnectionNotAutocommit() throws Exception {
		BoltNeo4jConnection mockConnection = mockConnectionOpenWithTransactionThatReturns(null);

		Neo4jPreparedStatement statement = new BoltNeo4jPreparedStatement(mockConnection, "", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
				ResultSet.HOLD_CURSORS_OVER_COMMIT);
		statement.executeQuery();

		verifyNew(BoltNeo4jResultSet.class).withArguments(statement,null, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
	}

	@Test public void executeQueryShouldThrowExceptionOnClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Neo4jPreparedStatement statement = new BoltNeo4jPreparedStatement(mockConnectionOpen(), StatementData.STATEMENT_MATCH_ALL);
		statement.close();

		statement.executeQuery();
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

		Neo4jPreparedStatement statement = new BoltNeo4jPreparedStatement(mockConnectionOpenWithTransactionThatReturns(mockResult),
				StatementData.STATEMENT_CREATE_TWO_PROPERTIES_PARAMETRIC, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
				ResultSet.HOLD_CURSORS_OVER_COMMIT);
		statement.setString(1, "test");
		statement.setString(2, "test2");
		statement.executeUpdate();
	}

	@Test public void executeUpdateShouldThrowExceptionOnClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Neo4jPreparedStatement statement = new BoltNeo4jPreparedStatement(mockConnectionOpen(), StatementData.STATEMENT_MATCH_ALL);
		statement.close();

		statement.executeUpdate();
	}

	@Test public void executeUpdateShouldThrowExceptionOnClosedConnection() throws SQLException {
		expectedEx.expect(SQLException.class);

		Neo4jPreparedStatement statement = new BoltNeo4jPreparedStatement(mockConnectionClosed(), "", 0, 0, 0);
		statement.executeUpdate();
	}

	/*------------------------------*/
	/*            execute           */
	/*------------------------------*/
	@Test public void executeShouldRunQuery() throws SQLException {
		BoltNeo4jPreparedStatement statement = mock(BoltNeo4jPreparedStatement.class);
		when(statement.executeQuery()).thenCallRealMethod();
		Whitebox.setInternalState(statement, "statement", StatementData.STATEMENT_MATCH_ALL);

		statement.execute();
	}

	@Test public void executeShouldRunUpdate() throws SQLException {
		StatementResult mockResult = mock(StatementResult.class);
		ResultSummary mockSummary = mock(ResultSummary.class);
		SummaryCounters mockSummaryCounters = mock(SummaryCounters.class);

		when(mockResult.consume()).thenReturn(mockSummary);
		when(mockSummary.counters()).thenReturn(mockSummaryCounters);
		when(mockSummaryCounters.nodesCreated()).thenReturn(1);
		when(mockSummaryCounters.nodesDeleted()).thenReturn(0);

		Neo4jPreparedStatement statement = new BoltNeo4jPreparedStatement(mockConnectionOpenWithTransactionThatReturns(mockResult),
				StatementData.STATEMENT_CREATE_TWO_PROPERTIES_PARAMETRIC, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
				ResultSet.HOLD_CURSORS_OVER_COMMIT);
		statement.setString(1, "test");
		statement.setString(2, "test2");
		statement.execute();
	}

	@Test public void executeShouldThrowExceptionOnQueryOnClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Neo4jPreparedStatement statement = new BoltNeo4jPreparedStatement(mockConnectionOpen(), StatementData.STATEMENT_MATCH_ALL);
		statement.close();

		statement.execute();
	}

	@Test public void executeShouldThrowExceptionOnUpdateOnClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Neo4jPreparedStatement statement = new BoltNeo4jPreparedStatement(mockConnectionOpen(), StatementData.STATEMENT_CREATE);
		statement.close();

		statement.execute();
	}

	@Test public void executeShouldThrowExceptionOnQueryOnClosedConnection() throws SQLException {
		expectedEx.expect(SQLException.class);

		Neo4jPreparedStatement statement = new BoltNeo4jPreparedStatement(mockConnectionClosed(), "", 0, 0, 0);
		when(statement.executeQuery()).thenCallRealMethod();
		Whitebox.setInternalState(statement, "statement", StatementData.STATEMENT_MATCH_ALL);
		statement.execute();
	}

	@Test public void executeShouldThrowExceptionOnUpdateOnClosedConnection() throws SQLException {
		expectedEx.expect(SQLException.class);

		Neo4jPreparedStatement statement = new BoltNeo4jPreparedStatement(mockConnectionClosed(), "", 0, 0, 0);
		when(statement.executeUpdate()).thenCallRealMethod();
		Whitebox.setInternalState(statement, "statement", StatementData.STATEMENT_CREATE);
		statement.execute();
	}

	/*------------------------------*/
	/*        getUpdateCount        */
	/*------------------------------*/
	@Test public void getUpdateCountShouldReturnOne() throws SQLException {
		BoltNeo4jPreparedStatement statement = mock(BoltNeo4jPreparedStatement.class);
		when(statement.isClosed()).thenReturn(false);
		when(statement.getUpdateCount()).thenCallRealMethod();
		org.mockito.internal.util.reflection.Whitebox.setInternalState(statement, "currentResultSet", null);
		Whitebox.setInternalState(statement, "currentUpdateCount", 1);

		assertEquals(1, statement.getUpdateCount());
	}

	@Test public void getUpdateCountShouldReturnMinusOne() throws SQLException {
		BoltNeo4jPreparedStatement statement = mock(BoltNeo4jPreparedStatement.class);
		when(statement.isClosed()).thenReturn(false);
		when(statement.getUpdateCount()).thenCallRealMethod();
		org.mockito.internal.util.reflection.Whitebox.setInternalState(statement, "currentResultSet", mock(BoltNeo4jResultSet.class));

		assertEquals(-1, statement.getUpdateCount());
	}

	@Test public void getUpdateCountShouldThrowExceptionOnClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Neo4jPreparedStatement statement = new BoltNeo4jPreparedStatement(mockConnectionOpen(), StatementData.STATEMENT_MATCH_ALL);
		statement.close();

		statement.getUpdateCount();
	}

	/*------------------------------*/
	/*         getResultSet         */
	/*------------------------------*/
	@Test public void getResultSetShouldNotReturnNull() throws SQLException {
		BoltNeo4jStatement statement = mock(BoltNeo4jStatement.class);
		when(statement.isClosed()).thenReturn(false);
		when(statement.getResultSet()).thenCallRealMethod();
		Whitebox.setInternalState(statement, "currentResultSet", mock(BoltNeo4jResultSet.class));
		Whitebox.setInternalState(statement, "currentUpdateCount", -1);

		assertTrue(statement.getResultSet() != null);
	}

	@Test public void getResultSetShouldReturnNull() throws SQLException {
		BoltNeo4jStatement statement = mock(BoltNeo4jStatement.class);
		when(statement.isClosed()).thenReturn(false);
		when(statement.getResultSet()).thenCallRealMethod();
		Whitebox.setInternalState(statement, "currentUpdateCount", 1);

		assertEquals(null, statement.getResultSet());
	}

	@Test public void getResultSetShouldThrowExceptionOnClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Neo4jPreparedStatement statement = new BoltNeo4jPreparedStatement(mockConnectionOpen(), StatementData.STATEMENT_MATCH_ALL);
		statement.close();

		statement.getResultSet();
	}

	/*------------------------------*/
	/*           addBatch           */
	/*------------------------------*/

	@Test public void addBatchShouldAddParametersToStack() throws SQLException {
		Neo4jPreparedStatement stmt = new BoltNeo4jPreparedStatement(Mocker.mockConnectionOpen(), "MATCH n WHERE id(n) = ? SET n.property=1");
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
		}), Whitebox.getInternalState(stmt, "batchParameters"));
	}

	@Test public void addBatchShouldClearParameters() throws SQLException {
		Neo4jPreparedStatement stmt = new BoltNeo4jPreparedStatement(Mocker.mockConnectionOpen(), "MATCH n WHERE id(n) = ? SET n.property=1");
		stmt.setInt(1, 1);
		stmt.addBatch();

		assertEquals(Collections.EMPTY_MAP, Whitebox.getInternalState(stmt, "parameters"));
	}

	@Test public void addBatchShouldThrowExceptionIfClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Neo4jPreparedStatement stmt = new BoltNeo4jPreparedStatement(Mocker.mockConnectionOpen(), "?");
		stmt.setInt(1, 1);
		stmt.close();
		stmt.addBatch();
	}

	/*------------------------------*/
	/*          clearBatch          */
	/*------------------------------*/

	@Test public void clearBatchShouldWork() throws SQLException {
		Neo4jPreparedStatement stmt = new BoltNeo4jPreparedStatement(Mocker.mockConnectionOpen(), "?");
		stmt.setInt(1, 1);
		stmt.clearBatch();

		assertEquals(Collections.EMPTY_LIST, Whitebox.getInternalState(stmt, "batchParameters"));
	}

	@Test public void clearBatchShouldThrowExceptionIfClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Neo4jPreparedStatement stmt = new BoltNeo4jPreparedStatement(Mocker.mockConnectionOpen(), "?");
		stmt.close();
		stmt.clearBatch();
	}

	/*------------------------------*/
	/*         executeBatch         */
	/*------------------------------*/

	@Test public void executeBatchShouldWork() throws SQLException {
		Neo4jPreparedStatement stmt = new BoltNeo4jPreparedStatement(Mocker.mockConnectionOpen(), "MATCH n WHERE id(n) = ? SET n.property=1");
		stmt.setInt(1, 1);
		stmt.addBatch();
		stmt.setInt(1, 2);
		stmt.addBatch();

		Session session = Mockito.mock(Session.class);
		StatementResult stmtResult = Mockito.mock(StatementResult.class);
		ResultSummary resultSummary = Mockito.mock(ResultSummary.class);
		SummaryCounters summaryCounters = Mockito.mock(SummaryCounters.class);

		Mockito.when(session.run(anyString(), anyMap())).thenReturn(stmtResult);
		Mockito.when(stmtResult.consume()).thenReturn(resultSummary);
		Mockito.when(resultSummary.counters()).thenReturn(summaryCounters);
		Mockito.when(summaryCounters.nodesCreated()).thenReturn(1);
		Mockito.when(summaryCounters.nodesDeleted()).thenReturn(0);

		BoltNeo4jConnection connection = (BoltNeo4jConnection) stmt.getConnection();
		Mockito.when(connection.getAutoCommit()).thenReturn(true);
		Mockito.when(connection.getSession()).thenReturn(session);

		assertArrayEquals(new int[] { 1, 1 }, stmt.executeBatch());
	}

	@Test public void executeBatchShouldThrowExceptionOnError() throws SQLException {
		Neo4jPreparedStatement stmt = new BoltNeo4jPreparedStatement(Mocker.mockConnectionOpen(), "?");
		stmt.setInt(1, 1);
		stmt.addBatch();
		stmt.setInt(1, 2);
		stmt.addBatch();

		Session session = Mockito.mock(Session.class);

		Mockito.when(session.run(anyString(), anyMap())).thenThrow(Exception.class);

		BoltNeo4jConnection connection = (BoltNeo4jConnection) stmt.getConnection();
		Mockito.when(connection.getSession()).thenReturn(session);

		try {
			stmt.executeBatch();
			fail();
		} catch (BatchUpdateException e) {
			assertArrayEquals(new int[0], e.getUpdateCounts());
		}
	}

	@Test public void executeBatchShouldThrowExceptionOnClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		Neo4jPreparedStatement stmt = new BoltNeo4jPreparedStatement(Mocker.mockConnectionClosed(), "?");
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
		Neo4jPreparedStatement stmt = new BoltNeo4jPreparedStatement(Mocker.mockConnectionOpen(), "?");

		assertNotNull(stmt.getConnection());
		assertEquals(Mocker.mockConnectionOpen().getClass(), stmt.getConnection().getClass());
	}

	@Test public void getConnectionShouldThrowExceptionOnClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Neo4jPreparedStatement stmt = new BoltNeo4jPreparedStatement(Mocker.mockConnectionClosed(), "?");
		stmt.close();

		stmt.getConnection();
	}
}
