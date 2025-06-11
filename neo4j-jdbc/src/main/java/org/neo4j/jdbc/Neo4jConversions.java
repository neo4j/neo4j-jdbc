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
package org.neo4j.jdbc;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.neo4j.jdbc.Neo4jException.GQLError;
import org.neo4j.jdbc.values.Type;
import org.neo4j.jdbc.values.Value;
import org.neo4j.jdbc.values.Values;

/**
 * Various mappings from old to new Cypher types and Cypher types to JDBC types,
 * additionally some more complex conversions and mappings.
 *
 * @author Neo4j Drivers Team
 * @since 6.0.0
 */
final class Neo4jConversions {

	private Neo4jConversions() {
	}

	/*
	 * This is required because of a bug in Neo4j 5 where java types are returned from the
	 * db.schema.nodeTypeProperties() procedure in Neo4j 6 this will change so will need
	 * to add another method to handle the new types returned here when working with Neo4j
	 * 6
	 */
	static int toSqlTypeFromOldCypherType(String neo4jType) {
		return toSqlType(valueOfV5Name(neo4jType));
	}

	/*
	 * This is required because of a bug in Neo4j 5 where java types are returned from the
	 * db.schema.nodeTypeProperties() procedure in Neo4j 6 this will change so will need
	 * to add another method to handle the new types returned here when working with Neo4j
	 * 6
	 */
	static String oldCypherTypesToNew(String neo4jType) {
		String value = neo4jType;
		if (value.startsWith("LIST<")) {
			value = "LIST";
		}
		value = value.replaceAll("( NOT)? NULL", "");
		return switch (value) {
			// Simple
			case "Boolean" -> "BOOLEAN";
			case "Double" -> "FLOAT";
			case "Long" -> "INTEGER";
			case "String" -> "STRING";
			// Structs
			case "Point" -> "POINT";

			// Lists no way to get better mapping so loose info here
			case "StringArray", "ByteArray", "DoubleArray", "LongArray" -> "LIST";
			// Temporal
			case "Date" -> "DATE";
			case "Duration" -> "DURATION";
			case "DateTime" -> "ZONED DATETIME";
			case "Time" -> "ZONED TIME";
			case "LocalDateTime" -> "LOCAL DATETIME";
			case "LocalTime" -> "LOCAL TIME";
			// Not really types but kinda are
			case "Null" -> "NULL";
			// this is not a cypher type but needs to be represented for jdbc
			case "Any" -> "ANY";
			default -> {
				try {
					yield valueOfV5Name(neo4jType).name();
				}
				catch (IllegalArgumentException ex) {
					yield "OTHER";
				}
			}
		};
	}

	static int toSqlType(Type neo4jType) {
		return switch (neo4jType) {
			case ANY, DURATION -> Types.OTHER;
			case BOOLEAN -> Types.BOOLEAN;
			case BYTES -> Types.BLOB;
			case STRING -> Types.VARCHAR;
			case NUMBER, INTEGER -> Types.BIGINT;
			case FLOAT -> Types.DOUBLE;
			case LIST -> Types.ARRAY;
			case MAP, POINT, PATH, RELATIONSHIP, NODE -> Types.STRUCT;
			case DATE -> Types.DATE;
			case TIME -> Types.TIME;
			case DATE_TIME, LOCAL_DATE_TIME, LOCAL_TIME -> Types.TIMESTAMP;
			case NULL -> Types.NULL;
		};
	}

