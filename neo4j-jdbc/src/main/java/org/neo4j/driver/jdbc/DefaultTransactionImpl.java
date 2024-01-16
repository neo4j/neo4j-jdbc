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

import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.neo4j.driver.jdbc.internal.bolt.BoltConnection;
import org.neo4j.driver.jdbc.internal.bolt.exception.MessageIgnoredException;
import org.neo4j.driver.jdbc.internal.bolt.exception.Neo4jException;
import org.neo4j.driver.jdbc.internal.bolt.response.DiscardResponse;
import org.neo4j.driver.jdbc.internal.bolt.response.PullResponse;
import org.neo4j.driver.jdbc.internal.bolt.response.RunResponse;

final class DefaultTransactionImpl implements Neo4jTransaction {

	private final BoltConnection boltConnection;

	private final Consumer<SQLException> fatalConnectionExceptionConsumer;

	private final CompletionStage<Void> beginStage;

	private final boolean autoCommit;

	private State state;

	private SQLException exception;

	DefaultTransactionImpl(BoltConnection boltConnection, Consumer<SQLException> fatalConnectionExceptionConsumer,
			CompletionStage<Void> beginStage, boolean autoCommit) {
		this(boltConnection, fatalConnectionExceptionConsumer, beginStage, autoCommit, State.NEW);
	}

	DefaultTransactionImpl(BoltConnection boltConnection, Consumer<SQLException> fatalConnectionExceptionConsumer,
			CompletionStage<Void> beginStage, boolean autoCommit, State state) {
		this.boltConnection = Objects.requireNonNull(boltConnection);
		this.fatalConnectionExceptionConsumer = Objects.requireNonNull(fatalConnectionExceptionConsumer);
		this.beginStage = Objects.requireNonNull(beginStage);
		this.autoCommit = autoCommit;
		this.state = state;
	}

	@Override
	public RunAndPullResponses runAndPull(String query, Map<String, Object> parameters, int fetchSize, int timeout)
			throws SQLException {
		assertNoException();
		assertRunnableState();
		var beginFuture = this.beginStage.toCompletableFuture();
		var runFuture = this.boltConnection.run(query, parameters, false).toCompletableFuture();
		var pullFuture = this.boltConnection.pull(runFuture, fetchSize).toCompletableFuture();
		var responsesFuture = CompletableFuture.allOf(beginFuture, runFuture)
			.thenCompose(ignored -> pullFuture)
			.thenApply(pullResponse -> new RunAndPullResponses(runFuture.join(), pullResponse));
		var responses = execute(responsesFuture, timeout);
		this.state = State.READY;
		return responses;
	}

	@Override
	public DiscardResponse runAndDiscard(String query, Map<String, Object> parameters, int timeout, boolean commit)
			throws SQLException {
		assertNoException();
		assertRunnableState();
		var beginFuture = this.beginStage.toCompletableFuture();
		var runFuture = this.boltConnection.run(query, parameters, false).toCompletableFuture();
		var discardFuture = this.boltConnection.discard(-1, !commit).toCompletableFuture();
		var commitFuture = commit ? this.boltConnection.commit().toCompletableFuture()
				: CompletableFuture.completedFuture(null);
		var responseFuture = CompletableFuture.allOf(beginFuture, runFuture, discardFuture, commitFuture)
			.thenCompose(ignored -> discardFuture);
		var response = execute(responseFuture, timeout);
		this.state = commit ? State.COMMITTED : State.READY;
		return response;
	}

	@Override
	public PullResponse pull(RunResponse runResponse, long request) throws SQLException {
		assertNoException();
		if (State.READY != this.state) {
			throw new SQLException(
					String.format("The requested action is not supported in %s transaction state.", this.state));
		}
		var responseFuture = this.boltConnection.pull(runResponse, request).toCompletableFuture();
		var pullResponse = execute(responseFuture, 0);
		this.state = State.READY;
		return pullResponse;
	}

	@Override
	public void commit() throws SQLException {
		assertNoException();
		assertRunnableState();
		var beginFuture = this.beginStage.toCompletableFuture();
		var commitFuture = this.boltConnection.commit().toCompletableFuture();
		execute(CompletableFuture.allOf(beginFuture, commitFuture), 0);
		this.state = State.COMMITTED;
	}

	@Override
	public void rollback() throws SQLException {
		if (State.OPEN_FAILED.equals(this.state)) {
			this.state = State.FAILED;
			return;
		}
		assertNoException();
		assertRunnableState();
		var beginFuture = this.beginStage.toCompletableFuture();
		var rollbackFuture = this.boltConnection.rollback().toCompletableFuture();
		execute(CompletableFuture.allOf(beginFuture, rollbackFuture), 0);
		this.state = State.ROLLEDBACK;
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
		fail(exception, false);
	}

	private void fail(SQLException exception, boolean notifyUpstream) {
		this.exception = exception;
		this.state = this.autoCommit ? State.FAILED : State.OPEN_FAILED;
		if (notifyUpstream) {
			this.fatalConnectionExceptionConsumer.accept(exception);
		}
	}

	private <T> T execute(CompletableFuture<T> future, int timeout) throws SQLException {
		try {
			return (timeout > 0) ? future.get(timeout, TimeUnit.SECONDS) : future.get();
		}
		catch (TimeoutException ignored) {
			fail(new SQLException("The transaction is no longer valid."));
			throw new SQLTimeoutException("The query timeout has been exceeded.");
		}
		catch (InterruptedException ex) {
			fail(new SQLException("The transaction is no longer valid."));
			throw new SQLException("The thread has been interrupted.", ex);
		}
		catch (ExecutionException ex) {
			var cause = ex.getCause();
			if (cause instanceof Neo4jException || cause instanceof MessageIgnoredException) {
				fail(new SQLException("The transaction is no longer valid."));
			}
			else {
				fail(new SQLException("The connection is no longer valid."), true);
			}
			throw new SQLException("An error occured while handling request.", ex);
		}
	}

	private void assertNoException() throws SQLException {
		if (this.exception != null) {
			throw this.exception;
		}
	}

	private void assertRunnableState() throws SQLException {
		if (!isRunnable()) {
			throw new SQLException(
					String.format("The requested action is not supported in %s transaction state.", this.state));
		}
	}

	private record QueryResponses(RunResponse runResponse, PullResponse pullResponse, DiscardResponse discardResponse) {
	}

}
