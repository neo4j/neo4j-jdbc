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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;

import org.neo4j.bolt.connection.AccessMode;
import org.neo4j.bolt.connection.AuthInfo;
import org.neo4j.bolt.connection.BasicResponseHandler;
import org.neo4j.bolt.connection.BoltConnection;
import org.neo4j.bolt.connection.NotificationConfig;
import org.neo4j.bolt.connection.TransactionType;
import org.neo4j.bolt.connection.exception.BoltFailureException;
import org.neo4j.bolt.connection.message.Message;
import org.neo4j.bolt.connection.message.Messages;
import org.neo4j.bolt.connection.summary.PullSummary;
import org.neo4j.bolt.connection.values.Value;
import org.neo4j.jdbc.Neo4jException.GQLError;
import org.neo4j.jdbc.authn.spi.Authentication;
import org.neo4j.jdbc.internal.bolt.BoltAdapters;
import org.neo4j.jdbc.values.Record;
import org.neo4j.jdbc.values.Values;

final class DefaultTransactionImpl implements Neo4jTransaction {

	private final BoltConnection boltConnection;

	private final FatalExceptionHandler fatalExceptionHandler;

	private final CompletionStage<Void> beginPipelinedStage;

	private final boolean autoCommit;

	private final BookmarkManager bookmarkManager;

	private final Consumer<State> onFailedCallback;

	private final Set<String> usedBookmarks;

	private final List<RunResponse> openResults = new ArrayList<>();

	private State state;

	private SQLException exception;

	DefaultTransactionImpl(BoltConnection boltConnection, BookmarkManager bookmarkManager,
			Map<String, Object> transactionMetadata, FatalExceptionHandler fatalExceptionHandler, boolean resetNeeded,
			boolean autoCommit, AccessMode accessMode, State state, String databaseName,
			Consumer<State> onFailedCallback, Authentication currentAuthentication) {

		this.boltConnection = Objects.requireNonNull(boltConnection);
		this.fatalExceptionHandler = Objects.requireNonNull(fatalExceptionHandler);

		this.bookmarkManager = Objects.requireNonNullElseGet(bookmarkManager, NoopBookmarkManagerImpl::new);
		this.onFailedCallback = onFailedCallback;
		this.usedBookmarks = this.bookmarkManager.getBookmarks(Function.identity());

		this.autoCommit = autoCommit;
		this.state = Objects.requireNonNullElse(state, State.NEW);

		this.beginPipelinedStage = this.boltConnection.authInfo()
			.thenApply(AuthInfo::authToken)
			.thenCompose(previousAuthToken -> {
				var txType = this.autoCommit ? TransactionType.UNCONSTRAINED : TransactionType.DEFAULT;
				var messages = new ArrayList<Message>(4);
				if (resetNeeded) {
					messages.add(Messages.reset());
				}
				var currentAuthToken = Neo4jDriver.toAuthToken(currentAuthentication);
				if (!previousAuthToken.asMap().equals(currentAuthToken.asMap())) {
					ConnectionImpl.LOGGER.log(Level.FINE,
							() -> "Authentication has changed, pipelining logoff and logon messages");
					messages.add(Messages.logoff());
					messages.add(Messages.logon(Neo4jDriver.toAuthToken(currentAuthentication)));
				}
				messages.add(Messages.beginTransaction(databaseName, accessMode, null, this.usedBookmarks, txType, null,
						BoltAdapters.adaptMap(transactionMetadata), NotificationConfig.defaultConfig()));
				return this.boltConnection.write(messages);
			});

	}

	@Override
	public RunAndPullResponses runAndPull(String query, Map<String, Object> parameters, int fetchSize, int timeout)
			throws SQLException {
		assertNoException();
		assertRunnableState();

		var handler = new BasicResponseHandler();
		var responsesFuture = this.beginPipelinedStage.thenCompose(ignored -> {
			var messages = List.of(Messages.run(query, BoltAdapters.adaptMap(parameters)),
					Messages.pull(-1, fetchSize));
			return this.boltConnection.writeAndFlush(handler, messages);
		})
			.thenCompose(ignored -> handler.summaries())
			.thenApply(DefaultTransactionImpl::asRunAndPullResponses)
			.toCompletableFuture();
		var responses = execute(responsesFuture, timeout);
		if (responses.pullResponse().hasMore()) {
			this.openResults.add(responses.runResponse());
		}
		this.state = State.READY;
		return responses;
	}

