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
package org.neo4j.jdbc.values;

/**
 * A Neo4j DBMS might have introduced new types that are not known in this version of the
 * Neo4j-JDBC driver. In such cases the server value is mapped into this type so that one
 * can retrieve the name, minimum bolt version and optional messages about the type.
 *
 * @param name the name of the type inside the Neo4j DBMS
 * @param minProtocolVersion the minimum protocol version required to use this type
 * @param message an optional message
 * @author Michael J. Simons
 * @since 6.9.0
 */
public record UnsupportedType(String name, String minProtocolVersion, String message) implements AsValue {

	@Override
	public Value asValue() {
		return new AbstractObjectValue<>(this) {
			@Override
			public Type type() {
				return Type.UNSUPPORTED;
			}
		};
	}
}
