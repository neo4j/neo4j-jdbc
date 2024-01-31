/*
 * Copyright (c) 2023-2024 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.driver.jdbc;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLType;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.sql.Wrapper;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class CallableStatementImplTests {

	private CallableStatementImpl statement;

	@ParameterizedTest
	@MethodSource("getShouldThrowWhenClosedArgs")
	void shouldThrowWhenClosed(StatementMethodRunner consumer) throws SQLException {
		this.statement = new CallableStatementImpl(mock(Connection.class), mock(Neo4jTransactionSupplier.class),
				UnaryOperator.identity(), UnaryOperator.identity(), false, "query");
		this.statement.close();
		assertThat(this.statement.isClosed()).isTrue();
		assertThatThrownBy(() -> consumer.run(this.statement)).isInstanceOf(SQLException.class);
	}

	static Stream<Arguments> getShouldThrowWhenClosedArgs() {
		return Stream.of(Arguments.of((StatementMethodRunner) statement -> statement.executeQuery("query")),
				Arguments.of((StatementMethodRunner) statement -> statement.executeUpdate("query")),
				Arguments.of((StatementMethodRunner) statement -> statement.setMaxFieldSize(1)),
				Arguments.of((StatementMethodRunner) Statement::getMaxFieldSize),
				Arguments.of((StatementMethodRunner) Statement::getMaxRows),
				Arguments.of((StatementMethodRunner) statement -> statement.setMaxRows(1)),
				Arguments.of((StatementMethodRunner) statement -> statement.setEscapeProcessing(true)),
				Arguments.of((StatementMethodRunner) Statement::getQueryTimeout),
				Arguments.of((StatementMethodRunner) statement -> statement.setQueryTimeout(1)),
				Arguments.of((StatementMethodRunner) Statement::getWarnings),
				Arguments.of((StatementMethodRunner) Statement::clearWarnings),
				Arguments.of((StatementMethodRunner) statement -> statement.execute("query")),
				Arguments.of((StatementMethodRunner) Statement::getResultSet),
				Arguments.of((StatementMethodRunner) Statement::getUpdateCount),
				Arguments.of((StatementMethodRunner) Statement::getMoreResults),
				Arguments.of((StatementMethodRunner) statement -> statement.setFetchDirection(ResultSet.FETCH_FORWARD)),
				Arguments.of((StatementMethodRunner) Statement::getFetchDirection),
				Arguments.of((StatementMethodRunner) statement -> statement.setFetchSize(1)),
				Arguments.of((StatementMethodRunner) Statement::getFetchSize),
				Arguments.of((StatementMethodRunner) Statement::getResultSetConcurrency),
				Arguments.of((StatementMethodRunner) Statement::getResultSetType),
				Arguments.of((StatementMethodRunner) Statement::getConnection),
				Arguments.of((StatementMethodRunner) Statement::getResultSetHoldability),
				Arguments.of((StatementMethodRunner) statement -> statement.setPoolable(true)),
				Arguments.of((StatementMethodRunner) Statement::isPoolable),
				Arguments.of((StatementMethodRunner) Statement::closeOnCompletion),
				Arguments.of((StatementMethodRunner) Statement::isCloseOnCompletion),
				Arguments.of((StatementMethodRunner) statement -> statement.setPoolable(true)),
				Arguments.of((StatementMethodRunner) statement -> statement.setPoolable(true)),
				// prepared statement
				Arguments.of((StatementMethodRunner) CallableStatement::executeQuery),
				Arguments.of((StatementMethodRunner) CallableStatement::executeUpdate),
				Arguments.of((StatementMethodRunner) CallableStatement::clearParameters),
				Arguments.of((StatementMethodRunner) statement -> statement.setNull(0, Types.NULL)),
				Arguments.of((StatementMethodRunner) statement -> statement.setBoolean(0, true)),
				Arguments.of((StatementMethodRunner) statement -> statement.setByte(0, (byte) 0)),
				Arguments.of((StatementMethodRunner) statement -> statement.setShort(0, (short) 0)),
				Arguments.of((StatementMethodRunner) statement -> statement.setInt(0, 0)),
				Arguments.of((StatementMethodRunner) statement -> statement.setLong(0, 0)),
				Arguments.of((StatementMethodRunner) statement -> statement.setFloat(0, 0.0f)),
				Arguments.of((StatementMethodRunner) statement -> statement.setDouble(0, 0.0)),
				Arguments.of((StatementMethodRunner) statement -> statement.setString(0, "string")),
				Arguments.of((StatementMethodRunner) statement -> statement.setBytes(0, new byte[0])),
				Arguments.of((StatementMethodRunner) statement -> statement.setDate(0, new Date(0))),
				Arguments.of((StatementMethodRunner) statement -> statement.setTime(0, new Time(0))),
				Arguments.of((StatementMethodRunner) statement -> statement.setTimestamp(0, new Timestamp(0))),
				Arguments.of((StatementMethodRunner) statement -> statement.setAsciiStream(0,
						new ByteArrayInputStream(new byte[0]), 0)),
				Arguments.of((StatementMethodRunner) statement -> statement.setBinaryStream(0,
						new ByteArrayInputStream(new byte[0]), 0)),
				Arguments.of((StatementMethodRunner) statement -> statement.setObject(0, "string")),
				Arguments.of((StatementMethodRunner) statement -> statement.setCharacterStream(0,
						new StringReader("string"), 0)),
				Arguments
					.of((StatementMethodRunner) statement -> statement.setDate(0, new Date(0), Calendar.getInstance())),
				Arguments
					.of((StatementMethodRunner) statement -> statement.setTime(0, new Time(0), Calendar.getInstance())),
				Arguments.of((StatementMethodRunner) statement -> statement.setTimestamp(0, new Timestamp(0),
						Calendar.getInstance())),
				Arguments.of((StatementMethodRunner) statement -> statement.setAsciiStream(0,
						new ByteArrayInputStream(new byte[0]), 0L)),
				Arguments.of((StatementMethodRunner) statement -> statement.setBinaryStream(0,
						new ByteArrayInputStream(new byte[0]), 0L)),
				Arguments.of((StatementMethodRunner) statement -> statement.setCharacterStream(0,
						new StringReader("string"), 0L)),
				// callable statement
				Arguments.of((StatementMethodRunner) CallableStatement::wasNull),
				Arguments.of((StatementMethodRunner) statement -> statement.getString(1)),
				Arguments.of((StatementMethodRunner) statement -> statement.getBoolean(1)),
				Arguments.of((StatementMethodRunner) statement -> statement.getByte(1)),
				Arguments.of((StatementMethodRunner) statement -> statement.getShort(1)),
				Arguments.of((StatementMethodRunner) statement -> statement.getInt(1)),
				Arguments.of((StatementMethodRunner) statement -> statement.getLong(1)),
				Arguments.of((StatementMethodRunner) statement -> statement.getFloat(1)),
				Arguments.of((StatementMethodRunner) statement -> statement.getDouble(1)),
				Arguments.of((StatementMethodRunner) statement -> statement.getBytes(1)),
				Arguments.of((StatementMethodRunner) statement -> statement.getDate(1)),
				Arguments.of((StatementMethodRunner) statement -> statement.getTime(1)),
				Arguments.of((StatementMethodRunner) statement -> statement.getTimestamp(1)),
				Arguments.of((StatementMethodRunner) statement -> statement.getObject(1)),
				Arguments.of((StatementMethodRunner) statement -> statement.getBigDecimal(1)),
				Arguments.of((StatementMethodRunner) statement -> statement.getDate(1, Calendar.getInstance())),
				Arguments.of((StatementMethodRunner) statement -> statement.getTime(1, Calendar.getInstance())),
				Arguments.of((StatementMethodRunner) statement -> statement.getTimestamp(1, Calendar.getInstance())),
				Arguments.of((StatementMethodRunner) statement -> statement.getCharacterStream(1)));
	}

	@ParameterizedTest
	@MethodSource("getShouldThrowUnsupportedArgs")
	void shouldThrowUnsupported(StatementMethodRunner consumer, Class<? extends SQLException> exceptionType) {
		this.statement = new CallableStatementImpl(mock(Connection.class), mock(Neo4jTransactionSupplier.class),
				UnaryOperator.identity(), UnaryOperator.identity(), false, "query");
		assertThatThrownBy(() -> consumer.run(this.statement)).isExactlyInstanceOf(exceptionType);
	}

	@SuppressWarnings("deprecation")
	static Stream<Arguments> getShouldThrowUnsupportedArgs() {
		return Stream.of(Arguments.of((StatementMethodRunner) Statement::cancel, SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setCursorName("name"),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.addBatch("query"), SQLException.class),
				Arguments.of(
						(StatementMethodRunner) statement -> statement.getMoreResults(Statement.CLOSE_CURRENT_RESULT),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) Statement::getGeneratedKeys,
						SQLFeatureNotSupportedException.class),
				// not currently supported
				Arguments.of((StatementMethodRunner) statement -> statement.setObject(1, null, Types.NULL),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setRef(1, null),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setBlob(1, mock(Blob.class)),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setClob(1, mock(Clob.class)),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setArray(1, mock(Array.class)),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) CallableStatement::getMetaData,
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setNull(1, Types.NULL, "name"),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setURL(1, mock(URL.class)),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setRowId(1, mock(RowId.class)),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setNString(1, "string"),
						SQLFeatureNotSupportedException.class),
				Arguments.of(
						(StatementMethodRunner) statement -> statement.setNCharacterStream(1, mock(Reader.class), 0),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setNClob(1, mock(NClob.class)),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setClob(1, mock(Reader.class), 0),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setBlob(1, mock(InputStream.class), 0),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setNClob(1, mock(Reader.class), 0),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setSQLXML(1, mock(SQLXML.class)),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setObject(1, null, Types.NULL, 0),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setAsciiStream(1, mock(InputStream.class)),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setBinaryStream(1, mock(InputStream.class)),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setCharacterStream(1, mock(Reader.class)),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setNCharacterStream(1, mock(Reader.class)),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setClob(1, mock(Reader.class)),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setBlob(1, mock(InputStream.class)),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setNClob(1, mock(Reader.class)),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setBigDecimal(1, BigDecimal.ONE),
						SQLException.class),
				// callable statement
				Arguments.of((StatementMethodRunner) statement -> statement.registerOutParameter(1, Types.DECIMAL),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.registerOutParameter(1, Types.DECIMAL, 1),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getBigDecimal(1, 1),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getObject(1, Collections.emptyMap()),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getRef(1),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getBlob(1),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getClob(1),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getArray(1),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.registerOutParameter(1, Types.DECIMAL,
						"typeName"), SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.registerOutParameter("parameterName",
						Types.DECIMAL), SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.registerOutParameter("parameterName",
						Types.DECIMAL, 1), SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.registerOutParameter("parameterName",
						Types.DECIMAL, "typeName"), SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getURL(1),
						SQLFeatureNotSupportedException.class),
				Arguments.of(
						(StatementMethodRunner) statement -> statement.setBigDecimal("parameterName", BigDecimal.ONE),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setObject("parameterName", new Object(),
						Types.DECIMAL, 1), SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setObject("parameterName", new Object(),
						Types.DECIMAL), SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setNull("parameterName", Types.DECIMAL,
						"typeName"), SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getString("parameterName"),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getBoolean("parameterName"),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getByte("parameterName"),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getShort("parameterName"),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getInt("parameterName"),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getLong("parameterName"),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getFloat("parameterName"),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getDouble("parameterName"),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getBytes("parameterName"),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getDate("parameterName"),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getTime("parameterName"),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getTimestamp("parameterName"),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getObject("parameterName"),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getBigDecimal("parameterName"),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getObject("parameterName",
						Collections.emptyMap()), SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getRef("parameterName"),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getBlob("parameterName"),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getClob("parameterName"),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getArray("parameterName"),
						SQLFeatureNotSupportedException.class),
				Arguments.of(
						(StatementMethodRunner) statement -> statement.getDate("parameterName", Calendar.getInstance()),
						SQLFeatureNotSupportedException.class),
				Arguments.of(
						(StatementMethodRunner) statement -> statement.getTime("parameterName", Calendar.getInstance()),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getTimestamp("parameterName",
						Calendar.getInstance()), SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getURL("parameterName"),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getRowId(1),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getRowId("parameterName"),
						SQLFeatureNotSupportedException.class),
				Arguments.of(
						(StatementMethodRunner) statement -> statement.setRowId("parameterName", mock(RowId.class)),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setNString("parameterName", "value"),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setNCharacterStream("parameterName",
						Reader.nullReader(), 0), SQLFeatureNotSupportedException.class),
				Arguments.of(
						(StatementMethodRunner) statement -> statement.setNClob("parameterName", mock(NClob.class)),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setClob("parameterName",
						Reader.nullReader(), 0L), SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setBlob("parameterName",
						InputStream.nullInputStream(), 0L), SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setNClob("parameterName",
						Reader.nullReader(), 0L), SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getNClob(1),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getNClob("parameterName"),
						SQLFeatureNotSupportedException.class),
				Arguments.of(
						(StatementMethodRunner) statement -> statement.setSQLXML("parameterName", mock(SQLXML.class)),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getSQLXML(1),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getSQLXML("parameterName"),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getNString(1),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getNString("parameterName"),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getNCharacterStream(1),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getNCharacterStream("parameterName"),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getCharacterStream("parameterName"),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setBlob("parameterName", mock(Blob.class)),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setClob("parameterName", mock(Clob.class)),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setAsciiStream("parameterName",
						InputStream.nullInputStream(), 0L), SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setBinaryStream("parameterName",
						InputStream.nullInputStream(), 0L), SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setCharacterStream("parameterName",
						Reader.nullReader(), 0L), SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setAsciiStream("parameterName",
						InputStream.nullInputStream()), SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setBinaryStream("parameterName",
						InputStream.nullInputStream()), SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setCharacterStream("parameterName",
						Reader.nullReader()), SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setNCharacterStream("parameterName",
						Reader.nullReader()), SQLFeatureNotSupportedException.class),
				Arguments.of(
						(StatementMethodRunner) statement -> statement.setClob("parameterName", Reader.nullReader()),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setBlob("parameterName",
						InputStream.nullInputStream()), SQLFeatureNotSupportedException.class),
				Arguments.of(
						(StatementMethodRunner) statement -> statement.setNClob("parameterName", Reader.nullReader()),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getObject(1, String.class),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) CallableStatement::wasNull, SQLException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getString(1), SQLException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getBoolean(1), SQLException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getByte(1), SQLException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getShort(1), SQLException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getInt(1), SQLException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getLong(1), SQLException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getFloat(1), SQLException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getDouble(1), SQLException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getBytes(1), SQLException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getDate(1), SQLException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getTime(1), SQLException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getTimestamp(1), SQLException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getBigDecimal(1), SQLException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getDate(1, Calendar.getInstance()),
						SQLException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getTime(1, Calendar.getInstance()),
						SQLException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getTimestamp(1, Calendar.getInstance()),
						SQLException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getCharacterStream(1), SQLException.class));
	}

	@ParameterizedTest
	@MethodSource("getUnwrapArgs")
	void shouldUnwrap(Class<?> cls, boolean shouldUnwrap) throws SQLException {
		// given
		this.statement = new CallableStatementImpl(mock(Connection.class), mock(Neo4jTransactionSupplier.class),
				UnaryOperator.identity(), UnaryOperator.identity(), false, "query");

		// when & then
		if (shouldUnwrap) {
			var unwrapped = this.statement.unwrap(cls);
			assertThat(unwrapped).isInstanceOf(cls);
		}
		else {
			assertThatThrownBy(() -> this.statement.unwrap(cls)).isInstanceOf(SQLException.class);
		}
	}

	@ParameterizedTest
	@MethodSource("getUnwrapArgs")
	void shouldHandleIsWrapperFor(Class<?> cls, boolean shouldUnwrap) {
		// given
		this.statement = new CallableStatementImpl(mock(Connection.class), mock(Neo4jTransactionSupplier.class),
				UnaryOperator.identity(), UnaryOperator.identity(), false, "query");

		// when
		var wrapperFor = this.statement.isWrapperFor(cls);

		// then
		assertThat(wrapperFor).isEqualTo(shouldUnwrap);
	}

	private static Stream<Arguments> getUnwrapArgs() {
		return Stream.of(Arguments.of(CallableStatementImpl.class, true),
				Arguments.of(PreparedStatementImpl.class, true), Arguments.of(PreparedStatement.class, true),
				Arguments.of(StatementImpl.class, true), Arguments.of(Statement.class, true),
				Arguments.of(Wrapper.class, true), Arguments.of(AutoCloseable.class, true),
				Arguments.of(Object.class, true), Arguments.of(ResultSet.class, false));
	}

	@ParameterizedTest
	@MethodSource("shouldNotAllowMixingParameterTypesArgs")
	void shouldNotAllowMixingParameterTypes(StatementMethodRunner firstSetter, StatementMethodRunner secondSetter)
			throws MalformedURLException, SQLException, IllegalAccessException {
		this.statement = new CallableStatementImpl(mock(Connection.class), mock(Neo4jTransactionSupplier.class),
				UnaryOperator.identity(), UnaryOperator.identity(), false, "query");

		try {
			firstSetter.run(this.statement);
		}
		catch (SQLFeatureNotSupportedException ignored) {
			return;
		}
		catch (SQLException sqlException) {
			// special case, skip the test as it is not supported at the moment
			if (sqlException.getMessage().equals("BigDecimal is not supported")) {
				return;
			}
			else {
				throw sqlException;
			}
		}

		assertThatThrownBy(() -> secondSetter.run(this.statement)).isInstanceOf(SQLException.class);
	}

	static Stream<Arguments> shouldNotAllowMixingParameterTypesArgs() {
		Map<Class<?>, Object> typeToValue = new HashMap<>();
		typeToValue.put(boolean.class, false);
		typeToValue.put(byte.class, (byte) 1);
		typeToValue.put(short.class, (short) 1);
		typeToValue.put(int.class, 1);
		typeToValue.put(long.class, 1L);
		typeToValue.put(float.class, 1.5f);
		typeToValue.put(double.class, 1.5);
		typeToValue.put(String.class, "value");
		typeToValue.put(InputStream.class, (Supplier<InputStream>) () -> new ByteArrayInputStream(new byte[0]));
		typeToValue.put(Reader.class, (Supplier<Reader>) () -> new StringReader("string"));
		typeToValue.put(Ref.class, mock(Ref.class));
		typeToValue.put(Object.class, "value");
		typeToValue.put(RowId.class, mock(RowId.class));
		typeToValue.put(byte[].class, new byte[] { 1 });
		typeToValue.put(Date.class, new Date(0));
		typeToValue.put(Time.class, new Time(0));
		typeToValue.put(Timestamp.class, new Timestamp(0));
		typeToValue.put(Calendar.class, Calendar.getInstance());
		typeToValue.put(BigDecimal.class, BigDecimal.ONE);
		typeToValue.put(SQLType.class, mock(SQLType.class));
		typeToValue.put(Array.class, mock(Array.class));
		typeToValue.put(SQLXML.class, mock(SQLXML.class));
		typeToValue.put(Blob.class, mock(Blob.class));
		typeToValue.put(Clob.class, mock(Clob.class));
		typeToValue.put(NClob.class, mock(NClob.class));
		typeToValue.put(URL.class, mock(URL.class));

		return streamSetterRunners(PreparedStatement.class, typeToValue)
			.flatMap(ordinalSetter -> streamSetterRunners(CallableStatement.class, typeToValue)
				.flatMap(namedSetter -> streamBothPermutations(ordinalSetter, namedSetter)));
	}

	static Stream<Named<StatementMethodRunner>> streamSetterRunners(Class<?> type, Map<Class<?>, Object> typeToValue) {
		return Arrays.stream(type.getDeclaredMethods())
			.filter(method -> method.getName().startsWith("set"))
			.map(method -> Named.of(method.toString(), newRunner(method, typeToValue)));
	}

	static StatementMethodRunner newRunner(Method method, Map<Class<?>, Object> typeToValue) {
		return statement -> {
			var args = Arrays.stream(method.getParameterTypes()).map(type -> {
				if (!typeToValue.containsKey(type)) {
					throw new RuntimeException(String.format("No type mapping for %s is defined.", type));
				}
				else {
					var value = typeToValue.get(type);
					if (value instanceof Supplier<?> supplier) {
						value = supplier.get();
					}
					return value;
				}
			}).toArray(Object[]::new);
			try {
				method.invoke(statement, args);
			}
			catch (InvocationTargetException ex) {
				var error = ex.getCause();
				if (error instanceof SQLException sqlException) {
					throw sqlException;
				}
				else {
					throw new RuntimeException(ex);
				}
			}
		};
	}

	static Stream<Arguments> streamBothPermutations(Named<StatementMethodRunner> ordinalSetter,
			Named<StatementMethodRunner> namedSetter) {
		return Stream.of(Arguments.of(ordinalSetter, namedSetter), Arguments.of(namedSetter, ordinalSetter));
	}

	@FunctionalInterface
	private interface StatementMethodRunner {

		void run(CallableStatement statement) throws SQLException, MalformedURLException, IllegalAccessException;

	}

}
