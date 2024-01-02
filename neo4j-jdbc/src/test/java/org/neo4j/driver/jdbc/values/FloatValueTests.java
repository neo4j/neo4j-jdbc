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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

class FloatValueTests {

	@Test
	void asInt() {

		var f = new FloatValue(1.0);
		assertThatNoException().isThrownBy(f::asInt);
	}

	@Test
	void asIntLossy() {

		var f = new FloatValue(1.1);
		assertThatExceptionOfType(LossyCoercion.class).isThrownBy(f::asInt);
	}

	@Test
	void asFloat() {

		var f = new FloatValue(1.0);
		assertThatNoException().isThrownBy(f::asFloat);
	}

	@Test
	void asFloatLossy() {

		var f = new FloatValue(1.1);
		assertThatExceptionOfType(LossyCoercion.class).isThrownBy(f::asFloat);
	}

	@Test
	void asLong() {

		var f = new FloatValue(1.0);
		assertThatNoException().isThrownBy(f::asLong);
	}

	@Test
	void asLongLossy() {

		var f = new FloatValue(1.1);
		assertThatExceptionOfType(LossyCoercion.class).isThrownBy(f::asLong);
	}

}
