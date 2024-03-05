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
package org.neo4j.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.sql.Wrapper;
import java.util.Collections;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.jdbc.internal.bolt.response.DiscardResponse;
import org.neo4j.jdbc.internal.bolt.response.PullResponse;
import org.neo4j.jdbc.internal.bolt.response.ResultSummary;
import org.neo4j.jdbc.internal.bolt.response.RunResponse;
import org.neo4j.jdbc.internal.bolt.response.SummaryCounters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

class StatementImplTests {

	private StatementImpl statement;

	static StatementImpl newStatement(Connection connection, Neo4jTransactionSupplier transactionSupplier) {
		return new StatementImpl(connection, transactionSupplier, UnaryOperator.identity(), null);
	}

	@Test
	void shouldExecuteQuery() throws SQLException {
		// given
		var query = "query";
		var runResponse = mock(RunResponse.class);
		var pullResponse = mock(PullResponse.class);
		var transactionSupplier = mock(Neo4jTransactionSupplier.class);
		var transaction = mock(Neo4jTransaction.class);
		given(transactionSupplier.getTransaction()).willReturn(transaction);
		given(transaction.runAndPull(query, Collections.emptyMap(), StatementImpl.DEFAULT_FETCH_SIZE, 0))
			.willReturn(new Neo4jTransaction.RunAndPullResponses(runResponse, pullResponse));

		this.statement = newStatement(mock(Connection.class), transactionSupplier);

		// when
		var resultSet = this.statement.executeQuery(query);
		var multipleResultsApiResultSet = this.statement.getResultSet();

		// then
		assertThat(resultSet).isNotNull();
		assertThat(multipleResultsApiResultSet).isNull();
		then(transactionSupplier).should().getTransaction();
		then(transaction).should().runAndPull(query, Collections.emptyMap(), StatementImpl.DEFAULT_FETCH_SIZE, 0);
		then(transaction).shouldHaveNoMoreInteractions();
	}

	@Test
	void shouldExecuteUpdate() throws SQLException {
		// given
		var query = "query";
		var discardResponse = mock(DiscardResponse.class);
		var transactionSupplier = mock(Neo4jTransactionSupplier.class);
		var transaction = mock(Neo4jTransaction.class);
		given(transactionSupplier.getTransaction()).willReturn(transaction);
		given(transaction.isAutoCommit()).willReturn(true);
		given(transaction.runAndDiscard(query, Collections.emptyMap(), 0, true)).willReturn(discardResponse);
		var response = mock(ResultSummary.class);
		given(discardResponse.resultSummary()).willReturn(Optional.of(response));
		var counters = mock(SummaryCounters.class);
		given(response.counters()).willReturn(counters);
		var totalUpdates = 5;
		given(counters.totalCount()).willReturn(totalUpdates);
		this.statement = newStatement(mock(Connection.class), transactionSupplier);

		// when
		var updates = this.statement.executeUpdate(query);

		// then
		assertThat(updates).isEqualTo(totalUpdates);
		then(transactionSupplier).should().getTransaction();
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
		given(transactionSupplier.getTransaction()).willReturn(transaction);
		given(transaction.isAutoCommit()).willReturn(true);
		var runResponse = mock(RunResponse.class);
		var pullResponse = mock(PullResponse.class);
		given(transaction.runAndPull(query, Collections.emptyMap(), StatementImpl.DEFAULT_FETCH_SIZE, 0))
			.willReturn(new Neo4jTransaction.RunAndPullResponses(runResponse, pullResponse));
		given(transaction.isRunnable()).willReturn(true);
		var resultSummary = mock(ResultSummary.class);
		given(pullResponse.resultSummary()).willReturn(Optional.of(resultSummary));
		var summaryCounters = mock(SummaryCounters.class);
		given(resultSummary.counters()).willReturn(summaryCounters);
		given(summaryCounters.totalCount()).willReturn(0);
		given(pullResponse.records()).willReturn(Collections.emptyList());
		this.statement = newStatement(mock(Connection.class), transactionSupplier);

		// when
		var hasResultSet = this.statement.execute(query);
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
		then(transactionSupplier).should().getTransaction();
		then(transaction).should().isAutoCommit();
		then(transaction).should().runAndPull(query, Collections.emptyMap(), StatementImpl.DEFAULT_FETCH_SIZE, 0);
		then(transaction).should().isRunnable();
		then(transaction).should().commit();
		then(transaction).shouldHaveNoMoreInteractions();
	}

