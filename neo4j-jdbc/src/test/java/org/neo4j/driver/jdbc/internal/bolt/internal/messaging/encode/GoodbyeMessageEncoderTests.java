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

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.ValuePacker;
import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.request.GoodbyeMessage;
import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.request.ResetMessage;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class GoodbyeMessageEncoderTests {

	private final GoodbyeMessageEncoder encoder = new GoodbyeMessageEncoder();

	private final ValuePacker packer = Mockito.mock(ValuePacker.class);

	@Test
	void shouldEncodeGoodbyeMessage() throws Exception {
		this.encoder.encode(GoodbyeMessage.GOODBYE, this.packer);

		Mockito.verify(this.packer).packStructHeader(0, GoodbyeMessage.SIGNATURE);
	}

	@Test
	void shouldFailToEncodeWrongMessage() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.encoder.encode(ResetMessage.RESET, this.packer));
	}

}
