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

import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.neo4j.bolt.connection.values.IsoDuration;
import org.neo4j.bolt.connection.values.Point;
import org.neo4j.bolt.connection.values.Type;
import org.neo4j.bolt.connection.values.Value;
import org.neo4j.jdbc.values.AsValue;
import org.neo4j.jdbc.values.Vector;

final class ValueImpl implements Value, AsValue {

	private static final AtomicReference<Function<Vector, Object>> ARRAY_VECTOR_ACCESSOR = new AtomicReference<>();

	private final org.neo4j.jdbc.values.Value value;

	private final Type type;

	ValueImpl(org.neo4j.jdbc.values.Value value, Type type) {
		this.value = Objects.requireNonNull(value);
		this.type = Objects.requireNonNull(type);
	}

	@Override
	public Type boltValueType() {
		return this.type;
	}

	@Override
	public boolean asBoolean() {
		return this.value.asBoolean();
	}

	@Override
	public byte[] asByteArray() {
		return this.value.asByteArray();
	}

	@Override
	public String asString() {
		return this.value.asString();
	}

	@Override
	public long asLong() {
		return this.value.asLong();
	}

	@Override
	public double asDouble() {
		return this.value.asDouble();
	}

	@Override
	public LocalDate asLocalDate() {
		return this.value.asLocalDate();
	}

	@Override
	public OffsetTime asOffsetTime() {
		return this.value.asOffsetTime();
	}

	@Override
	public LocalTime asLocalTime() {
		return this.value.asLocalTime();
	}

	@Override
	public LocalDateTime asLocalDateTime() {
		return this.value.asLocalDateTime();
	}

	@Override
	public ZonedDateTime asZonedDateTime() {
		return this.value.asZonedDateTime();
	}

	@Override
	public IsoDuration asBoltIsoDuration() {
		var jdbcDuration = this.value.asIsoDuration();
		return new BoltIsoDurationImpl(jdbcDuration.months(), jdbcDuration.days(), jdbcDuration.seconds(),
				jdbcDuration.nanoseconds());
	}

	@Override
	public Point asBoltPoint() {
		var jdbcPoint = this.value.asPoint();
		return new BoltPointImpl(jdbcPoint.srid(), jdbcPoint.x(), jdbcPoint.y(), jdbcPoint.z());
	}

	@Override
	public boolean isNull() {
		return this.value.isNull();
	}

	@Override
	public boolean isEmpty() {
		return this.value.isEmpty();
	}

	@Override
	public Iterable<String> keys() {
		return this.value.keys();
	}

	@Override
	public int size() {
		return this.value.size();
	}

	@Override
	public Value getBoltValue(String key) {
		return ValueFactoryImpl.asBoltValue(this.value.get(key));
	}

	@Override
	public Iterable<Value> boltValues() {
		return () -> new Iterator<>() {
			private final Iterator<org.neo4j.jdbc.values.Value> iterator = ValueImpl.this.value.values().iterator();

			@Override
			public boolean hasNext() {
				return this.iterator.hasNext();
			}

			@Override
			public Value next() {
				return ValueFactoryImpl.asBoltValue(this.iterator.next());
			}
		};
	}

	@Override
	public boolean containsKey(String key) {
		return this.value.containsKey(key);
	}

	@Override
	public Map<String, Value> asBoltMap() {
		return this.value.asMap(ValueFactoryImpl::asBoltValue);
	}

	@Override
	public org.neo4j.bolt.connection.values.Vector asBoltVector() {
		var jdbcVector = this.value.asVector();
		var accessor = ARRAY_VECTOR_ACCESSOR.updateAndGet(old -> {
			if (old != null) {
				return old;
			}
			try {
				var targetClass = Class.forName("org.neo4j.jdbc.values.ArrayBasedVectors");
				var vh = MethodHandles.privateLookupIn(targetClass, MethodHandles.lookup())
					.findStaticVarHandle(targetClass, "ELEMENT_ACCESSOR", Function.class);
				@SuppressWarnings("unchecked")
				var result = (Function<Vector, Object>) vh.get();
				return result;
			}
			catch (Throwable ex) {
				throw new RuntimeException(ex);
			}
		});
		return new BoltVectorImpl(jdbcVector.elementType().getJavaType(), accessor.apply(jdbcVector));
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		var boltValue = (ValueImpl) o;
		return Objects.equals(this.value, boltValue.value) && this.type == boltValue.type;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.value, this.type);
	}

	@Override
	public String toString() {
		return this.value.toString();
	}

	@Override
	public org.neo4j.jdbc.values.Value asValue() {
		return this.value;
	}

	record BoltVectorImpl(Class<?> elementType, Object elements) implements org.neo4j.bolt.connection.values.Vector {
	}

	record BoltIsoDurationImpl(long months, long days, long seconds, int nanoseconds) implements IsoDuration {
	}

	record BoltPointImpl(int srid, double x, double y, double z) implements Point {
	}

}
