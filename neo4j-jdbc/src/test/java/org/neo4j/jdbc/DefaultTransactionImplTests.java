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

import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.jdbc.internal.bolt.BoltConnection;
import org.neo4j.jdbc.internal.bolt.exception.BoltException;
import org.neo4j.jdbc.internal.bolt.exception.Neo4jException;
import org.neo4j.jdbc.internal.bolt.response.DiscardResponse;
import org.neo4j.jdbc.internal.bolt.response.PullResponse;
import org.neo4j.jdbc.internal.bolt.response.RunResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

class DefaultTransactionImplTests {

	DefaultTransactionImpl transaction;

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldHaveExpectedDefaults(boolean autoCommit) {
		var boltConnection = mock(BoltConnection.class);
		this.transaction = new DefaultTransactionImpl(boltConnection, (ignored) -> {
		}, new CompletableFuture<>(), autoCommit);

		assertThat(this.transaction.getState()).isEqualTo(Neo4jTransaction.State.NEW);
		assertThat(this.transaction.isRunnable()).isEqualTo(true);
		assertThat(this.transaction.isOpen()).isEqualTo(true);
		assertThat(this.transaction.isAutoCommit()).isEqualTo(autoCommit);
		then(boltConnection).shouldHaveNoMoreInteractions();
	}

