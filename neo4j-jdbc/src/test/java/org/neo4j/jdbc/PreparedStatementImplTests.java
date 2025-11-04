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
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
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
import java.util.Calendar;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.bolt.connection.SummaryCounters;
import org.neo4j.jdbc.values.Value;
import org.neo4j.jdbc.values.Values;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

class PreparedStatementImplTests {

	private PreparedStatementImpl statement;

	@ParameterizedTest
	@CsvSource(delimiterString = "|",
			textBlock = """
					MATCH statement RETURN same WHERE thereAreNoPlaceholders|MATCH statement RETURN same WHERE thereAreNoPlaceholders
					MATCH statement RETURN same WHERE thereIsAPlaceholder = ?|MATCH statement RETURN same WHERE thereIsAPlaceholder = $1
					MATCH statement RETURN same WHERE thereIsAPlaceholder = '?'|MATCH statement RETURN same WHERE thereIsAPlaceholder = '?'
					MATCH statement RETURN same WHERE thereIsAPlaceholder = ? AND another = ?|MATCH statement RETURN same WHERE thereIsAPlaceholder = $1 AND another = $2
					MATCH statement RETURN same WHERE thisIs = "a string ?" AND thereIsAPlaceholder = ? AND another = ? AND notInString = "shall I replace this?"|MATCH statement RETURN same WHERE thisIs = "a string ?" AND thereIsAPlaceholder = $1 AND another = $2 AND notInString = "shall I replace this?"
					MATCH statement RETURN same WHERE thereIsAPlaceholder = ? <NL>AND newLinePar = ?|MATCH statement RETURN same WHERE thereIsAPlaceholder = $1 <NL>AND newLinePar = $2
					MATCH statement RETURN same WHERE thisIs = "a string ?"<NL>AND thereIsAPlaceholder = ?<NL>AND another = ?|MATCH statement RETURN same WHERE thisIs = "a string ?"<NL>AND thereIsAPlaceholder = $1<NL>AND another = $2
					""")
	void placeHolderReplacementShouldWork(String in, String expected) {

		var out = PreparedStatementImpl.rewritePlaceholders(in.replace("<NL>", "\n"));
		assertThat(out).isEqualTo(expected.replace("<NL>", "\n"));
	}

	@Test
	void shouldExecuteQuery() throws SQLException {
		// given
		var query = "query";
		var transactionSupplier = mock(Neo4jTransactionSupplier.class);
		var transaction = mock(Neo4jTransaction.class);
		var runResponse = mock(Neo4jTransaction.RunResponse.class);
		var pullResponse = mock(Neo4jTransaction.PullResponse.class);
		given(transactionSupplier.getTransaction(any())).willReturn(transaction);
		given(transactionSupplier.getTransaction(any())).willReturn(transaction);
		given(transaction.runAndPull(query, Collections.emptyMap(), StatementImpl.DEFAULT_FETCH_SIZE, 0))
			.willReturn(new Neo4jTransaction.RunAndPullResponses(runResponse, pullResponse));
		this.statement = newStatement(StatementImplTests.mockConnection(), transactionSupplier, query);

		// when
		var resultSet = this.statement.executeQuery();
		var multipleResultsApiResultSet = this.statement.getResultSet();

		// then
		assertThat(resultSet).isNotNull();
		assertThat(multipleResultsApiResultSet).isNull();
		then(transactionSupplier).should().getTransaction(Collections.emptyMap());
		then(transaction).should().runAndPull(query, Collections.emptyMap(), StatementImpl.DEFAULT_FETCH_SIZE, 0);
		then(transaction).shouldHaveNoMoreInteractions();
	}

	private static PreparedStatementImpl newStatement(Connection connection,
			Neo4jTransactionSupplier transactionSupplier, String query) {
		return new PreparedStatementImpl(connection, transactionSupplier, null, null, null, false, false,
				Statement.NO_GENERATED_KEYS, query);
	}

