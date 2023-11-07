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
package org.neo4j.driver.jdbc.internal.bolt.internal.handler;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import io.netty.channel.Channel;
import org.neo4j.driver.jdbc.internal.bolt.exception.BoltException;
import org.neo4j.driver.jdbc.values.Value;

public final class LogonResponseHandler implements ResponseHandler {

	private final CompletableFuture<?> future;

	private final Channel channel;

	public LogonResponseHandler(CompletableFuture<?> future, Channel channel) {
		this.future = Objects.requireNonNull(future, "future must not be null");
		this.channel = Objects.requireNonNull(channel, "channel must not be null");
	}

	@Override
	public void onSuccess(Map<String, Value> metadata) {
		this.future.complete(null);
	}

	@Override
	public void onFailure(Throwable error) {
		this.channel.close().addListener(future -> this.future.completeExceptionally(error));
	}

	@Override
	public void onRecord(Value[] fields) {
		this.future.completeExceptionally(new BoltException("Records are not supported on LOGON"));
	}

}