	static Type valueOfV5Name(String in) {
		// See (2025-06-11)
		// https://neo4j.com/docs/cypher-manual/current/values-and-types/property-structural-constructed/
		var value = in.replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2")
			.replaceAll("([a-z])([A-Z])", "$1_$2")
			.toUpperCase(Locale.ROOT);
		if (value.startsWith("LIST<")) {
			value = "LIST";
		}
		value = value.replaceAll("( NOT)? NULL", "");
		value = switch (value) {
			case "LONG" -> Type.INTEGER.name();
			case "DOUBLE" -> Type.FLOAT.name();
			// Translation of Cypher 25 dates
			case "LOCAL TIME" -> Type.LOCAL_TIME.name();
			case "LOCAL DATETIME" -> Type.LOCAL_DATE_TIME.name();
			case "ZONED TIME" -> Type.TIME.name();
			case "ZONED DATETIME" -> Type.DATE_TIME.name();
			default -> value.endsWith("ARRAY") ? Type.LIST.name() : value;
		};
		return Type.valueOf(value);
	}

	private static ZoneOffset getZoneOffsetFrom(Calendar cal) {
		return Objects.requireNonNullElseGet(cal, Calendar::getInstance)
			.getTimeZone()
			.toZoneId()
			.getRules()
			.getOffset(cal.toInstant());
	}

	static Value asValue(Time time, Calendar calendar) {
		if (time == null) {
			return Values.NULL;
		}

		var offsetTime = time.toLocalTime().atOffset(getZoneOffsetFrom(calendar));
		return Values.value(offsetTime);
	}

	static Time asTime(Value value) throws SQLException {
		if (value == null || Type.NULL.isTypeOf(value)) {
			return null;
		}

		if (Type.TIME.isTypeOf(value)) {
			return Time.valueOf(value.asOffsetTime().toLocalTime());
		}
		if (Type.LOCAL_TIME.isTypeOf(value)) {
			return Time.valueOf(value.asLocalTime());
		}
		if (Type.DATE_TIME.isTypeOf(value)) {
			return Time.valueOf(value.asZonedDateTime().toLocalTime());
		}
		if (Type.LOCAL_DATE_TIME.isTypeOf(value)) {
			return Time.valueOf(value.asLocalDateTime().toLocalTime());
		}
		throw new Neo4jException(GQLError.$22N37.withTemplatedMessage(value.toDisplayString(), "java.sql.Time"));
	}

	static Time asTime(Value value, Calendar calendar) throws SQLException {
		if (value == null || Type.NULL.isTypeOf(value)) {
			return null;
		}

		OffsetTime offsetTime;
		var targetOffset = getZoneOffsetFrom(calendar);
		if (Type.TIME.isTypeOf(value)) {
			offsetTime = value.asOffsetTime().withOffsetSameInstant(targetOffset);
		}
		else if (Type.LOCAL_TIME.isTypeOf(value)) {
			offsetTime = value.asLocalTime().atOffset(targetOffset);
		}
		else if (Type.DATE_TIME.isTypeOf(value)) {
			offsetTime = value.asZonedDateTime().toOffsetDateTime().withOffsetSameInstant(targetOffset).toOffsetTime();
		}
		else if (Type.LOCAL_DATE_TIME.isTypeOf(value)) {
			offsetTime = value.asLocalDateTime().toLocalTime().atOffset(targetOffset);
		}
		else {
			throw new Neo4jException(GQLError.$22N37.withTemplatedMessage(value.toDisplayString(), "java.sql.Time"));
		}
		return Time.valueOf(offsetTime.toLocalTime());
	}

	static Value asValue(Timestamp timestamp, Calendar calendar) {
		if (timestamp == null) {
			return Values.NULL;
		}

		calendar = Objects.requireNonNullElseGet(calendar, Calendar::getInstance);
		var zonedDateTime = timestamp.toLocalDateTime().atZone(calendar.getTimeZone().toZoneId());
		return Values.value(zonedDateTime);
	}

	static Timestamp asTimestamp(Value value) throws SQLException {
		if (value == null || Type.NULL.isTypeOf(value)) {
			return null;
		}

		if (Type.DATE_TIME.isTypeOf(value)) {
			return Timestamp.valueOf(value.asZonedDateTime().toLocalDateTime());
		}
		if (Type.LOCAL_DATE_TIME.isTypeOf(value)) {
			return Timestamp.valueOf(value.asLocalDateTime());
		}
		throw new Neo4jException(GQLError.$22N37.withTemplatedMessage(value.toDisplayString(), "java.sql.Timestamp"));
	}

