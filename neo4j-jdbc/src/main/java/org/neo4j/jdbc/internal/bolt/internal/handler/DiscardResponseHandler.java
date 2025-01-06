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
package org.neo4j.jdbc.internal.bolt.internal.handler;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.neo4j.jdbc.internal.bolt.exception.BoltException;
import org.neo4j.jdbc.internal.bolt.internal.util.MetadataExtractor;
import org.neo4j.jdbc.internal.bolt.response.DiscardResponse;
import org.neo4j.jdbc.internal.bolt.response.ResultSummary;
import org.neo4j.jdbc.values.BooleanValue;
import org.neo4j.jdbc.values.Value;

public final class DiscardResponseHandler implements ResponseHandler {

	private final CompletableFuture<DiscardResponse> future;

	public DiscardResponseHandler(CompletableFuture<DiscardResponse> future) {
		this.future = Objects.requireNonNull(future, "future must not be null");
	}

	@Override
	public void onSuccess(Map<String, Value> metadata) {
		var hasMore = metadata.getOrDefault("has_more", BooleanValue.FALSE).asBoolean();
		var summary = (hasMore) ? null : MetadataExtractor.extractSummary(metadata);
		this.future.complete(new InternalDiscardResponse(summary));
	}

	@Override
	public void onFailure(Exception ex) {
		this.future.completeExceptionally(ex);
	}

	@Override
	public void onRecord(Value[] fields) {
		this.future.completeExceptionally(new BoltException("Records are not supported on DISCARD"));
	}

	private record InternalDiscardResponse(ResultSummary summary) implements DiscardResponse {
		@Override
		public Optional<ResultSummary> resultSummary() {
			return Optional.ofNullable(this.summary);
		}
	}

}
