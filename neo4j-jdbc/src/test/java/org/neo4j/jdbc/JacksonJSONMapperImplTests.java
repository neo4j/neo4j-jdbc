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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BigIntegerNode;
import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.FloatNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.neo4j.jdbc.values.Value;
import org.neo4j.jdbc.values.Values;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JacksonJSONMapperImplTests {

	private final JacksonJSONMapperImpl mapper = new JacksonJSONMapperImpl();

	static Stream<Arguments> toJsonShouldWork() {
		return Stream.of(Arguments.of(Values.value(true), BooleanNode.valueOf(true)),
				Arguments.of(Values.value(new byte[] { -54, -2, -70, -66 }), TextNode.valueOf("yv66vg==")),
				Arguments.of(Values
					.value(ZonedDateTime.of(LocalDate.of(2025, 7, 3), LocalTime.of(9, 56, 0), ZoneOffset.ofHours(2))),
						TextNode.valueOf("2025-07-03T09:56:00+02:00")),
				Arguments.of(
						Values.value(ZonedDateTime.of(LocalDate.of(2025, 7, 3), LocalTime.of(9, 56, 0),
								ZoneId.of("Europe/Berlin"))),
						TextNode.valueOf("2025-07-03T09:56:00+02:00[Europe/Berlin]")),
				Arguments.of(Values.value(LocalDate.of(2025, 7, 3)), TextNode.valueOf("2025-07-03")),
				Arguments.of(Values.value(Duration.ofHours(23).plusMinutes(2).plusSeconds(1)),
						TextNode.valueOf("PT23H2M1S")),
				Arguments.of(Values.value(Period.ofMonths(1)), TextNode.valueOf("P1M")),
				Arguments.of(Values.value(1.23), DoubleNode.valueOf(1.23)),
				Arguments.of(Values.value(666), LongNode.valueOf(666)),
				Arguments.of(Values.value(LocalDateTime.of(LocalDate.of(2025, 7, 3), LocalTime.of(9, 56, 0))),
						TextNode.valueOf("2025-07-03T09:56:00")),
				Arguments.of(Values.value(LocalTime.of(9, 56, 0)), TextNode.valueOf("09:56:00")),
				Arguments.of(null, NullNode.getInstance()), Arguments.of(Values.NULL, NullNode.getInstance()),
				Arguments.of(Values.point(4326, 56.7, 12.78), TextNode.valueOf("SRID=4326;POINT (56.7 12.78)")),
				Arguments.of(Values.point(9157, 2.3, 4.5, 2.0), TextNode.valueOf("SRID=9157;POINT Z (2.3 4.5 2.0)")),
				Arguments.of(Values.value("Hallo"), TextNode.valueOf("Hallo")),
				Arguments.of(Values.value(OffsetTime.of(LocalTime.of(11, 0), ZoneOffset.UTC)),
						TextNode.valueOf("11:00:00Z")));
	}

	@ParameterizedTest
	@MethodSource
	void toJsonShouldWork(Value in, JsonNode out) {
		assertThat(this.mapper.toJson(in)).isEqualTo(out);
	}

	@Test
	void toJsonShouldThrowMeaningfulErrorWhenUnsupported() {
		var unsupportedValue = Mockito.mock(Value.class);

		given(unsupportedValue.toString()).willReturn("whatever");
		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> this.mapper.toJson(unsupportedValue))
			.withMessage("Cannot map whatever to a JsonNode");
	}

	static Stream<Arguments> fromJsonShouldWork() {
		var om = new ObjectMapper();
		var on = om.createObjectNode();
		on.put("i", 1);
		on.put("bd", BigDecimal.TEN);
		on.put("d", 47.11);

		var an = om.createArrayNode();
		an.add(1);
		an.add(2);
		an.add(3);
		on.set("l1", an);

		an = om.createArrayNode();
		var on2 = om.createObjectNode();
		on2.put("s", "Hello");
		an.add(on2);
		on.set("l2", an);

		return Stream.of(Arguments.of(null, Values.NULL), Arguments.of(NullNode.getInstance(), Values.NULL),
				Arguments.of(BooleanNode.getTrue(), Values.value(true)),
				Arguments.of(DecimalNode.valueOf(BigDecimal.ONE), Values.value("1")),
				Arguments.of(BigIntegerNode.valueOf(BigInteger.ONE), Values.value(1L)),
				Arguments.of(DoubleNode.valueOf(47.11), Values.value(47.11)),
				Arguments.of(FloatNode.valueOf(2.2f), Values.value(2.2f)),
				Arguments.of(LongNode.valueOf(1979), Values.value(1979)),
				Arguments.of(IntNode.valueOf(1979), Values.value(1979)),
				Arguments.of(TextNode.valueOf("Guten Tag"), Values.value("Guten Tag")),
				Arguments.of(on,
						Values.value(Map.of("i", Values.value(1), "bd", Values.value("10"), "d", Values.value(47.11),
								"l1", Values.value(1, 2, 3), "l2",
								Values.value(List.of(Values.value(Map.of("s", Values.value("Hello")))))))),
				Arguments.of(BinaryNode.valueOf(new byte[] { -54, -2, -70, -66 }), Values.value("yv66vg==")));
	}

	@ParameterizedTest
	@MethodSource
	void fromJsonShouldWork(JsonNode in, Value out) {
		assertThat(this.mapper.fromJson(in)).isEqualTo(out);
	}

	@Test
	void fromJsonShouldThrowMeaningfulErrorWhenUnsupportedNode() {
		var unsupportedValue = Mockito.mock(JsonNode.class);

		given(unsupportedValue.toString()).willReturn("whatever");
		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> this.mapper.fromJson(unsupportedValue))
			.withMessage("Cannot map whatever to a Value");
	}

	@Test
	void fromJsonShouldThrowMeaningfulErrorWhenUnsupportedType() {
		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> this.mapper.fromJson("unsupportedValue"))
			.withMessage("Cannot map objects of type java.lang.String to JsonNode");
	}

}