	@Test
	void shouldExecuteUpdateQueryUsingMultipleResultsApi() throws SQLException {
		// given
		var query = "query";
		var transactionSupplier = mock(Neo4jTransactionSupplier.class);
		var transaction = mock(Neo4jTransaction.class);
		given(transactionSupplier.getTransaction()).willReturn(transaction);
		given(transaction.isAutoCommit()).willReturn(true);
		var runResponse = mock(RunResponse.class);
		var pullResponse = mock(PullResponse.class);
		given(transaction.runAndPull(query, Collections.emptyMap(), StatementImpl.DEFAULT_FETCH_SIZE, 0))
			.willReturn(new Neo4jTransaction.RunAndPullResponses(runResponse, pullResponse));
		given(transaction.isRunnable()).willReturn(true);
		var resultSummary = mock(ResultSummary.class);
		given(pullResponse.resultSummary()).willReturn(Optional.of(resultSummary));
		var summaryCounters = mock(SummaryCounters.class);
		given(resultSummary.counters()).willReturn(summaryCounters);
		var totalUpdates = 5;
		given(summaryCounters.totalCount()).willReturn(totalUpdates);
		this.statement = newStatement(mock(Connection.class), transactionSupplier);

		// when
		var hasResultSet = this.statement.execute(query);
		var resultSet = this.statement.getResultSet();
		var updates = this.statement.getUpdateCount();
		var hasMoreResults = this.statement.getMoreResults();
		var nextResultSet = this.statement.getResultSet();
		var nextUpdates = this.statement.getUpdateCount();

		// then
		assertThat(hasResultSet).isFalse();
		assertThat(resultSet).isNull();
		assertThat(updates).isEqualTo(totalUpdates);
		assertThat(hasMoreResults).isFalse();
		assertThat(nextResultSet).isNull();
		assertThat(nextUpdates).isEqualTo(-1);
		then(transactionSupplier).should().getTransaction();
		then(transaction).should().isAutoCommit();
		then(transaction).should().runAndPull(query, Collections.emptyMap(), StatementImpl.DEFAULT_FETCH_SIZE, 0);
		then(transaction).should().isRunnable();
		then(transaction).should().commit();
		then(transaction).shouldHaveNoMoreInteractions();
	}

	@Test
	void shouldNotBePoolableByDefault() throws SQLException {
		this.statement = newStatement(mock(Connection.class), mock(Neo4jTransactionSupplier.class));
		assertThat(this.statement.isPoolable()).isFalse();
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldSetPoolable(boolean poolable) throws SQLException {
		this.statement = newStatement(mock(Connection.class), mock(Neo4jTransactionSupplier.class));
		this.statement.setPoolable(poolable);
		assertThat(this.statement.isPoolable()).isEqualTo(poolable);
	}

	@ParameterizedTest
	@MethodSource("getThrowingMethodExecutorsWhenClosed")
	void shouldThrowWhenClosed(StatementMethodRunner consumer) throws SQLException {
		this.statement = newStatement(mock(Connection.class), mock(Neo4jTransactionSupplier.class));
		this.statement.close();
		assertThat(this.statement.isClosed()).isTrue();
		assertThatThrownBy(() -> consumer.run(this.statement)).isInstanceOf(SQLException.class);
	}

	static Stream<Arguments> getThrowingMethodExecutorsWhenClosed() {
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
				Arguments.of((StatementMethodRunner) statement -> statement.setPoolable(true)));
	}

	@ParameterizedTest
	@MethodSource("getUnsupportedMethodExecutors")
	void shouldThrowUnsupported(StatementMethodRunner consumer, Class<? extends SQLException> exceptionType) {
		this.statement = newStatement(mock(Connection.class), mock(Neo4jTransactionSupplier.class));
		assertThatThrownBy(() -> consumer.run(this.statement)).isExactlyInstanceOf(exceptionType);
	}

