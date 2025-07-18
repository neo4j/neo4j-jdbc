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

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.jdbc.values.VectorValue.Int64VectorValue;

class VectorValueTest {

	static Stream<Arguments> toArrayShouldWork() {
		return Stream.of(Arguments.of(VectorValue.int64(new long[] { 1L, 2L, 3L }), new long[] { 1L, 2L, 3L }));
	}

	@ParameterizedTest
	@MethodSource
	void toArrayShouldWork(VectorValue vectorValue, Object expected) {
		if (vectorValue instanceof Int64VectorValue int64VectorValue) {
			System.out.println(Objects.deepEquals(int64VectorValue.toLongArray(), expected));
		}
	}

	@Test
	void toLo() {
		var vectorValue = VectorValue.int64(new long[] { 1L, 2L, 3L });
		if (vectorValue instanceof Int64VectorValue int64VectorValue) {
			System.out.println(Arrays.toString(int64VectorValue.toLongArray()));

		}

	}

}