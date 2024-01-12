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
package org.neo4j.driver.jdbc.internal.bolt.internal.connection;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLHandshakeException;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.ReplayingDecoder;
import org.neo4j.driver.jdbc.internal.bolt.BoltMessageExchange;
import org.neo4j.driver.jdbc.internal.bolt.exception.BoltException;
import org.neo4j.driver.jdbc.internal.bolt.exception.Neo4jException;
import org.neo4j.driver.jdbc.internal.bolt.internal.BoltProtocol;
import org.neo4j.driver.jdbc.internal.bolt.internal.BoltProtocolVersion;
import org.neo4j.driver.jdbc.internal.bolt.internal.connection.init.ChannelPipelineBuilder;
import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.MessageFormat;
import org.neo4j.driver.jdbc.internal.bolt.internal.util.BoltProtocolUtil;
import org.neo4j.driver.jdbc.internal.bolt.internal.util.ErrorUtil;

public final class HandshakeHandler extends ReplayingDecoder<Void> {

	private static final Logger LOGGER = Logger.getLogger(HandshakeHandler.class.getCanonicalName());

	private static final Logger boltLogger = Logger.getLogger(BoltMessageExchange.class.getCanonicalName());

	private final ChannelPipelineBuilder pipelineBuilder;

	private final ChannelPromise handshakeCompletedPromise;

	private boolean failed;

	public HandshakeHandler(ChannelPipelineBuilder pipelineBuilder, ChannelPromise handshakeCompletedPromise) {
		this.pipelineBuilder = pipelineBuilder;
		this.handshakeCompletedPromise = handshakeCompletedPromise;
	}

	@Override
	protected void handlerRemoved0(ChannelHandlerContext ctx) {
		this.failed = false;
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) {
		LOGGER.log(Level.FINE, "Channel is inactive");

		if (!this.failed) {
			// channel became inactive while doing bolt handshake, not because of some
			// previous error
			var error = ErrorUtil.newConnectionTerminatedError();
			fail(ctx, error);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable error) {
		if (this.failed) {
			LOGGER.log(Level.FINE, "Another fatal error occurred in the pipeline", error);
		}
		else {
			this.failed = true;
			var cause = transformError(error);
			fail(ctx, cause);
		}
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
		var serverSuggestedVersion = BoltProtocolVersion.fromRawBytes(in.readInt());
		boltLogger.log(Level.FINE, "S: [Bolt Handshake] {0}", serverSuggestedVersion);

		// this is a one-time handler, remove it when protocol version has been read
		ctx.pipeline().remove(this);

		var protocol = protocolForVersion(serverSuggestedVersion);
		if (protocol != null) {
			protocolSelected(serverSuggestedVersion, protocol.createMessageFormat(), ctx);
		}
		else {
			handleUnknownSuggestedProtocolVersion(serverSuggestedVersion, ctx);
		}
	}

	private BoltProtocol protocolForVersion(BoltProtocolVersion version) {
		try {
			return BoltProtocol.forVersion(version);
		}
		catch (Neo4jException ignored) {
			return null;
		}
	}

	private void protocolSelected(BoltProtocolVersion version, MessageFormat messageFormat, ChannelHandlerContext ctx) {
		ChannelAttributes.setProtocolVersion(ctx.channel(), version);
		this.pipelineBuilder.build(messageFormat, ctx.pipeline());
		this.handshakeCompletedPromise.setSuccess();
	}

	private void handleUnknownSuggestedProtocolVersion(BoltProtocolVersion version, ChannelHandlerContext ctx) {
		if (BoltProtocolUtil.NO_PROTOCOL_VERSION.equals(version)) {
			fail(ctx, protocolNoSupportedByServerError());
		}
		else if (BoltProtocolVersion.isHttp(version)) {
			fail(ctx, httpEndpointError());
		}
		else {
			fail(ctx, protocolNoSupportedByDriverError(version));
		}
	}

	private void fail(ChannelHandlerContext ctx, Throwable error) {
		ctx.close().addListener(future -> this.handshakeCompletedPromise.tryFailure(error));
	}

	private static Throwable protocolNoSupportedByServerError() {
		return new BoltException("The server does not support any of the protocol versions supported by "
				+ "this driver. Ensure that you are using driver and server versions that "
				+ "are compatible with one another.");
	}

	private static Throwable httpEndpointError() {
		return new BoltException("Server responded HTTP. Make sure you are not trying to connect to the http endpoint "
				+ "(HTTP defaults to port 7474 whereas BOLT defaults to port 7687)");
	}

	private static Throwable protocolNoSupportedByDriverError(BoltProtocolVersion suggestedProtocolVersion) {
		return new BoltException(
				"Protocol error, server suggested unexpected protocol version: " + suggestedProtocolVersion);
	}

	private static Throwable transformError(Throwable error) {
		if (error instanceof DecoderException && error.getCause() != null) {
			// unwrap the DecoderException if it has a cause
			error = error.getCause();
		}

		if (error instanceof Neo4jException) {
			return error;
		}
		else if (error instanceof SSLHandshakeException) {
			return new BoltException("Failed to establish secured connection with the server", error);
		}
		else {
			return new BoltException("Failed to establish connection with the server", error);
		}
	}

}
