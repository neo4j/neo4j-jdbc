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

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;

class GQLConformanceTests {

	static DynamicContainer newContainer(String name, Holder... holder) {
		return DynamicContainer.dynamicContainer(name,
				DynamicTest.stream(Stream.of(holder), Holder::displayName, Holder::assertToString));
	}

	@TestFactory
	@DisplayName("toString methods should produce valid GQL literals")
	Stream<DynamicContainer> toStringShouldWork() {

		return Stream.of(
				newContainer("Boolean values", new Holder(BooleanValue.TRUE, "TRUE"),
						new Holder(BooleanValue.FALSE, "FALSE")),
				newContainer("Null", new Holder(NullValue.NULL, "NULL")),
				newContainer("Bytes",
						new Holder(Values.value("Neo4j".getBytes(StandardCharsets.UTF_8)), "X'4E656F346A'")),
				newContainer("Temporals", new Holder(Values.value(LocalDate.of(1979, 9, 21)), "DATE '1979-09-21'"),
						new Holder(Values.value(OffsetTime.of(LocalTime.of(21, 21, 0), ZoneOffset.of("-01:00"))),
								"TIME '21:21:00-01:00'"),
						new Holder(Values.value(LocalTime.of(21, 21, 0)), "TIME '21:21:00'"),
						new Holder(Values.value(LocalDateTime.of(2024, 1, 3, 15, 52)),
								"DATETIME '2024-01-03T15:52:00'"),
						new Holder(Values
							.value(OffsetDateTime.of(LocalDateTime.of(2024, 1, 3, 15, 52), ZoneOffset.of("-05:00"))),
								"DATETIME '2024-01-03T15:52:00-05:00'"),
						new Holder(Values
							.value(ZonedDateTime.of(LocalDateTime.of(2024, 1, 3, 15, 52), ZoneId.of("Europe/Berlin"))),
								"DATETIME '2024-01-03T15:52:00+01:00'")),
				newContainer("Duration and periods",
						new Holder(Values.value(Duration.ofDays(23).plusHours(23)), "DURATION 'PT575H'"),
						new Holder(Values.value(Period.ofMonths(3).plusDays(1)), "P3M1D")),
				newContainer("Collections", new Holder(Values.value(List.of(1, 2, 3, 4)), "[1, 2, 3, 4]")),
				newContainer("Strings", new Holder(Values.value("test"), "\"test\"")));
	}

	record Holder(Value value, String expected) {
		void assertToString() {
			assertThat(this.value).hasToString(this.expected);
		}

		String displayName() {
			return "%s with value %s has toString '%s'".formatted(this.value.getClass().getSimpleName(),
					this.value.asObject(), this.expected);
		}

	}

}
