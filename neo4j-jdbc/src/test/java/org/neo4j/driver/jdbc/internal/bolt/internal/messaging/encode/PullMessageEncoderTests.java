/*
 * Copyright (c) 2023 "Neo4j,"
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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.neo4j.driver.jdbc.internal.bolt.Value;
import org.neo4j.driver.jdbc.internal.bolt.Values;
import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.ValuePacker;
import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.request.PullMessage;
import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.request.ResetMessage;

class PullMessageEncoderTests {

	private final PullMessageEncoder encoder = new PullMessageEncoder();

	private final ValuePacker packer = Mockito.mock(ValuePacker.class);

	@Test
	void shouldEncodePullMessage() throws Exception {
		this.encoder.encode(new PullMessage(100, 200), this.packer);

		Map<String, Value> meta = new HashMap<>();
		meta.put("n", Values.value(100));
		meta.put("qid", Values.value(200));

		var order = Mockito.inOrder(this.packer);
		order.verify(this.packer).packStructHeader(1, PullMessage.SIGNATURE);
		order.verify(this.packer).pack(meta);
	}

	@Test
	void shouldAvoidQueryId() throws Exception {
		this.encoder.encode(new PullMessage(100, -1), this.packer);

		Map<String, Value> meta = new HashMap<>();
		meta.put("n", Values.value(100));

		var order = Mockito.inOrder(this.packer);
		order.verify(this.packer).packStructHeader(1, PullMessage.SIGNATURE);
		order.verify(this.packer).pack(meta);
	}

	@Test
	void shouldFailToEncodeWrongMessage() {
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> this.encoder.encode(ResetMessage.RESET, this.packer));
	}

}
