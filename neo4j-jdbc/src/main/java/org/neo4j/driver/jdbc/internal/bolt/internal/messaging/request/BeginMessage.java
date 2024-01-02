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
package org.neo4j.driver.jdbc.internal.bolt.internal.messaging.request;

import java.util.Objects;
import java.util.Set;

import org.neo4j.driver.jdbc.internal.bolt.AccessMode;
import org.neo4j.driver.jdbc.internal.bolt.TransactionType;

public final class BeginMessage extends MessageWithMetadata {

	public static final byte SIGNATURE = 0x11;

	public BeginMessage(Set<String> bookmarks, String databaseName, AccessMode mode, TransactionType transactionType) {
		super(TransactionMetadataBuilder.buildMetadata(databaseName, mode, transactionType, bookmarks));
	}

	@Override
	public byte signature() {
		return SIGNATURE;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		var that = (BeginMessage) o;
		return Objects.equals(metadata(), that.metadata());
	}

	@Override
	public int hashCode() {
		return Objects.hash(metadata());
	}

	@Override
	public String toString() {
		return "BEGIN " + metadata();
	}

}
