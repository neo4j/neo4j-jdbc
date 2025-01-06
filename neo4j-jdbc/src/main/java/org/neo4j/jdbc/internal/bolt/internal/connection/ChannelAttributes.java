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
package org.neo4j.jdbc.internal.bolt.internal.connection;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import org.neo4j.jdbc.internal.bolt.internal.BoltProtocolVersion;
import org.neo4j.jdbc.internal.bolt.internal.connection.inbound.InboundMessageDispatcher;

public final class ChannelAttributes {

	private static final AttributeKey<String> CONNECTION_ID = AttributeKey.newInstance("connectionId");

	private static final AttributeKey<BoltProtocolVersion> PROTOCOL_VERSION = AttributeKey
		.newInstance("protocolVersion");

	private static final AttributeKey<CompletionStage<Void>> HELLO_STAGE = AttributeKey.newInstance("helloStage");

	private static final AttributeKey<String> SERVER_AGENT = AttributeKey.newInstance("serverAgent");

	private static final AttributeKey<InboundMessageDispatcher> MESSAGE_DISPATCHER = AttributeKey
		.newInstance("messageDispatcher");

	private static final AttributeKey<String> TERMINATION_REASON = AttributeKey.newInstance("terminationReason");

	// configuration hints provided by the server
	private static final AttributeKey<Long> CONNECTION_READ_TIMEOUT = AttributeKey.newInstance("connectionReadTimeout");

	public static BoltProtocolVersion protocolVersion(Channel channel) {
		return get(channel, PROTOCOL_VERSION);
	}

	public static void setProtocolVersion(Channel channel, BoltProtocolVersion version) {
		setOnce(channel, PROTOCOL_VERSION, version);
	}

	public static InboundMessageDispatcher messageDispatcher(Channel channel) {
		return get(channel, MESSAGE_DISPATCHER);
	}

	public static void setMessageDispatcher(Channel channel, InboundMessageDispatcher messageDispatcher) {
		setOnce(channel, MESSAGE_DISPATCHER, messageDispatcher);
	}

	public static String terminationReason(Channel channel) {
		return get(channel, TERMINATION_REASON);
	}

	public static void setTerminationReason(Channel channel, String reason) {
		setOnce(channel, TERMINATION_REASON, reason);
	}

	public static void setServerAgent(Channel channel, String serverAgent) {
		setOnce(channel, SERVER_AGENT, serverAgent);
	}

	public static Optional<Long> connectionReadTimeout(Channel channel) {
		return Optional.ofNullable(get(channel, CONNECTION_READ_TIMEOUT));
	}

	public static void setConnectionReadTimeout(Channel channel, Long connectionReadTimeout) {
		set(channel, CONNECTION_READ_TIMEOUT, connectionReadTimeout);
	}

	public static String connectionId(Channel channel) {
		return get(channel, CONNECTION_ID);
	}

	public static void setConnectionId(Channel channel, String id) {
		setOnce(channel, CONNECTION_ID, id);
	}

	public static String serverAgent(Channel channel) {
		return get(channel, SERVER_AGENT);
	}

	private static <T> T get(Channel channel, AttributeKey<T> key) {
		return channel.attr(key).get();
	}

	private static <T> void set(Channel channel, AttributeKey<T> key, T value) {
		channel.attr(key).set(value);
	}

	private static <T> void setOnce(Channel channel, AttributeKey<T> key, T value) {
		var existingValue = channel.attr(key).setIfAbsent(value);
		if (existingValue != null) {
			throw new IllegalStateException(
					"Unable to set " + key.name() + " because it is already set to " + existingValue);
		}
	}

	private ChannelAttributes() {
	}

}
