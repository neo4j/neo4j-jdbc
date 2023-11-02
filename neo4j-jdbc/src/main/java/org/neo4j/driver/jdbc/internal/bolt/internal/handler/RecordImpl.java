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
package org.neo4j.driver.jdbc.internal.bolt.internal.handler;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.neo4j.driver.jdbc.internal.bolt.internal.util.Extract;
import org.neo4j.driver.jdbc.internal.bolt.values.AbstractMapAccessorWithDefaultValue;
import org.neo4j.driver.jdbc.internal.bolt.values.Record;
import org.neo4j.driver.jdbc.internal.bolt.values.Value;
import org.neo4j.driver.jdbc.internal.bolt.values.Values;

final class RecordImpl extends AbstractMapAccessorWithDefaultValue implements Record {

	private final List<String> keys;

	private final Value[] values;

	private int hashCode = 0;

	RecordImpl(List<String> keys, Value[] values) {
		this.keys = keys;
		this.values = values;
	}

	@Override
	public List<String> keys() {
		return this.keys;
	}

	@Override
	public List<Value> values() {
		return Arrays.asList(this.values);
	}

	@Override
	public <T> Iterable<T> values(Function<Value, T> mapFunction) {
		return values().stream().map(mapFunction).collect(Collectors.toList());
	}

	@Override
	public int index(String key) {
		var result = this.keys.indexOf(key);
		if (result == -1) {
			throw new NoSuchElementException("Unknown key: " + key);
		}
		else {
			return result;
		}
	}

	@Override
	public boolean containsKey(String key) {
		return this.keys.contains(key);
	}

	@Override
	public Value get(String key) {
		var fieldIndex = this.keys.indexOf(key);

		if (fieldIndex == -1) {
			return Values.NULL;
		}
		else {
			return this.values[fieldIndex];
		}
	}

	@Override
	public Value get(int index) {
		return (index >= 0 && index < this.values.length) ? this.values[index] : Values.NULL;
	}

	@Override
	public int size() {
		return this.values.length;
	}

	@Override
	public Map<String, Object> asMap() {
		return Extract.map(this, Values.ofObject());
	}

	@Override
	public <T> Map<String, T> asMap(Function<Value, T> mapper) {
		return Extract.map(this, mapper);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		else if (other instanceof Record otherRecord) {
			var size = size();
			if (!(size == otherRecord.size())) {
				return false;
			}
			if (!this.keys.equals(otherRecord.keys())) {
				return false;
			}
			for (var i = 0; i < size; i++) {
				var value = get(i);
				var otherValue = otherRecord.get(i);
				if (!value.equals(otherValue)) {
					return false;
				}
			}
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		if (this.hashCode == 0) {
			this.hashCode = 31 * this.keys.hashCode() + Arrays.hashCode(this.values);
		}
		return this.hashCode;
	}

	@Override
	public String toString() {
		return String.format("Record<%s>", formatPairs(asMap(Values.ofValue())));
	}

}
