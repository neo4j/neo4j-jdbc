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
package org.neo4j.jdbc.internal.bolt.internal.connection.inbound;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public final class ChunkDecoder extends LengthFieldBasedFrameDecoder {

	private static final int MAX_FRAME_BODY_LENGTH = 0xFFFF;

	private static final int LENGTH_FIELD_OFFSET = 0;

	private static final int LENGTH_FIELD_LENGTH = 2;

	private static final int LENGTH_ADJUSTMENT = 0;

	private static final int INITIAL_BYTES_TO_STRIP = LENGTH_FIELD_LENGTH;

	private static final int MAX_FRAME_LENGTH = LENGTH_FIELD_LENGTH + MAX_FRAME_BODY_LENGTH;

	public ChunkDecoder() {
		super(MAX_FRAME_LENGTH, LENGTH_FIELD_OFFSET, LENGTH_FIELD_LENGTH, LENGTH_ADJUSTMENT, INITIAL_BYTES_TO_STRIP);
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) {
	}

	@Override
	protected void handlerRemoved0(ChannelHandlerContext ctx) {
	}

	@Override
	protected ByteBuf extractFrame(ChannelHandlerContext ctx, ByteBuf buffer, int index, int length) {
		return super.extractFrame(ctx, buffer, index, length);
	}

}
