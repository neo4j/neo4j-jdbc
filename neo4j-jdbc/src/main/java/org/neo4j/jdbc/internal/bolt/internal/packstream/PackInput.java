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
package org.neo4j.jdbc.internal.bolt.internal.packstream;

import java.io.IOException;

/**
 * This is what {@link PackStream} uses to ingest data, implement this on top of any data
 * source of your choice to deserialize the stream with {@link PackStream}.
 *
 * @author Neo4j Drivers Team
 * @since 6.0.0
 */
public interface PackInput {

	/**
	 * Consume one byte.
	 * @return the byte value
	 */
	byte readByte() throws IOException;

	/**
	 * Consume a 2-byte signed integer.
	 * @return the short value
	 */
	short readShort() throws IOException;

	/**
	 * Consume a 4-byte signed integer.
	 * @return the int value
	 */
	int readInt() throws IOException;

	/**
	 * Consume an 8-byte signed integer.
	 * @return the long value
	 */
	long readLong() throws IOException;

	/**
	 * Consume an 8-byte IEEE 754 "double format" floating-point number.
	 * @return the double value
	 */
	double readDouble() throws IOException;

	/**
	 * Consume a specified number of bytes.
	 * @param into the target
	 * @param offset the offset
	 * @param toRead the number
	 */
	void readBytes(byte[] into, int offset, int toRead) throws IOException;

	/**
	 * Get the next byte without forwarding the internal pointer.
	 * @return the byte value
	 */
	byte peekByte() throws IOException;

}
