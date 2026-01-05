/*
 * Copyright (c) 2023-2026 "Neo4j,"
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

import org.neo4j.jdbc.values.Value;

/**
 * A mapper from {@link Value values} to arbitrary JSON types. A {@code JSONMapper} has a
 * base type ({@link #getBaseType()}) that represents a JSON type that the given mapper
 * can handle, such as {@code com.fasterxml.jackson.databind.JsonNode} or
 * {@code javax.json.JsonValue}. The base type will be used programmatically only to
 * generate helpful error messages, as the JDBC driver wants to avoid any premature
 * initialisation of optional types (i.e. the JDBC driver only optionally depends on
 * Jackson databind). Hence, all mappers must be registered manually in
 * {@link JSONMappers}.
 *
 * @param <T> the JSON type this mapper can produce
 * @author Michael J. Simons
 * @since 6.7.0
 */
interface JSONMapper<T> {

	/**
	 * Converts a Neo4j {@link Value} to a JSON object supported by this mapper. This
	 * method should be {@literal null} safe, meaning that for a {@literal null} input a
	 * Null-representation is returned and no exception should be thrown.
	 * @param value the value to convert
	 * @return a JSON value
	 */
	T toJson(Value value);

	/**
	 * Converts a JSON object supported by this mapper to a Neo4j {@link Value}. A
	 * {@literal null} JSON object or a null representation must be converted to
	 * {@link org.neo4j.jdbc.values.Values#NULL}.
	 * @param json the json value to convert
	 * @return a Neo4j value
	 */
	Value fromJson(Object json);

	/**
	 * {@return the JSON base type supported by this mapper}
	 */
	Class<T> getBaseType();

}
