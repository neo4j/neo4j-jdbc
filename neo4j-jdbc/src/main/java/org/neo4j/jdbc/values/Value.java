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
/*
 * Copyright (c) "Neo4j"
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

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A unit of data that adheres to the Neo4j type system.
 * <p>
 * This interface describes a number of <code>isType</code> methods along with
 * <code>typeValue</code> methods. The first set of these correlate with types from the
 * Neo4j Type System and are used to determine which Neo4j type is represented. The second
 * set of methods perform coercions to Java types (wherever possible). For example, a
 * common String value should be tested for using <code>isString</code> and extracted
 * using <code>stringValue</code>.
 *
 * <h2>Navigating a tree structure</h2>
 * <p>
 * Because Neo4j often handles dynamic structures, this interface is designed to help you
 * handle such structures in Java. Specifically, {@link Value} lets you navigate arbitrary
 * tree structures without having to resort to type casting.
 * <p>
 * Given a tree structure like:
 *
 * <pre>
 * {@code
 * {
 *   users : [
 *     { name : "Anders" },
 *     { name : "John" }
 *   ]
 * }
 * }
 * </pre>
 * <p>
 * You can retrieve the name of the second user, John, like so:
 * <pre class="docTest:ValueDocIT#classDocTreeExample">
 * {@code
 * String username = value.get("users").get(1).get("name").asString();
 * }
 * </pre>
 * <p>
 * You can also easily iterate over the users:
 * <pre class="docTest:ValueDocIT#classDocIterationExample">
 * {@code
 * List<String> names = new LinkedList<>();
 * for(Value user : value.get("users").values() )
 * {
 *     names.add(user.get("name").asString());
 * }
 * }
 * </pre>
 *
 * @author Neo4j Drivers Team
 * @since 6.0.0
 */
public interface Value extends MapAccessorWithDefaultValue {

	/**
	 * If the underlying value is a collection type, return the number of values in the
	 * collection.
	 * <p>
	 * For {@link Type#LIST list} values, this will return the size of the list.
	 * <p>
	 * For {@link Type#MAP map} values, this will return the number of entries in the map.
	 * <p>
	 * For {@link Type#NODE node} and {@link Type#RELATIONSHIP} relationship} values, this
	 * will return the number of properties.
	 * <p>
	 * For {@link Type#PATH path} values, this returns the length (number of
	 * relationships) in the path.
	 * @return the number of values in an underlying collection
	 */
	int size();

	/**
	 * If this value represents a list or map, test if the collection is empty.
	 * @return {@code true} if size() is 0, otherwise {@code false}
	 */
	boolean isEmpty();

	/**
	 * If the underlying value supports {@link #get(String) key-based indexing}, return an
	 * iterable of the keys in the map, this applies to {@link Type#MAP map},
	 * {@link #asNode() node} and {@link Type#RELATIONSHIP relationship} values.
	 * @return the keys in the value
	 */
	@Override
	Iterable<String> keys();

	/**
	 * Retrieve the value at the given index.
	 * @param index the index of the value
	 * @return the value or a {@link NullValue} if the index is out of bounds
	 * @throws ValueException if record has not been initialized
	 */
	Value get(int index);

	/**
	 * Returns the type.
	 * @return the type of this value as defined in the Neo4j type system
	 */
	Type type();

	/**
	 * Test if this value is a value of the given type.
	 * @param type the given type
	 * @return type.isTypeOf(this)
	 */
	boolean hasType(Type type);

	/**
	 * Returns {@code true} if the value is a Boolean value and has the value True.
	 * @return {@code true} if the value is a Boolean value and has the value True.
	 */
	boolean isTrue();

	/**
	 * Returns {@code true} if the value is a Boolean value and has the value False.
	 * @return {@code true} if the value is a Boolean value and has the value False.
	 */
	boolean isFalse();

	/**
	 * Returns {@code true} if the value is a Null, otherwise {@code false}.
	 * @return {@code true} if the value is a Null, otherwise {@code false}
	 */
	boolean isNull();

