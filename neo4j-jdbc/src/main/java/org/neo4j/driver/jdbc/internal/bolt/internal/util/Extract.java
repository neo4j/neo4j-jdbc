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
package org.neo4j.driver.jdbc.internal.bolt.internal.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.neo4j.driver.jdbc.internal.bolt.BoltRecord;
import org.neo4j.driver.jdbc.internal.bolt.Value;
import org.neo4j.driver.jdbc.internal.bolt.exception.BoltException;
import org.neo4j.driver.jdbc.internal.bolt.internal.value.NodeValue;
import org.neo4j.driver.jdbc.internal.bolt.internal.value.PathValue;
import org.neo4j.driver.jdbc.internal.bolt.internal.value.RelationshipValue;
import org.neo4j.driver.jdbc.internal.bolt.types.Node;
import org.neo4j.driver.jdbc.internal.bolt.types.Path;
import org.neo4j.driver.jdbc.internal.bolt.types.Relationship;

public final class Extract {

	private Extract() {
		throw new UnsupportedOperationException();
	}

	public static List<Value> list(Value[] values) {
		return switch (values.length) {
			case 0 -> Collections.emptyList();
			case 1 -> Collections.singletonList(values[0]);
			default -> List.of(values);
		};
	}

	public static <T> List<T> list(Value[] data, Function<Value, T> mapFunction) {
		var size = data.length;
		switch (size) {
			case 0 -> {
				return Collections.emptyList();
			}
			case 1 -> {
				return Collections.singletonList(mapFunction.apply(data[0]));
			}
			default -> {
				return Arrays.stream(data).map(mapFunction).toList();
			}
		}
	}

	public static <T> Map<String, T> map(Map<String, Value> data, Function<Value, T> mapFunction) {
		if (data.isEmpty()) {
			return Collections.emptyMap();
		}
		else {
			var size = data.size();
			if (size == 1) {
				var head = data.entrySet().iterator().next();
				return Collections.singletonMap(head.getKey(), mapFunction.apply(head.getValue()));
			}
			else {
				Map<String, T> map = Iterables.newLinkedHashMapWithSize(size);
				for (var entry : data.entrySet()) {
					map.put(entry.getKey(), mapFunction.apply(entry.getValue()));
				}
				return Collections.unmodifiableMap(map);
			}
		}
	}

	public static <T> Map<String, T> map(BoltRecord record, Function<Value, T> mapFunction) {
		var size = record.size();
		switch (size) {
			case 0 -> {
				return Collections.emptyMap();
			}
			case 1 -> {
				return Collections.singletonMap(record.keys().get(0), mapFunction.apply(record.get(0)));
			}
			default -> {
				Map<String, T> map = Iterables.newLinkedHashMapWithSize(size);
				var keys = record.keys();
				for (var i = 0; i < size; i++) {
					map.put(keys.get(i), mapFunction.apply(record.get(i)));
				}
				return Collections.unmodifiableMap(map);
			}
		}
	}

	public static void assertParameter(Object value) {
		if (value instanceof Node || value instanceof NodeValue) {
			throw new BoltException("Nodes can't be used as parameters.");
		}
		if (value instanceof Relationship || value instanceof RelationshipValue) {
			throw new BoltException("Relationships can't be used as parameters.");
		}
		if (value instanceof Path || value instanceof PathValue) {
			throw new BoltException("Paths can't be used as parameters.");
		}
	}

}
