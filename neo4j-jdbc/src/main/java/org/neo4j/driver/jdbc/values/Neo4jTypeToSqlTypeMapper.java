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

import java.sql.SQLException;
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
		return switch (neo4jType) {
			case "Any" -> Types.OTHER;
			case "Boolean" -> Types.BOOLEAN;
			case "Bytes" -> Types.BLOB;
			case "String" -> Types.VARCHAR;
			case "Integer", "Long" -> Types.INTEGER;
			case "Float", "Double" -> Types.FLOAT;
			case "StringArray", "DoubleArray", "LongArray" -> Types.ARRAY;
			case "Map", "Point", "Path", "Relationship", "Node" -> Types.STRUCT;
			case "Date", "DateTime" -> Types.DATE;
			case "Time", "LocalDateTime", "LocalTime" -> Types.TIME;
			case "Duration" -> Types.TIMESTAMP;
			case "Null" -> Types.NULL;
			default -> Types.OTHER;
		};
	}

	/*
	 * This is required because of a bug in 5 where java types are returned from the
	 * db.schema.nodeTypeProperties() procedure In 6 this will change so will need to add
	 * another method to handle the new types returned here when working with 6
	 */
	public static String oldCypherTypesToNew(String neo4jType) throws SQLException {
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

	public static int toSqlType(Type neo4jType) throws SQLException {
		switch (neo4jType) {
			case ANY -> {
				return Types.OTHER;
			}
			case BOOLEAN -> {
				return Types.BOOLEAN;
			}
			case BYTES -> {
				return Types.BLOB;
			}
			case STRING -> {
				return Types.VARCHAR;
			}
			case NUMBER -> {
				return Types.BIGINT;
			}
			case INTEGER -> {
				return Types.INTEGER;
			}
			case FLOAT -> {
				return Types.FLOAT;
			}
			case LIST -> {
				return Types.ARRAY;
			}
			case MAP, POINT, PATH, RELATIONSHIP, NODE -> {
				return Types.STRUCT;
			}
			case DATE, DATE_TIME -> {
				return Types.DATE;
			}
			case TIME, LOCAL_DATE_TIME, LOCAL_TIME -> {
				return Types.TIME;
			}
			case DURATION -> {
				return Types.TIMESTAMP;
			}
			case NULL -> {
				return Types.NULL;
			}
		}

		throw new SQLException("Unknown type");
	}

}
