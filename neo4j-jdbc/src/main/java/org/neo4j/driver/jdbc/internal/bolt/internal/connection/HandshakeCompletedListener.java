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
package org.neo4j.driver.jdbc.internal.bolt.internal.connection;

import java.time.Clock;
import java.util.Objects;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelPromise;
import org.neo4j.driver.jdbc.internal.bolt.AuthToken;
import org.neo4j.driver.jdbc.internal.bolt.BoltAgent;
import org.neo4j.driver.jdbc.internal.bolt.BoltServerAddress;
import org.neo4j.driver.jdbc.internal.bolt.internal.BoltProtocol;
import org.neo4j.driver.jdbc.internal.bolt.internal.InternalAuthToken;

public final class HandshakeCompletedListener implements ChannelFutureListener {

	private final BoltServerAddress address;

	private final String userAgent;

	private final BoltAgent boltAgent;

	private final AuthToken authToken;

	private final ChannelPromise connectionInitializedPromise;

	private final Clock clock;

	public HandshakeCompletedListener(BoltServerAddress address, String userAgent, BoltAgent boltAgent,
			AuthToken authToken, ChannelPromise connectionInitializedPromise, Clock clock) {
		this.address = Objects.requireNonNull(address);
		this.userAgent = Objects.requireNonNull(userAgent);
		this.boltAgent = Objects.requireNonNull(boltAgent);
		this.authToken = authToken;
		this.connectionInitializedPromise = Objects.requireNonNull(connectionInitializedPromise);
		this.clock = Objects.requireNonNull(clock);
	}

	@Override
	public void operationComplete(ChannelFuture future) {
		if (future.isSuccess()) {
			var protocol = BoltProtocol.forChannel(future.channel());
			protocol.initializeChannel(this.address, this.userAgent, this.boltAgent,
					((InternalAuthToken) this.authToken).toMap(), this.connectionInitializedPromise, this.clock);
		}
		else {
			this.connectionInitializedPromise.setFailure(future.cause());
		}
	}

}
