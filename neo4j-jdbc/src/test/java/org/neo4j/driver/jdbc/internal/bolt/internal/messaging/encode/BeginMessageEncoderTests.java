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
package org.neo4j.driver.jdbc.internal.bolt.internal.messaging.encode;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.neo4j.driver.jdbc.internal.bolt.AccessMode;
import org.neo4j.driver.jdbc.internal.bolt.TransactionType;
import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.ValuePacker;
import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.request.BeginMessage;
import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.request.ResetMessage;
import org.neo4j.driver.jdbc.values.Value;
import org.neo4j.driver.jdbc.values.Values;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class BeginMessageEncoderTests {

	private final BeginMessageEncoder encoder = new BeginMessageEncoder();

	private final ValuePacker packer = Mockito.mock(ValuePacker.class);

	@ParameterizedTest
	@MethodSource("arguments")
	void shouldEncodeBeginMessage(AccessMode mode, String impersonatedUser) throws Exception {
		var bookmarks = Collections.singleton("neo4j:bookmark:v1:tx42");
		var txTimeout = Duration.ofSeconds(1);

		this.encoder.encode(new BeginMessage(bookmarks, "neo4j", mode, TransactionType.DEFAULT), this.packer);

		var order = Mockito.inOrder(this.packer);
		order.verify(this.packer).packStructHeader(1, BeginMessage.SIGNATURE);

		Map<String, Value> expectedMetadata = new HashMap<>();
		expectedMetadata.put("bookmarks", Values.value(bookmarks));
		expectedMetadata.put("db", Values.value("neo4j"));
		if (mode == AccessMode.READ) {
			expectedMetadata.put("mode", Values.value("r"));
		}

		order.verify(this.packer).pack(expectedMetadata);
	}

	private static Stream<Arguments> arguments() {
		return Arrays.stream(AccessMode.values())
			.flatMap(accessMode -> Stream.of(Arguments.of(accessMode, "user"), Arguments.of(accessMode, null)));
	}

	@Test
	void shouldFailToEncodeWrongMessage() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.encoder.encode(ResetMessage.RESET, this.packer));
	}

}
