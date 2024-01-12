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
package org.neo4j.driver.jdbc.internal.bolt.internal;

import java.time.Clock;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import org.neo4j.driver.jdbc.internal.bolt.internal.connection.ChannelAttributes;
import org.neo4j.driver.jdbc.internal.bolt.internal.connection.inbound.ConnectionReadTimeoutHandler;
import org.neo4j.driver.jdbc.internal.bolt.internal.connection.inbound.InboundMessageDispatcher;
import org.neo4j.driver.jdbc.internal.bolt.internal.handler.ResponseHandler;
import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.Message;

public final class NetworkConnection implements Connection {

	private static final Logger LOGGER = Logger.getLogger(NetworkConnection.class.getCanonicalName());

	private final Channel channel;

	private final InboundMessageDispatcher messageDispatcher;

	private final BoltProtocol protocol;

	private final Long connectionReadTimeout;

	private final String databaseName;

	private ChannelHandler connectionReadTimeoutHandler;

	public NetworkConnection(Channel channel, Clock clock, String databaseName) {
		this.channel = channel;
		this.messageDispatcher = ChannelAttributes.messageDispatcher(channel);
		this.protocol = BoltProtocol.forChannel(channel);
		this.connectionReadTimeout = ChannelAttributes.connectionReadTimeout(channel).orElse(null);
		this.databaseName = databaseName;
	}

	@Override
	public void write(Message message, ResponseHandler handler, boolean flush) {
		writeMessageInEventLoop(message, handler, flush);
	}

	@Override
	public BoltProtocol protocol() {
		return this.protocol;
	}

	@Override
	public String databaseName() {
		return this.databaseName;
	}

	@Override
	public CompletionStage<Void> close() {
		var closeFuture = new CompletableFuture<Void>();
		this.channel.close().addListener((ChannelFutureListener) future -> {
			if (future.isSuccess()) {
				closeFuture.complete(null);
			}
			else {
				closeFuture.completeExceptionally(future.cause());
			}
		});
		return closeFuture;
	}

	private void writeMessageInEventLoop(Message message, ResponseHandler handler, boolean flush) {
		this.channel.eventLoop().execute(() -> {
			this.messageDispatcher.enqueue(handler);
			if (flush) {
				this.channel.writeAndFlush(message).addListener(future -> registerConnectionReadTimeout(this.channel));
			}
			else {
				this.channel.write(message, this.channel.voidPromise());
			}
		});
	}

	private void registerConnectionReadTimeout(Channel channel) {
		if (!channel.eventLoop().inEventLoop()) {
			throw new IllegalStateException("This method may only be called in the EventLoop");
		}

		if (this.connectionReadTimeout != null && this.connectionReadTimeoutHandler == null) {
			this.connectionReadTimeoutHandler = new ConnectionReadTimeoutHandler(this.connectionReadTimeout,
					TimeUnit.SECONDS);
			channel.pipeline().addFirst(this.connectionReadTimeoutHandler);
			LOGGER.log(Level.FINE, "Added ConnectionReadTimeoutHandler");
			this.messageDispatcher.setBeforeLastHandlerHook((messageType) -> {
				channel.pipeline().remove(this.connectionReadTimeoutHandler);
				this.connectionReadTimeoutHandler = null;
				this.messageDispatcher.setBeforeLastHandlerHook(null);
				LOGGER.log(Level.FINE, "Removed ConnectionReadTimeoutHandler");
			});
		}
	}

}