	@Test
	void shouldRunAndPull() throws SQLException {
		var boltConnection = mock(BoltConnection.class);
		this.transaction = new DefaultTransactionImpl(boltConnection, (ignored) -> {
		}, CompletableFuture.completedStage(null), true);
		var query = "query";
		var parameters = Collections.<String, Object>emptyMap();
		var fetchSize = 5;
		var runResponse = mock(RunResponse.class);
		var runFuture = CompletableFuture.completedFuture(runResponse);
		given(boltConnection.run(query, parameters, false)).willReturn(runFuture);
		var pullResponse = mock(PullResponse.class);
		given(boltConnection.pull(runFuture, fetchSize)).willReturn(CompletableFuture.completedFuture(pullResponse));

		var response = this.transaction.runAndPull(query, parameters, fetchSize, 5);

		assertThat(response).isNotNull();
		assertThat(response.runResponse()).isEqualTo(runResponse);
		assertThat(response.pullResponse()).isEqualTo(pullResponse);
		assertThat(this.transaction.getState()).isEqualTo(Neo4jTransaction.State.READY);
		assertThat(this.transaction.isRunnable()).isEqualTo(true);
		assertThat(this.transaction.isOpen()).isEqualTo(true);
		then(boltConnection).should().run(query, parameters, false);
		then(boltConnection).should().pull(runFuture, fetchSize);
		then(boltConnection).shouldHaveNoMoreInteractions();
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldRunAndDiscard(boolean commit) throws SQLException {
		var boltConnection = mock(BoltConnection.class);
		this.transaction = new DefaultTransactionImpl(boltConnection, (ignored) -> {
		}, CompletableFuture.completedStage(null), true);
		var query = "query";
		var parameters = Collections.<String, Object>emptyMap();
		var fetchSize = 5;
		var runResponse = mock(RunResponse.class);
		var runFuture = CompletableFuture.completedFuture(runResponse);
		given(boltConnection.run(query, parameters, false)).willReturn(runFuture);
		var discardResponse = mock(DiscardResponse.class);
		given(boltConnection.discard(-1, !commit)).willReturn(CompletableFuture.completedFuture(discardResponse));
		if (commit) {
			given(boltConnection.commit()).willReturn(CompletableFuture.completedFuture(null));
		}

		var response = this.transaction.runAndDiscard(query, parameters, fetchSize, commit);

		assertThat(response).isNotNull();
		assertThat(response).isEqualTo(discardResponse);
		assertThat(this.transaction.getState())
			.isEqualTo(commit ? Neo4jTransaction.State.COMMITTED : Neo4jTransaction.State.READY);
		assertThat(this.transaction.isRunnable()).isEqualTo(!commit);
		assertThat(this.transaction.isOpen()).isEqualTo(!commit);
		then(boltConnection).should().run(query, parameters, false);
		then(boltConnection).should().discard(-1, !commit);
		then(boltConnection).should(times(commit ? 1 : 0)).commit();
		then(boltConnection).shouldHaveNoMoreInteractions();
	}

	@Test
	void shouldPull() throws SQLException {
		var boltConnection = mock(BoltConnection.class);
		this.transaction = new DefaultTransactionImpl(boltConnection, (ignored) -> {
		}, CompletableFuture.completedStage(null), true, Neo4jTransaction.State.READY);
		var fetchSize = 5;
		var runResponse = mock(RunResponse.class);
		var pullResponse = mock(PullResponse.class);
		given(boltConnection.pull(runResponse, fetchSize)).willReturn(CompletableFuture.completedFuture(pullResponse));

		var response = this.transaction.pull(runResponse, fetchSize);

		assertThat(response).isNotNull();
		assertThat(response).isEqualTo(pullResponse);
		assertThat(this.transaction.getState()).isEqualTo(Neo4jTransaction.State.READY);
		assertThat(this.transaction.isRunnable()).isEqualTo(true);
		assertThat(this.transaction.isOpen()).isEqualTo(true);
		then(boltConnection).should().pull(runResponse, fetchSize);
		then(boltConnection).shouldHaveNoMoreInteractions();
	}

	@Test
	void shouldCommit() throws SQLException {
		var boltConnection = mock(BoltConnection.class);
		this.transaction = new DefaultTransactionImpl(boltConnection, (ignored) -> {
		}, CompletableFuture.completedStage(null), true, Neo4jTransaction.State.READY);
		given(boltConnection.commit()).willReturn(CompletableFuture.completedFuture(null));

		this.transaction.commit();

		assertThat(this.transaction.getState()).isEqualTo(Neo4jTransaction.State.COMMITTED);
		assertThat(this.transaction.isRunnable()).isEqualTo(false);
		assertThat(this.transaction.isOpen()).isEqualTo(false);
		then(boltConnection).should().commit();
		then(boltConnection).shouldHaveNoMoreInteractions();
	}

	@Test
	void shouldRollback() throws SQLException {
		var boltConnection = mock(BoltConnection.class);
		this.transaction = new DefaultTransactionImpl(boltConnection, (ignored) -> {
		}, CompletableFuture.completedStage(null), true, Neo4jTransaction.State.READY);
		given(boltConnection.rollback()).willReturn(CompletableFuture.completedFuture(null));

		this.transaction.rollback();

		assertThat(this.transaction.getState()).isEqualTo(Neo4jTransaction.State.ROLLEDBACK);
		assertThat(this.transaction.isRunnable()).isEqualTo(false);
		assertThat(this.transaction.isOpen()).isEqualTo(false);
		then(boltConnection).should().rollback();
		then(boltConnection).shouldHaveNoMoreInteractions();
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldFail(boolean autoCommit) throws SQLException {
		var boltConnection = mock(BoltConnection.class);
		this.transaction = new DefaultTransactionImpl(boltConnection, (ignored) -> {
		}, CompletableFuture.completedStage(null), autoCommit, Neo4jTransaction.State.READY);
		var exception = mock(SQLException.class);

		this.transaction.fail(exception);

		assertThat(this.transaction.getState())
			.isEqualTo(autoCommit ? Neo4jTransaction.State.FAILED : Neo4jTransaction.State.OPEN_FAILED);
		assertThat(this.transaction.isRunnable()).isEqualTo(false);
		assertThat(this.transaction.isOpen()).isEqualTo(!autoCommit);
		then(boltConnection).shouldHaveNoMoreInteractions();
	}

	@Test
	void shouldRollbackToFailed() throws SQLException {
		var boltConnection = mock(BoltConnection.class);
		this.transaction = new DefaultTransactionImpl(boltConnection, (ignored) -> {
		}, CompletableFuture.completedStage(null), false, Neo4jTransaction.State.OPEN_FAILED);

		this.transaction.rollback();

		assertThat(this.transaction.getState()).isEqualTo(Neo4jTransaction.State.FAILED);
		assertThat(this.transaction.isRunnable()).isEqualTo(false);
		assertThat(this.transaction.isOpen()).isEqualTo(false);
		then(boltConnection).shouldHaveNoMoreInteractions();
	}

	@ParameterizedTest
	@MethodSource("getShouldThrowInInvalidStateArgs")
	void shouldThrowInInvalidState(boolean autocommit, Neo4jTransaction.State state, TransactionMethodRunner runner) {
		var boltConnection = mock(BoltConnection.class);
		this.transaction = new DefaultTransactionImpl(boltConnection, (ignored) -> {
		}, CompletableFuture.completedStage(null), autocommit, state);

		assertThatThrownBy(() -> runner.run(this.transaction)).isExactlyInstanceOf(SQLException.class);
		assertThat(this.transaction.getState()).isEqualTo(state);
		then(boltConnection).shouldHaveNoMoreInteractions();
	}

	static Stream<Arguments> getShouldThrowInInvalidStateArgs() {
		return Stream.of(
				Arguments.of(true, Neo4jTransaction.State.NEW,
						(TransactionMethodRunner) transaction -> transaction.pull(mock(RunResponse.class), -1)),
				Arguments.of(false, Neo4jTransaction.State.NEW,
						(TransactionMethodRunner) transaction -> transaction.pull(mock(RunResponse.class), -1)),
				Arguments.of(true, Neo4jTransaction.State.FAILED,
						(TransactionMethodRunner) transaction -> transaction.pull(mock(RunResponse.class), -1)),
				Arguments.of(false, Neo4jTransaction.State.FAILED,
						(TransactionMethodRunner) transaction -> transaction.pull(mock(RunResponse.class), -1)),
				Arguments.of(true, Neo4jTransaction.State.COMMITTED,
						(TransactionMethodRunner) transaction -> transaction.pull(mock(RunResponse.class), -1)),
				Arguments.of(false, Neo4jTransaction.State.COMMITTED,
						(TransactionMethodRunner) transaction -> transaction.pull(mock(RunResponse.class), -1)),
				Arguments.of(true, Neo4jTransaction.State.ROLLEDBACK,
						(TransactionMethodRunner) transaction -> transaction.pull(mock(RunResponse.class), -1)),
				Arguments.of(false, Neo4jTransaction.State.ROLLEDBACK,
						(TransactionMethodRunner) transaction -> transaction.pull(mock(RunResponse.class), -1)),
				Arguments.of(true, Neo4jTransaction.State.FAILED,
						(TransactionMethodRunner) transaction -> transaction.runAndPull("query", Collections.emptyMap(),
								-1, 0)),
				Arguments.of(false, Neo4jTransaction.State.FAILED,
						(TransactionMethodRunner) transaction -> transaction.runAndPull("query", Collections.emptyMap(),
								-1, 0)),
				Arguments.of(true, Neo4jTransaction.State.COMMITTED,
						(TransactionMethodRunner) transaction -> transaction.runAndPull("query", Collections.emptyMap(),
								-1, 0)),
				Arguments.of(false, Neo4jTransaction.State.COMMITTED,
						(TransactionMethodRunner) transaction -> transaction.runAndPull("query", Collections.emptyMap(),
								-1, 0)),
				Arguments.of(true, Neo4jTransaction.State.ROLLEDBACK,
						(TransactionMethodRunner) transaction -> transaction.runAndPull("query", Collections.emptyMap(),
								-1, 0)),
				Arguments.of(false, Neo4jTransaction.State.ROLLEDBACK,
						(TransactionMethodRunner) transaction -> transaction.runAndPull("query", Collections.emptyMap(),
								-1, 0)),
				Arguments.of(true, Neo4jTransaction.State.FAILED,
						(TransactionMethodRunner) transaction -> transaction.runAndDiscard("query",
								Collections.emptyMap(), -1, true)),
				Arguments.of(false, Neo4jTransaction.State.FAILED,
						(TransactionMethodRunner) transaction -> transaction.runAndDiscard("query",
								Collections.emptyMap(), -1, true)),
				Arguments.of(true, Neo4jTransaction.State.COMMITTED,
						(TransactionMethodRunner) transaction -> transaction.runAndDiscard("query",
								Collections.emptyMap(), -1, true)),
				Arguments.of(false, Neo4jTransaction.State.COMMITTED,
						(TransactionMethodRunner) transaction -> transaction.runAndDiscard("query",
								Collections.emptyMap(), -1, true)),
				Arguments.of(true, Neo4jTransaction.State.ROLLEDBACK,
						(TransactionMethodRunner) transaction -> transaction.runAndDiscard("query",
								Collections.emptyMap(), -1, true)),
				Arguments.of(false, Neo4jTransaction.State.ROLLEDBACK,
						(TransactionMethodRunner) transaction -> transaction.runAndDiscard("query",
								Collections.emptyMap(), -1, true)),
				Arguments.of(true, Neo4jTransaction.State.FAILED,
						(TransactionMethodRunner) transaction -> transaction.runAndDiscard("query",
								Collections.emptyMap(), -1, false)),
				Arguments.of(false, Neo4jTransaction.State.FAILED,
						(TransactionMethodRunner) transaction -> transaction.runAndDiscard("query",
								Collections.emptyMap(), -1, false)),
				Arguments.of(true, Neo4jTransaction.State.COMMITTED,
						(TransactionMethodRunner) transaction -> transaction.runAndDiscard("query",
								Collections.emptyMap(), -1, false)),
				Arguments.of(false, Neo4jTransaction.State.COMMITTED,
						(TransactionMethodRunner) transaction -> transaction.runAndDiscard("query",
								Collections.emptyMap(), -1, false)),
				Arguments.of(true, Neo4jTransaction.State.ROLLEDBACK,
						(TransactionMethodRunner) transaction -> transaction.runAndDiscard("query",
								Collections.emptyMap(), -1, false)),
				Arguments.of(false, Neo4jTransaction.State.ROLLEDBACK,
						(TransactionMethodRunner) transaction -> transaction.runAndDiscard("query",
								Collections.emptyMap(), -1, false)),
				Arguments.of(true, Neo4jTransaction.State.FAILED, (TransactionMethodRunner) Neo4jTransaction::commit),
				Arguments.of(false, Neo4jTransaction.State.FAILED, (TransactionMethodRunner) Neo4jTransaction::commit),
				Arguments.of(true, Neo4jTransaction.State.COMMITTED,
						(TransactionMethodRunner) Neo4jTransaction::commit),
				Arguments.of(false, Neo4jTransaction.State.COMMITTED,
						(TransactionMethodRunner) Neo4jTransaction::commit),
				Arguments.of(true, Neo4jTransaction.State.ROLLEDBACK,
						(TransactionMethodRunner) Neo4jTransaction::commit),
				Arguments.of(false, Neo4jTransaction.State.ROLLEDBACK,
						(TransactionMethodRunner) Neo4jTransaction::commit),
				Arguments.of(true, Neo4jTransaction.State.FAILED, (TransactionMethodRunner) Neo4jTransaction::rollback),
				Arguments.of(false, Neo4jTransaction.State.FAILED,
						(TransactionMethodRunner) Neo4jTransaction::rollback),
				Arguments.of(true, Neo4jTransaction.State.COMMITTED,
						(TransactionMethodRunner) Neo4jTransaction::rollback),
				Arguments.of(false, Neo4jTransaction.State.COMMITTED,
						(TransactionMethodRunner) Neo4jTransaction::rollback),
				Arguments.of(true, Neo4jTransaction.State.ROLLEDBACK,
						(TransactionMethodRunner) Neo4jTransaction::rollback),
				Arguments.of(false, Neo4jTransaction.State.ROLLEDBACK,
						(TransactionMethodRunner) Neo4jTransaction::rollback));
	}

	@ParameterizedTest
	@EnumSource(Neo4jTransaction.State.class)
	void shouldDetermineIsRunnable(Neo4jTransaction.State state) {
		this.transaction = new DefaultTransactionImpl(mock(BoltConnection.class), (ignored) -> {
		}, CompletableFuture.completedStage(null), true, state);
		var runnable = switch (state) {
			case NEW, READY -> true;
			case OPEN_FAILED, FAILED, COMMITTED, ROLLEDBACK -> false;
		};

		assertThat(this.transaction.isRunnable()).isEqualTo(runnable);
	}

	@ParameterizedTest
	@EnumSource(Neo4jTransaction.State.class)
	void shouldDetermineIsOpen(Neo4jTransaction.State state) {
		this.transaction = new DefaultTransactionImpl(mock(BoltConnection.class), (ignored) -> {
		}, CompletableFuture.completedStage(null), true, state);
		var open = switch (state) {
			case NEW, READY, OPEN_FAILED -> true;
			case FAILED, COMMITTED, ROLLEDBACK -> false;
		};

		assertThat(this.transaction.isOpen()).isEqualTo(open);
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	@SuppressWarnings("unchecked")
	void shouldFailOnRunAndPullTimeout(boolean autoCommit) {
		var boltConnection = mock(BoltConnection.class);
		Consumer<SQLException> exceptionConsumer = mock(Consumer.class);
		this.transaction = new DefaultTransactionImpl(boltConnection, exceptionConsumer, new CompletableFuture<>(),
				autoCommit);
		var query = "query";
		var parameters = Collections.<String, Object>emptyMap();
		var fetchSize = 5;
		var runResponse = mock(RunResponse.class);
		var runFuture = CompletableFuture.completedFuture(runResponse);
		given(boltConnection.run(query, parameters, false)).willReturn(runFuture);
		var pullResponse = mock(PullResponse.class);
		given(boltConnection.pull(runFuture, fetchSize)).willReturn(CompletableFuture.completedFuture(pullResponse));

		assertThatThrownBy(() -> this.transaction.runAndPull(query, parameters, fetchSize, 1))
			.isExactlyInstanceOf(SQLTimeoutException.class);
		assertThat(this.transaction.getState())
			.isEqualTo(autoCommit ? Neo4jTransaction.State.FAILED : Neo4jTransaction.State.OPEN_FAILED);
		assertThat(this.transaction.isRunnable()).isFalse();
		assertThat(this.transaction.isOpen()).isEqualTo(!autoCommit);
		then(exceptionConsumer).shouldHaveNoMoreInteractions();
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	@SuppressWarnings("unchecked")
	void shouldFailOnRunAndDiscardTimeout(boolean autoCommit) {
		var boltConnection = mock(BoltConnection.class);
		Consumer<SQLException> exceptionConsumer = mock(Consumer.class);
		this.transaction = new DefaultTransactionImpl(boltConnection, exceptionConsumer, new CompletableFuture<>(),
				autoCommit);
		var query = "query";
		var parameters = Collections.<String, Object>emptyMap();
		var runResponse = mock(RunResponse.class);
		var runFuture = CompletableFuture.completedFuture(runResponse);
		given(boltConnection.run(query, parameters, false)).willReturn(runFuture);
		var discardResponse = mock(DiscardResponse.class);
		given(boltConnection.discard(-1, true)).willReturn(CompletableFuture.completedFuture(discardResponse));

		assertThatThrownBy(() -> this.transaction.runAndDiscard(query, parameters, 1, false))
			.isExactlyInstanceOf(SQLTimeoutException.class);
		assertThat(this.transaction.getState())
			.isEqualTo(autoCommit ? Neo4jTransaction.State.FAILED : Neo4jTransaction.State.OPEN_FAILED);
		assertThat(this.transaction.isRunnable()).isFalse();
		assertThat(this.transaction.isOpen()).isEqualTo(!autoCommit);
		then(exceptionConsumer).shouldHaveNoMoreInteractions();
	}

	@ParameterizedTest
	@MethodSource("getNetworkTransactionMethodRunners")
	@SuppressWarnings("unchecked")
	void shouldNotifyExceptionConsumerOnFatalException(TransactionMethodRunner runner) {
		var boltConnection = mock(BoltConnection.class);
		Consumer<SQLException> exceptionConsumer = mock(Consumer.class);
		var exception = new BoltException("Defunct connection");
		this.transaction = new DefaultTransactionImpl(boltConnection, exceptionConsumer,
				CompletableFuture.failedFuture(exception), true);
		given(boltConnection.run(any(), any(), anyBoolean()))
			.willReturn(CompletableFuture.completedFuture(mock(RunResponse.class)));
		given(boltConnection.pull(any(CompletableFuture.class), anyLong()))
			.willReturn(CompletableFuture.completedFuture(mock(PullResponse.class)));
		given(boltConnection.discard(anyLong(), anyBoolean()))
			.willReturn(CompletableFuture.completedFuture(mock(DiscardResponse.class)));
		given(boltConnection.discard(any(), anyLong(), anyBoolean()))
			.willReturn(CompletableFuture.completedFuture(mock(DiscardResponse.class)));
		given(boltConnection.commit()).willReturn(CompletableFuture.completedFuture(null));
		given(boltConnection.rollback()).willReturn(CompletableFuture.completedFuture(null));

		assertThatThrownBy(() -> runner.run(this.transaction)).isExactlyInstanceOf(SQLException.class);
		assertThat(this.transaction.getState()).isEqualTo(Neo4jTransaction.State.FAILED);
		assertThat(this.transaction.isRunnable()).isFalse();
		assertThat(this.transaction.isOpen()).isEqualTo(false);
		then(exceptionConsumer).should().accept(any(SQLException.class));
	}

	@ParameterizedTest
	@MethodSource("getNetworkTransactionMethodRunners")
	@SuppressWarnings("unchecked")
	void shouldThrowOnNeo4jException(TransactionMethodRunner runner) {
		var boltConnection = mock(BoltConnection.class);
		Consumer<SQLException> exceptionConsumer = mock(Consumer.class);
		var exception = new Neo4jException("code", "message");
		this.transaction = new DefaultTransactionImpl(boltConnection, exceptionConsumer,
				CompletableFuture.failedFuture(exception), true);
		given(boltConnection.run(any(), any(), anyBoolean()))
			.willReturn(CompletableFuture.completedFuture(mock(RunResponse.class)));
		given(boltConnection.pull(any(CompletableFuture.class), anyLong()))
			.willReturn(CompletableFuture.completedFuture(mock(PullResponse.class)));
		given(boltConnection.discard(anyLong(), anyBoolean()))
			.willReturn(CompletableFuture.completedFuture(mock(DiscardResponse.class)));
		given(boltConnection.discard(any(), anyLong(), anyBoolean()))
			.willReturn(CompletableFuture.completedFuture(mock(DiscardResponse.class)));
		given(boltConnection.commit()).willReturn(CompletableFuture.completedFuture(null));
		given(boltConnection.rollback()).willReturn(CompletableFuture.completedFuture(null));

		assertThatThrownBy(() -> runner.run(this.transaction)).isExactlyInstanceOf(SQLException.class);
		assertThat(this.transaction.getState()).isEqualTo(Neo4jTransaction.State.FAILED);
		assertThat(this.transaction.isRunnable()).isFalse();
		assertThat(this.transaction.isOpen()).isEqualTo(false);
		then(exceptionConsumer).shouldHaveNoMoreInteractions();
	}

	static Stream<Arguments> getNetworkTransactionMethodRunners() {
		return Stream.of(
				Arguments.of((TransactionMethodRunner) transaction -> transaction.runAndPull("query",
						Collections.emptyMap(), -1, 0)),
				Arguments.of((TransactionMethodRunner) transaction -> transaction.runAndDiscard("query",
						Collections.emptyMap(), -1, true)),
				Arguments.of((TransactionMethodRunner) transaction -> transaction.runAndDiscard("query",
						Collections.emptyMap(), -1, false)),
				Arguments.of((TransactionMethodRunner) Neo4jTransaction::commit),
				Arguments.of((TransactionMethodRunner) Neo4jTransaction::rollback));
	}

	interface TransactionMethodRunner {

		void run(Neo4jTransaction transaction) throws SQLException;

	}

}
