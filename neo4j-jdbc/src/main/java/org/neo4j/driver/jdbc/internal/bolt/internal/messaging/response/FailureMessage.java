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
package org.neo4j.driver.jdbc.internal.bolt.internal.messaging.response;

import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.Message;

public record FailureMessage(String code, String message) implements Message {
	public static final byte SIGNATURE = 0x7F;

	@Override
	public byte signature() {
		return SIGNATURE;
	}

	@Override
	public String toString() {
		return String.format("FAILURE %s \"%s\"", this.code, this.message);
	}
}