	static Timestamp asTimestamp(Value value, Calendar calendar) throws SQLException {
		if (value == null || Type.NULL.isTypeOf(value)) {
			return null;
		}

		ZonedDateTime hlp;
		var zonedDateTime = calendar.getTimeZone().toZoneId();
		if (Type.DATE_TIME.isTypeOf(value)) {
			hlp = value.asZonedDateTime().withZoneSameInstant(zonedDateTime);
		}
		else if (Type.LOCAL_DATE_TIME.isTypeOf(value)) {
			hlp = value.asLocalDateTime().atZone(zonedDateTime);
		}
		else {
			throw new Neo4jException(
					GQLError.$22N37.withTemplatedMessage(value.toDisplayString(), "java.sql.Timestamp"));
		}
		return Timestamp.valueOf(hlp.toLocalDateTime());
	}

	static Value asValue(Date date, Calendar calendar) {
		if (date == null) {
			return Values.NULL;
		}

		calendar = Objects.requireNonNullElseGet(calendar, Calendar::getInstance);
		var zonedDateTime = date.toLocalDate().atStartOfDay(calendar.getTimeZone().toZoneId());
		return Values.value(zonedDateTime);
	}

	static Date asDate(Value value) throws SQLException {
		if (value == null || Type.NULL.isTypeOf(value)) {
			return null;
		}

		if (Type.DATE.isTypeOf(value)) {
			return Date.valueOf(value.asLocalDate());
		}
		if (Type.DATE_TIME.isTypeOf(value)) {
			return Date.valueOf(value.asZonedDateTime().toLocalDate());
		}
		if (Type.LOCAL_DATE_TIME.isTypeOf(value)) {
			return Date.valueOf(value.asLocalDateTime().toLocalDate());
		}
		throw new Neo4jException(GQLError.$22N37.withTemplatedMessage(value.toDisplayString(), "java.sql.Date"));
	}

	static Date asDate(Value value, Calendar calendar) throws SQLException {
		if (value == null || Type.NULL.isTypeOf(value)) {
			return null;
		}

		ZonedDateTime zonedDateTime;
		var targetZone = calendar.getTimeZone().toZoneId();
		if (Type.DATE.isTypeOf(value)) {
			zonedDateTime = value.asLocalDate().atStartOfDay(targetZone);
		}
		else if (Type.DATE_TIME.isTypeOf(value)) {
			zonedDateTime = value.asZonedDateTime().withZoneSameInstant(targetZone);
		}
		else if (Type.LOCAL_DATE_TIME.isTypeOf(value)) {
			zonedDateTime = value.asLocalDateTime().atZone(targetZone);
		}
		else {
			throw new Neo4jException(GQLError.$22N37.withTemplatedMessage(value.toDisplayString(), "java.sql.Date"));
		}
		return Date.valueOf(zonedDateTime.toLocalDate());
	}

	/**
	 * Asserts that the type map is supported. As of 6.4.0 will always throw an exception
	 * on a non-null and not-empty map.
	 * @param map the map to assert
	 * @throws SQLException with SQL state 22N11 if the map is a non-empty map
	 */
	static void assertTypeMap(Map<String, Class<?>> map) throws SQLException {
		if (!(map == null || map.isEmpty())) {
			var mapValue = map.entrySet()
				.stream()
				.sorted(Map.Entry.comparingByKey())
				.map(e -> "%s = %s".formatted(e.getKey(), e.getValue()))
				.collect(Collectors.joining(", "));
			throw new Neo4jException(GQLError.$22N11.withTemplatedMessage("non-empty type map %s".formatted(mapValue)));
		}
	}

}
