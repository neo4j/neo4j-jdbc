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

import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.stubbing.Answer;
import org.neo4j.driver.internal.bolt.api.AccessMode;
import org.neo4j.driver.internal.bolt.api.BoltConnection;
import org.neo4j.driver.internal.bolt.api.DatabaseNameUtil;
import org.neo4j.driver.internal.bolt.api.ResponseHandler;
import org.neo4j.driver.internal.bolt.api.TransactionType;
import org.neo4j.driver.internal.bolt.api.exception.BoltException;
import org.neo4j.driver.internal.bolt.api.summary.CommitSummary;
import org.neo4j.driver.internal.bolt.api.summary.DiscardSummary;
import org.neo4j.driver.internal.bolt.api.summary.PullSummary;
import org.neo4j.driver.internal.bolt.api.summary.RollbackSummary;
import org.neo4j.driver.internal.bolt.api.summary.RunSummary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

class DefaultTransactionImplTests {

	static DefaultTransactionImpl.FatalExceptionHandler NOOP_HANDLER = (ignored1, ignored2) -> {
	};

	DefaultTransactionImpl transaction;

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldHaveExpectedDefaults(boolean autoCommit) {
		var boltConnection = mock(BoltConnection.class);
		this.transaction = new DefaultTransactionImpl(boltConnection, null, null, NOOP_HANDLER, false, autoCommit,
				AccessMode.WRITE, null, "aBeautifulDatabase");

		assertThat(this.transaction.getState()).isEqualTo(Neo4jTransaction.State.NEW);
		assertThat(this.transaction.isRunnable()).isEqualTo(true);
		assertThat(this.transaction.isOpen()).isEqualTo(true);
		assertThat(this.transaction.isAutoCommit()).isEqualTo(autoCommit);

		var transactionType = autoCommit ? TransactionType.UNCONSTRAINED : TransactionType.DEFAULT;
		var transactionMode = autoCommit ? "IMPLICIT" : null;

		then(boltConnection).should()
			.beginTransaction(eq(DatabaseNameUtil.database("aBeautifulDatabase")), eq(AccessMode.WRITE), any(),
					eq(Collections.emptySet()), eq(transactionType), any(), any(), eq(transactionMode), any());
		then(boltConnection).shouldHaveNoMoreInteractions();
	}

