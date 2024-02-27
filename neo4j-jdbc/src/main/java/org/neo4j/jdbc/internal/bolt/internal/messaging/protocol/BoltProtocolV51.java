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
package org.neo4j.jdbc.internal.bolt.internal.messaging.protocol;

import java.time.Clock;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import io.netty.channel.ChannelPromise;
import org.neo4j.jdbc.internal.bolt.AccessMode;
import org.neo4j.jdbc.internal.bolt.BoltAgent;
import org.neo4j.jdbc.internal.bolt.BoltServerAddress;
import org.neo4j.jdbc.internal.bolt.TransactionType;
import org.neo4j.jdbc.internal.bolt.internal.BoltProtocol;
import org.neo4j.jdbc.internal.bolt.internal.BoltProtocolVersion;
import org.neo4j.jdbc.internal.bolt.internal.Connection;
import org.neo4j.jdbc.internal.bolt.internal.connection.ChannelAttributes;
import org.neo4j.jdbc.internal.bolt.internal.handler.BasicPullResponseHandler;
import org.neo4j.jdbc.internal.bolt.internal.handler.BeginTxResponseHandler;
import org.neo4j.jdbc.internal.bolt.internal.handler.CommitTxResponseHandler;
import org.neo4j.jdbc.internal.bolt.internal.handler.DiscardResponseHandler;
import org.neo4j.jdbc.internal.bolt.internal.handler.GoodbyeResponseHandler;
import org.neo4j.jdbc.internal.bolt.internal.handler.HelloResponseHandler;
import org.neo4j.jdbc.internal.bolt.internal.handler.LogonResponseHandler;
import org.neo4j.jdbc.internal.bolt.internal.handler.ResetResponseHandler;
import org.neo4j.jdbc.internal.bolt.internal.handler.RollbackTxResponseHandler;
import org.neo4j.jdbc.internal.bolt.internal.handler.RunResponseHandler;
import org.neo4j.jdbc.internal.bolt.internal.messaging.MessageFormat;
import org.neo4j.jdbc.internal.bolt.internal.messaging.request.BeginMessage;
import org.neo4j.jdbc.internal.bolt.internal.messaging.request.CommitMessage;
import org.neo4j.jdbc.internal.bolt.internal.messaging.request.DiscardMessage;
import org.neo4j.jdbc.internal.bolt.internal.messaging.request.GoodbyeMessage;
import org.neo4j.jdbc.internal.bolt.internal.messaging.request.HelloMessage;
import org.neo4j.jdbc.internal.bolt.internal.messaging.request.LogonMessage;
import org.neo4j.jdbc.internal.bolt.internal.messaging.request.PullMessage;
import org.neo4j.jdbc.internal.bolt.internal.messaging.request.ResetMessage;
import org.neo4j.jdbc.internal.bolt.internal.messaging.request.RollbackMessage;
import org.neo4j.jdbc.internal.bolt.internal.messaging.request.RunWithMetadataMessage;
import org.neo4j.jdbc.internal.bolt.response.CommitResponse;
import org.neo4j.jdbc.internal.bolt.response.DiscardResponse;
import org.neo4j.jdbc.internal.bolt.response.PullResponse;
import org.neo4j.jdbc.internal.bolt.response.RunResponse;
import org.neo4j.jdbc.values.Value;
import org.neo4j.jdbc.values.Values;

public final class BoltProtocolV51 implements BoltProtocol {

	public static final BoltProtocolVersion VERSION = new BoltProtocolVersion(5, 1);

	public static final BoltProtocol INSTANCE = new BoltProtocolV51();

	@Override
	public MessageFormat createMessageFormat() {
		return new MessageFormatV51();
	}

