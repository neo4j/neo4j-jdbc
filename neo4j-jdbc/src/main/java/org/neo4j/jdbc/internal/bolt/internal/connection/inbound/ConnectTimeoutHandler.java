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
package org.neo4j.jdbc.internal.bolt.internal.connection.inbound;

import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.ReadTimeoutHandler;

public final class ConnectTimeoutHandler extends ReadTimeoutHandler {

	private final long timeoutMillis;

	private boolean triggered;

	public ConnectTimeoutHandler(long timeoutMillis) {
		super(timeoutMillis, TimeUnit.MILLISECONDS);
		this.timeoutMillis = timeoutMillis;
	}

	@Override
	protected void readTimedOut(ChannelHandlerContext ctx) {
		if (!this.triggered) {
			this.triggered = true;
			ctx.fireExceptionCaught(unableToConnectError());
		}
	}

	private RuntimeException unableToConnectError() {
		return new RuntimeException("Unable to establish connection in " + this.timeoutMillis + "ms");
	}

}
