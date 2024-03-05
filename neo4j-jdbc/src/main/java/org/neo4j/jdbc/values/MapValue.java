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
package org.neo4j.jdbc.values;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Representation of a Cypher map.
 *
 * @author Neo4j Drivers Team
 * @since 6.0.0
 */
public final class MapValue extends AbstractValue {

	private final Map<String, Value> val;

	MapValue(Map<String, Value> val) {
		if (val == null) {
			throw new IllegalArgumentException("Cannot construct MapValue from null");
		}
		this.val = val;
	}

	@Override
	public boolean isEmpty() {
		return this.val.isEmpty();
	}

	@Override
	public Map<String, Object> asObject() {
		return asMap(Values.ofObject());
	}

	@Override
	public Map<String, Object> asMap() {
		return map(this.val, Values.ofObject());
	}

	@Override
	public <T> Map<String, T> asMap(Function<Value, T> mapFunction) {
		return map(this.val, mapFunction);
	}

	@Override
	public int size() {
		return this.val.size();
	}

	@Override
	public boolean containsKey(String key) {
		return this.val.containsKey(key);
	}

	@Override
	public Iterable<String> keys() {
		return this.val.keySet();
	}

	@Override
	public Iterable<Value> values() {
		return this.val.values();
	}

	@Override
	public <T> Iterable<T> values(Function<Value, T> mapFunction) {
		return map(this.val, mapFunction).values();
	}

	@Override
	public Value get(String key) {
		var value = this.val.get(key);
		return (value != null) ? value : Values.NULL;
	}

	@Override
	public Type type() {
		return Type.MAP;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		var values = (MapValue) o;
		return this.val.equals(values.val);
	}

	@Override
	public int hashCode() {
		return this.val.hashCode();
	}

	@Override
	public String toString() {
		return formatPairs(asMap(Values.ofValue()));
	}

	private static <T> Map<String, T> map(Map<String, Value> data, Function<Value, T> mapFunction) {
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
				Map<String, T> map = new LinkedHashMap<>(size);
				for (var entry : data.entrySet()) {
					map.put(entry.getKey(), mapFunction.apply(entry.getValue()));
				}
				return Collections.unmodifiableMap(map);
			}
		}
	}

}