	/**
	 * This returns a java standard library representation of the underlying value, using
	 * a java type that is "sensible" given the underlying type. The mapping for common
	 * types is as follows:
	 *
	 * <ul>
	 * <li>{@link Type#NULL} - {@code null}</li>
	 * <li>{@link Type#LIST} - {@link List}</li>
	 * <li>{@link Type#MAP} - {@link Map}</li>
	 * <li>{@link Type#BOOLEAN} - {@link Boolean}</li>
	 * <li>{@link Type#INTEGER} - {@link Long}</li>
	 * <li>{@link Type#FLOAT} - {@link Double}</li>
	 * <li>{@link Type#STRING} - {@link String}</li>
	 * <li>{@link Type#BYTES} - {@literal byte[]}</li>
	 * <li>{@link Type#DATE} - {@link LocalDate}</li>
	 * <li>{@link Type#TIME} - {@link OffsetTime}</li>
	 * <li>{@link Type#LOCAL_TIME} - {@link LocalTime}</li>
	 * <li>{@link Type#DATE_TIME} - {@link ZonedDateTime}</li>
	 * <li>{@link Type#LOCAL_DATE_TIME} - {@link LocalDateTime}</li>
	 * <li>{@link Type#DURATION} - {@link IsoDuration}</li>
	 * <li>{@link Type#POINT} - {@link Point}</li>
	 * <li>{@link Type#NODE} - {@link Node}</li>
	 * <li>{@link Type#RELATIONSHIP} - {@link Relationship}</li>
	 * <li>{@link Type#PATH} - {@link Path}</li>
	 * </ul>
	 * <p>
	 * Note that the types refer to the Neo4j type system where {@link Type#INTEGER} and
	 * {@link Type#FLOAT} are both 64-bit precision. This is why these types return java
	 * {@link Long} and {@link Double}, respectively.
	 * @return the value as a Java Object.
	 * @throws DateTimeException if zone information supplied by server is not supported
	 * by driver runtime. Applicable to datetime values only.
	 */
	Object asObject();

	/**
	 * Apply the mapping function on the value if the value is not a {@link NullValue}, or
	 * the default value if the value is a {@link NullValue}.
	 * @param mapper the mapping function defines how to map a {@link Value} to T.
	 * @param defaultValue the value to return if the value is a {@link NullValue}
	 * @param <T> the return type
	 * @return the value after applying the given mapping function or the default value if
	 * the value is {@link NullValue}.
	 */
	<T> T computeOrDefault(Function<Value, T> mapper, T defaultValue);

	/**
	 * Returns value as boolean if possible.
	 * @return the value as a Java boolean, if possible.
	 * @throws UncoercibleException if value types are incompatible.
	 */
	boolean asBoolean();

	/**
	 * Returns value as boolean if possible or the default value otherwise.
	 * @param defaultValue return this value if the value is a {@link NullValue}.
	 * @return the value as a Java boolean, if possible.
	 * @throws UncoercibleException if value types are incompatible.
	 */
	boolean asBoolean(boolean defaultValue);

	/**
	 * Returns value as byte array if possible.
	 * @return the value as a Java byte array, if possible.
	 * @throws UncoercibleException if value types are incompatible.
	 */
	byte[] asByteArray();

	/**
	 * Returns value as byte array if possible or the default value otherwise.
	 * @param defaultValue default to this value if the original value is a
	 * {@link NullValue}
	 * @return the value as a Java byte array, if possible.
	 * @throws UncoercibleException if value types are incompatible.
	 */
	byte[] asByteArray(byte[] defaultValue);

	/**
	 * Returns value as string if possible.
	 * @return the value as a Java String, if possible.
	 * @throws UncoercibleException if value types are incompatible.
	 */
	String asString();