	@Test
	void shouldExecuteUpdate() throws SQLException {
		// given
		var query = "query";
		var discardResponse = mock(Neo4jTransaction.DiscardResponse.class);
		var transactionSupplier = mock(Neo4jTransactionSupplier.class);
		var transaction = mock(Neo4jTransaction.class);
		given(transactionSupplier.getTransaction(any())).willReturn(transaction);
		given(transaction.isAutoCommit()).willReturn(true);
		given(transaction.runAndDiscard(query, Collections.emptyMap(), 0, true)).willReturn(discardResponse);
		var response = mock(Neo4jTransaction.ResultSummary.class);
		given(discardResponse.resultSummary()).willReturn(Optional.of(response));
		var counters = mock(SummaryCounters.class);
		given(response.counters()).willReturn(counters);
		var totalUpdates = 5;
		given(counters.nodesCreated()).willReturn(totalUpdates);
		this.statement = newStatement(StatementImplTests.mockConnection(), transactionSupplier, query);

		// when
		var updates = this.statement.executeUpdate();

		// then
		assertThat(updates).isEqualTo(totalUpdates);
		then(transactionSupplier).should().getTransaction(Collections.emptyMap());
		then(transaction).should().isAutoCommit();
		then(transaction).should().runAndDiscard(query, Collections.emptyMap(), 0, true);
		then(transaction).shouldHaveNoMoreInteractions();
	}

	@Test
	void shouldExecuteQueryUsingMultipleResultsApi() throws SQLException {
		// given
		var query = "query";
		var transactionSupplier = mock(Neo4jTransactionSupplier.class);
		var transaction = mock(Neo4jTransaction.class);
		given(transactionSupplier.getTransaction(any())).willReturn(transaction);
		given(transaction.isAutoCommit()).willReturn(true);
		var runResponse = mock(Neo4jTransaction.RunResponse.class);
		var pullResponse = mock(Neo4jTransaction.PullResponse.class);
		given(transaction.runAndPull(query, Collections.emptyMap(), StatementImpl.DEFAULT_FETCH_SIZE, 0))
			.willReturn(new Neo4jTransaction.RunAndPullResponses(runResponse, pullResponse));
		given(transaction.isRunnable()).willReturn(true);
		var resultSummary = mock(Neo4jTransaction.ResultSummary.class);
		given(pullResponse.resultSummary()).willReturn(Optional.of(resultSummary));
		var summaryCounters = mock(SummaryCounters.class);
		given(resultSummary.counters()).willReturn(summaryCounters);
		given(summaryCounters.totalCount()).willReturn(0);
		given(pullResponse.records()).willReturn(Collections.emptyList());
		this.statement = newStatement(StatementImplTests.mockConnection(), transactionSupplier, query);

		// when
		var hasResultSet = this.statement.execute();
		var resultSet = this.statement.getResultSet();
		var updates = this.statement.getUpdateCount();
		var hasMoreResults = this.statement.getMoreResults();
		var nextResultSet = this.statement.getResultSet();
		var nextUpdates = this.statement.getUpdateCount();

		// then
		assertThat(hasResultSet).isTrue();
		assertThat(resultSet).isNotNull();
		assertThat(updates).isEqualTo(-1);
		assertThat(hasMoreResults).isFalse();
		assertThat(nextResultSet).isNull();
		assertThat(nextUpdates).isEqualTo(-1);
		assertThat(resultSet.isClosed()).isTrue();
		then(transactionSupplier).should().getTransaction(Collections.emptyMap());
		then(transaction).should().isAutoCommit();
		then(transaction).should().runAndPull(query, Collections.emptyMap(), StatementImpl.DEFAULT_FETCH_SIZE, 0);
		then(transaction).should().isRunnable();
		then(transaction).should().commit();
		then(transaction).shouldHaveNoMoreInteractions();
	}

	@Test
	void shouldBePoolableByDefault() throws SQLException {
		this.statement = newStatement(StatementImplTests.mockConnection(), mock(Neo4jTransactionSupplier.class), "");
		assertThat(this.statement.isPoolable()).isTrue();
	}

