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
package org.neo4j.jdbc.internal.bolt.internal.connection.outbound;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.neo4j.jdbc.internal.bolt.BoltMessageExchange;
import org.neo4j.jdbc.internal.bolt.internal.messaging.Message;
import org.neo4j.jdbc.internal.bolt.internal.messaging.MessageFormat;
import org.neo4j.jdbc.internal.bolt.internal.util.BoltProtocolUtil;

public final class OutboundMessageHandler extends MessageToMessageEncoder<Message> {

	private static final Logger boltLogger = Logger.getLogger(BoltMessageExchange.class.getCanonicalName());

	public static final String NAME = OutboundMessageHandler.class.getSimpleName();

	private final ChunkAwareByteBufOutput output;

	private final MessageFormat messageFormat;

	private MessageFormat.Writer writer;

	public OutboundMessageHandler(MessageFormat messageFormat) {
		this.output = new ChunkAwareByteBufOutput();
		this.messageFormat = messageFormat;
		this.writer = messageFormat.newWriter(this.output);
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) {
	}

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) {
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, Message msg, List<Object> out) {
		boltLogger.log(Level.FINE, "C: {0}", msg);

		var messageBuf = ctx.alloc().ioBuffer();
		this.output.start(messageBuf);
		try {
			this.writer.write(msg);
			this.output.stop();
		}
		catch (Exception ex) {
			this.output.stop();
			// release buffer because it will not get added to the out list and no other
			// handler is going to handle it
			messageBuf.release();
			throw new EncoderException("Failed to write outbound message: " + msg, ex);
		}

		BoltProtocolUtil.writeMessageBoundary(messageBuf);
		out.add(messageBuf);
	}

}
