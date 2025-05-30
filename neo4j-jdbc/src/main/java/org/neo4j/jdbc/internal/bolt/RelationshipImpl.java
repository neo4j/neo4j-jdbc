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
package org.neo4j.jdbc.internal.bolt;

import java.util.Map;

import org.neo4j.jdbc.values.Relationship;
import org.neo4j.jdbc.values.Value;

@SuppressWarnings("squid:S2160") // Not overriding equals is fine here
final class RelationshipImpl extends AbstractEntity
		implements Relationship, org.neo4j.bolt.connection.values.Relationship {

	private String startElementId;

	private String endElementId;

	private final String type;

	RelationshipImpl(long id, String elementId, String startElementId, String endElementId, String type,
			Map<String, Value> properties) {
		super(id, elementId, properties);
		this.startElementId = startElementId;
		this.endElementId = endElementId;
		this.type = type;
	}

	@Override
	public boolean hasType(String relationshipType) {
		return type().equals(relationshipType);
	}

	@Override
	public void setStartAndEnd(long start, String startElementId, long end, String endElementId) {
		this.startElementId = startElementId;
		this.endElementId = endElementId;
	}

	@Override
	public String startNodeElementId() {
		return this.startElementId;
	}

	@Override
	public String endNodeElementId() {
		return this.endElementId;
	}

	@Override
	public String type() {
		return this.type;
	}

	@Override
	@SuppressWarnings("deprecation")
	public String toString() {
		return String.format("relationship<%s>", id());
	}

}