	static Stream<Arguments> getUnsupportedMethodExecutors() {
		return Stream.of(Arguments.of((StatementMethodRunner) Statement::cancel, SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.setCursorName("name"),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.addBatch("query"), SQLException.class),
				Arguments.of((StatementMethodRunner) Statement::clearBatch, SQLException.class),
				Arguments.of((StatementMethodRunner) Statement::executeBatch, SQLException.class),
				Arguments.of(
						(StatementMethodRunner) statement -> statement.getMoreResults(Statement.CLOSE_CURRENT_RESULT),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) Statement::getGeneratedKeys,
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.executeUpdate("query",
						Statement.NO_GENERATED_KEYS), SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.executeUpdate("query", new int[0]),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.executeUpdate("query", new String[0]),
						SQLFeatureNotSupportedException.class),
				Arguments.of(
						(StatementMethodRunner) statement -> statement.execute("query", Statement.NO_GENERATED_KEYS),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.execute("query", new int[0]),
						SQLFeatureNotSupportedException.class),
				Arguments.of((StatementMethodRunner) statement -> statement.execute("query", new String[0]),
						SQLFeatureNotSupportedException.class));
	}

	@ParameterizedTest
	@MethodSource("getUnwrapArgs")
	void shouldUnwrap(Class<?> cls, boolean shouldUnwrap) throws SQLException {
		// given
		this.statement = newStatement(mock(Connection.class), mock(Neo4jTransactionSupplier.class));

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
		this.statement = newStatement(mock(Connection.class), mock(Neo4jTransactionSupplier.class));

		// when
		var wrapperFor = this.statement.isWrapperFor(cls);

		// then
		assertThat(wrapperFor).isEqualTo(shouldUnwrap);
	}

	private static Stream<Arguments> getUnwrapArgs() {
		return Stream.of(Arguments.of(StatementImpl.class, true), Arguments.of(Statement.class, true),
				Arguments.of(Wrapper.class, true), Arguments.of(AutoCloseable.class, true),
				Arguments.of(Object.class, true), Arguments.of(ResultSet.class, false));
	}

	@ParameterizedTest
	@MethodSource("getExecutionsWithInvalidArgument")
	void shouldThrowOnInvalidArgument(StatementMethodRunner consumer) {
		this.statement = newStatement(mock(Connection.class), mock(Neo4jTransactionSupplier.class));
		assertThatThrownBy(() -> consumer.run(this.statement)).isInstanceOf(SQLException.class);
	}

	static Stream<Arguments> getExecutionsWithInvalidArgument() {
		return Stream.of(Arguments.of((StatementMethodRunner) statement -> statement.setMaxFieldSize(-1)),
				Arguments.of((StatementMethodRunner) statement -> statement.setMaxRows(-1)),
				Arguments.of((StatementMethodRunner) statement -> statement.setFetchSize(-1)),
				Arguments.of((StatementMethodRunner) statement -> statement.setQueryTimeout(-1)));
	}

	@Test
	void shouldNotHaveMaxFieldSizeByDefault() throws SQLException {
		this.statement = newStatement(mock(Connection.class), mock(Neo4jTransactionSupplier.class));
		assertThat(this.statement.getMaxFieldSize()).isEqualTo(0);
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, 1 })
	void shouldSetFieldSize(int max) throws SQLException {
		this.statement = newStatement(mock(Connection.class), mock(Neo4jTransactionSupplier.class));
		this.statement.setMaxFieldSize(max);
		assertThat(this.statement.getMaxFieldSize()).isEqualTo(max);
	}

	@Test
	void shouldNotHaveMaxRowsByDefault() throws SQLException {
		this.statement = newStatement(mock(Connection.class), mock(Neo4jTransactionSupplier.class));
		assertThat(this.statement.getMaxRows()).isEqualTo(0);
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, 1 })
	void shouldSetMaxRows(int max) throws SQLException {
		this.statement = newStatement(mock(Connection.class), mock(Neo4jTransactionSupplier.class));
		this.statement.setMaxRows(max);
		assertThat(this.statement.getMaxRows()).isEqualTo(max);
	}

	@Test
	void shouldNotHaveQueryTimeoutByDefault() throws SQLException {
		this.statement = newStatement(mock(Connection.class), mock(Neo4jTransactionSupplier.class));
		assertThat(this.statement.getQueryTimeout()).isEqualTo(0);
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, 1 })
	void shouldSetQueryTimeout(int timeout) throws SQLException {
		this.statement = newStatement(mock(Connection.class), mock(Neo4jTransactionSupplier.class));
		this.statement.setQueryTimeout(timeout);
		assertThat(this.statement.getQueryTimeout()).isEqualTo(timeout);
	}

	@Test
	void shouldNotHaveWarnings() throws SQLException {
		this.statement = newStatement(mock(Connection.class), mock(Neo4jTransactionSupplier.class));
		assertThat((Object) this.statement.getWarnings()).isNull();
	}

	@Test
	void shouldHaveDefaultFetchSize() throws SQLException {
		this.statement = newStatement(mock(Connection.class), mock(Neo4jTransactionSupplier.class));
		assertThat(this.statement.getFetchSize()).isEqualTo(1000);
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, 1 })
	void shouldUpdateFetchSize(int fetchSize) throws SQLException {
		this.statement = newStatement(mock(Connection.class), mock(Neo4jTransactionSupplier.class));
		this.statement.setFetchSize(fetchSize);
		assertThat(this.statement.getFetchSize()).isEqualTo((fetchSize == 0) ? 1000 : fetchSize);
	}

	@Test
	void shouldHaveDefaultFetchDirection() throws SQLException {
		this.statement = newStatement(mock(Connection.class), mock(Neo4jTransactionSupplier.class));
		assertThat(this.statement.getFetchDirection()).isEqualTo(ResultSet.FETCH_FORWARD);
	}

	@ParameterizedTest
	@ValueSource(ints = { ResultSet.FETCH_REVERSE, ResultSet.FETCH_UNKNOWN })
	void shouldIgnoreFetchDirectionUpdates(int fetchDirection) throws SQLException {
		this.statement = newStatement(mock(Connection.class), mock(Neo4jTransactionSupplier.class));
		this.statement.setFetchDirection(fetchDirection);
		assertThat(this.statement.getFetchDirection()).isEqualTo(ResultSet.FETCH_FORWARD);
	}

	@Test
	void shouldHaveDefaultResultSetConcurrency() throws SQLException {
		this.statement = newStatement(mock(Connection.class), mock(Neo4jTransactionSupplier.class));
		assertThat(this.statement.getResultSetConcurrency()).isEqualTo(ResultSet.CONCUR_READ_ONLY);
	}

	@Test
	void shouldHaveDefaultResultSetType() throws SQLException {
		this.statement = newStatement(mock(Connection.class), mock(Neo4jTransactionSupplier.class));
		assertThat(this.statement.getResultSetType()).isEqualTo(ResultSet.TYPE_FORWARD_ONLY);
	}

	@Test
	void shouldHaveDefaultResultSetHoldability() throws SQLException {
		this.statement = newStatement(mock(Connection.class), mock(Neo4jTransactionSupplier.class));
		assertThat(this.statement.getResultSetHoldability()).isEqualTo(ResultSet.CLOSE_CURSORS_AT_COMMIT);
	}

	@Test
	void shouldReturnConnection() throws SQLException {
		var connection = mock(Connection.class);
		this.statement = newStatement(connection, mock(Neo4jTransactionSupplier.class));
		assertThat(this.statement.getConnection()).isEqualTo(connection);
	}

	@Test
	void shouldNotBeCloseOnCompletionByDefault() throws SQLException {
		this.statement = newStatement(mock(Connection.class), mock(Neo4jTransactionSupplier.class));
		assertThat(this.statement.isCloseOnCompletion()).isFalse();
	}

	@Test
	void shouldUpdateCloseOnCompletion() throws SQLException {
		this.statement = newStatement(mock(Connection.class), mock(Neo4jTransactionSupplier.class));
		this.statement.closeOnCompletion();
		assertThat(this.statement.isCloseOnCompletion()).isTrue();
	}

	static Stream<Arguments> shouldEnforceCypher() {
		var mlQuery = """
				/*+ NEO4J FORCE_CYPHER */
				MATCH (:Station { name: 'Denmark Hill' })<-[:CALLS_AT]-(d:Stop)
					((:Stop)-[:NEXT]->(:Stop)){1,3}
					(a:Stop)-[:CALLS_AT]->(:Station { name: 'Clapham Junction' })
				RETURN d.departs AS departureTime, a.arrives AS arrivalTime
				""";

		return Stream.of(Arguments.of("/*+ NEO4J FORCE_CYPHER */ MATCH (n) RETURN n", true),
				Arguments.of("MATCH /*+ NEO4J FORCE_CYPHER */ (n) RETURN n", true),
				Arguments.of("MATCH /*+ NEO4J FORCE_CYPHER */ (n)\nRETURN n", true),
				Arguments.of("/*+ NEO4J FORCE_CYPHER */ MATCH (n)\nRETURN n", true),
				Arguments.of("/*+ NEO4J FORCE_CYPHER */\nMATCH (n)\nRETURN n", true),
				Arguments.of("/*+ NEO4J FORCE_CYPHER */\nMATCH (n:'Movie')\nRETURN n", true),
				Arguments.of("/*+ NEO4J FORCE_CYPHER */\nMATCH (n:`Movie`)\nRETURN n", true),
				Arguments.of("MATCH (n:`/*+ NEO4J FORCE_CYPHER */`) RETURN n", false),
				Arguments.of("MATCH (n) SET n.f = '/*+ NEO4J FORCE_CYPHER */' RETURN n", false),
				Arguments.of("MATCH (n) SET n.f = '   /*+ NEO4J FORCE_CYPHER */    ' RETURN n", false),
				Arguments.of("MATCH (n) SET n.f = \"/*+ NEO4J FORCE_CYPHER */\" RETURN n", false),
				Arguments.of(mlQuery, true));
	}

	@ParameterizedTest
	@MethodSource
	void shouldEnforceCypher(String sql, boolean shouldEnforceCypher) {
		assertThat(StatementImpl.forceCypher(sql)).isEqualTo(shouldEnforceCypher);
	}

	@FunctionalInterface
	private interface StatementMethodRunner {

		void run(Statement statement) throws SQLException;

	}

}
