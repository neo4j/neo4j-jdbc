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

import java.util.List;
import java.util.Map;
import java.util.function.Function;

abstract class AbstractMapAccessorWithDefaultValue implements MapAccessorWithDefaultValue {

	@Override
	public Value get(String key, Value defaultValue) {
		return get(get(key), defaultValue);
	}

	private static Value get(Value value, Value defaultValue) {
		if (value.equals(Values.NULL)) {
			return defaultValue;
		}
		else {
			return ((AsValue) value).asValue();
		}
	}

	@Override
	public Object get(String key, Object defaultValue) {
		return get(get(key), defaultValue);
	}

	private static Object get(Value value, Object defaultValue) {
		if (value.equals(Values.NULL)) {
			return defaultValue;
		}
		else {
			return value.asObject();
		}
	}

	@Override
	public Number get(String key, Number defaultValue) {
		return get(get(key), defaultValue);
	}

	private static Number get(Value value, Number defaultValue) {
		if (value.equals(Values.NULL)) {
			return defaultValue;
		}
		else {
			return value.asNumber();
		}
	}

	@Override
	public Entity get(String key, Entity defaultValue) {
		return get(get(key), defaultValue);
	}

	private static Entity get(Value value, Entity defaultValue) {
		if (value.equals(Values.NULL)) {
			return defaultValue;
		}
		else {
			return value.asEntity();
		}
	}

	@Override
	public Node get(String key, Node defaultValue) {
		return get(get(key), defaultValue);
	}

	private static Node get(Value value, Node defaultValue) {
		if (value.equals(Values.NULL)) {
			return defaultValue;
		}
		else {
			return value.asNode();
		}
	}

	@Override
	public Path get(String key, Path defaultValue) {
		return get(get(key), defaultValue);
	}

	private static Path get(Value value, Path defaultValue) {
		if (value.equals(Values.NULL)) {
			return defaultValue;
		}
		else {
			return value.asPath();
		}
	}

	@Override
	public Relationship get(String key, Relationship defaultValue) {
		return get(get(key), defaultValue);
	}

	private static Relationship get(Value value, Relationship defaultValue) {
		if (value.equals(Values.NULL)) {
			return defaultValue;
		}
		else {
			return value.asRelationship();
		}
	}

	@Override
	public List<Object> get(String key, List<Object> defaultValue) {
		return get(get(key), defaultValue);
	}

	private static List<Object> get(Value value, List<Object> defaultValue) {
		if (value.equals(Values.NULL)) {
			return defaultValue;
		}
		else {
			return value.asList();
		}
	}

	@Override
	public <T> List<T> get(String key, List<T> defaultValue, Function<Value, T> mapFunc) {
		return get(get(key), defaultValue, mapFunc);
	}

	private static <T> List<T> get(Value value, List<T> defaultValue, Function<Value, T> mapFunc) {
		if (value.equals(Values.NULL)) {
			return defaultValue;
		}
		else {
			return value.asList(mapFunc);
		}
	}

	@Override
	public Map<String, Object> get(String key, Map<String, Object> defaultValue) {
		return get(get(key), defaultValue);
	}

	private static Map<String, Object> get(Value value, Map<String, Object> defaultValue) {
		if (value.equals(Values.NULL)) {
			return defaultValue;
		}
		else {
			return value.asMap();
		}
	}

	@Override
	public <T> Map<String, T> get(String key, Map<String, T> defaultValue, Function<Value, T> mapFunc) {
		return get(get(key), defaultValue, mapFunc);
	}

	private static <T> Map<String, T> get(Value value, Map<String, T> defaultValue, Function<Value, T> mapFunc) {
		if (value.equals(Values.NULL)) {
			return defaultValue;
		}
		else {
			return value.asMap(mapFunc);
		}
	}

	@Override
	public int get(String key, int defaultValue) {
		return get(get(key), defaultValue);
	}

	private static int get(Value value, int defaultValue) {
		if (value.equals(Values.NULL)) {
			return defaultValue;
		}
		else {
			return value.asInt();
		}
	}

	@Override
	public long get(String key, long defaultValue) {
		return get(get(key), defaultValue);
	}

	private static long get(Value value, long defaultValue) {
		if (value.equals(Values.NULL)) {
			return defaultValue;
		}
		else {
			return value.asLong();
		}
	}

	@Override
	public boolean get(String key, boolean defaultValue) {
		return get(get(key), defaultValue);
	}

	private static boolean get(Value value, boolean defaultValue) {
		if (value.equals(Values.NULL)) {
			return defaultValue;
		}
		else {
			return value.asBoolean();
		}
	}

	@Override
	public String get(String key, String defaultValue) {
		return get(get(key), defaultValue);
	}

	private static String get(Value value, String defaultValue) {
		if (value.equals(Values.NULL)) {
			return defaultValue;
		}
		else {
			return value.asString();
		}
	}

	@Override
	public float get(String key, float defaultValue) {
		return get(get(key), defaultValue);
	}

	private static float get(Value value, float defaultValue) {
		if (value.equals(Values.NULL)) {
			return defaultValue;
		}
		else {
			return value.asFloat();
		}
	}

	@Override
	public double get(String key, double defaultValue) {
		return get(get(key), defaultValue);
	}

	private static double get(Value value, double defaultValue) {
		if (value.equals(Values.NULL)) {
			return defaultValue;
		}
		else {
			return value.asDouble();
		}
	}

	/**
	 * Formats the content of a bolt map into a string reassembling the shape of a Cypher
	 * map, i.e. using {@code :} instead of {@code =} as the key-value separator.
	 * @param map the map to format
	 * @param <V> the type of the maps values.
	 * @return a string representation of the map
	 */
	protected static <V> String formatPairs(Map<String, V> map) {
		var iterator = map.entrySet().iterator();
		switch (map.size()) {
			case 0 -> {
				return "{}";
			}
			case 1 -> {
				return String.format("{%s}", keyValueString(iterator.next()));
			}
			default -> {
				var builder = new StringBuilder();
				builder.append("{");
				builder.append(keyValueString(iterator.next()));
				while (iterator.hasNext()) {
					builder.append(',');
					builder.append(' ');
					builder.append(keyValueString(iterator.next()));
				}
				builder.append("}");
				return builder.toString();
			}
		}
	}

	private static <V> String keyValueString(Map.Entry<String, V> entry) {
		return String.format("%s: %s", entry.getKey(), entry.getValue());
	}

}
