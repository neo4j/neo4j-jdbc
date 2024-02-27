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
package org.neo4j.jdbc.internal.bolt.internal.handler;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.neo4j.jdbc.values.Value;

public final class BeginTxResponseHandler implements ResponseHandler {

	private final CompletableFuture<Void> beginTxFuture;

	public BeginTxResponseHandler(CompletableFuture<Void> beginTxFuture) {
		this.beginTxFuture = Objects.requireNonNull(beginTxFuture);
	}

	@Override
	public void onSuccess(Map<String, Value> metadata) {
		this.beginTxFuture.complete(null);
	}

	@Override
	public void onFailure(Throwable error) {
		this.beginTxFuture.completeExceptionally(error);
	}

	@Override
	public void onRecord(Value[] fields) {
		throw new UnsupportedOperationException(
				"Transaction begin is not expected to receive records: " + Arrays.toString(fields));
	}

}