	/**
	 * Returns value as string if possible or the default value otherwise.
	 * @param defaultValue return this value if the value is null.
	 * @return the value as a Java String, if possible
	 * @throws UncoercibleException if value types are incompatible.
	 */
	String asString(String defaultValue);

	/**
	 * Returns value as number if possible.
	 * @return the value as a Java Number, if possible.
	 * @throws UncoercibleException if value types are incompatible.
	 */
	Number asNumber();

	/**
	 * Returns a Java long if no precision is lost in the conversion. Returns a Java long
	 * if no precision is lost in the conversion.
	 * @return the value as a Java long.
	 * @throws LossyCoercion if it is not possible to convert the value without loosing
	 * precision.
	 * @throws UncoercibleException if value types are incompatible.
	 */
	long asLong();

	/**
	 * Returns a Java long if no precision is lost in the conversion.
	 * @param defaultValue return this default value if the value is a {@link NullValue}.
	 * @return the value as a Java long.
	 * @throws LossyCoercion if it is not possible to convert the value without loosing
	 * precision.
	 * @throws UncoercibleException if value types are incompatible.
	 */
	long asLong(long defaultValue);

	/**
	 * Returns a Java int if no precision is lost in the conversion.
	 * @return the value as a Java int.
	 * @throws LossyCoercion if it is not possible to convert the value without loosing
	 * precision.
	 * @throws UncoercibleException if value types are incompatible.
	 */
	int asInt();

	/**
	 * Returns a Java int if no precision is lost in the conversion.
	 * @param defaultValue return this default value if the value is a {@link NullValue}.
	 * @return the value as a Java int.
	 * @throws LossyCoercion if it is not possible to convert the value without loosing
	 * precision.
	 * @throws UncoercibleException if value types are incompatible.
	 */
	int asInt(int defaultValue);

	/**
	 * Returns a Java double if no precision is lost in the conversion.
	 * @return the value as a Java double.
	 * @throws LossyCoercion if it is not possible to convert the value without loosing
	 * precision.
	 * @throws UncoercibleException if value types are incompatible.
	 */
	double asDouble();

	/**
	 * Returns a Java double if no precision is lost in the conversion.
	 * @param defaultValue default to this value if the value is a {@link NullValue}.
	 * @return the value as a Java double.
	 * @throws LossyCoercion if it is not possible to convert the value without loosing
	 * precision.
	 * @throws UncoercibleException if value types are incompatible.
	 */
	double asDouble(double defaultValue);

	/**
	 * Returns a Java float if no precision is lost in the conversion.
	 * @return the value as a Java float.
	 * @throws LossyCoercion if it is not possible to convert the value without loosing
	 * precision.
	 * @throws UncoercibleException if value types are incompatible.
	 */
	float asFloat();

	/**
	 * Returns a Java float if no precision is lost in the conversion.
	 * @param defaultValue default to this value if the value is a {@link NullValue}
	 * @return the value as a Java float.
	 * @throws LossyCoercion if it is not possible to convert the value without loosing
	 * precision.
	 * @throws UncoercibleException if value types are incompatible.
	 */
	float asFloat(float defaultValue);

	/**
	 * If the underlying type can be viewed as a list, returns a java list of values,
	 * where each value has been converted using {@link #asObject()}.
	 * @return the value as a Java list of values, if possible
	 * @see #asObject()
	 */
	List<Object> asList();

	/**
	 * If the underlying type can be viewed as a list, returns a java list of values,
	 * where each value has been converted using {@link #asObject()}.
	 * @param defaultValue default to this value if the value is a {@link NullValue}
	 * @return the value as a Java list of values, if possible
	 * @see #asObject()
	 */
	List<Object> asList(List<Object> defaultValue);

	/**
	 * If the underlying type can be viewed as a list, returns a java list of values,
	 * where each value has been converted using the provided map function.
	 * @param mapFunction a function to map from Value to T. See {@link Values} for some
	 * predefined functions, such as {@link Values#ofBoolean()},
	 * {@link Values#ofList(Function)}.
	 * @param <T> the type of target list elements
	 * @return the value as a list of T obtained by mapping from the list elements, if
	 * possible
	 * @see Values for a long list of built-in conversion functions
	 */
	<T> List<T> asList(Function<Value, T> mapFunction);

