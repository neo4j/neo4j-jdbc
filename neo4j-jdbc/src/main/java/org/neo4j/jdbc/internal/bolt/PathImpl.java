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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.neo4j.jdbc.values.AsValue;
import org.neo4j.jdbc.values.Entity;
import org.neo4j.jdbc.values.Node;
import org.neo4j.jdbc.values.Path;
import org.neo4j.jdbc.values.Relationship;
import org.neo4j.jdbc.values.Value;
import org.neo4j.jdbc.values.Values;

final class PathImpl implements Path, AsValue, org.neo4j.bolt.connection.values.Path {

	@SuppressWarnings("deprecation")
	private static boolean isEndpoint(Node node, Relationship relationship) {
		return node.elementId().equals(relationship.startNodeElementId())
				|| node.elementId().equals(relationship.endNodeElementId());
	}

	private final List<Node> nodes;

	private final List<Relationship> relationships;

	private final List<Segment> segments;

	PathImpl(List<Entity> alternatingNodeAndRel) {
		this.nodes = newList(alternatingNodeAndRel.size() / 2 + 1);
		this.relationships = newList(alternatingNodeAndRel.size() / 2);
		this.segments = newList(alternatingNodeAndRel.size() / 2);

		if (alternatingNodeAndRel.size() % 2 == 0) {
			throw new IllegalArgumentException("An odd number of entities are required to build a path");
		}
		Node lastNode = null;
		Relationship lastRelationship = null;
		var index = 0;
		for (var entity : alternatingNodeAndRel) {
			if (entity == null) {
				throw new IllegalArgumentException("Path entities cannot be null");
			}
			if (index % 2 == 0) {
				// even index - this should be a node
				try {
					lastNode = (Node) entity;
					if (this.nodes.isEmpty() || (lastRelationship != null && isEndpoint(lastNode, lastRelationship))) {
						this.nodes.add(lastNode);
					}
					else {
						throw new IllegalArgumentException("Node argument " + index
								+ " is not an endpoint of relationship argument " + (index - 1));
					}
				}
				catch (ClassCastException ex) {
					var cls = entity.getClass().getName();
					throw new IllegalArgumentException("Expected argument " + index + " to be a node " + index
							+ " but found a " + cls + " " + "instead");
				}
			}
			else {
				// odd index - this should be a relationship
				try {
					lastRelationship = (Relationship) entity;
					if (isEndpoint(lastNode, lastRelationship)) {
						this.relationships.add(lastRelationship);
					}
					else {
						throw new IllegalArgumentException("Node argument " + (index - 1)
								+ " is not an endpoint of relationship argument " + index);
					}
				}
				catch (ClassCastException ex) {
					var cls = entity.getClass().getName();
					throw new IllegalArgumentException(
							"Expected argument " + index + " to be a relationship but found a " + cls + " instead");
				}
			}
			index += 1;
		}
		buildSegments();
	}

	PathImpl(Entity... alternatingNodeAndRel) {
		this(Arrays.asList(alternatingNodeAndRel));
	}

	PathImpl(List<Segment> segments, List<Node> nodes, List<Relationship> relationships) {
		this.segments = segments;
		this.nodes = nodes;
		this.relationships = relationships;
	}

	private <T> List<T> newList(int size) {
		return (size == 0) ? Collections.emptyList() : new ArrayList<>(size);
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

	private void buildSegments() {
		for (var i = 0; i < this.relationships.size(); i++) {
			this.segments
				.add(new SelfContainedSegment(this.nodes.get(i), this.relationships.get(i), this.nodes.get(i + 1)));
		}
	}

	record SelfContainedSegment(Node start, Relationship relationship,
			Node end) implements Segment, org.neo4j.bolt.connection.values.Segment {

		@Override
		public int hashCode() {
			return Objects.hash(this.start, this.relationship, this.end);
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (other == null || getClass() != other.getClass()) {
				return false;
			}

			var that = (SelfContainedSegment) other;
			return this.start.equals(that.start) && this.end.equals(that.end)
					&& this.relationship.equals(that.relationship);
		}

		@Override
		@SuppressWarnings("deprecation")
		public String toString() {
			return String.format("(%s)-[%s:%s]->(%s)", this.start.id(), this.relationship.id(),
					this.relationship.type(), this.end.id());
		}
	}

}
