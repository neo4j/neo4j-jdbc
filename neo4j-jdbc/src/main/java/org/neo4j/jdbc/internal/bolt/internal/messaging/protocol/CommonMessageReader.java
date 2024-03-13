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
package org.neo4j.jdbc.internal.bolt.internal.messaging.protocol;

import java.io.IOException;

import org.neo4j.jdbc.internal.bolt.internal.messaging.MessageFormat;
import org.neo4j.jdbc.internal.bolt.internal.messaging.ResponseMessageHandler;
import org.neo4j.jdbc.internal.bolt.internal.messaging.ValueUnpacker;
import org.neo4j.jdbc.internal.bolt.internal.messaging.response.FailureMessage;
import org.neo4j.jdbc.internal.bolt.internal.messaging.response.IgnoredMessage;
import org.neo4j.jdbc.internal.bolt.internal.messaging.response.RecordMessage;
import org.neo4j.jdbc.internal.bolt.internal.messaging.response.SuccessMessage;

abstract class CommonMessageReader implements MessageFormat.Reader {

	private final ValueUnpacker unpacker;

	CommonMessageReader(ValueUnpacker unpacker) {
		this.unpacker = unpacker;
	}

	@Override
	public void read(ResponseMessageHandler handler) throws IOException {
		this.unpacker.unpackStructHeader();
		var type = this.unpacker.unpackStructSignature();
		switch (type) {
			case SuccessMessage.SIGNATURE -> unpackSuccessMessage(handler);
			case FailureMessage.SIGNATURE -> unpackFailureMessage(handler);
			case IgnoredMessage.SIGNATURE -> handler.handleIgnoredMessage();
			case RecordMessage.SIGNATURE -> unpackRecordMessage(handler);
			default -> throw new IOException("Unknown message type: " + type);
		}
	}

	private void unpackSuccessMessage(ResponseMessageHandler output) throws IOException {
		var map = this.unpacker.unpackMap();
		output.handleSuccessMessage(map);
	}

	private void unpackFailureMessage(ResponseMessageHandler output) throws IOException {
		var params = this.unpacker.unpackMap();
		var code = params.get("code").asString();
		var message = params.get("message").asString();
		output.handleFailureMessage(code, message);
	}

	private void unpackRecordMessage(ResponseMessageHandler output) throws IOException {
		var fields = this.unpacker.unpackArray();
		output.handleRecordMessage(fields);
	}

}
