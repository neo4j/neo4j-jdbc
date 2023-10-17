/*
 * Copyright (c) 2023 "Neo4j,"
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
package org.neo4j.driver.jdbc.internal.bolt.internal;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.neo4j.driver.jdbc.internal.bolt.AccessMode;
import org.neo4j.driver.jdbc.internal.bolt.BoltConnection;
import org.neo4j.driver.jdbc.internal.bolt.internal.util.MetadataExtractor;
import org.neo4j.driver.jdbc.internal.bolt.response.CommitResponse;
import org.neo4j.driver.jdbc.internal.bolt.response.DiscardResponse;
import org.neo4j.driver.jdbc.internal.bolt.response.PullResponse;
import org.neo4j.driver.jdbc.internal.bolt.response.RunResponse;

public final class BoltConnectionImpl implements BoltConnection {

	private final BoltProtocol protocol;

	private final Connection connection;

	public BoltConnectionImpl(BoltProtocol protocol, Connection connection) {
		this.protocol = Objects.requireNonNull(protocol);
		this.connection = Objects.requireNonNull(connection);
	}

	@Override
	public CompletionStage<Void> beginTransaction(Set<String> bookmarks, AccessMode accessMode,
			TransactionType transactionType, boolean flush) {
		Objects.requireNonNull(bookmarks);
		Objects.requireNonNull(accessMode);
		Objects.requireNonNull(transactionType);
		return this.protocol.beginTransaction(this.connection, bookmarks, accessMode, transactionType, flush);
	}

	@Override
	public CompletionStage<RunResponse> run(String query, Map<String, Object> parameters, boolean flush) {
		Objects.requireNonNull(query);
		Objects.requireNonNull(parameters);
		return this.protocol.run(this.connection, query, parameters, flush);
	}

	@Override
	public CompletionStage<PullResponse> pull(CompletionStage<RunResponse> runStage, long request) {
		Objects.requireNonNull(runStage);
		return this.protocol.pull(this.connection, runStage, MetadataExtractor.ABSENT_QUERY_ID, request);
	}

	@Override
	public CompletionStage<PullResponse> pull(RunResponse runResponse, long request) {
		Objects.requireNonNull(runResponse);
		return this.protocol.pull(this.connection, CompletableFuture.completedStage(runResponse), runResponse.queryId(),
				request);
	}

	@Override
	public CompletionStage<DiscardResponse> discard(long number, boolean flush) {
		return this.protocol.discard(this.connection, MetadataExtractor.ABSENT_QUERY_ID, number, flush);
	}

	@Override
	public CompletionStage<DiscardResponse> discard(RunResponse runResponse, long number, boolean flush) {
		return this.protocol.discard(this.connection, runResponse.queryId(), number, flush);
	}

	@Override
	public CompletionStage<CommitResponse> commit() {
		return this.protocol.commit(this.connection);
	}

	@Override
	public CompletionStage<Void> rollback() {
		return this.protocol.rollback(this.connection);
	}

	@Override
	public CompletionStage<Void> reset(boolean flush) {
		return this.protocol.reset(this.connection, flush);
	}

	@Override
	public CompletionStage<Void> close() {
		return this.protocol.close(this.connection);
	}

}
