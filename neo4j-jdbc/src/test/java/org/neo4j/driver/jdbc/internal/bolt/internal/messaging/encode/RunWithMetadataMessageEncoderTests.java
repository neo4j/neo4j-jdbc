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

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import org.neo4j.driver.jdbc.internal.bolt.AccessMode;
import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.ValuePacker;
import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.request.ResetMessage;
import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.request.RunWithMetadataMessage;
import org.neo4j.driver.jdbc.values.Values;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class RunWithMetadataMessageEncoderTests {

	private final RunWithMetadataMessageEncoder encoder = new RunWithMetadataMessageEncoder();

	private final ValuePacker packer = Mockito.mock(ValuePacker.class);

	@ParameterizedTest
	@EnumSource(AccessMode.class)
	void shouldEncodeRunWithMetadataMessage(AccessMode mode) throws Exception {
		var query = "RETURN $answer";
		var params = Collections.singletonMap("answer", Values.value(42));

		this.encoder.encode(new RunWithMetadataMessage(query, params), this.packer);

		var order = Mockito.inOrder(this.packer);
		order.verify(this.packer).packStructHeader(3, RunWithMetadataMessage.SIGNATURE);
		order.verify(this.packer).pack("RETURN $answer");
		order.verify(this.packer).pack(params);
		order.verify(this.packer).pack(Collections.emptyMap());
	}

	@Test
	void shouldFailToEncodeWrongMessage() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.encoder.encode(ResetMessage.RESET, this.packer));
	}

}
