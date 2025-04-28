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
package org.neo4j.jdbc;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.jdbc.values.Type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class ArrayImplTests {

	@Test
	void creationOfByteArrayShouldWork() throws SQLException {
		var bytes = "Hallo".getBytes(StandardCharsets.UTF_8);

		var elements = new Object[bytes.length];
		for (var i = 0; i < bytes.length; i++) {
			elements[i] = bytes[i];
		}
		var array = ArrayImpl.of(null, Type.BYTES.name(), elements);
		assertThat(array.getArray()).isEqualTo(bytes);
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "null", "  \t " })
	void mustFailOnInvalidType(String typeContent) {

		var type = "null".equals(typeContent) ? null : typeContent;
		var elements = new Object[0];
		assertThatExceptionOfType(SQLException.class).isThrownBy(() -> ArrayImpl.of(null, type, elements))
			.withMessage("data exception - Invalid argument, typename is required");
	}

	@Test
	void mustNotFailOnEmptyElements() throws SQLException {

		for (Object[] elements : new Object[][] { null, new Object[0] }) {
			var array = ArrayImpl.of(null, Type.ANY.name(), elements);
			assertThat(array.getBaseTypeName()).isEqualTo("ANY");
			assertThat(((Object[]) array.getArray())).isEmpty();
		}
	}

	@Test
	void mustValidateTypeName() {

		var elements = new String[] { "test" };

		assertThatExceptionOfType(SQLException.class).isThrownBy(() -> ArrayImpl.of(null, "asd", elements))
			.withMessage("data exception - Invalid type name asd");
	}

	@Test
	void mustValidateType() {

		var typeName = Type.INTEGER.name();
		var elements = new String[] { "test" };

		assertThatExceptionOfType(SQLException.class).isThrownBy(() -> ArrayImpl.of(null, typeName, elements))
			.withMessage("data exception - Cannot satisfy type INTEGER with the elements provided");
	}

	@Test
	void nullsOk() throws SQLException {

		var elements = new String[] { null, "test", null };

		var array = ArrayImpl.of(null, Type.STRING.name(), elements);
		assertThat(array.getBaseTypeName()).isEqualTo("STRING");
		assertThat(((Object[]) array.getArray())).isEqualTo(elements);
	}

	@Test
	void derivedType() throws SQLException {

		var elements = new Object[] { null, "test", null };

		var array = ArrayImpl.of(null, Type.STRING.name(), elements);
		assertThat(array.getBaseTypeName()).isEqualTo("STRING");
		assertThat(((Object[]) array.getArray())).isEqualTo(elements);
	}

}
