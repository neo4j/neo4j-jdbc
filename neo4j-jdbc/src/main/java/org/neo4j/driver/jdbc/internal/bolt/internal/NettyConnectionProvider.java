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

import java.net.InetSocketAddress;
import java.time.Clock;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.neo4j.driver.jdbc.internal.bolt.AuthToken;
import org.neo4j.driver.jdbc.internal.bolt.BoltAgent;
import org.neo4j.driver.jdbc.internal.bolt.BoltServerAddress;
import org.neo4j.driver.jdbc.internal.bolt.SecurityPlan;
import org.neo4j.driver.jdbc.internal.bolt.internal.connection.HandshakeCompletedListener;
import org.neo4j.driver.jdbc.internal.bolt.internal.connection.NettyChannelInitializer;
import org.neo4j.driver.jdbc.internal.bolt.internal.connection.inbound.ConnectTimeoutHandler;
import org.neo4j.driver.jdbc.internal.bolt.internal.connection.init.ChannelConnectedListener;
import org.neo4j.driver.jdbc.internal.bolt.internal.connection.init.ChannelPipelineBuilderImpl;

public final class NettyConnectionProvider implements ConnectionProvider {

	private final EventLoopGroup eventLoopGroup;

	private final Clock clock;

	public NettyConnectionProvider(EventLoopGroup eventLoopGroup, Clock clock) {
		this.eventLoopGroup = eventLoopGroup;
		this.clock = clock;
	}

	@Override
	public CompletionStage<Connection> acquireConnection(BoltServerAddress address, SecurityPlan securityPlan,
			String databaseName, AuthToken authToken, BoltAgent boltAgent, String userAgent, int connectTimeoutMillis) {
		var bootstrap = new Bootstrap();
		bootstrap.group(this.eventLoopGroup)
			.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMillis)
			.channel(NioSocketChannel.class)
			.remoteAddress(new InetSocketAddress(address.host(), address.port()))
			.handler(new NettyChannelInitializer(address, securityPlan, connectTimeoutMillis));

		var connectedFuture = bootstrap.connect();

		var channel = connectedFuture.channel();
		var handshakeCompleted = channel.newPromise();
		var connectionInitialized = channel.newPromise();

		installChannelConnectedListeners(address, connectedFuture, handshakeCompleted, connectTimeoutMillis);
		installHandshakeCompletedListeners(handshakeCompleted, connectionInitialized, address, authToken, boltAgent,
				userAgent);

		var future = new CompletableFuture<Connection>();
		connectionInitialized.addListener((ChannelFutureListener) f -> {
			var throwable = f.cause();
			if (throwable != null) {
				future.completeExceptionally(throwable);
			}
			else {
				var connection = new NetworkConnection(channel, this.clock, databaseName);
				future.complete(connection);
			}
		});
		return future;
	}

	private static void installChannelConnectedListeners(BoltServerAddress address, ChannelFuture channelConnected,
			ChannelPromise handshakeCompleted, int connectTimeoutMillis) {
		var pipeline = channelConnected.channel().pipeline();

		// add timeout handler to the pipeline when channel is connected. it's needed to
		// limit amount of time code
		// spends in TLS and Bolt handshakes. prevents infinite waiting when database does
		// not respond
		channelConnected.addListener(future -> pipeline.addFirst(new ConnectTimeoutHandler(connectTimeoutMillis)));

		// add listener that sends Bolt handshake bytes when channel is connected
		channelConnected
			.addListener(new ChannelConnectedListener(address, new ChannelPipelineBuilderImpl(), handshakeCompleted));
	}

	private void installHandshakeCompletedListeners(ChannelPromise handshakeCompleted,
			ChannelPromise connectionInitialized, BoltServerAddress address, AuthToken authToken, BoltAgent boltAgent,
			String userAgent) {
		var pipeline = handshakeCompleted.channel().pipeline();

		// remove timeout handler from the pipeline once TLS and Bolt handshakes are
		// completed. regular protocol
		// messages will flow next and we do not want to have read timeout for them
		handshakeCompleted.addListener(future -> {
			if (future.isSuccess()) {
				pipeline.remove(ConnectTimeoutHandler.class);
			}
		});

		// add listener that sends an INIT message. connection is now fully established.
		// channel pipeline is fully
		// set to send/receive messages for a selected protocol version
		handshakeCompleted.addListener(new HandshakeCompletedListener(address, userAgent, boltAgent, authToken,
				connectionInitialized, this.clock));
	}

}
