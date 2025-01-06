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
package org.neo4j.jdbc.internal.bolt.internal.connection.inbound;

import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.neo4j.jdbc.internal.bolt.exception.ConnectionReadTimeoutException;

public final class ConnectionReadTimeoutHandler extends ReadTimeoutHandler {

	private final long timeout;

	private final TimeUnit unit;

	private boolean triggered;

	public ConnectionReadTimeoutHandler(long timeout, TimeUnit unit) {
		super(timeout, unit);
		this.timeout = timeout;
		this.unit = unit;
	}

	@Override
	protected void readTimedOut(ChannelHandlerContext ctx) {
		if (!this.triggered) {
			ctx.fireExceptionCaught(new ConnectionReadTimeoutException(String
				.format("Connection read timed out due to it taking longer than %s %s.", this.timeout, this.unit)));
			ctx.close();
			this.triggered = true;
		}
	}

}
