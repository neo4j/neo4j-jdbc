/*
 * Copyright (c) 2023-2025 "Neo4j,"
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
package org.neo4j.jdbc;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.jdbc.values.Value;
import org.neo4j.jdbc.values.Values;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class CallableStatementImplTests {

	public static final String TEST_STATEMENT = "RETURN pi()";

	private CallableStatementImpl statement;

	@ParameterizedTest
	@MethodSource
	void shouldSetParameter(StatementMethodRunner parameterSettingRunner, Value expectedValue)
			throws SQLException, MalformedURLException, IllegalAccessException {
		this.statement = new CallableStatementImpl(mock(Connection.class), mock(Neo4jTransactionSupplier.class), false,
				"RETURN $x");

		parameterSettingRunner.run(this.statement);

		assertThat(this.statement.getCurrentBatch()).isEqualTo(Map.of("x", expectedValue));
	}

	static Stream<Arguments> shouldSetParameter() {

		var zoneId = ZoneId.of("America/Los_Angeles");
		var offset = zoneId.getRules().getOffset(Instant.now());

		return Stream.of(
				Arguments.of((StatementMethodRunner) statement -> statement.setNull("x", Types.NULL), Values.NULL),
				Arguments.of((StatementMethodRunner) statement -> statement.setBoolean("x", true), Values.value(true)),
				Arguments.of((StatementMethodRunner) statement -> statement.setURL("x", new URL("https://neo4j.com")),
						Values.value("https://neo4j.com")),
				Arguments.of((StatementMethodRunner) statement -> statement.setURL("x", null), Values.NULL),
				Arguments.of((StatementMethodRunner) statement -> statement.setBoolean("x", false),
						Values.value(false)),
				Arguments.of((StatementMethodRunner) statement -> statement.setByte("x", (byte) 1),
						Values.value((byte) 1)),
				Arguments.of((StatementMethodRunner) statement -> statement.setShort("x", (short) 1),
						Values.value((short) 1)),
				Arguments.of((StatementMethodRunner) statement -> statement.setInt("x", 1), Values.value(1)),
				Arguments.of((StatementMethodRunner) statement -> statement.setLong("x", 1), Values.value(1L)),
				Arguments.of((StatementMethodRunner) statement -> statement.setFloat("x", 1.0f), Values.value(1.0f)),
				Arguments.of((StatementMethodRunner) statement -> statement.setDouble("x", 1.0), Values.value(1.0)),
				Arguments.of((StatementMethodRunner) statement -> statement.setString("x", "string"),
						Values.value("string")),
				Arguments.of((StatementMethodRunner) statement -> statement.setBytes("x", new byte[] { 0, 1 }),
						Values.value(new byte[] { 0, 1 })),
				Arguments.of((StatementMethodRunner) statement -> statement.setDate("x",
						Date.valueOf(LocalDate.of(2000, 1, 1))), Values.value(LocalDate.of(2000, 1, 1))),
				Arguments.of((StatementMethodRunner) statement -> statement.setTime("x",
						Time.valueOf(LocalTime.of(1, 1, 1))), Values.value(LocalTime.of(1, 1, 1))),
				Arguments.of(
						(StatementMethodRunner) statement -> statement.setTimestamp("x",
								Timestamp.valueOf(LocalDateTime.of(2000, 1, 1, 1, 1, 1))),
						Values.value(LocalDateTime.of(2000, 1, 1, 1, 1, 1))),
				Arguments.of(
						(StatementMethodRunner) statement -> statement.setAsciiStream("x",
								new ByteArrayInputStream("string".getBytes(StandardCharsets.US_ASCII)), 6),
						Values.value("string")),
				Arguments.of((StatementMethodRunner) statement -> statement.setBinaryStream("x",
						new ByteArrayInputStream(new byte[] { 0, 1 }), 6), Values.value(new byte[] { 0, 1 })),
				Arguments.of((StatementMethodRunner) statement -> statement.setCharacterStream("x",
						new StringReader("string"), 6), Values.value("string")),
				Arguments.of(
						(StatementMethodRunner) statement -> statement.setDate("x",
								Date.valueOf(LocalDate.of(2000, 1, 1)),
								Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"))),
						Values.value(ZonedDateTime.of(LocalDate.of(2000, 1, 1), LocalTime.of(0, 0),
								ZoneId.of("America/Los_Angeles")))),
				Arguments.of(
						(StatementMethodRunner) statement -> statement.setTime("x", Time.valueOf(LocalTime.of(1, 1, 1)),
								Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"))),
						Values.value(OffsetTime.of(LocalTime.of(1, 1, 1), offset))),
				Arguments.of(
						(StatementMethodRunner) statement -> statement.setTimestamp("x",
								Timestamp.valueOf(LocalDateTime.of(2000, 1, 1, 1, 1, 1)),
								Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"))),
						Values.value(ZonedDateTime.of(LocalDateTime.of(2000, 1, 1, 1, 1, 1),
								ZoneId.of("America/Los_Angeles")))),
				Arguments.of(
						(StatementMethodRunner) statement -> statement.setAsciiStream("x",
								new ByteArrayInputStream("string".getBytes(StandardCharsets.US_ASCII)), 6L),
						Values.value("string")),
				Arguments.of((StatementMethodRunner) statement -> statement.setBinaryStream("x",
						new ByteArrayInputStream(new byte[] { 0, 1 }), 6L), Values.value(new byte[] { 0, 1 })),
				Arguments.of((StatementMethodRunner) statement -> statement.setCharacterStream("x",
						new StringReader("string"), 6L), Values.value("string")),
				Arguments.of((StatementMethodRunner) statement -> statement.setObject("x", LocalDate.MAX),
						Values.value(LocalDate.MAX)),
				Arguments.of((StatementMethodRunner) statement -> statement.setObject("x", LocalTime.MAX),
						Values.value(LocalTime.MAX)),
				Arguments.of((StatementMethodRunner) statement -> statement.setObject("x", LocalDateTime.MAX),
						Values.value(LocalDateTime.MAX)),
				Arguments.of((StatementMethodRunner) statement -> statement.setObject("x", OffsetTime.MAX),
						Values.value(OffsetTime.MAX)),
				Arguments.of((StatementMethodRunner) statement -> statement.setObject("x", OffsetDateTime.MAX),
						Values.value(OffsetDateTime.MAX)),
				Arguments.of(
						(StatementMethodRunner) statement -> statement.setObject("x",
								ZonedDateTime.of(LocalDateTime.MAX, ZoneId.of("UTC"))),
						Values.value(ZonedDateTime.of(LocalDateTime.MAX, ZoneId.of("UTC")))),
				Arguments.of((StatementMethodRunner) statement -> statement.setObject("x", Period.ZERO),
						Values.value(Period.ZERO)),
				Arguments.of((StatementMethodRunner) statement -> statement.setObject("x", Duration.ZERO),
						Values.value(Duration.ZERO)));
	}

	@ParameterizedTest
	@MethodSource
	void shouldThrowWhenClosed(StatementMethodRunner consumer) throws SQLException {
		this.statement = new CallableStatementImpl(mock(Connection.class), mock(Neo4jTransactionSupplier.class), false,
				TEST_STATEMENT);
		this.statement.close();
		assertThat(this.statement.isClosed()).isTrue();
		assertThatThrownBy(() -> consumer.run(this.statement)).isInstanceOf(SQLException.class);
	}

	static Stream<Arguments> shouldThrowWhenClosed() {
		return Stream.of(Arguments.of((StatementMethodRunner) statement -> statement.executeQuery(TEST_STATEMENT)),
				Arguments.of((StatementMethodRunner) statement -> statement.executeUpdate(TEST_STATEMENT)),
				Arguments.of((StatementMethodRunner) statement -> statement.setMaxFieldSize(1)),
				Arguments.of((StatementMethodRunner) Statement::getMaxFieldSize),
				Arguments.of((StatementMethodRunner) Statement::getMaxRows),
				Arguments.of((StatementMethodRunner) statement -> statement.setMaxRows(1)),
				Arguments.of((StatementMethodRunner) statement -> statement.setEscapeProcessing(true)),
				Arguments.of((StatementMethodRunner) Statement::getQueryTimeout),
				Arguments.of((StatementMethodRunner) statement -> statement.setQueryTimeout(1)),
				Arguments.of((StatementMethodRunner) Statement::getWarnings),
				Arguments.of((StatementMethodRunner) Statement::clearWarnings),
				Arguments.of((StatementMethodRunner) statement -> statement.execute(TEST_STATEMENT)),
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
				Arguments.of((StatementMethodRunner) statement -> statement.getURL(1)),
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
	@MethodSource
	void shouldThrowUnsupported(StatementMethodRunner consumer, Class<? extends SQLException> exceptionType) {
		this.statement = new CallableStatementImpl(mock(Connection.class), mock(Neo4jTransactionSupplier.class), false,
				TEST_STATEMENT);
		assertThatThrownBy(() -> consumer.run(this.statement)).isExactlyInstanceOf(exceptionType);
	}

	@SuppressWarnings("deprecation")
	static Stream<Arguments> shouldThrowUnsupported() {
		return Stream.of(Arguments.of((StatementMethodRunner) Statement::cancel, SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setCursorName("name"),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.addBatch(TEST_STATEMENT),
						SQLException.class),
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
				Arguments.of((StatementMethodRunner) statement -> statement.setRowId(1, mock(RowId.class)),
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
				Arguments.of((StatementMethodRunner) statement -> statement.setClob(1, mock(Reader.class)),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setBlob(1, mock(InputStream.class)),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setNClob(1, mock(Reader.class)),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setObject("parameterName", new Object(),
						Types.DECIMAL, 1), SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setObject("parameterName", new Object(),
						Types.DECIMAL), SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setNull("parameterName", Types.DECIMAL,
						"typeName"), SQLFeatureNotSupportedException.class),
				Arguments.of(
						(StatementMethodRunner) statement -> statement.setRowId("parameterName", mock(RowId.class)),
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
				Arguments.of(
						(StatementMethodRunner) statement -> statement.setSQLXML("parameterName", mock(SQLXML.class)),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setBlob("parameterName", mock(Blob.class)),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setClob("parameterName", mock(Clob.class)),
						SQLFeatureNotSupportedException.class),
				Arguments.of(
						(StatementMethodRunner) statement -> statement.setClob("parameterName", Reader.nullReader()),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setBlob("parameterName",
						InputStream.nullInputStream()), SQLFeatureNotSupportedException.class),
				Arguments.of(
						(StatementMethodRunner) statement -> statement.setNClob("parameterName", Reader.nullReader()),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) CallableStatement::wasNull, SQLException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getString(1), SQLException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.getURL(1), SQLException.class),
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
		this.statement = new CallableStatementImpl(mock(Connection.class), mock(Neo4jTransactionSupplier.class), false,
				TEST_STATEMENT);

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
		this.statement = new CallableStatementImpl(mock(Connection.class), mock(Neo4jTransactionSupplier.class), false,
				TEST_STATEMENT);

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
		this.statement = new CallableStatementImpl(mock(Connection.class), mock(Neo4jTransactionSupplier.class), false,
				TEST_STATEMENT);

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
			.filter(method -> !method.getName().endsWith("setNull"))
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

	// Using <BLANK> as leading whitespace to make Checkstyle happier

	@ParameterizedTest
	@CsvSource(
			textBlock = """
					{call db.schema.nodeTypeProperties()}, db.schema.nodeTypeProperties, NONE, 0,,CALL db.schema.nodeTypeProperties
					{call apoc.periodic.cancel()}, apoc.periodic.cancel, NONE, 0,,CALL apoc.periodic.cancel
					{call db.schema.nodeTypeProperties}, db.schema.nodeTypeProperties, NONE, 0,,CALL db.schema.nodeTypeProperties
					{? = call db.schema.nodeTypeProperties}, db.schema.nodeTypeProperties, ORDINAL, 0,,CALL db.schema.nodeTypeProperties YIELD *
					{$nodeType = call db.schema.nodeTypeProperties}, db.schema.nodeTypeProperties, NAMED, 0,,CALL db.schema.nodeTypeProperties YIELD nodeType
					{:nodeType = call db.schema.nodeTypeProperties}, db.schema.nodeTypeProperties, NAMED, 0,,CALL db.schema.nodeTypeProperties YIELD nodeType
					{?=call db.schema.nodeTypeProperties}, db.schema.nodeTypeProperties, ORDINAL, 0,,CALL db.schema.nodeTypeProperties YIELD *
					{? =call db.schema.nodeTypeProperties}, db.schema.nodeTypeProperties, ORDINAL, 0,,CALL db.schema.nodeTypeProperties YIELD *
					{?= call db.schema.nodeTypeProperties}, db.schema.nodeTypeProperties, ORDINAL, 0,,CALL db.schema.nodeTypeProperties YIELD *
					{? = call db.schema.nodeTypeProperties()}, db.schema.nodeTypeProperties, ORDINAL, 0,,CALL db.schema.nodeTypeProperties YIELD *
					{  ?  = call db.schema.nodeTypeProperties()  }, db.schema.nodeTypeProperties, ORDINAL, 0,,CALL db.schema.nodeTypeProperties YIELD *
					<BLANK>{  ?  =  call db.schema.nodeTypeProperties  (  )  } , db.schema.nodeTypeProperties, ORDINAL, 0,,CALL db.schema.nodeTypeProperties YIELD *
					{?=call db.schema.nodeTypeProperties()}, db.schema.nodeTypeProperties, ORDINAL, 0,,CALL db.schema.nodeTypeProperties YIELD *
					{ ? =CALL db.schema.nodeTypeProperties() }, db.schema.nodeTypeProperties, ORDINAL, 0,,CALL db.schema.nodeTypeProperties YIELD *
					'{? = call i.dont_Exists(?, ?)}', i.dont_Exists, ORDINAL, 2,,'CALL i.dont_Exists($1, $2) YIELD *'
					'{ ? = call i.dont_Exists( ?,? )}', i.dont_Exists, ORDINAL, 2,,'CALL i.dont_Exists($1, $2) YIELD *'
					'{ ? = call i.dont_Exists(?,?)}', i.dont_Exists, ORDINAL, 2,,'CALL i.dont_Exists($1, $2) YIELD *'
					'{ ? = call i.dont_Exists(?)}', i.dont_Exists, ORDINAL, 1,,'CALL i.dont_Exists($1) YIELD *'
					RETURN sin(?), sin, ORDINAL, 1,,RETURN sin($1)
					RETURN trim(' ? '), trim, ORDINAL, 0,,RETURN trim(' ? ')
					RETURN i.dont_Exists('test'), i.dont_Exists, ORDINAL, 0,,RETURN i.dont_Exists('test')
					'{ ? = call blah(''test'', ?, ?, ")?", ''hallo, welt?'')}', blah, ORDINAL, 2,,'CALL blah(''test'', $1, $2, ")?", ''hallo, welt?'') YIELD *'
					call db.schema.nodeTypeProperties yield *, db.schema.nodeTypeProperties, ORDINAL, 0,,CALL db.schema.nodeTypeProperties YIELD *
					call db.schema.nodeTypeProperties() yield nodeType, db.schema.nodeTypeProperties, NAMED, 0,,CALL db.schema.nodeTypeProperties YIELD nodeType
					'{? = call db.index.fulltext.queryNodes(?, ?)}', db.index.fulltext.queryNodes, ORDINAL, 2,,'CALL db.index.fulltext.queryNodes($1, $2) YIELD *'
					'call db.index.fulltext.queryNodes(?, ?) yield *', db.index.fulltext.queryNodes, ORDINAL, 2,,'CALL db.index.fulltext.queryNodes($1, $2) YIELD *'
					'{$node = call db.index.fulltext.queryNodes($indexName, $queryString)}', db.index.fulltext.queryNodes, NAMED, 0, 'node, indexName, queryString','CALL db.index.fulltext.queryNodes($indexName, $queryString) YIELD node'
					'{:node = call db.index.fulltext.queryNodes(:indexName, :queryString)}', db.index.fulltext.queryNodes, NAMED, 0, 'node, indexName, queryString','CALL db.index.fulltext.queryNodes($indexName, $queryString) YIELD node'
					'{:node=call db.index.fulltext.queryNodes(:indexName, :queryString)}', db.index.fulltext.queryNodes, NAMED, 0, 'node, indexName, queryString','CALL db.index.fulltext.queryNodes($indexName, $queryString) YIELD node'
					'{:node =call db.index.fulltext.queryNodes(:indexName, :queryString)}', db.index.fulltext.queryNodes, NAMED, 0, 'node, indexName, queryString','CALL db.index.fulltext.queryNodes($indexName, $queryString) YIELD node'
					'{:node= call db.index.fulltext.queryNodes(:indexName, :queryString)}', db.index.fulltext.queryNodes, NAMED, 0, 'node, indexName, queryString','CALL db.index.fulltext.queryNodes($indexName, $queryString) YIELD node'
					'CALL dbms.cluster.routing.getRoutingTable(?, ?)', dbms.cluster.routing.getRoutingTable, NONE, 2,,'CALL dbms.cluster.routing.getRoutingTable($1, $2)'
					'CALL dbms.cluster.routing.getRoutingTable($1, $2)', dbms.cluster.routing.getRoutingTable, NONE, 2,,'CALL dbms.cluster.routing.getRoutingTable($1, $2)'
					'CALL dbms.cluster.routing.getRoutingTable(?, $2)', dbms.cluster.routing.getRoutingTable, NONE, 2,,'CALL dbms.cluster.routing.getRoutingTable($1, $2)'
					'CALL dbms.cluster.routing.getRoutingTable($1, ?)', dbms.cluster.routing.getRoutingTable, NONE, 2,,'CALL dbms.cluster.routing.getRoutingTable($1, $2)'
					'CALL dbms.cluster.routing.getRoutingTable(?, $1)', dbms.cluster.routing.getRoutingTable, NONE, 2,,'CALL dbms.cluster.routing.getRoutingTable($2, $1)'
					'CALL dbms.cluster.routing.getRoutingTable($2, ?)', dbms.cluster.routing.getRoutingTable, NONE, 2,,'CALL dbms.cluster.routing.getRoutingTable($2, $1)'
					'CALL f(?, ?, $2, ?, $3, $1)', f, NONE, 6,,'CALL f($4, $5, $2, $6, $3, $1)'
					'CALL f($0)', f, NONE, 0,,'CALL f($0)'
					'CALL f(?, ''blub'', $2, ?, $3, $1, ?, ''foo'')', f, NONE, 6,,'CALL f($4, ''blub'', $2, $5, $3, $1, $6, ''foo'')'
					'CALL f(?, ''blub'', $2, ?, $3, $1, ''foo'', ?)', f, NONE, 6,,'CALL f($4, ''blub'', $2, $5, $3, $1, ''foo'', $6)'
					""")
	void shouldBeAbleToParseValidCallStatements(String statement, String expectedFqn,
			CallableStatementImpl.ReturnType expectedReturnType, long expectedNumParameters, String names,
			String cypher) {
		var descriptor = CallableStatementImpl.parse(statement.replace("<BLANK>", " "));
		assertThat(descriptor).isNotNull();
		assertThat(descriptor.fqn()).isEqualTo(expectedFqn);
		assertThat(descriptor.returnType()).isEqualTo(expectedReturnType);
		if (names != null) {
			var expectedNames = Arrays.stream(names.split(",")).map(String::trim).toList();
			var parsedNames = new HashSet<>();
			if (descriptor.yieldedValues() != null) {
				parsedNames.addAll(descriptor.yieldedValues());
			}
			parsedNames.addAll(descriptor.parameterList().namedParameters().values());
			assertThat(parsedNames).containsExactlyInAnyOrderElementsOf(expectedNames);

		}
		else {
			assertThat(descriptor.parameterList().ordinalParameters()).hasSize((int) expectedNumParameters);
		}
		assertThat(descriptor.toCypher(Map.of())).isEqualTo(cypher);
	}

	@ParameterizedTest
	@CsvSource(textBlock = """
			'CALL f($b, $c, $a)', 'CALL f($a, $b, $c)', a;b;c
			'CALL f($a)', 'CALL f($a)', a
			'CALL f()', 'CALL f',
			""")
	void shouldApplyParameterOrder(String in, String expected, String order) {
		var parameterOrder = new HashMap<String, Integer>();
		if (order != null && !order.isBlank()) {
			var hlp = order.split(";");
			for (int i = 0; i < hlp.length; i++) {
				parameterOrder.put(hlp[i].trim(), i);
			}
		}
		assertThat(CallableStatementImpl.parse(in).toCypher(parameterOrder)).isEqualTo(expected);

	}

	@ParameterizedTest
	@ValueSource(strings = { "{$node = call db.index.fulltext.queryNodes(?, ?)}",
			"{? = call db.index.fulltext.queryNodes($x, ?)}", "{call db.index.fulltext.queryNodes($x, ?)}" })
	void preventMixingOfIndexedAndNamedParameters(String statement) {
		assertThatIllegalArgumentException().isThrownBy(() -> CallableStatementImpl.parse(statement))
			.withMessage("Index- and named ordinalParameters cannot be mixed: `" + statement + "`");
	}

	@ParameterizedTest
	@ValueSource(strings = { "{? = call db.index.fulltext.queryNodes($x, 'foo')}",
			"{call db.index.fulltext.queryNodes('foo', $x)}" })
	void preventMixingOfConstantAndNamedParameters(String statement) {
		assertThatIllegalArgumentException().isThrownBy(() -> CallableStatementImpl.parse(statement))
			.withMessage("Named parameters cannot be used together with constant arguments: `" + statement + "`");
	}

	@Test
	void returnTypeMustBeAlignedWithReturnParameterName() {
		// This cannot happen with the regex, but the check is there for safety reasons
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new CallableStatementImpl.Descriptor("a", CallableStatementImpl.ReturnType.ORDINAL,
					List.of("whatever"),
					new CallableStatementImpl.ParameterListDescriptor(Map.of(), Map.of(), Map.of()), false))
			.withMessage("A name for the return parameter is only supported with named returns");
	}

	@Test
	void nullStatementsAreNotAllowed() {
		assertThatNullPointerException().isThrownBy(() -> CallableStatementImpl.parse(null))
			.withMessage("Callable statements cannot be null");
	}

	@ParameterizedTest
	@ValueSource(strings = { "", " ", " \t " })
	void blanksStatementsAreNotAllowed(String statement) {
		assertThatIllegalArgumentException().isThrownBy(() -> CallableStatementImpl.parse(statement))
			.withMessage("Callable statements cannot be blank");
	}

	@FunctionalInterface
	private interface StatementMethodRunner {

		void run(CallableStatement statement) throws SQLException, MalformedURLException, IllegalAccessException;

	}

}
