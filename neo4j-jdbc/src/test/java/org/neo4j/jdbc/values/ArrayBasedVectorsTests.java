package org.neo4j.jdbc.values;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class ArrayBasedVectorsTests {

	static Stream<Arguments> hasToString() {
		return Stream.of(Arguments.of(Vector.int8(new byte[1]), "VECTOR<INTEGER8>(1)"),
				Arguments.of(Vector.int16(new short[2]), "VECTOR<INTEGER16>(2)"),
				Arguments.of(Vector.int32(new int[3]), "VECTOR<INTEGER32>(3)"),
				Arguments.of(Vector.int64(new long[4]), "VECTOR<INTEGER64>(4)"),
				Arguments.of(Vector.float32(new float[5]), "VECTOR<FLOAT32>(5)"),
				Arguments.of(Vector.float64(new double[6]), "VECTOR<FLOAT64>(6)")

		);
	}

	@ParameterizedTest
	@MethodSource
	void hasToString(Vector vector, String expected) {
		assertThat(vector).hasToString(expected);
	}

}
