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

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;

class ValueTests {

	@Test
	void notEverythingTheseDaysIsAVector() {

		var value = Mockito.mock(Value.class);
		given(value.asVector()).willCallRealMethod();
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(value::asVector);

		value = Values.value("hallo");
		assertThatExceptionOfType(UncoercibleException.class).isThrownBy(value::asVector);
	}

	@Test
	void asVectorShouldWork() {
		Value value = Values.value(Vector.of(new int[] { 1 }));
		Vector vector = value.asVector();
		assertThat(vector).isEqualTo(Vector.of(new int[] { 1 }));
		vector = value.asVector(Vector.of(new double[] { 1.1 }));
		assertThat(vector).isEqualTo(Vector.of(new int[] { 1 }));
		vector = NullValue.NULL.asVector(Vector.of(new double[] { 1.1 }));
		assertThat(vector).isEqualTo(Vector.of(new double[] { 1.1 }));
	}

}
