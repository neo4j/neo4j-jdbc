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
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class ArrayBasedVectorsTests {

	static Stream<Arguments> hasToString() {

		var longDoubleArray = ThreadLocalRandom.current().doubles(4096).toArray();
		var doubles = Arrays.stream(longDoubleArray).mapToObj(Double::toString).collect(Collectors.joining(", "));

		return Stream.of(Arguments.of(Vector.int8(new byte[1]), "VECTOR([0], 1, INTEGER8 NOT NULL)"),
				Arguments.of(Vector.int16(new short[2]), "VECTOR([0, 0], 2, INTEGER16 NOT NULL)"),
				Arguments.of(Vector.int32(new int[3]), "VECTOR([0, 0, 0], 3, INTEGER32 NOT NULL)"),
				Arguments.of(Vector.int64(new long[4]), "VECTOR([0, 0, 0, 0], 4, INTEGER NOT NULL)"),
				Arguments.of(Vector.float32(new float[5]), "VECTOR([0.0, 0.0, 0.0, 0.0, 0.0], 5, FLOAT32 NOT NULL)"),
				Arguments.of(Vector.float64(new double[6]),
						"VECTOR([0.0, 0.0, 0.0, 0.0, 0.0, 0.0], 6, FLOAT NOT NULL)"),
				Arguments.of(Vector.float64(longDoubleArray), "VECTOR([%s], 4096, FLOAT NOT NULL)".formatted(doubles))

		);
	}

	@ParameterizedTest
	@MethodSource
	void hasToString(Vector vector, String expected) {
		assertThat(vector).hasToString(expected);
	}

	static Stream<Arguments> nullCheckShouldWork() {
		return Stream.of(Arguments.of((Supplier<Vector>) () -> Vector.int8(null)),
				Arguments.of((Supplier<Vector>) () -> Vector.int16(null)),
				Arguments.of((Supplier<Vector>) () -> Vector.int32(null)),
				Arguments.of((Supplier<Vector>) () -> Vector.int64(null)),
				Arguments.of((Supplier<Vector>) () -> Vector.float32(null)),
				Arguments.of((Supplier<Vector>) () -> Vector.float64(null)));
	}

	@ParameterizedTest
	@MethodSource
	void nullCheckShouldWork(Supplier<Vector> factory) {
		assertThatNullPointerException().isThrownBy(factory::get)
			.withMessage("Vector elements must not be literal null");
	}

	static Stream<Arguments> rangeCheckShouldWorkLower() {
		return Stream.of(Arguments.of((Supplier<Vector>) () -> Vector.int8(new byte[0])),
				Arguments.of((Supplier<Vector>) () -> Vector.int16(new short[0])),
				Arguments.of((Supplier<Vector>) () -> Vector.int32(new int[0])),
				Arguments.of((Supplier<Vector>) () -> Vector.int64(new long[0])),
				Arguments.of((Supplier<Vector>) () -> Vector.float32(new float[0])),
				Arguments.of((Supplier<Vector>) () -> Vector.float64(new double[0])));
	}

	@ParameterizedTest
	@MethodSource
	void rangeCheckShouldWorkLower(Supplier<Vector> factory) {
		assertThatIllegalArgumentException().isThrownBy(factory::get)
			.withMessage("'0' is not a valid value. Must be a number in the range 1 to 4096 (GQL 42N31)");
	}

	static Stream<Arguments> rangeCheckShouldWorkUpper() {
		return Stream.of(Arguments.of((Supplier<Vector>) () -> Vector.int8(new byte[4097])),
				Arguments.of((Supplier<Vector>) () -> Vector.int16(new short[4097])),
				Arguments.of((Supplier<Vector>) () -> Vector.int32(new int[4097])),
				Arguments.of((Supplier<Vector>) () -> Vector.int64(new long[4097])),
				Arguments.of((Supplier<Vector>) () -> Vector.float32(new float[4097])),
				Arguments.of((Supplier<Vector>) () -> Vector.float64(new double[4097])));
	}

	@ParameterizedTest
	@MethodSource
	void rangeCheckShouldWorkUpper(Supplier<Vector> factory) {
		Vector.CHECK_UPPER_RANGE.compareAndSet(false, true);
		assertThatIllegalArgumentException().isThrownBy(factory::get)
			.withMessage("'4097' is not a valid value. Must be a number in the range 1 to 4096 (GQL 42N31)");
	}

	@ParameterizedTest
	@MethodSource("rangeCheckShouldWorkUpper")
	void rangeCheckShouldWorkUpperDisabled(Supplier<Vector> factory) {
		Vector.CHECK_UPPER_RANGE.compareAndSet(true, false);
		assertThatNoException().isThrownBy(factory::get);
	}

}
