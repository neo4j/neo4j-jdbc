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
package org.neo4j.jdbc.internal.bolt.internal;

import java.time.Clock;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

import io.netty.channel.EventLoopGroup;
import org.neo4j.jdbc.internal.bolt.AuthToken;
import org.neo4j.jdbc.internal.bolt.BoltAgent;
import org.neo4j.jdbc.internal.bolt.BoltConnection;
import org.neo4j.jdbc.internal.bolt.BoltConnectionProvider;
import org.neo4j.jdbc.internal.bolt.BoltServerAddress;
import org.neo4j.jdbc.internal.bolt.SecurityPlan;

public final class NettyBoltConnectionProvider implements BoltConnectionProvider {

	private final ConnectionProvider connectionProvider;

	public NettyBoltConnectionProvider(EventLoopGroup eventLoopGroup, Clock clock) {
		Objects.requireNonNull(eventLoopGroup);
		Objects.requireNonNull(clock);
		this.connectionProvider = ConnectionProviders.netty(eventLoopGroup, clock);
	}

	@Override
	public CompletionStage<BoltConnection> connect(BoltServerAddress address, SecurityPlan securityPlan,
			String databaseName, AuthToken authToken, BoltAgent boltAgent, String userAgent, int connectTimeoutMillis) {
		Objects.requireNonNull(address);
		Objects.requireNonNull(securityPlan);
		Objects.requireNonNull(databaseName);
		Objects.requireNonNull(authToken);
		Objects.requireNonNull(boltAgent);
		Objects.requireNonNull(userAgent);
		return this.connectionProvider
			.acquireConnection(address, securityPlan, databaseName, authToken, boltAgent, userAgent,
					connectTimeoutMillis)
			.thenApply(connection -> new BoltConnectionImpl(connection.protocol(), connection));
	}

}
