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

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.neo4j.jdbc.internal.bolt.internal.util.MetadataExtractor;
import org.neo4j.jdbc.internal.bolt.response.RunResponse;
import org.neo4j.jdbc.values.Value;

public final class RunResponseHandler implements ResponseHandler {

	private final CompletableFuture<RunResponse> runFuture;

	public RunResponseHandler(CompletableFuture<RunResponse> runFuture) {
		this.runFuture = runFuture;
	}

	@Override
	public void onSuccess(Map<String, Value> metadata) {
		var queryId = MetadataExtractor.extractQueryId(metadata);
		var keys = MetadataExtractor.extractQueryKeys(metadata);
		this.runFuture.complete(new InternalRunResponse(queryId, keys));
	}

	@Override
	public void onFailure(Exception ex) {
		this.runFuture.completeExceptionally(ex);
	}

	@Override
	public void onRecord(Value[] fields) {
		throw new UnsupportedOperationException();
	}

	private record InternalRunResponse(long queryId, List<String> keys) implements RunResponse {
	}

}
