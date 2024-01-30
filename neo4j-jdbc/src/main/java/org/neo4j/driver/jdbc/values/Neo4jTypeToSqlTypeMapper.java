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
	public static int toSqlTypeFromOldCypherType(String neo4jType) throws SQLException {
		switch (neo4jType) {
			case "Any" -> {
				return Types.OTHER;
			}
			case "Boolean" -> {
				return Types.BOOLEAN;
			}
			case "Bytes" -> {
				return Types.BLOB;
			}
			case "String" -> {
				return Types.VARCHAR;
			}
			case "Integer", "Long" -> {
				return Types.INTEGER;
			}
			case "Float", "Double" -> {
				return Types.FLOAT;
			}
			case "StringArray", "DoubleArray", "LongArray" -> {
				return Types.ARRAY;
			}
			case "Map", "Point", "Path", "Relationship", "Node" -> {
				return Types.STRUCT;
			}
			case "Date", "DateTime" -> {
				return Types.DATE;
			}
			case "Time", "LocalDateTime", "LocalTime" -> {
				return Types.TIME;
			}
			case "Duration" -> {
				return Types.TIMESTAMP;
			}
			case "Null" -> {
				return Types.NULL;
			}
		}

		throw new SQLException("Unknown type");
	}

	/*
	 * This is required because of a bug in 5 where java types are returned from the
	 * db.schema.nodeTypeProperties() procedure In 6 this will change so will need to add
	 * another method to handle the new types returned here when working with 6
	 */
	public static String oldCypherTypesToNew(String neo4jType) throws SQLException {
		switch (neo4jType) {
			// Simple
			case "Boolean" -> {
				return "BOOLEAN";
			}
			case "Double" -> {
				return "FLOAT";
			}
			case "Long" -> {
				return "INTEGER";
			}
			case "String" -> {
				return "STRING";
			}
			// Structs
			case "Point" -> {
				return "POINT";
			}

			// Lists no way to get better mapping so loose info here
			case "StringArray", "DoubleArray", "LongArray" -> {
				return "LIST";
			}
			// Temporal
			case "Date" -> {
				return "DATE";
			}
			case "Duration" -> {
				return "DURATION";
			}
			case "DateTime" -> {
				return "ZONED DATETIME";
			}
			case "Time" -> {
				return "ZONED TIME";
			}
			case "LocalDateTime" -> {
				return "LOCAL DATETIME";
			}
			case "LocalTime" -> {
				return "LOCAL TIME";
			}
			// Not really types but kinda are
			case "Null" -> {
				return "NULL";
			}
			case "Any" -> {
				return "ANY"; // this is not a cypher type but needs to be represented for
								// jdbc
			}
		}

		throw new SQLException("Unknown type");
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
