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
package org.neo4j.jdbc.internal.bolt.internal.messaging.encode;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.neo4j.jdbc.internal.bolt.BoltAgent;
import org.neo4j.jdbc.internal.bolt.BoltServerAddress;
import org.neo4j.jdbc.internal.bolt.internal.messaging.ValuePacker;
import org.neo4j.jdbc.internal.bolt.internal.messaging.request.HelloMessage;
import org.neo4j.jdbc.internal.bolt.internal.messaging.request.ResetMessage;
import org.neo4j.jdbc.values.Value;
import org.neo4j.jdbc.values.Values;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class HelloMessageEncoderTests {

	private static final BoltAgent VALUE = new BoltAgent("agent", null, null, null);

	private final HelloMessageEncoder encoder = new HelloMessageEncoder();

	private final ValuePacker packer = Mockito.mock(ValuePacker.class);

	@Test
	void shouldEncodeHelloMessage() throws Exception {
		Map<String, Value> authToken = new HashMap<>();
		authToken.put("username", Values.value("bob"));
		authToken.put("password", Values.value("secret"));

		this.encoder.encode(new HelloMessage(new BoltServerAddress("localhost", 7687), "MyDriver", VALUE, authToken),
				this.packer);

		var order = Mockito.inOrder(this.packer);
		order.verify(this.packer).packStructHeader(1, HelloMessage.SIGNATURE);

		Map<String, Value> expectedMetadata = new HashMap<>(authToken);
		expectedMetadata.put("user_agent", Values.value("MyDriver"));
		expectedMetadata.put("bolt_agent", Values.value(Map.of("product", VALUE.product())));
		expectedMetadata.put("routing", Values.value(Map.of("address", "localhost:7687")));
		order.verify(this.packer).pack(expectedMetadata);
	}

	@Test
	void shouldFailToEncodeWrongMessage() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.encoder.encode(ResetMessage.RESET, this.packer));
	}

}
