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
package org.neo4j.jdbc;

import java.lang.reflect.InvocationTargetException;
import java.time.DateTimeException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.driver.internal.bolt.api.values.Node;
import org.neo4j.driver.internal.bolt.api.values.Path;
import org.neo4j.driver.internal.bolt.api.values.Relationship;
import org.neo4j.driver.internal.bolt.api.values.Segment;
import org.neo4j.driver.internal.bolt.api.values.Value;
import org.neo4j.driver.internal.bolt.api.values.ValueFactory;
import org.neo4j.jdbc.values.UnsupportedDateTimeValue;
import org.neo4j.jdbc.values.Values;

final class BoltValueFactory implements ValueFactory {

	private static final BoltValueFactory INSTANCE = new BoltValueFactory();

	static BoltValueFactory getInstance() {
		return INSTANCE;
	}

	private BoltValueFactory() {
	}

	@Override
	public Value value(Object value) {
		return asBoltValue(Values.value(value));
	}

	@Override
	public Node node(long id, String elementId, Collection<String> labels, Map<String, Value> properties) {
		return new NodeImpl(id, elementId, labels, toDriverMap(properties));
	}

	@Override
	public Relationship relationship(long id, String elementId, long start, String startElementId, long end,
			String endElementId, String type, Map<String, Value> properties) {
		return new RelationshipImpl(id, elementId, start, startElementId, end, endElementId, type,
				toDriverMap(properties));
	}

	@Override
	public Segment segment(Node start, Relationship relationship, Node end) {
		return new PathImpl.SelfContainedSegment((NodeImpl) start, (RelationshipImpl) relationship, (NodeImpl) end);
	}

	@Override
	public Path path(List<Segment> segments, List<Node> nodes, List<Relationship> relationships) {
		var segments0 = segments.stream().map(segment -> (org.neo4j.jdbc.values.Path.Segment) segment).toList();
		var nodes0 = nodes.stream().map(node -> (org.neo4j.jdbc.values.Node) node).toList();
		var relationships0 = relationships.stream()
			.map(relationship -> (org.neo4j.jdbc.values.Relationship) relationship)
			.toList();
		return new PathImpl(segments0, nodes0, relationships0);
	}

	@Override
	public Value isoDuration(long months, long days, long seconds, int nanoseconds) {
		return asBoltValue(Values.isoDuration(months, days, seconds, nanoseconds));
	}

	@Override
	public Value point(int srid, double x, double y) {
		return asBoltValue(Values.point(srid, x, y));
	}

	@Override
	public Value point(int srid, double x, double y, double z) {
		return asBoltValue(Values.point(srid, x, y, z));
	}

	@Override
	public Value unsupportedDateTimeValue(DateTimeException e) {
		return asBoltValue(new UnsupportedDateTimeValue(e));
	}

	Map<String, Value> toBoltMap(Map<String, Object> map) {
		var result = new HashMap<String, Value>(map.size());
		for (var entry : map.entrySet()) {
			var boltValue = asBoltValue(Values.value(entry.getValue()));
			result.put(entry.getKey(), boltValue);
		}
		return Collections.unmodifiableMap(result);
	}

	Map<String, org.neo4j.jdbc.values.Value> toDriverMap(Map<String, Value> map) {
		var result = new HashMap<String, org.neo4j.jdbc.values.Value>(map.size());
		for (var entry : map.entrySet()) {
			var boltValue = Values.value(entry.getValue());
			result.put(entry.getKey(), boltValue);
		}
		return Collections.unmodifiableMap(result);
	}

	// todo look at this again
	private Value asBoltValue(org.neo4j.jdbc.values.Value value) {
		try {
			var method = value.getClass().getDeclaredMethod("asBoltValue");
			method.setAccessible(true);
			return (Value) method.invoke(value);
		}
		catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
			return null;
		}
	}

}
