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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.stubbing.Answer;
import org.neo4j.bolt.connection.AccessMode;
import org.neo4j.bolt.connection.AuthInfo;
import org.neo4j.bolt.connection.AuthToken;
import org.neo4j.bolt.connection.AuthTokens;
import org.neo4j.bolt.connection.BoltConnection;
import org.neo4j.bolt.connection.ResponseHandler;
import org.neo4j.bolt.connection.TransactionType;
import org.neo4j.bolt.connection.exception.BoltException;
import org.neo4j.bolt.connection.message.BeginMessage;
import org.neo4j.bolt.connection.message.CommitMessage;
import org.neo4j.bolt.connection.message.DiscardMessage;
import org.neo4j.bolt.connection.message.LogoffMessage;
import org.neo4j.bolt.connection.message.LogonMessage;
import org.neo4j.bolt.connection.message.Message;
import org.neo4j.bolt.connection.message.PullMessage;
import org.neo4j.bolt.connection.message.ResetMessage;
import org.neo4j.bolt.connection.message.RollbackMessage;
import org.neo4j.bolt.connection.message.RunMessage;
import org.neo4j.bolt.connection.summary.CommitSummary;
import org.neo4j.bolt.connection.summary.DiscardSummary;
import org.neo4j.bolt.connection.summary.PullSummary;
import org.neo4j.bolt.connection.summary.RollbackSummary;
import org.neo4j.bolt.connection.summary.RunSummary;
import org.neo4j.jdbc.internal.bolt.BoltAdapters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
		var boltConnection = mockBoltConnection();
		this.transaction = new DefaultTransactionImpl(boltConnection, null, null, NOOP_HANDLER, false, autoCommit,
				AccessMode.WRITE, null, "aBeautifulDatabase", state -> {
				}, Authentication.usernameAndPassword("foo", "bar"));

		assertThat(this.transaction.getState()).isEqualTo(Neo4jTransaction.State.NEW);
		assertThat(this.transaction.isRunnable()).isEqualTo(true);
		assertThat(this.transaction.isOpen()).isEqualTo(true);
		assertThat(this.transaction.isAutoCommit()).isEqualTo(autoCommit);

		var transactionType = autoCommit ? TransactionType.UNCONSTRAINED : TransactionType.DEFAULT;

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Message>> argumentCaptor = ArgumentCaptor.forClass(List.class);
		then(boltConnection).should().write(argumentCaptor.capture());
		var beginMessage = (BeginMessage) argumentCaptor.getValue().get(0);
		assertThat(beginMessage.databaseName().orElse(null)).isEqualTo("aBeautifulDatabase");
		assertThat(beginMessage.transactionType()).isEqualTo(transactionType);
		assertThat(beginMessage.bookmarks().isEmpty()).isTrue();
		then(boltConnection).should().authInfo();
		then(boltConnection).shouldHaveNoMoreInteractions();
	}

	@Test
	void shouldRunAndPull() throws SQLException {
		var boltConnection = mockBoltConnection();
		this.transaction = new DefaultTransactionImpl(boltConnection, null, null, NOOP_HANDLER, false, true,
				AccessMode.WRITE, null, "aBeautifulDatabase", state -> {
				}, Authentication.usernameAndPassword("foo", "bar"));
		var query = "query";
		var fetchSize = 5;
		given(boltConnection.writeAndFlush(any(), messageTypeMatcher(List.of(RunMessage.class, PullMessage.class))))
			.willAnswer((Answer<CompletableFuture<Void>>) invocation -> {
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
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Message>> beginMessagesCaptor = ArgumentCaptor.forClass(List.class);
		then(boltConnection).should().write(beginMessagesCaptor.capture());
		var beginMessage = (BeginMessage) beginMessagesCaptor.getValue().get(0);
		assertThat(beginMessage.databaseName().orElse(null)).isEqualTo("aBeautifulDatabase");
		assertThat(beginMessage.transactionType()).isEqualTo(TransactionType.UNCONSTRAINED);
		assertThat(beginMessage.bookmarks().isEmpty()).isTrue();
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Message>> runMessagesCaptor = ArgumentCaptor.forClass(List.class);
		then(boltConnection).should().writeAndFlush(any(), runMessagesCaptor.capture());
		var messages = runMessagesCaptor.getValue();
		assertThat(messages.get(0)).isInstanceOf(RunMessage.class);
		var runMessage = (RunMessage) messages.get(0);
		assertThat(runMessage.query()).isEqualTo("query");
		assertThat(messages.get(1)).isInstanceOf(PullMessage.class);
		then(boltConnection).should().authInfo();
		then(boltConnection).shouldHaveNoMoreInteractions();
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldRunAndDiscard(boolean commit) throws SQLException {
		var boltConnection = mockBoltConnection();
		this.transaction = new DefaultTransactionImpl(boltConnection, null, null, NOOP_HANDLER, false, true,
				AccessMode.WRITE, null, "aBeautifulDatabase", state -> {
				}, Authentication.usernameAndPassword("foo", "bar"));
		var query = "query";
		var fetchSize = 5;

		given(boltConnection.writeAndFlush(any(), anyList()))
			.willAnswer((Answer<CompletableFuture<Void>>) invocation -> {
				invocation.<ResponseHandler>getArgument(0).onRunSummary(mock(RunSummary.class));
				invocation.<ResponseHandler>getArgument(0).onDiscardSummary(mock(DiscardSummary.class));
				invocation.<ResponseHandler>getArgument(0).onComplete();
				return CompletableFuture.completedFuture(null);
			});

		var response = this.transaction.runAndDiscard(query, Collections.emptyMap(), fetchSize, commit);

		assertThat(response).isNotNull();
		assertThat(this.transaction.getState())
			.isEqualTo(commit ? Neo4jTransaction.State.COMMITTED : Neo4jTransaction.State.READY);
		assertThat(this.transaction.isRunnable()).isEqualTo(!commit);
		assertThat(this.transaction.isOpen()).isEqualTo(!commit);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Message>> beginMessagesCaptor = ArgumentCaptor.forClass(List.class);
		then(boltConnection).should().write(beginMessagesCaptor.capture());
		var beginMessage = (BeginMessage) beginMessagesCaptor.getValue().get(0);
		assertThat(beginMessage.databaseName().orElse(null)).isEqualTo("aBeautifulDatabase");
		assertThat(beginMessage.transactionType()).isEqualTo(TransactionType.UNCONSTRAINED);
		assertThat(beginMessage.bookmarks().isEmpty()).isTrue();
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Message>> runMessagesCaptor = ArgumentCaptor.forClass(List.class);
		then(boltConnection).should().writeAndFlush(any(), runMessagesCaptor.capture());
		var messages = runMessagesCaptor.getValue();
		assertThat(messages).hasSize(commit ? 3 : 2);
		if (commit) {
			assertThat(messages.get(2)).isInstanceOf(CommitMessage.class);
		}
		then(boltConnection).should().authInfo();
		then(boltConnection).shouldHaveNoMoreInteractions();
	}

	@Test
	void shouldPull() throws SQLException {
		var boltConnection = mockBoltConnection();
		this.transaction = new DefaultTransactionImpl(boltConnection, null, null, NOOP_HANDLER, false, true,
				AccessMode.WRITE, Neo4jTransaction.State.READY, "aBeautifulDatabase", state -> {
				}, Authentication.usernameAndPassword("foo", "bar"));
		var fetchSize = 5;
		var runResponse = mock(Neo4jTransaction.RunResponse.class);
		given(runResponse.queryId()).willReturn(-1L);

		given(boltConnection.writeAndFlush(any(), any(PullMessage.class)))
			.willAnswer((Answer<CompletableFuture<Void>>) invocation -> {
				invocation.<ResponseHandler>getArgument(0).onPullSummary(mock(PullSummary.class));
				invocation.<ResponseHandler>getArgument(0).onComplete();
				return CompletableFuture.completedFuture(null);
			});

		var response = this.transaction.pull(runResponse, fetchSize);

		assertThat(response).isNotNull();
		assertThat(this.transaction.getState()).isEqualTo(Neo4jTransaction.State.READY);
		assertThat(this.transaction.isRunnable()).isEqualTo(true);
		assertThat(this.transaction.isOpen()).isEqualTo(true);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Message>> beginMessagesCaptor = ArgumentCaptor.forClass(List.class);
		then(boltConnection).should().write(beginMessagesCaptor.capture());
		var beginMessage = (BeginMessage) beginMessagesCaptor.getValue().get(0);
		assertThat(beginMessage.databaseName().orElse(null)).isEqualTo("aBeautifulDatabase");
		assertThat(beginMessage.transactionType()).isEqualTo(TransactionType.UNCONSTRAINED);
		assertThat(beginMessage.bookmarks().isEmpty()).isTrue();
		var pullMessageCaptor = ArgumentCaptor.forClass(Message.class);
		then(boltConnection).should().writeAndFlush(any(), pullMessageCaptor.capture());
		assertThat(pullMessageCaptor.getValue()).isInstanceOf(PullMessage.class);
		then(boltConnection).should().authInfo();
		then(boltConnection).shouldHaveNoMoreInteractions();
	}

	@Test
	void shouldCommit() throws SQLException {
		var boltConnection = mockBoltConnection();
		this.transaction = new DefaultTransactionImpl(boltConnection, null, null, NOOP_HANDLER, false, true,
				AccessMode.WRITE, Neo4jTransaction.State.READY, "aBeautifulDatabase", state -> {
				}, Authentication.usernameAndPassword("foo", "bar"));
		given(boltConnection.writeAndFlush(any(), anyList()))
			.willAnswer((Answer<CompletableFuture<Void>>) invocation -> {
				invocation.<ResponseHandler>getArgument(0).onCommitSummary(mock(CommitSummary.class));
				invocation.<ResponseHandler>getArgument(0).onComplete();
				return CompletableFuture.completedFuture(null);
			});

		this.transaction.commit();

		assertThat(this.transaction.getState()).isEqualTo(Neo4jTransaction.State.COMMITTED);
		assertThat(this.transaction.isRunnable()).isEqualTo(false);
		assertThat(this.transaction.isOpen()).isEqualTo(false);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Message>> beginMessagesCaptor = ArgumentCaptor.forClass(List.class);
		then(boltConnection).should().write(beginMessagesCaptor.capture());
		var beginMessage = (BeginMessage) beginMessagesCaptor.getValue().get(0);
		assertThat(beginMessage.databaseName().orElse(null)).isEqualTo("aBeautifulDatabase");
		assertThat(beginMessage.transactionType()).isEqualTo(TransactionType.UNCONSTRAINED);
		assertThat(beginMessage.bookmarks().isEmpty()).isTrue();
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Message>> commitMessagesCaptor = ArgumentCaptor.forClass(List.class);
		then(boltConnection).should().writeAndFlush(any(), commitMessagesCaptor.capture());
		var messages = commitMessagesCaptor.getValue();
		assertThat(messages).hasSize(1);
		assertThat(messages.get(0)).isInstanceOf(CommitMessage.class);
		then(boltConnection).should().authInfo();
		then(boltConnection).shouldHaveNoMoreInteractions();
	}

	@Test
	void shouldRollback() throws SQLException {
		var boltConnection = mockBoltConnection();
		this.transaction = new DefaultTransactionImpl(boltConnection, null, null, NOOP_HANDLER, false, true,
				AccessMode.WRITE, Neo4jTransaction.State.READY, "aBeautifulDatabase", state -> {
				}, Authentication.usernameAndPassword("foo", "bar"));
		given(boltConnection.writeAndFlush(any(), anyList()))
			.willAnswer((Answer<CompletableFuture<Void>>) invocation -> {
				invocation.<ResponseHandler>getArgument(0).onRollbackSummary(mock(RollbackSummary.class));
				invocation.<ResponseHandler>getArgument(0).onComplete();
				return CompletableFuture.completedFuture(null);
			});

		this.transaction.rollback();

		assertThat(this.transaction.getState()).isEqualTo(Neo4jTransaction.State.ROLLEDBACK);
		assertThat(this.transaction.isRunnable()).isEqualTo(false);
		assertThat(this.transaction.isOpen()).isEqualTo(false);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Message>> beginMessagesCaptor = ArgumentCaptor.forClass(List.class);
		then(boltConnection).should().write(beginMessagesCaptor.capture());
		var beginMessage = (BeginMessage) beginMessagesCaptor.getValue().get(0);
		assertThat(beginMessage.databaseName().orElse(null)).isEqualTo("aBeautifulDatabase");
		assertThat(beginMessage.transactionType()).isEqualTo(TransactionType.UNCONSTRAINED);
		assertThat(beginMessage.bookmarks().isEmpty()).isTrue();
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Message>> rollbackMessagesCaptor = ArgumentCaptor.forClass(List.class);
		then(boltConnection).should().writeAndFlush(any(), rollbackMessagesCaptor.capture());
		var messages = rollbackMessagesCaptor.getValue();
		assertThat(messages).hasSize(1);
		assertThat(messages.get(0)).isInstanceOf(RollbackMessage.class);
		then(boltConnection).should().authInfo();
		then(boltConnection).shouldHaveNoMoreInteractions();
	}

	private static BoltConnection mockBoltConnection() {
		var boltConnection = mock(BoltConnection.class);
		given(boltConnection.authInfo()).willReturn(CompletableFuture.completedFuture(new AuthInfo() {

			@Override
			public AuthToken authToken() {
				return AuthTokens.basic("foo", "bar", null, BoltAdapters.getValueFactory());
			}

			@Override
			public long authAckMillis() {
				return 0;
			}
		}));
		given(boltConnection.write(ArgumentMatchers.<List<Message>>argThat(argument -> {
			var message = argument.get(0);
			return message instanceof ResetMessage || message instanceof BeginMessage beginMessage
					&& "aBeautifulDatabase".equals(beginMessage.databaseName().orElse(null))
					&& AccessMode.WRITE.equals(beginMessage.accessMode()) && beginMessage.bookmarks().isEmpty();
		}))).willReturn(CompletableFuture.completedStage(null));
		return boltConnection;
	}

	@Test
	void shouldReauthWhenAuthMismatch() {
		var boltConnection = mockBoltConnection();
		this.transaction = new DefaultTransactionImpl(boltConnection, null, null, NOOP_HANDLER, false, false,
				AccessMode.WRITE, Neo4jTransaction.State.READY, "aBeautifulDatabase", state -> {
				}, Authentication.none());
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Message>> messageCaptor = ArgumentCaptor.forClass(List.class);
		then(boltConnection).should().write(messageCaptor.capture());
		var messages = messageCaptor.getValue();
		assertThat(messages).hasSize(3);
		assertThat(messages).first().isInstanceOf(LogoffMessage.class);
		assertThat(messages.get(1)).isInstanceOf(LogonMessage.class).satisfies(m -> {
			var logonMessage = (LogonMessage) m;
			assertThat(logonMessage.authToken()).isEqualTo(AuthTokens.none(BoltAdapters.getValueFactory()));
		});
		then(boltConnection).should().authInfo();
		then(boltConnection).shouldHaveNoMoreInteractions();
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldFail(boolean autoCommit) throws SQLException {
		var boltConnection = mockBoltConnection();
		this.transaction = new DefaultTransactionImpl(boltConnection, null, null, NOOP_HANDLER, false, autoCommit,
				AccessMode.WRITE, Neo4jTransaction.State.READY, "aBeautifulDatabase", state -> {
				}, Authentication.usernameAndPassword("foo", "bar"));
		given(boltConnection.writeAndFlush(any(), anyList()))
			.willAnswer((Answer<CompletableFuture<Void>>) invocation -> {
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
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Message>> beginMessagesCaptor = ArgumentCaptor.forClass(List.class);
		then(boltConnection).should().write(beginMessagesCaptor.capture());
		var beginMessage = (BeginMessage) beginMessagesCaptor.getValue().get(0);
		assertThat(beginMessage.databaseName().orElse(null)).isEqualTo("aBeautifulDatabase");
		assertThat(beginMessage.transactionType()).isEqualTo(transactionType);
		assertThat(beginMessage.bookmarks().isEmpty()).isTrue();
		then(boltConnection).should().authInfo();
		then(boltConnection).shouldHaveNoMoreInteractions();
	}

	@Test
	void shouldRollbackToFailed() throws SQLException {
		var boltConnection = mockBoltConnection();
		this.transaction = new DefaultTransactionImpl(boltConnection, null, null, NOOP_HANDLER, false, false,
				AccessMode.WRITE, Neo4jTransaction.State.OPEN_FAILED, "aBeautifulDatabase", state -> {
				}, Authentication.usernameAndPassword("foo", "bar"));
		this.transaction.rollback();

		assertThat(this.transaction.getState()).isEqualTo(Neo4jTransaction.State.FAILED);
		assertThat(this.transaction.isRunnable()).isEqualTo(false);
		assertThat(this.transaction.isOpen()).isEqualTo(false);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Message>> beginMessagesCaptor = ArgumentCaptor.forClass(List.class);
		then(boltConnection).should().write(beginMessagesCaptor.capture());
		var beginMessage = (BeginMessage) beginMessagesCaptor.getValue().get(0);
		assertThat(beginMessage.databaseName().orElse(null)).isEqualTo("aBeautifulDatabase");
		assertThat(beginMessage.accessMode()).isEqualTo(AccessMode.WRITE);
		assertThat(beginMessage.transactionType()).isEqualTo(TransactionType.DEFAULT);
		assertThat(beginMessage.bookmarks().isEmpty()).isTrue();
		then(boltConnection).should().authInfo();
		then(boltConnection).shouldHaveNoMoreInteractions();
	}

	@ParameterizedTest
	@MethodSource("getShouldThrowInInvalidStateArgs")
	void shouldThrowInInvalidState(boolean autocommit, Neo4jTransaction.State state, TransactionMethodRunner runner) {
		var boltConnection = mockBoltConnection();
		this.transaction = new DefaultTransactionImpl(boltConnection, null, null, NOOP_HANDLER, false, autocommit,
				AccessMode.WRITE, state, "aBeautifulDatabase", failureState -> {
				}, Authentication.usernameAndPassword("foo", "bar"));

		assertThatThrownBy(() -> runner.run(this.transaction)).isExactlyInstanceOf(Neo4jException.class);
		assertThat(this.transaction.getState()).isEqualTo(state);
		var transactionType = autocommit ? TransactionType.UNCONSTRAINED : TransactionType.DEFAULT;

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Message>> beginMessagesCaptor = ArgumentCaptor.forClass(List.class);
		then(boltConnection).should().write(beginMessagesCaptor.capture());
		var beginMessage = (BeginMessage) beginMessagesCaptor.getValue().get(0);
		assertThat(beginMessage.databaseName().orElse(null)).isEqualTo("aBeautifulDatabase");
		assertThat(beginMessage.accessMode()).isEqualTo(AccessMode.WRITE);
		assertThat(beginMessage.transactionType()).isEqualTo(transactionType);
		assertThat(beginMessage.bookmarks().isEmpty()).isTrue();
		then(boltConnection).should().authInfo();
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
				AccessMode.WRITE, state, "aBeautifulDatabase", failureState -> {
				}, Authentication.usernameAndPassword("foo", "bar"));

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
				AccessMode.WRITE, state, "aBeautifulDatabase", failureState -> {
				}, Authentication.usernameAndPassword("foo", "bar"));
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
				autoCommit, AccessMode.WRITE, null, "aBeautifulDatabase", state -> {
				}, Authentication.usernameAndPassword("foo", "bar"));

		var query = "query";
		var parameters = Collections.<String, Object>emptyMap();
		var fetchSize = 5;
		given(boltConnection.writeAndFlush(any(), anyList()))
			.willAnswer((Answer<CompletableFuture<Void>>) invocation -> {
				invocation.<ResponseHandler>getArgument(0).onRunSummary(mock(RunSummary.class));
				invocation.<ResponseHandler>getArgument(0).onPullSummary(mock(PullSummary.class));
				return CompletableFuture.completedFuture(null);
			});

		var transactionType = autoCommit ? TransactionType.UNCONSTRAINED : TransactionType.DEFAULT;
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Message>> beginMessagesCaptor = ArgumentCaptor.forClass(List.class);
		then(boltConnection).should().write(beginMessagesCaptor.capture());
		var beginMessage = (BeginMessage) beginMessagesCaptor.getValue().get(0);
		assertThat(beginMessage.databaseName().orElse(null)).isEqualTo("aBeautifulDatabase");
		assertThat(beginMessage.accessMode()).isEqualTo(AccessMode.WRITE);
		assertThat(beginMessage.transactionType()).isEqualTo(transactionType);
		assertThat(beginMessage.bookmarks().isEmpty()).isTrue();
		assertThatThrownBy(() -> this.transaction.runAndPull(query, parameters, fetchSize, 1))
			.isExactlyInstanceOf(SQLTimeoutException.class);
		assertThat(this.transaction.getState())
			.isEqualTo(autoCommit ? Neo4jTransaction.State.FAILED : Neo4jTransaction.State.OPEN_FAILED);
		assertThat(this.transaction.isRunnable()).isFalse();
		assertThat(this.transaction.isOpen()).isEqualTo(!autoCommit);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Message>> runMessagesCaptor = ArgumentCaptor.forClass(List.class);
		then(boltConnection).should().writeAndFlush(any(), runMessagesCaptor.capture());
		var messages = runMessagesCaptor.getValue();
		var runMessage = (RunMessage) messages.get(0);
		assertThat(runMessage.query()).isEqualTo(query);
		assertThat(runMessage.parameters()).isEqualTo(parameters);
		var pullMessage = (PullMessage) messages.get(1);
		assertThat(pullMessage.qid()).isEqualTo(-1L);
		assertThat(pullMessage.request()).isEqualTo(fetchSize);
		then(boltConnection).should().authInfo();
		then(fatalExceptionHandler).shouldHaveNoMoreInteractions();
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldFailOnRunAndDiscardTimeout(boolean autoCommit) {
		var boltConnection = mockBoltConnection();
		var fatalExceptionHandler = mock(DefaultTransactionImpl.FatalExceptionHandler.class);
		this.transaction = new DefaultTransactionImpl(boltConnection, null, null, fatalExceptionHandler, false,
				autoCommit, AccessMode.WRITE, null, "aBeautifulDatabase", state -> {
				}, Authentication.usernameAndPassword("foo", "bar"));

		var query = "query";
		var parameters = Collections.<String, Object>emptyMap();
		given(boltConnection.writeAndFlush(any(), anyList()))
			.willAnswer((Answer<CompletableFuture<Void>>) invocation -> {
				invocation.<ResponseHandler>getArgument(0).onRunSummary(mock(RunSummary.class));
				invocation.<ResponseHandler>getArgument(0).onDiscardSummary(mock(DiscardSummary.class));
				return CompletableFuture.completedFuture(null);
			});

		assertThatThrownBy(() -> this.transaction.runAndDiscard(query, parameters, 1, false))
			.isExactlyInstanceOf(SQLTimeoutException.class);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Message>> runMessagesCaptor = ArgumentCaptor.forClass(List.class);
		then(boltConnection).should().writeAndFlush(any(), runMessagesCaptor.capture());
		var messages = runMessagesCaptor.getValue();
		assertThat(messages).hasSize(2);
		var runMessage = (RunMessage) messages.get(0);
		assertThat(runMessage.query()).isEqualTo(query);
		assertThat(runMessage.parameters()).isEqualTo(parameters);
		var discardMessage = (DiscardMessage) messages.get(1);
		assertThat(discardMessage.qid()).isEqualTo(-1L);
		assertThat(this.transaction.getState())
			.isEqualTo(autoCommit ? Neo4jTransaction.State.FAILED : Neo4jTransaction.State.OPEN_FAILED);
		assertThat(this.transaction.isRunnable()).isFalse();
		assertThat(this.transaction.isOpen()).isEqualTo(!autoCommit);
		then(boltConnection).should().authInfo();
		then(fatalExceptionHandler).shouldHaveNoMoreInteractions();
	}

	@ParameterizedTest
	@MethodSource("getNetworkTransactionMethodRunners")
	void shouldNotifyExceptionHandlerOnFatalException(TransactionMethodRunner runner) {
		var boltConnection = mockBoltConnection();
		var fatalExceptionHandler = mock(DefaultTransactionImpl.FatalExceptionHandler.class);
		var writeFuture = CompletableFuture.<Void>completedFuture(null);
		var exception = new BoltException("Defunct connection");

		given(boltConnection.writeAndFlush(any(), any(ResetMessage.class)))
			.willReturn(CompletableFuture.failedFuture(exception));
		given(boltConnection.writeAndFlush(any(), any(RunMessage.class))).willReturn(writeFuture);
		given(boltConnection.writeAndFlush(any(), any(PullMessage.class))).willReturn(writeFuture);
		given(boltConnection.writeAndFlush(any(), any(DiscardMessage.class))).willReturn(writeFuture);
		given(boltConnection.writeAndFlush(any(), any(CommitMessage.class))).willReturn(writeFuture);
		given(boltConnection.writeAndFlush(any(), any(RollbackMessage.class))).willReturn(writeFuture);

		this.transaction = new DefaultTransactionImpl(boltConnection, null, null, fatalExceptionHandler, true, true,
				AccessMode.WRITE, null, "aBeautifulDatabase", state -> {
				}, Authentication.usernameAndPassword("foo", "bar"));

		assertThatThrownBy(() -> runner.run(this.transaction)).isExactlyInstanceOf(Neo4jException.class);
		assertThat(this.transaction.getState()).isEqualTo(Neo4jTransaction.State.FAILED);
		assertThat(this.transaction.isRunnable()).isFalse();
		assertThat(this.transaction.isOpen()).isEqualTo(false);
		then(fatalExceptionHandler).should().handle(any(SQLException.class), any(SQLException.class));
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldDiscardOpenCursorsOnClose(boolean commit) throws SQLException {
		var boltConnection = mockBoltConnection();
		this.transaction = new DefaultTransactionImpl(boltConnection, null, null, NOOP_HANDLER, false, false,
				AccessMode.WRITE, Neo4jTransaction.State.READY, "aBeautifulDatabase", state -> {
				}, Authentication.usernameAndPassword("foo", "bar"));

		var query = "query";
		var parameters = Collections.<String, Object>emptyMap();
		var counter = new AtomicInteger();
		given(boltConnection.writeAndFlush(any(), messageTypeMatcher(List.of(RunMessage.class, PullMessage.class))))
			.willAnswer((Answer<CompletableFuture<Void>>) invocation -> {
				invocation.<ResponseHandler>getArgument(0).onRunSummary(mock(RunSummary.class));
				var pullSummary = mock(PullSummary.class);
				given(pullSummary.hasMore()).willReturn(counter.compareAndSet(0, 1));
				invocation.<ResponseHandler>getArgument(0).onPullSummary(pullSummary);
				invocation.<ResponseHandler>getArgument(0).onComplete();
				return CompletableFuture.completedFuture(null);
			});
		given(boltConnection.writeAndFlush(any(),
				messageTypeMatcher(
						List.of(DiscardMessage.class, commit ? CommitMessage.class : RollbackMessage.class))))
			.willAnswer((Answer<CompletableFuture<Void>>) invocation -> {
				invocation.<ResponseHandler>getArgument(0).onDiscardSummary(mock(DiscardSummary.class));
				if (commit) {
					invocation.<ResponseHandler>getArgument(0).onCommitSummary(mock(CommitSummary.class));
				}
				else {
					invocation.<ResponseHandler>getArgument(0).onRollbackSummary(mock(RollbackSummary.class));
				}
				invocation.<ResponseHandler>getArgument(0).onComplete();
				return CompletableFuture.completedFuture(null);
			});

		this.transaction.runAndPull(query, parameters, 1, 0);
		this.transaction.runAndPull(query, parameters, 1, 0);

		if (commit) {
			this.transaction.commit();
		}
		else {
			this.transaction.rollback();
		}

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Message>> beginMessagesCaptor = ArgumentCaptor.forClass(List.class);
		then(boltConnection).should().write(beginMessagesCaptor.capture());
		var beginMessage = (BeginMessage) beginMessagesCaptor.getValue().get(0);
		assertThat(beginMessage.databaseName().orElse(null)).isEqualTo("aBeautifulDatabase");
		assertThat(beginMessage.accessMode()).isEqualTo(AccessMode.WRITE);
		assertThat(beginMessage.transactionType()).isEqualTo(TransactionType.DEFAULT);
		assertThat(beginMessage.bookmarks().isEmpty()).isTrue();
		then(boltConnection).should(times(2))
			.writeAndFlush(any(), messageTypeMatcher(List.of(RunMessage.class, PullMessage.class)));
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Message>> runMessagesCaptor = ArgumentCaptor.forClass(List.class);
		then(boltConnection).should(times(3)).writeAndFlush(any(), runMessagesCaptor.capture());
		var messages = runMessagesCaptor.getAllValues().get(0);
		var runMessage = (RunMessage) messages.get(0);
		assertThat(runMessage.query()).isEqualTo(query);
		assertThat(runMessage.parameters()).isEqualTo(parameters);
		var pullMessage = (PullMessage) messages.get(1);
		assertThat(pullMessage.qid()).isEqualTo(-1L);
		then(boltConnection).should()
			.writeAndFlush(any(), messageTypeMatcher(
					List.of(DiscardMessage.class, commit ? CommitMessage.class : RollbackMessage.class)));
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Message>> finishingMessagesCaptor = ArgumentCaptor.forClass(List.class);
		then(boltConnection).should(times(3)).writeAndFlush(any(), finishingMessagesCaptor.capture());
		var finishMessages = finishingMessagesCaptor.getAllValues().get(2);
		assertThat(finishMessages.get(0)).isInstanceOf(DiscardMessage.class);
		if (commit) {
			assertThat(finishMessages.get(1)).isInstanceOf(CommitMessage.class);
		}
		else {
			assertThat(finishMessages.get(1)).isInstanceOf(RollbackMessage.class);
		}
		then(boltConnection).should().authInfo();
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

	private static List<Message> messageTypeMatcher(List<Class<? extends Message>> messageTypes) {
		return ArgumentMatchers.argThat(messages -> {
			var matches = messages != null && messages.size() == messageTypes.size();
			if (matches) {
				for (var i = 0; i < messageTypes.size(); i++) {
					var message = messages.get(i);
					var messageType = messageTypes.get(i);
					if (!messageType.isAssignableFrom(message.getClass())) {
						matches = false;
						break;
					}
				}
			}
			return matches;
		});
	}

	interface TransactionMethodRunner {

		void run(Neo4jTransaction transaction) throws SQLException;

	}

}
