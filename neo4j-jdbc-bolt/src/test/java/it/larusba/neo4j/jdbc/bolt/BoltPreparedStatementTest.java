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
 * Created on 23/03/16
 */
package it.larusba.neo4j.jdbc.bolt;

import it.larusba.neo4j.jdbc.PreparedStatement;
import it.larusba.neo4j.jdbc.bolt.data.StatementData;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.powermock.reflect.Whitebox;

import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.HashMap;

import static it.larusba.neo4j.jdbc.bolt.utils.Mocker.mockConnectionOpen;
import static it.larusba.neo4j.jdbc.bolt.utils.Mocker.mockConnectionOpenWithTransactionThatReturns;
import static java.sql.Types.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.whenNew;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltPreparedStatementTest {

	@Rule public ExpectedException expectedEx = ExpectedException.none();

	PreparedStatement preparedStatementOneParam;
	PreparedStatement preparedStatementTwoParam;

	private BoltResultSet mockedRS;

	@Before public void interceptBoltResultSetConstructor() throws Exception {
		this.preparedStatementOneParam = new BoltPreparedStatement(mockConnectionOpenWithTransactionThatReturns(null), "MATCH n RETURN n WHERE n.name = ?");
		this.preparedStatementTwoParam = new BoltPreparedStatement(mockConnectionOpenWithTransactionThatReturns(null),
				"MATCH n RETURN n WHERE n.name = ? AND n.surname = ?");

		this.mockedRS = mock(BoltResultSet.class);
		doNothing().when(this.mockedRS).close();
		whenNew(BoltResultSet.class).withAnyArguments().thenReturn(this.mockedRS);
	}

	/*------------------------------*/
	/*             close            */
	/*------------------------------*/
	@Ignore @Test public void closeShouldCloseExistingResultSet() throws Exception {
		PreparedStatement prStatement = new BoltPreparedStatement(mockConnectionOpenWithTransactionThatReturns(null), "");
		prStatement.executeQuery();
		prStatement.close();

		verify(this.mockedRS, times(1)).close();
	}

	@Test public void closeShouldNotCallCloseOnAnyResultSet() throws Exception {
		PreparedStatement prStatement = new BoltPreparedStatement(mockConnectionOpenWithTransactionThatReturns(null), "");
		prStatement.close();

		verify(this.mockedRS, never()).close();
	}

	@Ignore @Test public void closeMultipleTimesIsNOOP() throws Exception {
		PreparedStatement prStatement = new BoltPreparedStatement(mockConnectionOpenWithTransactionThatReturns(null), "");
		prStatement.executeQuery(StatementData.STATEMENT_MATCH_ALL);
		prStatement.close();
		prStatement.close();
		prStatement.close();

		verify(this.mockedRS, times(1)).close();
	}

	/*------------------------------*/
	/*           isClosed           */
	/*------------------------------*/
	@Test public void isClosedShouldReturnFalseWhenCreated() throws SQLException {
		Statement statement = new BoltStatement(mockConnectionOpen());

		assertFalse(statement.isClosed());
	}

	/*------------------------------*/
	/*            setInt            */
	/*------------------------------*/

	@Test public void setIntShouldInsertTheCorrectIntegerValue() throws SQLException {
		this.preparedStatementOneParam.setInt(1, 10);
		HashMap<String, Object> value = Whitebox.getInternalState(this.preparedStatementOneParam, "parameters");
		assertEquals(10, value.get("1"));

		this.preparedStatementTwoParam.setInt(2, 125);
		value = Whitebox.getInternalState(this.preparedStatementTwoParam, "parameters");
		assertEquals(125, value.get("2"));
	}

	@Test public void setIntShouldThrowExceptionIfIndexDoesNotCorrespondToParameterMarker() throws SQLException {
		expectedEx.expect(SQLException.class);

		this.preparedStatementOneParam.setInt(99, 10);
	}

	@Test public void setIntShouldThrowExceptionIfIndexDoesNotCorrespondToParameterMarkerTwoParams() throws SQLException {
		expectedEx.expect(SQLException.class);

		this.preparedStatementTwoParam.setInt(99, 10);
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

		this.preparedStatementTwoParam.setLong(2, 125L);
		value = Whitebox.getInternalState(this.preparedStatementTwoParam, "parameters");
		assertEquals(125L, value.get("2"));
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

		this.preparedStatementTwoParam.setFloat(2, 125.5F);
		value = Whitebox.getInternalState(this.preparedStatementTwoParam, "parameters");
		assertEquals(125.5F, value.get("2"));
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

		this.preparedStatementTwoParam.setDouble(2, 125.5);
		value = Whitebox.getInternalState(this.preparedStatementTwoParam, "parameters");
		assertEquals(125.5, value.get("2"));
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

		this.preparedStatementTwoParam.setShort(2, (short) 125);
		value = Whitebox.getInternalState(this.preparedStatementTwoParam, "parameters");
		assertEquals((short) 125, value.get("2"));
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
	/*           setString           */
	/*------------------------------*/

	@Test public void setStringShouldInsertTheCorrectStringValue() throws SQLException {
		this.preparedStatementOneParam.setString(1, "string");
		HashMap<String, Object> value = Whitebox.getInternalState(this.preparedStatementOneParam, "parameters");
		assertEquals("string", value.get("1"));

		this.preparedStatementTwoParam.setString(2, "text");
		value = Whitebox.getInternalState(this.preparedStatementTwoParam, "parameters");
		assertEquals("text", value.get("2"));
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
	/*           setNull           */
	/*------------------------------*/

	@Test public void setNullShouldInsertTheCorrectNullValue() throws SQLException {
		this.preparedStatementOneParam.setNull(1, NULL);
		HashMap<String, Object> value = Whitebox.getInternalState(this.preparedStatementOneParam, "parameters");
		assertEquals(null, value.get("1"));

		this.preparedStatementTwoParam.setNull(2, NULL);
		value = Whitebox.getInternalState(this.preparedStatementTwoParam, "parameters");
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

		value.put("1","string");
		assertEquals(1, value.size());

		this.preparedStatementOneParam.clearParameters();
		assertEquals(0, value.size());
	}

	@Test public void clearParametersShouldThrowExceptionIfStatementClosed() throws SQLException{
		expectedEx.expect(SQLException.class);

		this.preparedStatementOneParam.close();

		this.preparedStatementOneParam.clearParameters();
	}

}
