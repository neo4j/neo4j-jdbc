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
package org.neo4j.driver.jdbc.internal.bolt.internal.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.neo4j.driver.jdbc.internal.bolt.internal.BoltProtocolVersion;
import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.protocol.BoltProtocolV51;

public final class BoltProtocolUtil {

	public static final int BOLT_MAGIC_PREAMBLE = 0x6060B017;

	public static final BoltProtocolVersion NO_PROTOCOL_VERSION = new BoltProtocolVersion(0, 0);

	public static final int CHUNK_HEADER_SIZE_BYTES = 2;

	public static final int DEFAULT_MAX_OUTBOUND_CHUNK_SIZE_BYTES = Short.MAX_VALUE / 2;

	private static final ByteBuf HANDSHAKE_BUF = Unpooled
		.unreleasableBuffer(Unpooled.copyInt(BOLT_MAGIC_PREAMBLE, BoltProtocolV51.VERSION.toInt(), 0, 0, 0))
		.asReadOnly();

	private static final String HANDSHAKE_STRING = createHandshakeString();

	private BoltProtocolUtil() {
	}

	public static ByteBuf handshakeBuf() {
		return HANDSHAKE_BUF.duplicate();
	}

	public static String handshakeString() {
		return HANDSHAKE_STRING;
	}

	public static void writeMessageBoundary(ByteBuf buf) {
		buf.writeShort(0);
	}

	public static void writeEmptyChunkHeader(ByteBuf buf) {
		buf.writeShort(0);
	}

	public static void writeChunkHeader(ByteBuf buf, int chunkStartIndex, int headerValue) {
		buf.setShort(chunkStartIndex, headerValue);
	}

	private static String createHandshakeString() {
		var buf = handshakeBuf();
		return String.format("[0x%s, %s, %s, %s, %s]", Integer.toHexString(buf.readInt()), buf.readInt(), buf.readInt(),
				buf.readInt(), buf.readInt());
	}

}
