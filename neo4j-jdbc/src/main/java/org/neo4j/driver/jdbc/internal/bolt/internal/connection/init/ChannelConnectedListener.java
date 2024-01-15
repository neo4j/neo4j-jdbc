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
package org.neo4j.driver.jdbc.internal.bolt.internal.connection.init;

import java.util.logging.Level;
import java.util.logging.Logger;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelPromise;
import org.neo4j.driver.jdbc.internal.bolt.BoltMessageExchange;
import org.neo4j.driver.jdbc.internal.bolt.BoltServerAddress;
import org.neo4j.driver.jdbc.internal.bolt.exception.BoltException;
import org.neo4j.driver.jdbc.internal.bolt.internal.connection.HandshakeHandler;
import org.neo4j.driver.jdbc.internal.bolt.internal.util.BoltProtocolUtil;

public final class ChannelConnectedListener implements ChannelFutureListener {

	private static final Logger boltLogger = Logger.getLogger(BoltMessageExchange.class.getCanonicalName());

	private final BoltServerAddress address;

	private final ChannelPipelineBuilder pipelineBuilder;

	private final ChannelPromise handshakeCompletedPromise;

	public ChannelConnectedListener(BoltServerAddress address, ChannelPipelineBuilder pipelineBuilder,
			ChannelPromise handshakeCompletedPromise) {
		this.address = address;
		this.pipelineBuilder = pipelineBuilder;
		this.handshakeCompletedPromise = handshakeCompletedPromise;
	}

	@Override
	public void operationComplete(ChannelFuture future) {
		var channel = future.channel();

		if (future.isSuccess()) {
			var pipeline = channel.pipeline();
			pipeline.addLast(new HandshakeHandler(this.pipelineBuilder, this.handshakeCompletedPromise));
			boltLogger.log(Level.FINE, "C: [Bolt Handshake] {0}", BoltProtocolUtil.handshakeString());
			channel.writeAndFlush(BoltProtocolUtil.handshakeBuf()).addListener(f -> {
				if (!f.isSuccess()) {
					this.handshakeCompletedPromise.setFailure(
							new BoltException(String.format("Unable to write to %s.", this.address), f.cause()));
				}
			});
		}
		else {
			this.handshakeCompletedPromise.setFailure(databaseUnavailableError(this.address, future.cause()));
		}
	}

	private static Throwable databaseUnavailableError(BoltServerAddress address, Throwable cause) {
		return new BoltException(String.format("Unable to connect to %s, ensure the database is running and that there "
				+ "is a working network connection to it.", address), cause);
	}

}
