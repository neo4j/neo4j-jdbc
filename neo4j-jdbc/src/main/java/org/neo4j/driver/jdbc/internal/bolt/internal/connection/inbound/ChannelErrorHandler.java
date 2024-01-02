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
package org.neo4j.driver.jdbc.internal.bolt.internal.connection.inbound;

import java.io.IOException;
import java.util.Objects;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.CodecException;
import org.neo4j.driver.jdbc.internal.bolt.exception.BoltException;
import org.neo4j.driver.jdbc.internal.bolt.internal.connection.ChannelAttributes;
import org.neo4j.driver.jdbc.internal.bolt.internal.util.ErrorUtil;

public final class ChannelErrorHandler extends ChannelInboundHandlerAdapter {

	private InboundMessageDispatcher messageDispatcher;

	private boolean failed;

	public ChannelErrorHandler() {
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) {
		this.messageDispatcher = Objects.requireNonNull(ChannelAttributes.messageDispatcher(ctx.channel()));
	}

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) {
		this.messageDispatcher = null;
		this.failed = false;
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) {
		var terminationReason = ChannelAttributes.terminationReason(ctx.channel());
		Throwable error = ErrorUtil.newConnectionTerminatedError(terminationReason);

		if (!this.failed) {
			// channel became inactive not because of a fatal exception that came from
			// exceptionCaught
			// it is most likely inactive because actual network connection broke or was
			// explicitly closed by the driver

			this.messageDispatcher.handleChannelInactive(error);
		}
		else {
			fail(error);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable error) {
		if (!this.failed) {
			this.failed = true;
			fail(error);
		}
	}

	private void fail(Throwable error) {
		var cause = transformError(error);
		this.messageDispatcher.handleChannelError(cause);
	}

	private static Throwable transformError(Throwable error) {
		if (error instanceof CodecException && error.getCause() != null) {
			// unwrap the CodecException if it has a cause
			error = error.getCause();
		}

		if (error instanceof IOException) {
			return new BoltException("Connection to the database failed", error);
		}
		else {
			return error;
		}
	}

}
