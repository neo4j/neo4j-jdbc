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
package org.neo4j.jdbc.internal.bolt;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import org.neo4j.jdbc.internal.bolt.response.CommitResponse;
import org.neo4j.jdbc.internal.bolt.response.DiscardResponse;
import org.neo4j.jdbc.internal.bolt.response.PullResponse;
import org.neo4j.jdbc.internal.bolt.response.RunResponse;

/**
 * A Bolt connection.
 *
 * @author Neo4j Drivers Team
 * @since 6.0.0
 */
public interface BoltConnection {

	/**
	 * Appends BEGIN message to outbound messages and optionally flushes the pending
	 * messages to the network.
	 * @param bookmarks the bookmarks, must not be {@code null}
	 * @param transactionMetadata metadata to be attached to the Neo4j transaction, must
	 * not be {@code null}
	 * @param accessMode the access mode, must not be {@code null}
	 * @param transactionType the transaction type, must not be {@code null}
	 * @param flush determines if the pending outbound messages must be flushed to the
	 * network
	 * @return a completion stage that completes when there is a response to the BEGIN
	 * message
	 */
	CompletionStage<Void> beginTransaction(Set<String> bookmarks, Map<String, Object> transactionMetadata,
			AccessMode accessMode, TransactionType transactionType, boolean flush);

	/**
	 * Appends RUN message to outbound messages and optionally flushes the pending
	 * messages to the network.
	 * @param query the query, must not be {@code null}
	 * @param parameters the query parameters, must not be {@code null}
	 * @param flush determines if the pending outbound messages must be flushed to the
	 * network
	 * @return a completion stage that completes when there is a response to the RUN
	 * message
	 */
	CompletionStage<RunResponse> run(String query, Map<String, Object> parameters, boolean flush);

	/**
	 * Appends PULL message to outbound messages and flushes the pending messages to the
	 * network.
	 * <p>
	 * The PULL message dispatched by this method does not include the 'qid'.
	 * @param runStage the RUN message stage that this PULL belongs to
	 * @param request the number of records to request, {@code -1} means all
	 * @return a completion stage that completes when there is a response to the PULL
	 * message
	 */
	CompletionStage<PullResponse> pull(CompletionStage<RunResponse> runStage, long request);

	/**
	 * Appends PULL message to outbound messages and flushes the pending messages to the
	 * network.
	 * <p>
	 * Unlike with the {@link #pull(CompletionStage, long)} method, the PULL message
	 * dispatched by this method includes the `qid` of a specific RUN.
	 * @param runResponse the run response that this PULL belongs to
	 * @param request the number of records to request, {@code -1} means all
	 * @return a completion stage that completes when there is a response to the PULL
	 * message
	 */
	CompletionStage<PullResponse> pull(RunResponse runResponse, long request);

	/**
	 * Appends DISCARD message to outbound messages and optionally flushes the pending
	 * messages to the network.
	 * @param number the number of records to discard, {@code -1} means all
	 * @param flush determines if the pending outbound messages must be flushed to the
	 * network
	 * @return a completion stage that completes when there is a response to the DISCARD
	 * message
	 */
	CompletionStage<DiscardResponse> discard(long number, boolean flush);

	/**
	 * Appends DISCARD message to outbound messages and optionally flushes the pending
	 * messages to the network.
	 * <p>
	 * Unlike with the {@link #discard(long, boolean)} method, the DISCARD message
	 * dispatched by this method includes the `qid` of a specific RUN.
	 * @param runResponse the run response that this DISCARD belongs to
	 * @param number the number of records to discard, {@code -1} means all
	 * @param flush determines if the pending outbound messages must be flushed to the
	 * network
	 * @return a completion stage that completes when there is a response to the DISCARD
	 * message
	 */
	CompletionStage<DiscardResponse> discard(RunResponse runResponse, long number, boolean flush);

	/**
	 * Appends COMMIT message to outbound messages and flushes the pending messages to the
	 * network.
	 * @return a completion stage that completes when there is a response to the COMMIT
	 * message
	 */
	CompletionStage<CommitResponse> commit();

	/**
	 * Appends ROLLBACK message to outbound messages and flushes the pending messages to
	 * the network.
	 * @return a completion stage that completes when there is a response to the ROLLBACK
	 * message
	 */
	CompletionStage<Void> rollback();

	/**
	 * Appends RESET message to outbound messages and optionally flushes the pending
	 * messages to the network.
	 * @param flush determines if the pending outbound messages must be flushed to the
	 * network
	 * @return a completion stage that completes when there is a response to the RESET
	 * message
	 */
	CompletionStage<Void> reset(boolean flush);

	/**
	 * Appends GOODBYE message to outbound messages, flushes the pending messages to the
	 * network and closes the connection.
	 * @return a completion stage that completes when the connection is closed
	 */
	CompletionStage<Void> close();

	/**
	 * Gets the Database name that was passed to the driver on creation.
	 * @return currently connected DB name
	 */
	String getDatabaseName();

	Optional<Long> defaultReadTimeoutMillis();

	void setReadTimeoutMillis(Long timeout);

}
