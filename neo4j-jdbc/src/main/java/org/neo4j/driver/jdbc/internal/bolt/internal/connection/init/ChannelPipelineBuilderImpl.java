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
package org.neo4j.driver.jdbc.internal.bolt.internal.connection.init;

import io.netty.channel.ChannelPipeline;
import org.neo4j.driver.jdbc.internal.bolt.internal.connection.inbound.ChannelErrorHandler;
import org.neo4j.driver.jdbc.internal.bolt.internal.connection.inbound.ChunkDecoder;
import org.neo4j.driver.jdbc.internal.bolt.internal.connection.inbound.InboundMessageHandler;
import org.neo4j.driver.jdbc.internal.bolt.internal.connection.inbound.MessageDecoder;
import org.neo4j.driver.jdbc.internal.bolt.internal.connection.outbound.OutboundMessageHandler;
import org.neo4j.driver.jdbc.internal.bolt.internal.messaging.MessageFormat;

public final class ChannelPipelineBuilderImpl implements ChannelPipelineBuilder {

	@Override
	public void build(MessageFormat messageFormat, ChannelPipeline pipeline) {
		// inbound handlers
		pipeline.addLast(new ChunkDecoder());
		pipeline.addLast(new MessageDecoder());
		var inboundMessageHandler = new InboundMessageHandler(messageFormat);
		pipeline.addLast(inboundMessageHandler);

		// outbound handlers
		var outboundMessageHandler = new OutboundMessageHandler(messageFormat);
		pipeline.addLast(OutboundMessageHandler.NAME, outboundMessageHandler);

		// last one - error handler
		pipeline.addLast(new ChannelErrorHandler());
	}

}
