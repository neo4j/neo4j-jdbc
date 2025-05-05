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

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.neo4j.bolt.connection.exception.BoltGqlErrorException;
import org.neo4j.jdbc.internal.bolt.BoltAdapters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.neo4j.jdbc.Neo4jException.withMessageAndCause;

class Neo4jExceptionTests {

	@Test
	void diagnosticRecordShouldBeImmutable() {

		var ex1 = new BoltGqlErrorException("n/a", "1", "n/a", Map.of("a", BoltAdapters.getValueFactory().value("b")),
				null);

		var status1 = new Neo4jException(withMessageAndCause("n/a", ex1));
		var dr = status1.diagnosticRecord();
		assertThat(dr).containsEntry("a", "b");
		// noinspection DataFlowIssue
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> dr.put("c", "d"));
	}

}
