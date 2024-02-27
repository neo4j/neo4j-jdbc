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
package org.neo4j.jdbc.internal.bolt.internal.handler;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import io.netty.channel.Channel;
import org.neo4j.jdbc.internal.bolt.internal.connection.ChannelAttributes;
import org.neo4j.jdbc.internal.bolt.internal.util.MetadataExtractor;
import org.neo4j.jdbc.values.Value;

public final class HelloResponseHandler implements ResponseHandler {

	private static final String CONNECTION_ID_METADATA_KEY = "connection_id";

	public static final String CONFIGURATION_HINTS_KEY = "hints";

	public static final String CONNECTION_RECEIVE_TIMEOUT_SECONDS_KEY = "connection.recv_timeout_seconds";

	private final Channel channel;

	private final CompletableFuture<Void> helloFuture;

	public HelloResponseHandler(Channel channel, CompletableFuture<Void> helloFuture) {
		this.channel = channel;
		this.helloFuture = helloFuture;
	}

	@Override
	public void onSuccess(Map<String, Value> metadata) {
		try {
			var serverAgent = MetadataExtractor.extractServer(metadata).asString();
			ChannelAttributes.setServerAgent(this.channel, serverAgent);

			var connectionId = extractConnectionId(metadata);
			ChannelAttributes.setConnectionId(this.channel, connectionId);

			processConfigurationHints(metadata);

			this.helloFuture.complete(null);
		}
		catch (Throwable error) {
			onFailure(error);
			throw error;
		}
	}

	@Override
	public void onFailure(Throwable error) {
		this.channel.close().addListener(future -> this.helloFuture.completeExceptionally(error));
	}

	@Override
	public void onRecord(Value[] fields) {
		throw new UnsupportedOperationException();
	}

	private void processConfigurationHints(Map<String, Value> metadata) {
		var configurationHints = metadata.get(CONFIGURATION_HINTS_KEY);
		if (configurationHints != null) {
			getFromSupplierOrEmptyOnException(
					() -> configurationHints.get(CONNECTION_RECEIVE_TIMEOUT_SECONDS_KEY).asLong())
				.ifPresent(timeout -> ChannelAttributes.setConnectionReadTimeout(this.channel, timeout));
		}
	}

	private static String extractConnectionId(Map<String, Value> metadata) {
		var value = metadata.get(CONNECTION_ID_METADATA_KEY);
		if (value == null || value.isNull()) {
			throw new IllegalStateException("Unable to extract " + CONNECTION_ID_METADATA_KEY
					+ " from a response to HELLO message. " + "Received metadata: " + metadata);
		}
		return value.asString();
	}

	private static <T> Optional<T> getFromSupplierOrEmptyOnException(Supplier<T> supplier) {
		try {
			return Optional.of(supplier.get());
		}
		catch (Exception ignored) {
			return Optional.empty();
		}
	}

}
