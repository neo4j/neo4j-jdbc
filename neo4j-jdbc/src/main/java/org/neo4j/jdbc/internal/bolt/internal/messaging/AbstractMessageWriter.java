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
package org.neo4j.jdbc.internal.bolt.internal.messaging;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

public abstract class AbstractMessageWriter implements MessageFormat.Writer {

	private final ValuePacker packer;

	private final Map<Byte, MessageEncoder> encodersByMessageSignature;

	protected AbstractMessageWriter(ValuePacker packer, Map<Byte, MessageEncoder> encodersByMessageSignature) {
		this.packer = Objects.requireNonNull(packer);
		this.encodersByMessageSignature = Objects.requireNonNull(encodersByMessageSignature);
	}

	@Override
	public final void write(Message msg) throws IOException {
		var signature = msg.signature();
		var encoder = this.encodersByMessageSignature.get(signature);
		if (encoder == null) {
			throw new IOException("No encoder found for message " + msg + " with signature " + signature);
		}
		encoder.encode(msg, this.packer);
	}

}
