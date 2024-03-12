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
package org.neo4j.jdbc.internal.bolt.internal.connection.inbound;

import java.util.Objects;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.DecoderException;
import org.neo4j.jdbc.internal.bolt.internal.connection.ChannelAttributes;
import org.neo4j.jdbc.internal.bolt.internal.messaging.MessageFormat;

public final class InboundMessageHandler extends SimpleChannelInboundHandler<ByteBuf> {

	private final ByteBufInput input;

	private final MessageFormat.Reader reader;

	private InboundMessageDispatcher messageDispatcher;

	public InboundMessageHandler(MessageFormat messageFormat) {
		this.input = new ByteBufInput();
		this.reader = messageFormat.newReader(this.input);
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) {
		var channel = ctx.channel();
		this.messageDispatcher = Objects.requireNonNull(ChannelAttributes.messageDispatcher(channel));
	}

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) {
		this.messageDispatcher = null;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
		if (this.messageDispatcher.fatalErrorOccurred()) {
			return;
		}

		this.input.start(msg);
		try {
			this.reader.read(this.messageDispatcher);
		}
		catch (Exception ex) {
			throw new DecoderException("Failed to read inbound message:\n" + ByteBufUtil.hexDump(msg) + "\n", ex);
		}
		finally {
			this.input.stop();
		}
	}

}