	/**
	 * If the underlying type can be viewed as a list, returns a java list of values,
	 * where each value has been converted using the provided map function. Alternatively,
	 * returns the default value.
	 * @param mapFunction a function to map from Value to T. See {@link Values} for some
	 * predefined functions, such as {@link Values#ofBoolean()},
	 * {@link Values#ofList(Function)}.
	 * @param <T> the type of target list elements
	 * @param defaultValue default to this value if the value is a {@link NullValue}
	 * @return the value as a list of T obtained by mapping from the list elements, if
	 * possible
	 * @see Values for a long list of built-in conversion functions
	 */
	<T> List<T> asList(Function<Value, T> mapFunction, List<T> defaultValue);

	/**
	 * Returns the value as entity if possible.
	 * @return the value as a {@link Entity}, if possible.
	 * @throws UncoercibleException if value types are incompatible.
	 */
	Entity asEntity();

	/**
	 * Returns the value as node if possible.
	 * @return the value as a {@link Node}, if possible.
	 * @throws UncoercibleException if value types are incompatible.
	 */
	Node asNode();

	/**
	 * Returns the value as retionship if possible.
	 * @return the value as a {@link Relationship}, if possible.
	 * @throws UncoercibleException if value types are incompatible.
	 */
	Relationship asRelationship();

	/**
	 * Returns the value as path if possible.
	 * @return the value as a {@link Path}, if possible.
	 * @throws UncoercibleException if value types are incompatible.
	 */
	Path asPath();

	/**
	 * Returns the value as local date if possible.
	 * @return the value as a {@link LocalDate}, if possible.
	 * @throws UncoercibleException if value types are incompatible.
	 */
	LocalDate asLocalDate();

	/**
	 * Returns the value as offset time if possible.
	 * @return the value as a {@link OffsetTime}, if possible.
	 * @throws UncoercibleException if value types are incompatible.
	 */
	OffsetTime asOffsetTime();

	/**
	 * Returns the value as local time if possible.
	 * @return the value as a {@link LocalTime}, if possible.
	 * @throws UncoercibleException if value types are incompatible.
	 */
	LocalTime asLocalTime();

	/**
	 * Returns the value as local date time if possible.
	 * @return the value as a {@link LocalDateTime}, if possible.
	 * @throws UncoercibleException if value types are incompatible.
	 */
	LocalDateTime asLocalDateTime();

	/**
	 * Returns the value as offset date time if possible.
	 * @return the value as a {@link java.time.OffsetDateTime}, if possible.
	 * @throws UncoercibleException if value types are incompatible.
	 * @throws DateTimeException if zone information supplied by server is not supported
	 * by driver runtime.
	 */
	OffsetDateTime asOffsetDateTime();

	/**
	 * Returns the value as zoned date time if possible.
	 * @return the value as a {@link ZonedDateTime}, if possible.
	 * @throws UncoercibleException if value types are incompatible.
	 * @throws DateTimeException if zone information supplied by server is not supported
	 * by driver runtime.
	 */
	ZonedDateTime asZonedDateTime();

	/**
	 * Returns the value as iso duration if possible.
	 * @return the value as a {@link IsoDuration}, if possible.
	 * @throws UncoercibleException if value types are incompatible.
	 */
	IsoDuration asIsoDuration();

	/**
	 * Returns the value as point if possible.
	 * @return the value as a {@link Point}, if possible.
	 * @throws UncoercibleException if value types are incompatible.
	 */
	Point asPoint();

	/**
	 * Returns the value as local date if possible or the default value otherwise.
	 * @param defaultValue default to this value if the value is a {@link NullValue}
	 * @return the value as a {@link LocalDate}, if possible.
	 * @throws UncoercibleException if value types are incompatible.
	 */
	LocalDate asLocalDate(LocalDate defaultValue);

