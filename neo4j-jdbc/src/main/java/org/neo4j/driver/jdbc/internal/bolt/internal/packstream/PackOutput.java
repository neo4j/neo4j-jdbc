/*
 * Copyright (c) 2023 "Neo4j,"
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
package org.neo4j.driver.jdbc.internal.bolt.internal.packstream;

import java.io.IOException;

/**
 * This is where {@link PackStream} writes its output to.
 *
 * @author Neo4j Drivers Team
 * @since 1.0.0
 */
public interface PackOutput {

	/**
	 * Produce a single byte.
	 * @param value the value
	 * @return this output
	 */
	PackOutput writeByte(byte value) throws IOException;

	/**
	 * Produce binary data.
	 * @param data the data
	 * @return this output
	 */
	@SuppressWarnings("UnusedReturnValue")
	PackOutput writeBytes(byte[] data) throws IOException;

	/**
	 * Produce a 4-byte signed integer.
	 * @param value the value
	 * @return this output
	 */
	PackOutput writeShort(short value) throws IOException;

	/**
	 * Produce a 4-byte signed integer.
	 * @param value the value
	 * @return this output
	 */
	@SuppressWarnings("UnusedReturnValue")
	PackOutput writeInt(int value) throws IOException;

	/**
	 * Produce an 8-byte signed integer.
	 * @param value the value
	 * @return this output
	 */
	@SuppressWarnings("UnusedReturnValue")
	PackOutput writeLong(long value) throws IOException;

	/**
	 * Produce an 8-byte IEEE 754 "double format" floating-point number.
	 * @param value the value
	 * @return this output
	 */
	@SuppressWarnings("UnusedReturnValue")
	PackOutput writeDouble(double value) throws IOException;

}