	@Test
	void shouldRunAndPull() throws SQLException {
		var boltConnection = mockBoltConnection();
		this.transaction = new DefaultTransactionImpl(boltConnection, null, null, NOOP_HANDLER, false, true,
				AccessMode.WRITE, null, "aBeautifulDatabase");
		var query = "query";
		var fetchSize = 5;
		var connectionFuture = CompletableFuture.completedFuture(boltConnection);

		given(boltConnection.run(query, Collections.emptyMap())).willReturn(connectionFuture);
		given(boltConnection.pull(-1, fetchSize)).willReturn(connectionFuture);
		given(boltConnection.flush(any())).willAnswer((Answer<CompletableFuture<Void>>) invocation -> {
			invocation.<ResponseHandler>getArgument(0).onRunSummary(mock(RunSummary.class));
			invocation.<ResponseHandler>getArgument(0).onPullSummary(mock(PullSummary.class));
			invocation.<ResponseHandler>getArgument(0).onComplete();
			return CompletableFuture.completedFuture(null);
		});

		var response = this.transaction.runAndPull(query, Collections.emptyMap(), fetchSize, 5);

		assertThat(response).isNotNull();
		assertThat(response.runResponse().queryId()).isZero();
		assertThat(response.runResponse().keys()).isEmpty();
		assertThat(response.pullResponse()).isNotNull();
		assertThat(this.transaction.getState()).isEqualTo(Neo4jTransaction.State.READY);
		assertThat(this.transaction.isRunnable()).isEqualTo(true);
		assertThat(this.transaction.isOpen()).isEqualTo(true);
		then(boltConnection).should()
			.beginTransaction(eq(DatabaseNameUtil.database("aBeautifulDatabase")), eq(AccessMode.WRITE), any(),
					eq(Collections.emptySet()), eq(TransactionType.UNCONSTRAINED), any(), any(), eq("IMPLICIT"), any());
		then(boltConnection).should().run(eq("query"), any());
		then(boltConnection).should().pull(anyLong(), anyLong());
		then(boltConnection).should().flush(any());
		then(boltConnection).shouldHaveNoMoreInteractions();
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldRunAndDiscard(boolean commit) throws SQLException {
		var boltConnection = mockBoltConnection();
		this.transaction = new DefaultTransactionImpl(boltConnection, null, null, NOOP_HANDLER, false, true,
				AccessMode.WRITE, null, "aBeautifulDatabase");
		var query = "query";
		var fetchSize = 5;
		var connectionFuture = CompletableFuture.completedFuture(boltConnection);

		given(boltConnection.run(query, Collections.emptyMap())).willReturn(connectionFuture);
		given(boltConnection.discard(eq(-1L), anyLong())).willReturn(connectionFuture);
		given(boltConnection.flush(any())).willAnswer((Answer<CompletableFuture<Void>>) invocation -> {
			invocation.<ResponseHandler>getArgument(0).onRunSummary(mock(RunSummary.class));
			invocation.<ResponseHandler>getArgument(0).onDiscardSummary(mock(DiscardSummary.class));
			invocation.<ResponseHandler>getArgument(0).onComplete();
			return CompletableFuture.completedFuture(null);
		});
		if (commit) {
			given(boltConnection.commit()).willReturn(connectionFuture);
		}

		var response = this.transaction.runAndDiscard(query, Collections.emptyMap(), fetchSize, commit);

		assertThat(response).isNotNull();
		assertThat(this.transaction.getState())
			.isEqualTo(commit ? Neo4jTransaction.State.COMMITTED : Neo4jTransaction.State.READY);
		assertThat(this.transaction.isRunnable()).isEqualTo(!commit);
		assertThat(this.transaction.isOpen()).isEqualTo(!commit);
		then(boltConnection).should()
			.beginTransaction(eq(DatabaseNameUtil.database("aBeautifulDatabase")), eq(AccessMode.WRITE), any(),
					eq(Collections.emptySet()), eq(TransactionType.UNCONSTRAINED), any(), any(), eq("IMPLICIT"), any());
		then(boltConnection).should().run(eq("query"), any());
		then(boltConnection).should().discard(eq(-1L), anyLong());
		then(boltConnection).should(times(commit ? 1 : 0)).commit();
		then(boltConnection).should().flush(any());
		then(boltConnection).shouldHaveNoMoreInteractions();
	}

	@Test
	void shouldPull() throws SQLException {
		var boltConnection = mockBoltConnection();
		this.transaction = new DefaultTransactionImpl(boltConnection, null, null, NOOP_HANDLER, false, true,
				AccessMode.WRITE, Neo4jTransaction.State.READY, "aBeautifulDatabase");
		var fetchSize = 5;
		var connectionFuture = CompletableFuture.completedFuture(boltConnection);
		var runResponse = mock(Neo4jTransaction.RunResponse.class);
		given(runResponse.queryId()).willReturn(-1L);

		given(boltConnection.pull(-1, fetchSize)).willReturn(connectionFuture);
		given(boltConnection.flush(any())).willAnswer((Answer<CompletableFuture<Void>>) invocation -> {
			invocation.<ResponseHandler>getArgument(0).onPullSummary(mock(PullSummary.class));
			invocation.<ResponseHandler>getArgument(0).onComplete();
			return CompletableFuture.completedFuture(null);
		});

		var response = this.transaction.pull(runResponse, fetchSize);

		assertThat(response).isNotNull();
		assertThat(this.transaction.getState()).isEqualTo(Neo4jTransaction.State.READY);
		assertThat(this.transaction.isRunnable()).isEqualTo(true);
		assertThat(this.transaction.isOpen()).isEqualTo(true);
		then(boltConnection).should()
			.beginTransaction(eq(DatabaseNameUtil.database("aBeautifulDatabase")), eq(AccessMode.WRITE), any(),
					eq(Collections.emptySet()), eq(TransactionType.UNCONSTRAINED), any(), any(), eq("IMPLICIT"), any());
		then(boltConnection).should().pull(anyLong(), anyLong());
		then(boltConnection).should().flush(any());
		then(boltConnection).shouldHaveNoMoreInteractions();
	}

	@Test
	void shouldCommit() throws SQLException {
		var boltConnection = mockBoltConnection();
		this.transaction = new DefaultTransactionImpl(boltConnection, null, null, NOOP_HANDLER, false, true,
				AccessMode.WRITE, Neo4jTransaction.State.READY, "aBeautifulDatabase");
		var connectionFuture = CompletableFuture.completedFuture(boltConnection);
		given(boltConnection.commit()).willReturn(connectionFuture);
		given(boltConnection.flush(any())).willAnswer((Answer<CompletableFuture<Void>>) invocation -> {
			invocation.<ResponseHandler>getArgument(0).onCommitSummary(mock(CommitSummary.class));
			invocation.<ResponseHandler>getArgument(0).onComplete();
			return CompletableFuture.completedFuture(null);
		});

		this.transaction.commit();

		assertThat(this.transaction.getState()).isEqualTo(Neo4jTransaction.State.COMMITTED);
		assertThat(this.transaction.isRunnable()).isEqualTo(false);
		assertThat(this.transaction.isOpen()).isEqualTo(false);
		then(boltConnection).should()
			.beginTransaction(eq(DatabaseNameUtil.database("aBeautifulDatabase")), eq(AccessMode.WRITE), any(),
					eq(Collections.emptySet()), eq(TransactionType.UNCONSTRAINED), any(), any(), eq("IMPLICIT"), any());
		then(boltConnection).should().commit();
		then(boltConnection).should().flush(any());
		then(boltConnection).shouldHaveNoMoreInteractions();
	}

	@Test
	void shouldRollback() throws SQLException {
		var boltConnection = mockBoltConnection();
		this.transaction = new DefaultTransactionImpl(boltConnection, null, null, NOOP_HANDLER, false, true,
				AccessMode.WRITE, Neo4jTransaction.State.READY, "aBeautifulDatabase");
		var connectionFuture = CompletableFuture.completedFuture(boltConnection);
		given(boltConnection.rollback()).willReturn(connectionFuture);
		given(boltConnection.flush(any())).willAnswer((Answer<CompletableFuture<Void>>) invocation -> {
			invocation.<ResponseHandler>getArgument(0).onRollbackSummary(mock(RollbackSummary.class));
			invocation.<ResponseHandler>getArgument(0).onComplete();
			return CompletableFuture.completedFuture(null);
		});

		this.transaction.rollback();

		assertThat(this.transaction.getState()).isEqualTo(Neo4jTransaction.State.ROLLEDBACK);
		assertThat(this.transaction.isRunnable()).isEqualTo(false);
		assertThat(this.transaction.isOpen()).isEqualTo(false);
		then(boltConnection).should()
			.beginTransaction(eq(DatabaseNameUtil.database("aBeautifulDatabase")), eq(AccessMode.WRITE), any(),
					eq(Collections.emptySet()), eq(TransactionType.UNCONSTRAINED), any(), any(), eq("IMPLICIT"), any());
		then(boltConnection).should().rollback();
		then(boltConnection).should().flush(any());
		then(boltConnection).shouldHaveNoMoreInteractions();
	}

	private static BoltConnection mockBoltConnection() {
		var boltConnection = mock(BoltConnection.class);
		given(boltConnection.beginTransaction(eq(DatabaseNameUtil.database("aBeautifulDatabase")), eq(AccessMode.WRITE),
				any(), eq(Collections.emptySet()), any(TransactionType.class), any(), any(), any(), any()))
			.willReturn(CompletableFuture.completedStage(boltConnection));
		return boltConnection;
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldFail(boolean autoCommit) throws SQLException {
		var boltConnection = mockBoltConnection();
		this.transaction = new DefaultTransactionImpl(boltConnection, null, null, NOOP_HANDLER, false, autoCommit,
				AccessMode.WRITE, Neo4jTransaction.State.READY, "aBeautifulDatabase");
		var connectionFuture = CompletableFuture.completedFuture(boltConnection);
		given(boltConnection.rollback()).willReturn(connectionFuture);
		given(boltConnection.flush(any())).willAnswer((Answer<CompletableFuture<Void>>) invocation -> {
			invocation.<ResponseHandler>getArgument(0).onRollbackSummary(mock(RollbackSummary.class));
			invocation.<ResponseHandler>getArgument(0).onComplete();
			return CompletableFuture.completedFuture(null);
		});

		var exception = mock(SQLException.class);

		this.transaction.fail(exception);

		assertThat(this.transaction.getState())
			.isEqualTo(autoCommit ? Neo4jTransaction.State.FAILED : Neo4jTransaction.State.OPEN_FAILED);
		assertThat(this.transaction.isRunnable()).isEqualTo(false);
		assertThat(this.transaction.isOpen()).isEqualTo(!autoCommit);

		var transactionType = autoCommit ? TransactionType.UNCONSTRAINED : TransactionType.DEFAULT;
		var transactionMode = autoCommit ? "IMPLICIT" : null;
		then(boltConnection).should()
			.beginTransaction(eq(DatabaseNameUtil.database("aBeautifulDatabase")), eq(AccessMode.WRITE), any(),
					eq(Collections.emptySet()), eq(transactionType), any(), any(), eq(transactionMode), any());
		then(boltConnection).shouldHaveNoMoreInteractions();
	}

	@Test
	void shouldRollbackToFailed() throws SQLException {
		var boltConnection = mockBoltConnection();
		this.transaction = new DefaultTransactionImpl(boltConnection, null, null, NOOP_HANDLER, false, false,
				AccessMode.WRITE, Neo4jTransaction.State.OPEN_FAILED, "aBeautifulDatabase");
		this.transaction.rollback();

		assertThat(this.transaction.getState()).isEqualTo(Neo4jTransaction.State.FAILED);
		assertThat(this.transaction.isRunnable()).isEqualTo(false);
		assertThat(this.transaction.isOpen()).isEqualTo(false);

		then(boltConnection).should()
			.beginTransaction(eq(DatabaseNameUtil.database("aBeautifulDatabase")), eq(AccessMode.WRITE), any(),
					eq(Collections.emptySet()), eq(TransactionType.DEFAULT), any(), any(), eq(null), any());
		then(boltConnection).shouldHaveNoMoreInteractions();
	}

	@ParameterizedTest
	@MethodSource("getShouldThrowInInvalidStateArgs")
	void shouldThrowInInvalidState(boolean autocommit, Neo4jTransaction.State state, TransactionMethodRunner runner) {
		var boltConnection = mockBoltConnection();
		this.transaction = new DefaultTransactionImpl(boltConnection, null, null, NOOP_HANDLER, false, autocommit,
				AccessMode.WRITE, state, "aBeautifulDatabase");

		assertThatThrownBy(() -> runner.run(this.transaction)).isExactlyInstanceOf(SQLException.class);
		assertThat(this.transaction.getState()).isEqualTo(state);
		var transactionType = autocommit ? TransactionType.UNCONSTRAINED : TransactionType.DEFAULT;
		var transactionMode = autocommit ? "IMPLICIT" : null;
		then(boltConnection).should()
			.beginTransaction(eq(DatabaseNameUtil.database("aBeautifulDatabase")), eq(AccessMode.WRITE), any(),
					eq(Collections.emptySet()), eq(transactionType), any(), any(), eq(transactionMode), any());
		then(boltConnection).shouldHaveNoMoreInteractions();
	}

	static Stream<Arguments> getShouldThrowInInvalidStateArgs() {
		return Stream.of(
				Arguments.of(true, Neo4jTransaction.State.NEW,
						(TransactionMethodRunner) transaction -> transaction
							.pull(mock(Neo4jTransaction.RunResponse.class), -1)),
				Arguments.of(false, Neo4jTransaction.State.NEW,
						(TransactionMethodRunner) transaction -> transaction
							.pull(mock(Neo4jTransaction.RunResponse.class), -1)),
				Arguments.of(true, Neo4jTransaction.State.FAILED,
						(TransactionMethodRunner) transaction -> transaction
							.pull(mock(Neo4jTransaction.RunResponse.class), -1)),
				Arguments.of(false, Neo4jTransaction.State.FAILED,
						(TransactionMethodRunner) transaction -> transaction
							.pull(mock(Neo4jTransaction.RunResponse.class), -1)),
				Arguments.of(true, Neo4jTransaction.State.COMMITTED,
						(TransactionMethodRunner) transaction -> transaction
							.pull(mock(Neo4jTransaction.RunResponse.class), -1)),
				Arguments.of(false, Neo4jTransaction.State.COMMITTED,
						(TransactionMethodRunner) transaction -> transaction
							.pull(mock(Neo4jTransaction.RunResponse.class), -1)),
				Arguments.of(true, Neo4jTransaction.State.ROLLEDBACK,
						(TransactionMethodRunner) transaction -> transaction
							.pull(mock(Neo4jTransaction.RunResponse.class), -1)),
				Arguments.of(false, Neo4jTransaction.State.ROLLEDBACK,
						(TransactionMethodRunner) transaction -> transaction
							.pull(mock(Neo4jTransaction.RunResponse.class), -1)),
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
		this.transaction = new DefaultTransactionImpl(mockBoltConnection(), null, null, NOOP_HANDLER, false, true,
				AccessMode.WRITE, state, "aBeautifulDatabase");

		var runnable = switch (state) {
			case NEW, READY -> true;
			case OPEN_FAILED, FAILED, COMMITTED, ROLLEDBACK -> false;
		};

		assertThat(this.transaction.isRunnable()).isEqualTo(runnable);
	}

	@ParameterizedTest
	@EnumSource(Neo4jTransaction.State.class)
	void shouldDetermineIsOpen(Neo4jTransaction.State state) {
		this.transaction = new DefaultTransactionImpl(mockBoltConnection(), null, null, NOOP_HANDLER, false, true,
				AccessMode.WRITE, state, "aBeautifulDatabase");
		var open = switch (state) {
			case NEW, READY, OPEN_FAILED -> true;
			case FAILED, COMMITTED, ROLLEDBACK -> false;
		};

		assertThat(this.transaction.isOpen()).isEqualTo(open);
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldFailOnRunAndPullTimeout(boolean autoCommit) {
		var boltConnection = mockBoltConnection();
		var fatalExceptionHandler = mock(DefaultTransactionImpl.FatalExceptionHandler.class);
		this.transaction = new DefaultTransactionImpl(boltConnection, null, null, fatalExceptionHandler, false,
				autoCommit, AccessMode.WRITE, null, "aBeautifulDatabase");
		var connectionFuture = CompletableFuture.completedFuture(boltConnection);

		var query = "query";
		var parameters = Collections.<String, Object>emptyMap();
		var fetchSize = 5;
		given(boltConnection.run(query, Collections.emptyMap())).willReturn(connectionFuture);
		given(boltConnection.pull(-1L, fetchSize)).willReturn(connectionFuture);
		given(boltConnection.flush(any())).willAnswer((Answer<CompletableFuture<Void>>) invocation -> {
			invocation.<ResponseHandler>getArgument(0).onRunSummary(mock(RunSummary.class));
			invocation.<ResponseHandler>getArgument(0).onPullSummary(mock(PullSummary.class));
			return CompletableFuture.completedFuture(null);
		});

		var transactionType = autoCommit ? TransactionType.UNCONSTRAINED : TransactionType.DEFAULT;
		var transactionMode = autoCommit ? "IMPLICIT" : null;
		then(boltConnection).should()
			.beginTransaction(eq(DatabaseNameUtil.database("aBeautifulDatabase")), eq(AccessMode.WRITE), any(),
					eq(Collections.emptySet()), eq(transactionType), any(), any(), eq(transactionMode), any());
		assertThatThrownBy(() -> this.transaction.runAndPull(query, parameters, fetchSize, 1))
			.isExactlyInstanceOf(SQLTimeoutException.class);
		assertThat(this.transaction.getState())
			.isEqualTo(autoCommit ? Neo4jTransaction.State.FAILED : Neo4jTransaction.State.OPEN_FAILED);
		assertThat(this.transaction.isRunnable()).isFalse();
		assertThat(this.transaction.isOpen()).isEqualTo(!autoCommit);
		then(fatalExceptionHandler).shouldHaveNoMoreInteractions();
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldFailOnRunAndDiscardTimeout(boolean autoCommit) {
		var boltConnection = mockBoltConnection();
		var fatalExceptionHandler = mock(DefaultTransactionImpl.FatalExceptionHandler.class);
		this.transaction = new DefaultTransactionImpl(boltConnection, null, null, fatalExceptionHandler, false,
				autoCommit, AccessMode.WRITE, null, "aBeautifulDatabase");
		var connectionFuture = CompletableFuture.completedFuture(boltConnection);

		var query = "query";
		var parameters = Collections.<String, Object>emptyMap();
		given(boltConnection.run(query, Collections.emptyMap())).willReturn(connectionFuture);
		given(boltConnection.discard(eq(-1L), anyLong())).willReturn(connectionFuture);
		given(boltConnection.flush(any())).willAnswer((Answer<CompletableFuture<Void>>) invocation -> {
			invocation.<ResponseHandler>getArgument(0).onRunSummary(mock(RunSummary.class));
			invocation.<ResponseHandler>getArgument(0).onDiscardSummary(mock(DiscardSummary.class));
			return CompletableFuture.completedFuture(null);
		});

		assertThatThrownBy(() -> this.transaction.runAndDiscard(query, parameters, 1, false))
			.isExactlyInstanceOf(SQLTimeoutException.class);
		assertThat(this.transaction.getState())
			.isEqualTo(autoCommit ? Neo4jTransaction.State.FAILED : Neo4jTransaction.State.OPEN_FAILED);
		assertThat(this.transaction.isRunnable()).isFalse();
		assertThat(this.transaction.isOpen()).isEqualTo(!autoCommit);
		then(fatalExceptionHandler).shouldHaveNoMoreInteractions();
	}

	@ParameterizedTest
	@MethodSource("getNetworkTransactionMethodRunners")
	@SuppressWarnings("unchecked")
	void shouldNotifyExceptionHandlerOnFatalException(TransactionMethodRunner runner) {
		var boltConnection = mockBoltConnection();
		var fatalExceptionHandler = mock(DefaultTransactionImpl.FatalExceptionHandler.class);
		var connectionFuture = CompletableFuture.completedFuture(boltConnection);
		var exception = new BoltException("Defunct connection");

		given(boltConnection.reset()).willReturn(CompletableFuture.failedFuture(exception));
		given(boltConnection.run(any(), any())).willReturn(connectionFuture);
		given(boltConnection.pull(anyLong(), anyLong())).willReturn(connectionFuture);
		given(boltConnection.discard(anyLong(), anyLong())).willReturn(connectionFuture);
		given(boltConnection.discard(anyLong(), anyLong())).willReturn(connectionFuture);
		given(boltConnection.commit()).willReturn(connectionFuture);
		given(boltConnection.rollback()).willReturn(connectionFuture);

		this.transaction = new DefaultTransactionImpl(boltConnection, null, null, fatalExceptionHandler, true, true,
				AccessMode.WRITE, null, "aBeautifulDatabase");

		assertThatThrownBy(() -> runner.run(this.transaction)).isExactlyInstanceOf(SQLException.class);
		assertThat(this.transaction.getState()).isEqualTo(Neo4jTransaction.State.FAILED);
		assertThat(this.transaction.isRunnable()).isFalse();
		assertThat(this.transaction.isOpen()).isEqualTo(false);
		then(fatalExceptionHandler).should().handle(any(SQLException.class), any(SQLException.class));
	}

	@SuppressWarnings("unchecked")
	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldDiscardOpenCursorsOnClose(boolean commit) throws SQLException {
		var boltConnection = mockBoltConnection();
		this.transaction = new DefaultTransactionImpl(boltConnection, null, null, NOOP_HANDLER, false, false,
				AccessMode.WRITE, Neo4jTransaction.State.READY, "aBeautifulDatabase");
		var connectionFuture = CompletableFuture.completedFuture(boltConnection);

		var query = "query";
		var parameters = Collections.<String, Object>emptyMap();
		given(boltConnection.run(query, Collections.emptyMap())).willReturn(connectionFuture);

		given(boltConnection.pull(eq(-1L), anyLong())).willReturn(connectionFuture);
		given(boltConnection.discard(anyLong(), anyLong())).willReturn(connectionFuture);
		var counter = new AtomicInteger();
		given(boltConnection.flush(any())).willAnswer((Answer<CompletableFuture<Void>>) invocation -> {
			invocation.<ResponseHandler>getArgument(0).onRunSummary(mock(RunSummary.class));
			var pullSummary = mock(PullSummary.class);
			given(pullSummary.hasMore()).willReturn(counter.compareAndSet(0, 1));
			invocation.<ResponseHandler>getArgument(0).onPullSummary(pullSummary);
			invocation.<ResponseHandler>getArgument(0).onDiscardSummary(mock(DiscardSummary.class));
			invocation.<ResponseHandler>getArgument(0).onComplete();
			return CompletableFuture.completedFuture(null);
		});

		if (commit) {
			given(boltConnection.commit()).willReturn(connectionFuture);
		}
		else {
			given(boltConnection.rollback()).willReturn(connectionFuture);
		}

		this.transaction.runAndPull(query, parameters, 1, 0);
		this.transaction.runAndPull(query, parameters, 1, 0);

		if (commit) {
			this.transaction.commit();
		}
		else {
			this.transaction.rollback();
		}

		then(boltConnection).should()
			.beginTransaction(eq(DatabaseNameUtil.database("aBeautifulDatabase")), eq(AccessMode.WRITE), any(),
					eq(Collections.emptySet()), eq(TransactionType.DEFAULT), any(), any(), any(), any());
		then(boltConnection).should(times(2)).run(query, Collections.emptyMap());
		then(boltConnection).should(times(2)).pull(anyLong(), anyLong());
		then(boltConnection).should().discard(anyLong(), anyLong());
		then(boltConnection).should(times(3)).flush(any());
		if (commit) {
			then(boltConnection).should().commit();
		}
		else {
			then(boltConnection).should().rollback();
		}
		then(boltConnection).shouldHaveNoMoreInteractions();
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