	@Override
	public void initializeChannel(BoltServerAddress address, String userAgent, BoltAgent boltAgent,
			Map<String, Value> authToken, ChannelPromise channelInitializedPromise, Clock clock) {
		var channel = channelInitializedPromise.channel();

		var helloMessage = new HelloMessage(address, userAgent, boltAgent, Collections.emptyMap());
		var helloFuture = new CompletableFuture<Void>();
		ChannelAttributes.messageDispatcher(channel).enqueue(new HelloResponseHandler(channel, helloFuture));
		channel.write(helloMessage, channel.voidPromise());

		var logonMessage = new LogonMessage(authToken);
		var logonFuture = new CompletableFuture<Void>();
		ChannelAttributes.messageDispatcher(channel).enqueue(new LogonResponseHandler(logonFuture, channel));
		channel.writeAndFlush(logonMessage, channel.voidPromise());

		helloFuture.thenCompose(ignored -> logonFuture).whenComplete((ignored, throwable) -> {
			if (throwable != null) {
				channelInitializedPromise.setFailure(throwable);
			}
			else {
				channelInitializedPromise.setSuccess();
			}
		});
	}

	@Override
	public CompletionStage<Void> beginTransaction(Connection connection, Set<String> bookmarks, AccessMode accessMode,
			TransactionType transactionType, boolean flush) {
		var beginFuture = new CompletableFuture<Void>();
		var beginMessage = new BeginMessage(bookmarks, connection.databaseName(), accessMode, transactionType);
		var beginHandler = new BeginTxResponseHandler(beginFuture);
		connection.write(beginMessage, beginHandler, flush);
		return beginFuture;
	}

	@Override
	public CompletionStage<RunResponse> run(Connection connection, String query, Map<String, Object> parameters,
			boolean flush) {
		var parameterToValue = parameters.entrySet()
			.stream()
			.collect(Collectors.toMap(Map.Entry::getKey, entry -> Values.value(entry.getValue())));
		var runMessage = new RunWithMetadataMessage(query, parameterToValue);
		var runFuture = new CompletableFuture<RunResponse>();
		var runHandler = new RunResponseHandler(runFuture);
		connection.write(runMessage, runHandler, flush);
		return runFuture;
	}

	@Override
	public CompletionStage<PullResponse> pull(Connection connection, CompletionStage<RunResponse> runStage, long qid,
			long request) {
		var pullMessage = new PullMessage(request, qid);
		var pullFuture = new CompletableFuture<PullResponse>();
		var pullHandler = new BasicPullResponseHandler(runStage, pullFuture);
		connection.write(pullMessage, pullHandler, true);
		return pullFuture;
	}

	@Override
	public CompletionStage<DiscardResponse> discard(Connection connection, long qid, long number, boolean flush) {
		var discardMessage = new DiscardMessage(number, qid);
		var discardFuture = new CompletableFuture<DiscardResponse>();
		var discardHandler = new DiscardResponseHandler(discardFuture);
		connection.write(discardMessage, discardHandler, flush);
		return discardFuture;
	}

	@Override
	public CompletionStage<CommitResponse> commit(Connection connection) {
		var commitFuture = new CompletableFuture<CommitResponse>();
		var commitHandler = new CommitTxResponseHandler(commitFuture);
		connection.write(CommitMessage.COMMIT, commitHandler, true);
		return commitFuture;
	}

	@Override
	public CompletionStage<Void> rollback(Connection connection) {
		var rollbackFuture = new CompletableFuture<Void>();
		var rollbackHandler = new RollbackTxResponseHandler(rollbackFuture);
		connection.write(RollbackMessage.ROLLBACK, rollbackHandler, true);
		return rollbackFuture;
	}

	@Override
	public CompletionStage<Void> reset(Connection connection, boolean flush) {
		var resetFuture = new CompletableFuture<Void>();
		var resetHandler = new ResetResponseHandler(resetFuture);
		connection.write(ResetMessage.RESET, resetHandler, flush);
		return resetFuture;
	}

	@Override
	public CompletionStage<Void> close(Connection connection) {
		var goodbyeHandler = new GoodbyeResponseHandler();
		connection.write(GoodbyeMessage.GOODBYE, goodbyeHandler, true);
		return connection.close();
	}

}
