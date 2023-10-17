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
package org.neo4j.driver.jdbc.internal.bolt.internal.messaging.v51;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.neo4j.driver.jdbc.internal.bolt.Value;
import org.neo4j.driver.jdbc.internal.bolt.internal.InternalNode;
import org.neo4j.driver.jdbc.internal.bolt.internal.InternalPath;
import org.neo4j.driver.jdbc.internal.bolt.internal.InternalRelationship;
import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.common.CommonValueUnpacker;
import org.neo4j.driver.jdbc.internal.bolt.internal.packstream.PackInput;
import org.neo4j.driver.jdbc.internal.bolt.internal.types.TypeConstructor;
import org.neo4j.driver.jdbc.internal.bolt.internal.util.Iterables;
import org.neo4j.driver.jdbc.internal.bolt.internal.value.PathValue;
import org.neo4j.driver.jdbc.internal.bolt.internal.value.RelationshipValue;
import org.neo4j.driver.jdbc.internal.bolt.types.Node;
import org.neo4j.driver.jdbc.internal.bolt.types.Path;
import org.neo4j.driver.jdbc.internal.bolt.types.Relationship;

public final class ValueUnpackerV51 extends CommonValueUnpacker {

	private static final int NODE_FIELDS = 4;

	private static final int RELATIONSHIP_FIELDS = 8;

	public ValueUnpackerV51(PackInput input) {
		super(input, true);
	}

	@Override
	protected int getNodeFields() {
		return NODE_FIELDS;
	}

	@Override
	protected int getRelationshipFields() {
		return RELATIONSHIP_FIELDS;
	}

	@Override
	protected InternalNode unpackNode() throws IOException {
		var urn = this.unpacker.unpackLong();

		var numLabels = (int) this.unpacker.unpackListHeader();
		List<String> labels = new ArrayList<>(numLabels);
		for (var i = 0; i < numLabels; i++) {
			labels.add(this.unpacker.unpackString());
		}
		var numProps = (int) this.unpacker.unpackMapHeader();
		Map<String, Value> props = Iterables.newHashMapWithSize(numProps);
		for (var j = 0; j < numProps; j++) {
			var key = this.unpacker.unpackString();
			props.put(key, unpack());
		}

		var elementId = this.unpacker.unpackString();

		return new InternalNode(urn, elementId, labels, props);
	}

	@Override
	protected Value unpackPath() throws IOException {
		// List of unique nodes
		var uniqNodes = new InternalNode[(int) this.unpacker.unpackListHeader()];
		for (var i = 0; i < uniqNodes.length; i++) {
			ensureCorrectStructSize(TypeConstructor.NODE, getNodeFields(), this.unpacker.unpackStructHeader());
			ensureCorrectStructSignature("NODE", NODE, this.unpacker.unpackStructSignature());
			uniqNodes[i] = unpackNode();
		}

		// List of unique relationships, without start/end information
		var uniqRels = new InternalRelationship[(int) this.unpacker.unpackListHeader()];
		for (var i = 0; i < uniqRels.length; i++) {
			ensureCorrectStructSize(TypeConstructor.RELATIONSHIP, 4, this.unpacker.unpackStructHeader());
			ensureCorrectStructSignature("UNBOUND_RELATIONSHIP", UNBOUND_RELATIONSHIP,
					this.unpacker.unpackStructSignature());
			var id = this.unpacker.unpackLong();
			var relType = this.unpacker.unpackString();
			var props = unpackMap();
			var elementId = this.unpacker.unpackString();
			uniqRels[i] = new InternalRelationship(id, elementId, -1, String.valueOf(-1), -1, String.valueOf(-1),
					relType, props);
		}

		// Path sequence
		var length = (int) this.unpacker.unpackListHeader();

		// Knowing the sequence length, we can create the arrays that will represent the
		// nodes, rels and segments in
		// their "path order"
		var segments = new Path.Segment[length / 2];
		var nodes = new Node[segments.length + 1];
		var rels = new Relationship[segments.length];

		var prevNode = uniqNodes[0];
		InternalNode nextNode; // Start node is always 0, and isn't encoded in the
								// sequence

		nodes[0] = prevNode;
		InternalRelationship rel;
		for (var i = 0; i < segments.length; i++) {
			var relIdx = (int) this.unpacker.unpackLong();
			nextNode = uniqNodes[(int) this.unpacker.unpackLong()];
			// Negative rel index means this rel was traversed "inversed" from its
			// direction
			if (relIdx < 0) {
				rel = uniqRels[(-relIdx) - 1]; // -1 because rel idx are 1-indexed
				setStartAndEnd(rel, nextNode, prevNode);
			}
			else {
				rel = uniqRels[relIdx - 1];
				setStartAndEnd(rel, prevNode, nextNode);
			}

			nodes[i + 1] = nextNode;
			rels[i] = rel;
			segments[i] = new InternalPath.SelfContainedSegment(prevNode, rel, nextNode);
			prevNode = nextNode;
		}
		return new PathValue(new InternalPath(Arrays.asList(segments), Arrays.asList(nodes), Arrays.asList(rels)));
	}

	@SuppressWarnings("deprecation")
	private void setStartAndEnd(InternalRelationship rel, InternalNode start, InternalNode end) {
		rel.setStartAndEnd(start.id(), start.elementId(), end.id(), end.elementId());
	}

	@Override
	protected Value unpackRelationship() throws IOException {
		var urn = this.unpacker.unpackLong();
		var startUrn = this.unpacker.unpackLong();
		var endUrn = this.unpacker.unpackLong();
		var relType = this.unpacker.unpackString();
		var props = unpackMap();
		var elementId = this.unpacker.unpackString();
		var startElementId = this.unpacker.unpackString();
		var endElementId = this.unpacker.unpackString();

		var adapted = new InternalRelationship(urn, elementId, startUrn, startElementId, endUrn, endElementId, relType,
				props);
		return new RelationshipValue(adapted);
	}

}
