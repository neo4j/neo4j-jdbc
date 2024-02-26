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
package org.neo4j.driver.jdbc.values;

import java.sql.Types;

public final class Neo4jTypeToSqlTypeMapper {

	private Neo4jTypeToSqlTypeMapper() {
	}

	/*
	 * This is required because of a bug in 5 where java types are returned from the
	 * db.schema.nodeTypeProperties() procedure In 6 this will change so will need to add
	 * another method to handle the new types returned here when working with 6
	 */
	public static int toSqlTypeFromOldCypherType(String neo4jType) {
		return toSqlType(Type.valueOfV5Name(neo4jType));
	}

	/*
	 * This is required because of a bug in 5 where java types are returned from the
	 * db.schema.nodeTypeProperties() procedure In 6 this will change so will need to add
	 * another method to handle the new types returned here when working with 6
	 */
	public static String oldCypherTypesToNew(String neo4jType) {
		return switch (neo4jType) {
			// Simple
			case "Boolean" -> "BOOLEAN";
			case "Double" -> "FLOAT";
			case "Long" -> "INTEGER";
			case "String" -> "STRING";
			// Structs
			case "Point" -> "POINT";

			// Lists no way to get better mapping so loose info here
			case "StringArray", "DoubleArray", "LongArray" -> "LIST";
			// Temporal
			case "Date" -> "DATE";
			case "Duration" -> "DURATION";
			case "DateTime" -> "ZONED DATETIME";
			case "Time" -> "ZONED TIME";
			case "LocalDateTime" -> "LOCAL DATETIME";
			case "LocalTime" -> "LOCAL TIME";
			// Not really types but kinda are
			case "Null" -> "NULL";
			// this is not a cypher type but needs to be represented for// jdbc
			case "Any" -> "ANY";
			default -> "OTHER";
		};
	}

	public static int toSqlType(Type neo4jType) {
		return switch (neo4jType) {
			case ANY, DURATION -> Types.OTHER;
			case BOOLEAN -> Types.BOOLEAN;
			case BYTES -> Types.BLOB;
			case STRING -> Types.VARCHAR;
			case NUMBER -> Types.BIGINT;
			case INTEGER -> Types.INTEGER;
			case FLOAT -> Types.FLOAT;
			case LIST -> Types.ARRAY;
			case MAP, POINT, PATH, RELATIONSHIP, NODE -> Types.STRUCT;
			case DATE -> Types.DATE;
			case TIME -> Types.TIME;
			case DATE_TIME, LOCAL_DATE_TIME, LOCAL_TIME -> Types.TIMESTAMP;
			case NULL -> Types.NULL;
		};
	}

}
