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
package org.neo4j.driver.jdbc.internal.bolt.internal.messaging.v51;

import java.util.Map;

import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.AbstractMessageWriter;
import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.MessageEncoder;
import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.common.CommonValuePacker;
import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.encode.BeginMessageEncoder;
import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.encode.CommitMessageEncoder;
import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.encode.DiscardMessageEncoder;
import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.encode.GoodbyeMessageEncoder;
import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.encode.HelloMessageEncoder;
import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.encode.LogonMessageEncoder;
import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.encode.PullMessageEncoder;
import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.encode.ResetMessageEncoder;
import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.encode.RollbackMessageEncoder;
import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.encode.RunWithMetadataMessageEncoder;
import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.request.BeginMessage;
import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.request.CommitMessage;
import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.request.DiscardMessage;
import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.request.GoodbyeMessage;
import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.request.HelloMessage;
import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.request.LogonMessage;
import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.request.PullMessage;
import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.request.ResetMessage;
import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.request.RollbackMessage;
import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.request.RunWithMetadataMessage;
import org.neo4j.driver.jdbc.internal.bolt.internal.packstream.PackOutput;

public final class MessageWriterV51 extends AbstractMessageWriter {

	public MessageWriterV51(PackOutput output) {
		super(new CommonValuePacker(output, true), buildEncoders());
	}

	private static Map<Byte, MessageEncoder> buildEncoders() {
		return Map.ofEntries(Map.entry(HelloMessage.SIGNATURE, new HelloMessageEncoder()),
				Map.entry(GoodbyeMessage.SIGNATURE, new GoodbyeMessageEncoder()),
				Map.entry(LogonMessage.SIGNATURE, new LogonMessageEncoder()),
				Map.entry(RunWithMetadataMessage.SIGNATURE, new RunWithMetadataMessageEncoder()),
				Map.entry(PullMessage.SIGNATURE, new PullMessageEncoder()),
				Map.entry(DiscardMessage.SIGNATURE, new DiscardMessageEncoder()),
				Map.entry(BeginMessage.SIGNATURE, new BeginMessageEncoder()),
				Map.entry(CommitMessage.SIGNATURE, new CommitMessageEncoder()),
				Map.entry(RollbackMessage.SIGNATURE, new RollbackMessageEncoder()),
				Map.entry(ResetMessage.SIGNATURE, new ResetMessageEncoder()));
	}

}
