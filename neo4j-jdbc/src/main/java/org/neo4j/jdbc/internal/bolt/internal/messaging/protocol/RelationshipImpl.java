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
package org.neo4j.jdbc.internal.bolt.internal.messaging.protocol;

import java.util.Map;

import org.neo4j.jdbc.values.Relationship;
import org.neo4j.jdbc.values.Value;

@SuppressWarnings("squid:S2160") // Not overriding equals is fine here
final class RelationshipImpl extends AbstractEntity implements Relationship {

	private long start;

	private String startElementId;

	private long end;

	private String endElementId;

	private final String type;

	RelationshipImpl(long id, String elementId, long start, String startElementId, long end, String endElementId,
			String type, Map<String, Value> properties) {
		super(id, elementId, properties);
		this.start = start;
		this.startElementId = startElementId;
		this.end = end;
		this.endElementId = endElementId;
		this.type = type;
	}

	@Override
	public boolean hasType(String relationshipType) {
		return type().equals(relationshipType);
	}

	/**
	 * Modify the start/end identities of this relationship.
	 * @param start the start
	 * @param startElementId the start element id
	 * @param end the end
	 * @param endElementId the end element id
	 */
	void setStartAndEnd(long start, String startElementId, long end, String endElementId) {
		this.start = start;
		this.startElementId = startElementId;
		this.end = end;
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
