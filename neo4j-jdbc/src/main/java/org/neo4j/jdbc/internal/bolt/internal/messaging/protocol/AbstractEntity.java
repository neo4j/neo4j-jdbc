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
package org.neo4j.jdbc.internal.bolt.internal.messaging.protocol;

import java.util.Map;
import java.util.function.Function;

import org.neo4j.jdbc.internal.bolt.internal.util.Iterables;
import org.neo4j.jdbc.values.Entity;
import org.neo4j.jdbc.values.Value;
import org.neo4j.jdbc.values.Values;

abstract class AbstractEntity implements Entity {

	private final long id;

	private final String elementId;

	private final Value properties;

	AbstractEntity(long id, String elementId, Map<String, Value> properties) {
		this.id = id;
		this.elementId = elementId;
		this.properties = Values.value(properties);
	}

	@Override
	@Deprecated
	public long id() {
		return this.id;
	}

	@Override
	public String elementId() {
		return this.elementId;
	}

	@Override
	public int size() {
		return this.properties.size();
	}

	@Override
	public Map<String, Object> asMap() {
		return asMap(Values.ofObject());
	}

	@Override
	public <T> Map<String, T> asMap(Function<Value, T> mapFunction) {
		return this.properties.asMap(mapFunction);
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

		var that = (AbstractEntity) o;

		return this.id == that.id;
	}

	@Override
	public int hashCode() {
		return (int) (this.id ^ (this.id >>> 32));
	}

	@Override
	public String toString() {
		return "Entity{" + "id=" + this.id + ", properties=" + this.properties + '}';
	}

	@Override
	public boolean containsKey(String key) {
		return this.properties.containsKey(key);
	}

	@Override
	public Iterable<String> keys() {
		return this.properties.keys();
	}

	@Override
	public Value get(String key) {
		var value = this.properties.get(key);
		return (value != null) ? value : Values.NULL;
	}

	@Override
	public Iterable<Value> values() {
		return this.properties.values();
	}

	@Override
	public <T> Iterable<T> values(Function<Value, T> mapFunction) {
		return Iterables.map(this.properties.values(), mapFunction);
	}

}