	@Override
	public DiscardResponse runAndDiscard(String query, Map<String, Object> parameters, int timeout, boolean commit)
			throws SQLException {
		assertNoException();
		assertRunnableState();

		var handler = new BasicResponseHandler();
		var responsesFuture = this.beginPipelinedStage.thenCompose(ignored -> {
			var messages = new ArrayList<Message>(3);
			messages.add(Messages.run(query, BoltAdapters.adaptMap(parameters)));
			messages.add(Messages.discard(-1, -1));
			if (commit) {
				messages.add(Messages.commit());
			}
			return this.boltConnection.writeAndFlush(handler, messages);
		})
			.thenCompose(ignored -> handler.summaries())
			.thenApply(DefaultTransactionImpl::asDiscardResponse)
			.toCompletableFuture();
		var response = execute(responsesFuture, timeout);
		if (!State.COMMITTED.equals(this.state)) {
			this.state = commit ? State.COMMITTED : State.READY;
		}
		return response;
	}

	@Override
	public PullResponse pull(RunResponse runResponse, long request) throws SQLException {
		assertNoException();
		if (!State.READY.equals(this.state)) {
			throw new Neo4jException(Neo4jException.withReason(
					String.format("The requested action is not supported in %s transaction state", this.state)));
		}
		var handler = new BasicResponseHandler();
		var responseFuture = this.boltConnection.writeAndFlush(handler, Messages.pull(runResponse.queryId(), request))
			.thenCompose(ignored -> handler.summaries())
			.thenApply(summaries -> asPullResponse(runResponse.keys(), summaries.valuesList(), summaries.pullSummary()))
			.toCompletableFuture();
		var pullResponse = execute(responseFuture, 0);
		if (!pullResponse.hasMore()) {
			this.openResults.remove(runResponse);
		}
		return pullResponse;
	}

	@Override
	public void commit() throws SQLException {
		assertNoException();
		assertRunnableState();

		var handler = new BasicResponseHandler();
		var messages = new ArrayList<Message>(this.openResults.size() + 1);
		appendDiscards(messages);
		messages.add(Messages.commit());
		var responsesFuture = this.beginPipelinedStage
			.thenCompose(ignored -> this.boltConnection.writeAndFlush(handler, messages))
			.thenCompose(ignored -> handler.summaries())
			.thenApply(BasicResponseHandler.Summaries::commitSummary)
			.whenComplete((response, error) -> {
				if (!(response == null || response.bookmark().orElse("").isBlank())) {
					this.bookmarkManager.updateBookmarks(Function.identity(), this.usedBookmarks,
							List.of(response.bookmark().orElse("")));
				}
				if (error == null) {
					this.state = State.COMMITTED;
				}
			})
			.toCompletableFuture();
		execute(responsesFuture, 0);
		this.openResults.clear();
	}

	@Override
	public void rollback() throws SQLException {
		if (State.OPEN_FAILED.equals(this.state)) {
			this.state = State.FAILED;
			return;
		}
		assertNoException();
		assertRunnableState();

		var handler = new BasicResponseHandler();
		var messages = new ArrayList<Message>(this.openResults.size() + 1);
		appendDiscards(messages);
		messages.add(Messages.rollback());
		var responsesFuture = this.beginPipelinedStage
			.thenCompose(ignored -> this.boltConnection.writeAndFlush(handler, messages))
			.thenCompose(ignored -> handler.summaries())
			.toCompletableFuture();

		execute(responsesFuture, 0);
		this.state = State.ROLLEDBACK;
		this.openResults.clear();
	}

	@Override
	public boolean isAutoCommit() {
		return this.autoCommit;
	}

	@Override
	public State getState() {
		return this.state;
	}

