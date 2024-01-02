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

import java.util.Objects;

import io.netty.buffer.ByteBuf;
import org.neo4j.driver.jdbc.internal.bolt.internal.packstream.PackInput;

public final class ByteBufInput implements PackInput {

	private ByteBuf buf;

	public void start(ByteBuf newBuf) {
		assertNotStarted();
		this.buf = Objects.requireNonNull(newBuf);
	}

	public void stop() {
		this.buf = null;
	}

	@Override
	public byte readByte() {
		return this.buf.readByte();
	}

	@Override
	public short readShort() {
		return this.buf.readShort();
	}

	@Override
	public int readInt() {
		return this.buf.readInt();
	}

	@Override
	public long readLong() {
		return this.buf.readLong();
	}

	@Override
	public double readDouble() {
		return this.buf.readDouble();
	}

	@Override
	public void readBytes(byte[] into, int offset, int toRead) {
		this.buf.readBytes(into, offset, toRead);
	}

	@Override
	public byte peekByte() {
		return this.buf.getByte(this.buf.readerIndex());
	}

	private void assertNotStarted() {
		if (this.buf != null) {
			throw new IllegalStateException("Already started");
		}
	}

}
