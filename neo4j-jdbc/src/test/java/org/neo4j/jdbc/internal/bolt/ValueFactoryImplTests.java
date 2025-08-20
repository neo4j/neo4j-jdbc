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
package org.neo4j.jdbc.internal.bolt;

import org.junit.jupiter.api.Test;
import org.neo4j.bolt.connection.values.Vector;
import org.neo4j.jdbc.values.AsValue;
import org.neo4j.jdbc.values.VectorValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ValueFactoryImplTests {

	@Test
	void vectorOfBoltValueMustWork() {
		var boltVector = new Vector() {

			@Override
			public Class<?> elementType() {
				return int.class;
			}

			@Override
			public Object elements() {
				return new int[] { 6, 6, 6 };
			}
		};
		var value = ValueFactoryImpl.INSTANCE.value(boltVector);
		assertThat(value).isNotNull();
		assertThat(value).isInstanceOf(AsValue.class);
		var jdbcValue = ((AsValue) value).asValue();
		assertThat(jdbcValue).isInstanceOf(VectorValue.class);
		var vector = jdbcValue.asVector();
		assertThat(vector).isEqualTo(org.neo4j.jdbc.values.Vector.of(new int[] { 6, 6, 6 }));
	}

	@Test
	void vectorMustFailWithInvalidType() {
		var elements = new String[0];
		assertThatIllegalArgumentException().isThrownBy(() -> ValueFactoryImpl.INSTANCE.vector(String.class, elements))
			.withMessage("Element type java.lang.String is not a supported element type for vectors");
	}

}