	@Override
	public void fail(SQLException exception) throws SQLException {
		assertRunnableState();
		this.exception = exception;
		this.state = this.autoCommit ? State.FAILED : State.OPEN_FAILED;
		this.onFailedCallback.accept(this.state);
	}

	private <T> T execute(CompletableFuture<T> future, int timeout) throws SQLException {
		try {
			return (timeout > 0) ? future.get(timeout, TimeUnit.SECONDS) : future.get();
		}
		catch (TimeoutException ignored) {
			fail(new Neo4jException(GQLError.$25N02.withMessage("The transaction is no longer valid")));
			throw new SQLTimeoutException("The query timeout has been exceeded");
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			fail(new Neo4jException(GQLError.$25N02.withMessage("The transaction is no longer valid")));
			throw new Neo4jException(Neo4jException.withInternal(ex, "The thread has been interrupted."));
		}
		catch (ExecutionException ex) {
			var cause = ex.getCause();
			if (cause == null) {
				cause = ex;
			}

			var sqlException = new Neo4jException(
					Neo4jException.withMessageAndCause("An error occurred while handling request", cause));

			if (cause instanceof BoltFailureException) {
				fail(new Neo4jException(GQLError.$25N02.withMessage("The transaction is no longer valid")));
			}
			else {
				fail(new Neo4jException(GQLError.$08000.withMessage("The connection is no longer valid")));
				this.fatalExceptionHandler.handle(this.exception, sqlException);
			}
			throw sqlException;
		}
	}

	private void appendDiscards(List<Message> messages) {
		for (var runResponse : this.openResults) {
			messages.add(Messages.discard(runResponse.queryId(), -1));
		}
	}

	private void assertNoException() throws SQLException {
		if (this.exception != null) {
			throw this.exception;
		}
	}

	private void assertRunnableState() throws SQLException {
		if (!isRunnable()) {
			throw new Neo4jException(Neo4jException.withReason(
					String.format("The requested action is not supported in %s transaction state", this.state)));
		}
	}

	private static RunAndPullResponses asRunAndPullResponses(BasicResponseHandler.Summaries summaries) {
		return new RunAndPullResponses(asRunResponse(summaries),
				asPullResponse(summaries.runSummary().keys(), summaries.valuesList(), summaries.pullSummary()));
	}

	private static RunResponse asRunResponse(BasicResponseHandler.Summaries summaries) {
		return new RunResponseImpl(summaries.runSummary().queryId(), summaries.runSummary().keys());
	}

	private static PullResponse asPullResponse(List<String> keys, List<List<Value>> valuesList,
			PullSummary pullSummary) {
		return new PullResponseImpl(pullSummary.hasMore(), valuesList.stream().map(v -> asRecord(keys, v)).toList(),
				asResultSummary(pullSummary.metadata()));
	}

	private static Record asRecord(List<String> keys, List<Value> values) {
		return Record.of(keys, values.stream().map(Values::value).toArray(org.neo4j.jdbc.values.Value[]::new));
	}

	private static ResultSummary asResultSummary(Map<String, Value> metadata) {
		return new ResultSummary(BoltAdapters.newSummaryCounters(metadata.get("stats")));
	}

	private static DiscardResponse asDiscardResponse(BasicResponseHandler.Summaries summaries) {
		return new DiscardResponseImpl(asResultSummary(summaries.discardSummary().metadata()));
	}

	@FunctionalInterface
	interface FatalExceptionHandler {

		/**
		 * Handles a fatal connection exception.
		 * @param fatalSqlException the fatal SQL connection exception.
		 * @param sqlException the SQL exception with the original cause.
		 */
		void handle(SQLException fatalSqlException, SQLException sqlException);

	}

	private record RunResponseImpl(long queryId, List<String> keys) implements RunResponse {
	}

	private record PullResponseImpl(boolean hasMore, List<Record> records,
			ResultSummary summary) implements PullResponse {
		@Override
		public Optional<ResultSummary> resultSummary() {
			return Optional.ofNullable(this.summary);
		}
	}

	record DiscardResponseImpl(ResultSummary summary) implements DiscardResponse {
		@Override
		public Optional<ResultSummary> resultSummary() {
			return Optional.ofNullable(this.summary);
		}
	}

}
