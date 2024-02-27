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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

abstract class AbstractValue extends AbstractMapAccessorWithDefaultValue implements Value {

	@Override
	public boolean hasType(Type type) {
		return type.isTypeOf(this);
	}

	@Override
	public boolean isTrue() {
		return false;
	}

	@Override
	public boolean isFalse() {
		return false;
	}

	@Override
	public boolean isNull() {
		return false;
	}

	@Override
	public boolean containsKey(String key) {
		throw new NotMultiValuedException(type().name() + " is not a keyed collection");
	}

	@Override
	public String asString() {
		throw new UncoercibleException(type().name(), "Java String");
	}

	@Override
	public boolean asBoolean(boolean defaultValue) {
		return computeOrDefault(Value::asBoolean, defaultValue);
	}

	@Override
	public String asString(String defaultValue) {
		return computeOrDefault((Value::asString), defaultValue);
	}

	@Override
	public long asLong(long defaultValue) {
		return computeOrDefault(Value::asLong, defaultValue);
	}

	@Override
	public int asInt(int defaultValue) {
		return computeOrDefault(Value::asInt, defaultValue);
	}

	@Override
	public double asDouble(double defaultValue) {
		return computeOrDefault(Value::asDouble, defaultValue);
	}

	@Override
	public float asFloat(float defaultValue) {
		return computeOrDefault(Value::asFloat, defaultValue);
	}

	@Override
	public long asLong() {
		throw new UncoercibleException(type().name(), "Java long");
	}

	@Override
	public int asInt() {
		throw new UncoercibleException(type().name(), "Java int");
	}

	@Override
	public float asFloat() {
		throw new UncoercibleException(type().name(), "Java float");
	}

	@Override
	public double asDouble() {
		throw new UncoercibleException(type().name(), "Java double");
	}

	@Override
	public boolean asBoolean() {
		throw new UncoercibleException(type().name(), "Java boolean");
	}

	@Override
	public List<Object> asList() {
		return asList(Values.ofObject());
	}

	@Override
	public <T> List<T> asList(Function<Value, T> mapFunction) {
		throw new UncoercibleException(type().name(), "Java List");
	}

	@Override
	public Map<String, Object> asMap() {
		return asMap(Values.ofObject());
	}

	@Override
	public <T> Map<String, T> asMap(Function<Value, T> mapFunction) {
		throw new UncoercibleException(type().name(), "Java Map");
	}

	@Override
	public Object asObject() {
		throw new UncoercibleException(type().name(), "Java Object");
	}

	@Override
	public <T> T computeOrDefault(Function<Value, T> mapper, T defaultValue) {
		if (isNull()) {
			return defaultValue;
		}
		return mapper.apply(this);
	}

	@Override
	public Map<String, Object> asMap(Map<String, Object> defaultValue) {
		return computeOrDefault(Value::asMap, defaultValue);
	}

	@Override
	public <T> Map<String, T> asMap(Function<Value, T> mapFunction, Map<String, T> defaultValue) {
		return computeOrDefault(value -> value.asMap(mapFunction), defaultValue);
	}

	@Override
	public byte[] asByteArray(byte[] defaultValue) {
		return computeOrDefault(Value::asByteArray, defaultValue);
	}

	@Override
	public List<Object> asList(List<Object> defaultValue) {
		return computeOrDefault(Value::asList, defaultValue);
	}

	@Override
	public <T> List<T> asList(Function<Value, T> mapFunction, List<T> defaultValue) {
		return computeOrDefault(value -> value.asList(mapFunction), defaultValue);
	}

	@Override
	public LocalDate asLocalDate(LocalDate defaultValue) {
		return computeOrDefault(Value::asLocalDate, defaultValue);
	}

	@Override
	public OffsetTime asOffsetTime(OffsetTime defaultValue) {
		return computeOrDefault(Value::asOffsetTime, defaultValue);
	}

	@Override
	public LocalTime asLocalTime(LocalTime defaultValue) {
		return computeOrDefault(Value::asLocalTime, defaultValue);
	}

	@Override
	public LocalDateTime asLocalDateTime(LocalDateTime defaultValue) {
		return computeOrDefault(Value::asLocalDateTime, defaultValue);
	}

	@Override
	public OffsetDateTime asOffsetDateTime(OffsetDateTime defaultValue) {
		return computeOrDefault(Value::asOffsetDateTime, defaultValue);
	}

	@Override
	public ZonedDateTime asZonedDateTime(ZonedDateTime defaultValue) {
		return computeOrDefault(Value::asZonedDateTime, defaultValue);
	}

	@Override
	public IsoDuration asIsoDuration(IsoDuration defaultValue) {
		return computeOrDefault(Value::asIsoDuration, defaultValue);
	}

	@Override
	public Point asPoint(Point defaultValue) {
		return computeOrDefault(Value::asPoint, defaultValue);
	}

	@Override
	public byte[] asByteArray() {
		throw new UncoercibleException(type().name(), "Byte array");
	}

	@Override
	public Number asNumber() {
		throw new UncoercibleException(type().name(), "Java Number");
	}

	@Override
	public Entity asEntity() {
		throw new UncoercibleException(type().name(), "Entity");
	}

	@Override
	public Node asNode() {
		throw new UncoercibleException(type().name(), "Node");
	}

	@Override
	public Path asPath() {
		throw new UncoercibleException(type().name(), "Path");
	}

	@Override
	public Relationship asRelationship() {
		throw new UncoercibleException(type().name(), "Relationship");
	}

	@Override
	public LocalDate asLocalDate() {
		throw new UncoercibleException(type().name(), "LocalDate");
	}

	@Override
	public OffsetTime asOffsetTime() {
		throw new UncoercibleException(type().name(), "OffsetTime");
	}

	@Override
	public LocalTime asLocalTime() {
		throw new UncoercibleException(type().name(), "LocalTime");
	}

	@Override
	public LocalDateTime asLocalDateTime() {
		throw new UncoercibleException(type().name(), "LocalDateTime");
	}

	@Override
	public OffsetDateTime asOffsetDateTime() {
		throw new UncoercibleException(type().name(), "OffsetDateTime");
	}

	@Override
	public ZonedDateTime asZonedDateTime() {
		throw new UncoercibleException(type().name(), "ZonedDateTime");
	}

	@Override
	public IsoDuration asIsoDuration() {
		throw new UncoercibleException(type().name(), "Duration");
	}

	@Override
	public Point asPoint() {
		throw new UncoercibleException(type().name(), "Point");
	}

	@Override
	public Value get(int index) {
		throw new NotMultiValuedException(type().name() + " is not an indexed collection");
	}

	@Override
	public Value get(String key) {
		throw new NotMultiValuedException(type().name() + " is not a keyed collection");
	}

	@Override
	public int size() {
		throw new UnsizableException(type().name() + " does not have size");
	}

	@Override
	public Iterable<String> keys() {
		return Collections.emptyList();
	}

	@Override
	public boolean isEmpty() {
		return !values().iterator().hasNext();
	}

	@Override
	public Iterable<Value> values() {
		return values(Values.ofValue());
	}

	@Override
	public <T> Iterable<T> values(Function<Value, T> mapFunction) {
		throw new NotMultiValuedException(type().name() + " is not iterable");
	}

	// Force implementation
	@Override
	public abstract boolean equals(Object obj);

	// Force implementation
	@Override
	public abstract int hashCode();

	// Force implementation
	@Override
	public abstract String toString();

}
