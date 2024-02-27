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
package org.neo4j.jdbc.values;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class TypeTests {

	static Stream<Arguments> shouldParseNames() {
		return Stream.of(Arguments.of("Any", Type.ANY), Arguments.of("Boolean", Type.BOOLEAN),
				Arguments.of("Bytes", Type.BYTES), Arguments.of("String", Type.STRING),
				Arguments.of("Integer", Type.INTEGER), Arguments.of("Long", Type.INTEGER),
				Arguments.of("Float", Type.FLOAT), Arguments.of("Double", Type.FLOAT),
				Arguments.of("StringArray", Type.LIST), Arguments.of("DoubleArray", Type.LIST),
				Arguments.of("LongArray", Type.LIST), Arguments.of("Map", Type.MAP), Arguments.of("Point", Type.POINT),
				Arguments.of("Path", Type.PATH), Arguments.of("Relationship", Type.RELATIONSHIP),
				Arguments.of("Node", Type.NODE), Arguments.of("Date", Type.DATE),
				Arguments.of("DATE_TIME", Type.DATE_TIME), Arguments.of("Time", Type.TIME),
				Arguments.of("LocalDateTime", Type.LOCAL_DATE_TIME), Arguments.of("LocalTime", Type.LOCAL_TIME),
				Arguments.of("Duration", Type.DURATION), Arguments.of("Null", Type.NULL));
	}

	@ParameterizedTest
	@MethodSource
	void shouldParseNames(String in, Type expected) {
		var type = Type.valueOfV5Name(in);
		assertThat(type).isEqualTo(expected);
	}

}
