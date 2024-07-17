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
package org.neo4j.jdbc.internal.bolt.internal;

import java.time.Clock;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPromise;
import org.neo4j.jdbc.internal.bolt.AccessMode;
import org.neo4j.jdbc.internal.bolt.BoltAgent;
import org.neo4j.jdbc.internal.bolt.BoltServerAddress;
import org.neo4j.jdbc.internal.bolt.TransactionType;
import org.neo4j.jdbc.internal.bolt.exception.BoltException;
import org.neo4j.jdbc.internal.bolt.exception.Neo4jException;
import org.neo4j.jdbc.internal.bolt.internal.connection.ChannelAttributes;
import org.neo4j.jdbc.internal.bolt.internal.messaging.MessageFormat;
import org.neo4j.jdbc.internal.bolt.internal.messaging.protocol.BoltProtocolV51;
import org.neo4j.jdbc.internal.bolt.response.CommitResponse;
import org.neo4j.jdbc.internal.bolt.response.DiscardResponse;
import org.neo4j.jdbc.internal.bolt.response.PullResponse;
import org.neo4j.jdbc.internal.bolt.response.RunResponse;
import org.neo4j.jdbc.values.Value;

public interface BoltProtocol {

	/**
	 * Instantiate {@link MessageFormat} used by this Bolt protocol verison.
	 * @return new message format.
	 */
	MessageFormat createMessageFormat();

	/**
	 * Initialize channel after it is connected and handshake selected this protocol
	 * version.
	 * @param address the server address for routing context
	 * @param userAgent the user agent string
	 * @param boltAgent the bolt agent
	 * @param authToken the auth token
	 * @param channelInitializedPromise the promise to be notified when initialization is
	 * completed
	 * @param clock the clock to use
	 */
	void initializeChannel(BoltServerAddress address, String userAgent, BoltAgent boltAgent,
			Map<String, Value> authToken, ChannelPromise channelInitializedPromise, Clock clock);

	CompletionStage<Void> beginTransaction(Connection connection, Set<String> bookmarks,
			Map<String, Object> transactionMetadata, AccessMode accessMode, TransactionType transactionType,
			boolean flush);

	CompletionStage<RunResponse> run(Connection connection, String query, Map<String, Object> parameters,
			boolean flush);

	CompletionStage<PullResponse> pull(Connection connection, CompletionStage<RunResponse> runStage, long qid,
			long request);

	CompletionStage<DiscardResponse> discard(Connection connection, long qid, long number, boolean flush);

	CompletionStage<CommitResponse> commit(Connection connection);

	CompletionStage<Void> rollback(Connection connection);

	CompletionStage<Void> reset(Connection connection, boolean flush);

	CompletionStage<Void> close(Connection connection);

	/**
	 * Obtain an instance of the protocol for the given channel.
	 * @param version the version of the protocol.
	 * @return the protocol.
	 * @throws Neo4jException when unable to find protocol with the given version.
	 */
	static BoltProtocol forVersion(BoltProtocolVersion version) {
		if (BoltProtocolV51.VERSION.equals(version)) {
			return BoltProtocolV51.INSTANCE;
		}
		throw new BoltException("Unknown protocol version: " + version);
	}

	static BoltProtocol forChannel(Channel channel) {
		return forVersion(ChannelAttributes.protocolVersion(channel));
	}

}
