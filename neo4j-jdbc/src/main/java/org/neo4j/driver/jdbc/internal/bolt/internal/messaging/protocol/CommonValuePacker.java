/*
 * Copyright (c) 2023 "Neo4j,"
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
package org.neo4j.driver.jdbc.internal.bolt.internal.messaging.protocol;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;

import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.ValuePacker;
import org.neo4j.driver.jdbc.internal.bolt.internal.packstream.PackOutput;
import org.neo4j.driver.jdbc.internal.bolt.internal.packstream.PackStream;
import org.neo4j.driver.jdbc.internal.bolt.values.IsoDuration;
import org.neo4j.driver.jdbc.internal.bolt.values.Point;
import org.neo4j.driver.jdbc.internal.bolt.values.Value;

class CommonValuePacker implements ValuePacker {

	public static final byte DATE = 'D';

	public static final int DATE_STRUCT_SIZE = 1;

	public static final byte TIME = 'T';

	public static final int TIME_STRUCT_SIZE = 2;

	public static final byte LOCAL_TIME = 't';

	public static final int LOCAL_TIME_STRUCT_SIZE = 1;

	public static final byte LOCAL_DATE_TIME = 'd';

	public static final int LOCAL_DATE_TIME_STRUCT_SIZE = 2;

	public static final byte DATE_TIME_WITH_ZONE_OFFSET = 'F';

	public static final byte DATE_TIME_WITH_ZONE_OFFSET_UTC = 'I';

	public static final byte DATE_TIME_WITH_ZONE_ID = 'f';

	public static final byte DATE_TIME_WITH_ZONE_ID_UTC = 'i';

	public static final int DATE_TIME_STRUCT_SIZE = 3;

	public static final byte DURATION = 'E';

	public static final int DURATION_TIME_STRUCT_SIZE = 4;

	public static final byte POINT_2D_STRUCT_TYPE = 'X';

	public static final int POINT_2D_STRUCT_SIZE = 3;

	public static final byte POINT_3D_STRUCT_TYPE = 'Y';

	public static final int POINT_3D_STRUCT_SIZE = 4;

	private final boolean dateTimeUtcEnabled;

	protected final PackStream.Packer packer;

	CommonValuePacker(PackOutput output, boolean dateTimeUtcEnabled) {
		this.dateTimeUtcEnabled = dateTimeUtcEnabled;
		this.packer = new PackStream.Packer(output);
	}

	@Override
	public final void packStructHeader(int size, byte signature) throws IOException {
		this.packer.packStructHeader(size, signature);
	}

	@Override
	public final void pack(String string) throws IOException {
		this.packer.pack(string);
	}

	@Override
	public final void pack(Value value) throws IOException {
		switch (value.type()) {
			case DATE -> packDate(value.asLocalDate());
			case TIME -> packTime(value.asOffsetTime());
			case LOCAL_TIME -> packLocalTime(value.asLocalTime());
			case LOCAL_DATE_TIME -> packLocalDateTime(value.asLocalDateTime());
			case DATE_TIME -> {
				if (this.dateTimeUtcEnabled) {
					packZonedDateTimeUsingUtcBaseline(value.asZonedDateTime());
				}
				else {
					packZonedDateTime(value.asZonedDateTime());
				}
			}
			case DURATION -> packDuration(value.asIsoDuration());
			case POINT -> packPoint(value.asPoint());
			case NULL -> this.packer.packNull();
			case BYTES -> this.packer.pack(value.asByteArray());
			case STRING -> this.packer.pack(value.asString());
			case BOOLEAN -> this.packer.pack(value.asBoolean());
			case INTEGER -> this.packer.pack(value.asLong());
			case FLOAT -> this.packer.pack(value.asDouble());
			case MAP -> {
				this.packer.packMapHeader(value.size());
				for (var s : value.keys()) {
					this.packer.pack(s);
					pack(value.get(s));
				}
			}
			case LIST -> {
				this.packer.packListHeader(value.size());
				for (var item : value.values()) {
					pack(item);
				}
			}
			default -> throw new IOException("Unknown type: " + value.type().name());
		}
	}

	@Override
	public final void pack(Map<String, Value> map) throws IOException {
		if (map == null || map.isEmpty()) {
			this.packer.packMapHeader(0);
			return;
		}
		this.packer.packMapHeader(map.size());
		for (var entry : map.entrySet()) {
			this.packer.pack(entry.getKey());
			pack(entry.getValue());
		}
	}

	private void packDate(LocalDate localDate) throws IOException {
		this.packer.packStructHeader(DATE_STRUCT_SIZE, DATE);
		this.packer.pack(localDate.toEpochDay());
	}

	private void packTime(OffsetTime offsetTime) throws IOException {
		var nanoOfDayLocal = offsetTime.toLocalTime().toNanoOfDay();
		var offsetSeconds = offsetTime.getOffset().getTotalSeconds();

		this.packer.packStructHeader(TIME_STRUCT_SIZE, TIME);
		this.packer.pack(nanoOfDayLocal);
		this.packer.pack(offsetSeconds);
	}

	private void packLocalTime(LocalTime localTime) throws IOException {
		this.packer.packStructHeader(LOCAL_TIME_STRUCT_SIZE, LOCAL_TIME);
		this.packer.pack(localTime.toNanoOfDay());
	}

	private void packLocalDateTime(LocalDateTime localDateTime) throws IOException {
		var epochSecondUtc = localDateTime.toEpochSecond(ZoneOffset.UTC);
		var nano = localDateTime.getNano();

		this.packer.packStructHeader(LOCAL_DATE_TIME_STRUCT_SIZE, LOCAL_DATE_TIME);
		this.packer.pack(epochSecondUtc);
		this.packer.pack(nano);
	}

	private void packZonedDateTimeUsingUtcBaseline(ZonedDateTime zonedDateTime) throws IOException {
		var instant = zonedDateTime.toInstant();
		var epochSecondLocal = instant.getEpochSecond();
		var nano = zonedDateTime.getNano();
		var zone = zonedDateTime.getZone();

		if (zone instanceof ZoneOffset) {
			var offsetSeconds = ((ZoneOffset) zone).getTotalSeconds();

			this.packer.packStructHeader(DATE_TIME_STRUCT_SIZE, DATE_TIME_WITH_ZONE_OFFSET_UTC);
			this.packer.pack(epochSecondLocal);
			this.packer.pack(nano);
			this.packer.pack(offsetSeconds);
		}
		else {
			var zoneId = zone.getId();

			this.packer.packStructHeader(DATE_TIME_STRUCT_SIZE, DATE_TIME_WITH_ZONE_ID_UTC);
			this.packer.pack(epochSecondLocal);
			this.packer.pack(nano);
			this.packer.pack(zoneId);
		}
	}

	private void packZonedDateTime(ZonedDateTime zonedDateTime) throws IOException {
		var epochSecondLocal = zonedDateTime.toLocalDateTime().toEpochSecond(ZoneOffset.UTC);
		var nano = zonedDateTime.getNano();

		var zone = zonedDateTime.getZone();
		if (zone instanceof ZoneOffset) {
			var offsetSeconds = ((ZoneOffset) zone).getTotalSeconds();

			this.packer.packStructHeader(DATE_TIME_STRUCT_SIZE, DATE_TIME_WITH_ZONE_OFFSET);
			this.packer.pack(epochSecondLocal);
			this.packer.pack(nano);
			this.packer.pack(offsetSeconds);
		}
		else {
			var zoneId = zone.getId();

			this.packer.packStructHeader(DATE_TIME_STRUCT_SIZE, DATE_TIME_WITH_ZONE_ID);
			this.packer.pack(epochSecondLocal);
			this.packer.pack(nano);
			this.packer.pack(zoneId);
		}
	}

	private void packDuration(IsoDuration duration) throws IOException {
		this.packer.packStructHeader(DURATION_TIME_STRUCT_SIZE, DURATION);
		this.packer.pack(duration.months());
		this.packer.pack(duration.days());
		this.packer.pack(duration.seconds());
		this.packer.pack(duration.nanoseconds());
	}

	private void packPoint(Point point) throws IOException {
		if (Double.isNaN(point.z())) {
			packPoint2D(point);
		}
		else {
			packPoint3D(point);
		}
	}

	private void packPoint2D(Point point) throws IOException {
		this.packer.packStructHeader(POINT_2D_STRUCT_SIZE, POINT_2D_STRUCT_TYPE);
		this.packer.pack(point.srid());
		this.packer.pack(point.x());
		this.packer.pack(point.y());
	}

	private void packPoint3D(Point point) throws IOException {
		this.packer.packStructHeader(POINT_3D_STRUCT_SIZE, POINT_3D_STRUCT_TYPE);
		this.packer.pack(point.srid());
		this.packer.pack(point.x());
		this.packer.pack(point.y());
		this.packer.pack(point.z());
	}

}
