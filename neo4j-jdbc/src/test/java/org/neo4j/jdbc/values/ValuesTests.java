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
package org.neo4j.jdbc.values;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class ValuesTests {

	static Stream<Arguments> shouldConvertToCorrectValueClass() {
		return Stream.of(Arguments.of('c', StringValue.class), Arguments.of(new int[] { 1 }, ListValue.class),
				Arguments.of(Vector.of(new int[] { 1 }), VectorValue.class));
	}

	@ParameterizedTest
	@MethodSource
	void shouldConvertToCorrectValueClass(Object input, Class<?> expected) {
		assertThat(Values.value(input)).isInstanceOf(expected);
	}

}
