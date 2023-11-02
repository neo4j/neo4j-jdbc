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
package org.neo4j.driver.jdbc.internal.bolt.internal.messaging.request;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import org.neo4j.driver.jdbc.internal.bolt.values.Value;

public final class RunWithMetadataMessage extends MessageWithMetadata {

	public static final byte SIGNATURE = 0x10;

	private final String query;

	private final Map<String, Value> parameters;

	public RunWithMetadataMessage(String query, Map<String, Value> parameters) {
		super(Collections.emptyMap());
		this.query = query;
		this.parameters = parameters;
	}

	public String query() {
		return this.query;
	}

	public Map<String, Value> parameters() {
		return this.parameters;
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
		var that = (RunWithMetadataMessage) o;
		return Objects.equals(this.query, that.query) && Objects.equals(this.parameters, that.parameters)
				&& Objects.equals(metadata(), that.metadata());
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.query, this.parameters, metadata());
	}

	@Override
	public String toString() {
		return "RUN \"" + this.query + "\" " + this.parameters + " " + metadata();
	}

}
