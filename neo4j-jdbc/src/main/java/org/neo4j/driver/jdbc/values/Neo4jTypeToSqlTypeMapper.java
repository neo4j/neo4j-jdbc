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
			case MAP -> {
				return Types.STRUCT;
			}
			case NODE -> {
				return Types.STRUCT;
			}
			case RELATIONSHIP -> {
				return Types.STRUCT;
			}
			case PATH -> {
				return Types.STRUCT;
			}
			case POINT -> {
				return Types.STRUCT;
			}
			case DATE -> {
				return Types.DATE;
			}
			case TIME -> {
				return Types.TIME;
			}
			case LOCAL_TIME -> {
				return Types.TIME;
			}
			case LOCAL_DATE_TIME -> {
				return Types.TIME;
			}
			case DATE_TIME -> {
				return Types.DATE;
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