	/**
	 * Returns the value as offset time if possible or the default value otherwise.
	 * @param defaultValue default to this value if the value is a {@link NullValue}
	 * @return the value as a {@link OffsetTime}, if possible.
	 * @throws UncoercibleException if value types are incompatible.
	 */
	OffsetTime asOffsetTime(OffsetTime defaultValue);

	/**
	 * Returns the value as local time if possible or the default value otherwise.
	 * @param defaultValue default to this value if the value is a {@link NullValue}
	 * @return the value as a {@link LocalTime}, if possible.
	 * @throws UncoercibleException if value types are incompatible.
	 */
	LocalTime asLocalTime(LocalTime defaultValue);

	/**
	 * Returns the value as local date time if possible or the default value otherwise.
	 * @param defaultValue default to this value if the value is a {@link NullValue}
	 * @return the value as a {@link LocalDateTime}, if possible.
	 * @throws UncoercibleException if value types are incompatible.
	 */
	LocalDateTime asLocalDateTime(LocalDateTime defaultValue);

	/**
	 * Returns the value as offset date time if possible or the default value otherwise.
	 * @param defaultValue default to this value if the value is a {@link NullValue}
	 * @return the value as a {@link OffsetDateTime}, if possible.
	 * @throws UncoercibleException if value types are incompatible.
	 */
	OffsetDateTime asOffsetDateTime(OffsetDateTime defaultValue);

	/**
	 * Returns the value as zoned date time if possible or the default value otherwise.
	 * @param defaultValue default to this value if the value is a {@link NullValue}
	 * @return the value as a {@link ZonedDateTime}, if possible.
	 * @throws UncoercibleException if value types are incompatible.
	 */
	ZonedDateTime asZonedDateTime(ZonedDateTime defaultValue);

	/**
	 * Returns the value as iso duration if possible or the default value otherwise.
	 * @param defaultValue default to this value if the value is a {@link NullValue}
	 * @return the value as a {@link IsoDuration}, if possible.
	 * @throws UncoercibleException if value types are incompatible.
	 */
	IsoDuration asIsoDuration(IsoDuration defaultValue);

	/**
	 * Returns the value as point if possible or the default value otherwise.
	 * @param defaultValue default to this value if the value is a {@link NullValue}
	 * @return the value as a {@link Point}, if possible.
	 * @throws UncoercibleException if value types are incompatible.
	 */
	Point asPoint(Point defaultValue);

	/**
	 * Return as a map of string keys and values converted using {@link Value#asObject()}.
	 * <p>
	 * This is equivalent to calling {@link #asMap(Function, Map)} with
	 * {@link Values#ofObject()}.
	 * @param defaultValue default to this value if the value is a {@link NullValue}
	 * @return the value as a Java map
	 */
	Map<String, Object> asMap(Map<String, Object> defaultValue);

	/**
	 * Return as a map of string keys and values converted using the provided map function
	 * if possible or the default value.
	 * @param mapFunction a function to map from Value to T. See {@link Values} for some
	 * predefined functions, such as {@link Values#ofBoolean()},
	 * {@link Values#ofList(Function)}.
	 * @param <T> the type of map values
	 * @param defaultValue default to this value if the value is a {@link NullValue}
	 * @return the value as a map from string keys to values of type T obtained from
	 * mapping the original map values, if possible
	 * @see Values for a long list of built-in conversion functions
	 */
	<T> Map<String, T> asMap(Function<Value, T> mapFunction, Map<String, T> defaultValue);

	@Override
	boolean equals(Object other);

	@Override
	int hashCode();

	@Override
	String toString();

	/**
	 * A utility method to enhance {@link #toString()} with the type information.
	 * @return string representation including the type
	 * @since 6.4.0
	 */
	default String toDisplayString() {
		return "%s (%s)".formatted(this, this.type());
	}

}
