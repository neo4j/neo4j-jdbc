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

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.neo4j.bolt.connection.exception.BoltGqlErrorException;
import org.neo4j.jdbc.internal.bolt.BoltAdapters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class GqlStatusObjectImplTests {

	@Test
	void recursiveCreationShouldWork() {
		var ex1 = new BoltGqlErrorException("n/a", "1", "n/a", Map.of(), null);
		var ex2 = new BoltGqlErrorException("n/a", "2", "n/a", Map.of(), ex1);
		var ex3 = new BoltGqlErrorException("n/a", "3", "n/a", Map.of(), ex2);

		var status1 = GqlStatusObjectImpl.of(ex3);
		assertThat(status1.gqlStatus()).isEqualTo("3");
		assertThat(status1.cause()).hasValueSatisfying(status2 -> {
			assertThat(status2.gqlStatus()).isEqualTo("2");
			assertThat(status2.cause()).hasValueSatisfying(status3 -> {
				assertThat(status3.gqlStatus()).isEqualTo("1");
				assertThat(status3.cause()).isEmpty();
			});
		});
	}

	@Test
	void diagnosticRecordShouldBeImmutable() {

		var ex1 = new BoltGqlErrorException("n/a", "1", "n/a", Map.of("a", BoltAdapters.getValueFactory().value("b")),
				null);

		var status1 = GqlStatusObjectImpl.of(ex1);
		var dr = status1.diagnosticRecord();
		assertThat(dr).containsEntry("a", "b");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> dr.put("c", "d"));
	}

}
