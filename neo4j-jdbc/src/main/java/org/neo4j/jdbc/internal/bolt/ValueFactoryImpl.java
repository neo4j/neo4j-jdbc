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
import org.neo4j.driver.internal.bolt.api.values.Type;
import org.neo4j.driver.internal.bolt.api.values.Value;
import org.neo4j.driver.internal.bolt.api.values.ValueFactory;
import org.neo4j.jdbc.values.BooleanValue;
import org.neo4j.jdbc.values.BytesValue;
import org.neo4j.jdbc.values.DateTimeValue;
import org.neo4j.jdbc.values.DateValue;
import org.neo4j.jdbc.values.DurationValue;
import org.neo4j.jdbc.values.FloatValue;
import org.neo4j.jdbc.values.IntegerValue;
import org.neo4j.jdbc.values.ListValue;
import org.neo4j.jdbc.values.LocalDateTimeValue;
import org.neo4j.jdbc.values.LocalTimeValue;
import org.neo4j.jdbc.values.MapValue;
import org.neo4j.jdbc.values.NodeValue;
import org.neo4j.jdbc.values.NullValue;
import org.neo4j.jdbc.values.PathValue;
import org.neo4j.jdbc.values.PointValue;
import org.neo4j.jdbc.values.RelationshipValue;
import org.neo4j.jdbc.values.StringValue;
import org.neo4j.jdbc.values.TimeValue;
import org.neo4j.jdbc.values.UnsupportedDateTimeValue;
import org.neo4j.jdbc.values.Values;

enum ValueFactoryImpl implements ValueFactory {

	INSTANCE;

	private static final Map<Class<? extends org.neo4j.jdbc.values.Value>, Type> TYPE_MAP;

	static {
		var hlp = new HashMap<Class<? extends org.neo4j.jdbc.values.Value>, Type>();
		hlp.put(NullValue.class, Type.NULL);
		hlp.put(DateValue.class, Type.DATE);
		hlp.put(BooleanValue.class, Type.BOOLEAN);
		hlp.put(LocalDateTimeValue.class, Type.LOCAL_DATE_TIME);
		hlp.put(StringValue.class, Type.STRING);
		hlp.put(TimeValue.class, Type.TIME);
		hlp.put(DurationValue.class, Type.DURATION);
		hlp.put(RelationshipValue.class, Type.RELATIONSHIP);
		hlp.put(UnsupportedDateTimeValue.class, Type.DATE_TIME);
		hlp.put(LocalTimeValue.class, Type.LOCAL_TIME);
		hlp.put(FloatValue.class, Type.FLOAT);
		hlp.put(MapValue.class, Type.MAP);
		hlp.put(DateTimeValue.class, Type.DATE_TIME);
		hlp.put(IntegerValue.class, Type.INTEGER);
		hlp.put(PointValue.class, Type.POINT);
		hlp.put(NodeValue.class, Type.NODE);
		hlp.put(PathValue.class, Type.PATH);
		hlp.put(ListValue.class, Type.LIST);
		hlp.put(BytesValue.class, Type.BYTES);
		TYPE_MAP = Map.copyOf(hlp);
	}

	@Override
	public Value value(Object value) {
		if (value instanceof ValueImpl boltValue) {
			return boltValue;
		}
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

	private static Map<String, org.neo4j.jdbc.values.Value> toDriverMap(Map<String, Value> map) {
		var result = new HashMap<String, org.neo4j.jdbc.values.Value>(map.size());
		for (var entry : map.entrySet()) {
			var boltValue = Values.value(entry.getValue());
			result.put(entry.getKey(), boltValue);
		}
		return Collections.unmodifiableMap(result);
	}

	static Value asBoltValue(org.neo4j.jdbc.values.Value value) {
		var type = TYPE_MAP.get(value.getClass());
		if (type == null && value instanceof BooleanValue) {
			type = Type.BOOLEAN;
		}
		return new ValueImpl(value, type);
	}

}
