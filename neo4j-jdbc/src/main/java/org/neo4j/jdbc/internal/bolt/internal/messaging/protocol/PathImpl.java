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

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.neo4j.jdbc.values.Node;
import org.neo4j.jdbc.values.Path;
import org.neo4j.jdbc.values.Relationship;
import org.neo4j.jdbc.values.Value;
import org.neo4j.jdbc.values.Values;

final class PathImpl implements Path {

	private final List<Node> nodes;

	private final List<Relationship> relationships;

	private final List<Segment> segments;

	PathImpl(List<Segment> segments, List<Node> nodes, List<Relationship> relationships) {
		this.segments = segments;
		this.nodes = nodes;
		this.relationships = relationships;
	}

	@Override
	public int length() {
		return this.relationships.size();
	}

	@Override
	public boolean contains(Node node) {
		return this.nodes.contains(node);
	}

	@Override
	public boolean contains(Relationship relationship) {
		return this.relationships.contains(relationship);
	}

	@Override
	public Iterable<Node> nodes() {
		return this.nodes;
	}

	@Override
	public Iterable<Relationship> relationships() {
		return this.relationships;
	}

	@Override
	public Node start() {
		return this.nodes.get(0);
	}

	@Override
	public Node end() {
		return this.nodes.get(this.nodes.size() - 1);
	}

	@Override
	public Iterator<Segment> iterator() {
		return this.segments.iterator();
	}

	@Override
	public Value asValue() {
		return Values.value(this);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		var segments1 = (PathImpl) o;

		return this.segments.equals(segments1.segments);
	}

	@Override
	public int hashCode() {
		return this.segments.hashCode();
	}

	@Override
	public String toString() {

		return "path" + this.segments;
	}

	public record SelfContainedSegment(Node start, Relationship relationship, Node end) implements Segment {
		@Override
		@SuppressWarnings("deprecation")
		public String toString() {
			return String.format(
					Objects.equals(this.relationship.startNodeElementId(), this.start.elementId())
							? "(%s)-[%s:%s]->(%s)" : "(%s)<-[%s:%s]-(%s)",
					this.start.id(), this.relationship.id(), this.relationship.type(), this.end.id());
		}
	}

}