	@ParameterizedTest
	@MethodSource("getShouldThrowWhenClosedArgs")
	void shouldThrowWhenClosed(StatementMethodRunner consumer) throws SQLException {
		this.statement = newStatement(StatementImplTests.mockConnection(), mock(Neo4jTransactionSupplier.class),
				"query");
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
				Arguments.of((StatementMethodRunner) PreparedStatementImpl::executeQuery),
				Arguments.of((StatementMethodRunner) PreparedStatementImpl::executeUpdate),
				Arguments.of((StatementMethodRunner) PreparedStatementImpl::clearParameters),
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
						new StringReader("string"), 0L)));
	}

	@ParameterizedTest
	@MethodSource("getShouldThrowUnsupportedArgs")
	void shouldThrowUnsupported(StatementMethodRunner consumer, Class<? extends SQLException> exceptionType)
			throws SQLException {
		this.statement = newStatement(StatementImplTests.mockConnection(), mock(Neo4jTransactionSupplier.class),
				"query");
		assertThatThrownBy(() -> consumer.run(this.statement)).isInstanceOf(exceptionType);
	}

	static Stream<Arguments> getShouldThrowUnsupportedArgs() {
		return Stream.of(Arguments.of((StatementMethodRunner) Statement::cancel, SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setCursorName("name"),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.addBatch("query"), SQLException.class),
				Arguments.of(
						(StatementMethodRunner) statement -> statement.getMoreResults(Statement.CLOSE_CURRENT_RESULT),
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
						SQLFeatureNotSupportedException.class));
	}

	@ParameterizedTest
	@MethodSource("getShouldThrowOnExplicitlyProhibitedMethodsArgs")
	void shouldThrowOnExplicitlyProhibitedMethods(StatementMethodRunner consumer) throws SQLException {
		this.statement = newStatement(StatementImplTests.mockConnection(), mock(Neo4jTransactionSupplier.class),
				"query");
		assertThatThrownBy(() -> consumer.run(this.statement)).isExactlyInstanceOf(Neo4jException.class);
	}

	static Stream<Arguments> getShouldThrowOnExplicitlyProhibitedMethodsArgs() {
		return Stream.of(Arguments.of((StatementMethodRunner) statement -> statement.executeQuery("query")),
				Arguments.of((StatementMethodRunner) statement -> statement.executeUpdate("query")),
				Arguments.of((StatementMethodRunner) statement -> statement.execute("query")),
				Arguments.of((StatementMethodRunner) statement -> statement.addBatch("query")),
				Arguments.of((StatementMethodRunner) statement -> statement.executeUpdate("query",
						Statement.NO_GENERATED_KEYS)),
				Arguments.of((StatementMethodRunner) statement -> statement.executeUpdate("query", new int[0])),
				Arguments.of((StatementMethodRunner) statement -> statement.executeUpdate("query", new String[0])),
				Arguments
					.of((StatementMethodRunner) statement -> statement.execute("query", Statement.NO_GENERATED_KEYS)),
				Arguments.of((StatementMethodRunner) statement -> statement.execute("query", new int[0])),
				Arguments.of((StatementMethodRunner) statement -> statement.execute("query", new String[0])),
				Arguments.of((StatementMethodRunner) statement -> statement.executeLargeUpdate("query")),
				Arguments.of((StatementMethodRunner) statement -> statement.executeLargeUpdate("query",
						Statement.NO_GENERATED_KEYS)),
				Arguments.of((StatementMethodRunner) statement -> statement.executeLargeUpdate("query", new int[0])),
				Arguments
					.of((StatementMethodRunner) statement -> statement.executeLargeUpdate("query", new String[0])));
	}

	@ParameterizedTest
	@MethodSource("getShouldThrowOnInvalidParameterIndexArgs")
	void shouldThrowOnInvalidParameterIndex(StatementMethodRunner consumer) throws SQLException {
		this.statement = newStatement(StatementImplTests.mockConnection(), mock(Neo4jTransactionSupplier.class),
				"query");
		assertThatThrownBy(() -> consumer.run(this.statement)).isExactlyInstanceOf(Neo4jException.class);
	}

	static Stream<Arguments> getShouldThrowOnInvalidParameterIndexArgs() {
		return Stream.of(Arguments.of((StatementMethodRunner) statement -> statement.setNull(0, Types.NULL)),
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
						new StringReader("string"), 0L)));
	}

	@ParameterizedTest
	@MethodSource
	void shouldSetParameter(StatementMethodRunner parameterSettingRunner, Value expectedValue)
			throws SQLException, MalformedURLException {
		this.statement = newStatement(StatementImplTests.mockConnection(), mock(Neo4jTransactionSupplier.class),
				"query");

		parameterSettingRunner.run(this.statement);

		assertThat(this.statement.getCurrentBatch()).isEqualTo(Map.of("1", expectedValue));
	}

	static Stream<Arguments> shouldSetParameter() throws MalformedURLException {

		var zoneId = ZoneId.of("America/Los_Angeles");
		var offset = zoneId.getRules().getOffset(Instant.now());
		@SuppressWarnings("squid:S1874")
		var url = new URL("https://neo4j.com");

		return Stream.of(
				Arguments.of((StatementMethodRunner) statement -> statement.setNull(1, Types.NULL), Values.NULL),
				Arguments.of((StatementMethodRunner) statement -> statement.setBoolean(1, true), Values.value(true)),
				Arguments.of((StatementMethodRunner) statement -> statement.setURL(1, url),
						Values.value("https://neo4j.com")),
				Arguments.of((StatementMethodRunner) statement -> statement.setBoolean(1, false), Values.value(false)),
				Arguments.of((StatementMethodRunner) statement -> statement.setByte(1, (byte) 1),
						Values.value((byte) 1)),
				Arguments.of((StatementMethodRunner) statement -> statement.setShort(1, (short) 1),
						Values.value((short) 1)),
				Arguments.of((StatementMethodRunner) statement -> statement.setInt(1, 1), Values.value(1)),
				Arguments.of((StatementMethodRunner) statement -> statement.setLong(1, 1), Values.value(1L)),
				Arguments.of((StatementMethodRunner) statement -> statement.setFloat(1, 1.0f), Values.value(1.0f)),
				Arguments.of((StatementMethodRunner) statement -> statement.setDouble(1, 1.0), Values.value(1.0)),
				Arguments.of((StatementMethodRunner) statement -> statement.setString(1, "string"),
						Values.value("string")),
				Arguments.of((StatementMethodRunner) statement -> statement.setBytes(1, new byte[] { 0, 1 }),
						Values.value(new byte[] { 0, 1 })),
				Arguments.of((StatementMethodRunner) statement -> statement.setDate(1,
						Date.valueOf(LocalDate.of(2000, 1, 1))), Values.value(LocalDate.of(2000, 1, 1))),
				Arguments.of(
						(StatementMethodRunner) statement -> statement.setTime(1, Time.valueOf(LocalTime.of(1, 1, 1))),
						Values.value(LocalTime.of(1, 1, 1))),
				Arguments.of(
						(StatementMethodRunner) statement -> statement.setTimestamp(1,
								Timestamp.valueOf(LocalDateTime.of(2000, 1, 1, 1, 1, 1))),
						Values.value(LocalDateTime.of(2000, 1, 1, 1, 1, 1))),
				Arguments.of(
						(StatementMethodRunner) statement -> statement.setAsciiStream(1,
								new ByteArrayInputStream("string".getBytes(StandardCharsets.US_ASCII)), 6),
						Values.value("string")),
				Arguments.of((StatementMethodRunner) statement -> statement.setBinaryStream(1,
						new ByteArrayInputStream(new byte[] { 0, 1 }), 6), Values.value(new byte[] { 0, 1 })),
				Arguments.of((StatementMethodRunner) statement -> statement.setCharacterStream(1,
						new StringReader("string"), 6), Values.value("string")),
				Arguments.of(
						(StatementMethodRunner) statement -> statement.setDate(1,
								Date.valueOf(LocalDate.of(2000, 1, 1)),
								Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"))),
						Values.value(ZonedDateTime.of(LocalDate.of(2000, 1, 1), LocalTime.of(0, 0),
								ZoneId.of("America/Los_Angeles")))),
				Arguments.of(
						(StatementMethodRunner) statement -> statement.setTime(1, Time.valueOf(LocalTime.of(1, 1, 1)),
								Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"))),
						Values.value(OffsetTime.of(LocalTime.of(1, 1, 1), offset))),
				Arguments.of((StatementMethodRunner) statement -> statement.setTimestamp(1,
						Timestamp.valueOf(LocalDateTime.of(2000, 1, 1, 1, 1, 1)),
						Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"))),
						Values.value(ZonedDateTime.of(LocalDateTime.of(2000, 1, 1, 1, 1, 1),
								ZoneId.of("America/Los_Angeles")))),
				Arguments.of(
						(StatementMethodRunner) statement -> statement.setAsciiStream(1,
								new ByteArrayInputStream("string".getBytes(StandardCharsets.US_ASCII)), 6L),
						Values.value("string")),
				Arguments.of((StatementMethodRunner) statement -> statement.setBinaryStream(1,
						new ByteArrayInputStream(new byte[] { 0, 1 }), 6L), Values.value(new byte[] { 0, 1 })),
				Arguments.of((StatementMethodRunner) statement -> statement.setCharacterStream(1,
						new StringReader("string"), 6L), Values.value("string")),
				Arguments.of((StatementMethodRunner) statement -> statement.setObject(1, LocalDate.MAX),
						Values.value(LocalDate.MAX)),
				Arguments.of((StatementMethodRunner) statement -> statement.setObject(1, LocalTime.MAX),
						Values.value(LocalTime.MAX)),
				Arguments.of((StatementMethodRunner) statement -> statement.setObject(1, LocalDateTime.MAX),
						Values.value(LocalDateTime.MAX)),
				Arguments.of((StatementMethodRunner) statement -> statement.setObject(1, OffsetTime.MAX),
						Values.value(OffsetTime.MAX)),
				Arguments.of((StatementMethodRunner) statement -> statement.setObject(1, OffsetDateTime.MAX),
						Values.value(OffsetDateTime.MAX)),
				Arguments.of(
						(StatementMethodRunner) statement -> statement.setObject(1,
								ZonedDateTime.of(LocalDateTime.MAX, ZoneId.of("UTC"))),
						Values.value(ZonedDateTime.of(LocalDateTime.MAX, ZoneId.of("UTC")))),
				Arguments.of((StatementMethodRunner) statement -> statement.setObject(1, Period.ZERO),
						Values.value(Period.ZERO)),
				Arguments.of((StatementMethodRunner) statement -> statement.setObject(1, Duration.ZERO),
						Values.value(Duration.ZERO)));
	}

	@ParameterizedTest
	@MethodSource("getShouldSetObjectParameterArgs")
	void shouldSetObjectParameter(Object object, Value expectedValue) throws SQLException {
		this.statement = newStatement(StatementImplTests.mockConnection(), mock(Neo4jTransactionSupplier.class),
				"query");

		this.statement.setObject(1, object);

		assertThat(this.statement.getCurrentBatch()).isEqualTo(Map.of("1", expectedValue));
	}

	static Stream<Arguments> getShouldSetObjectParameterArgs() {
		return Stream.of(Arguments.of(null, Values.NULL), Arguments.of(true, Values.value(true)),
				Arguments.of(false, Values.value(false)), Arguments.of((byte) 1, Values.value((byte) 1)),
				Arguments.of((short) 1, Values.value((short) 1)), Arguments.of(1, Values.value(1)),
				Arguments.of(1L, Values.value(1L)), Arguments.of(1.0f, Values.value(1.0f)),
				Arguments.of(1.0, Values.value(1.0)), Arguments.of("string", Values.value("string")),
				Arguments.of(new byte[] { 0, 1 }, Values.value(new byte[] { 0, 1 })),
				Arguments.of(Date.valueOf(LocalDate.of(2000, 1, 1)), Values.value(LocalDate.of(2000, 1, 1))),
				Arguments.of(Time.valueOf(LocalTime.of(1, 1, 1)), Values.value(LocalTime.of(1, 1, 1))),
				Arguments.of(Timestamp.valueOf(LocalDateTime.of(2000, 1, 1, 1, 1, 1)),
						Values.value(LocalDateTime.of(2000, 1, 1, 1, 1, 1))));
	}

	@Test
	void shouldClearParameters() throws SQLException {
		this.statement = newStatement(StatementImplTests.mockConnection(), mock(Neo4jTransactionSupplier.class),
				"query");
		this.statement.setBoolean(1, true);

		this.statement.clearParameters();

		assertThat(this.statement.getCurrentBatch().isEmpty()).isTrue();
	}

	@Test
	void shouldReturnParameterMetaData() throws SQLException {
		this.statement = newStatement(StatementImplTests.mockConnection(), mock(Neo4jTransactionSupplier.class),
				"query");

		var metaData = this.statement.getParameterMetaData();

		assertThat(metaData).isNotNull();
	}

	@ParameterizedTest
	@MethodSource("getUnwrapArgs")
	void shouldUnwrap(Class<?> cls, boolean shouldUnwrap) throws SQLException {
		// given
		this.statement = newStatement(StatementImplTests.mockConnection(), mock(Neo4jTransactionSupplier.class),
				"query");

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
	void shouldHandleIsWrapperFor(Class<?> cls, boolean shouldUnwrap) throws SQLException {
		// given
		this.statement = newStatement(StatementImplTests.mockConnection(), mock(Neo4jTransactionSupplier.class),
				"query");

		// when
		var wrapperFor = this.statement.isWrapperFor(cls);

		// then
		assertThat(wrapperFor).isEqualTo(shouldUnwrap);
	}

	private static Stream<Arguments> getUnwrapArgs() {
		return Stream.of(Arguments.of(PreparedStatementImpl.class, true), Arguments.of(PreparedStatement.class, true),
				Arguments.of(StatementImpl.class, true), Arguments.of(Statement.class, true),
				Arguments.of(Wrapper.class, true), Arguments.of(AutoCloseable.class, true),
				Arguments.of(Object.class, true), Arguments.of(ResultSet.class, false));
	}

	@FunctionalInterface
	private interface StatementMethodRunner {

		void run(PreparedStatementImpl statement) throws SQLException, MalformedURLException;

	}

}
