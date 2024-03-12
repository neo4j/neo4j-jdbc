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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.neo4j.jdbc.internal.bolt.internal.util.MetadataExtractor;
import org.neo4j.jdbc.internal.bolt.response.PullResponse;
import org.neo4j.jdbc.internal.bolt.response.ResultSummary;
import org.neo4j.jdbc.internal.bolt.response.RunResponse;
import org.neo4j.jdbc.values.BooleanValue;
import org.neo4j.jdbc.values.Record;
import org.neo4j.jdbc.values.Value;

public final class BasicPullResponseHandler implements ResponseHandler {

	private final CompletionStage<RunResponse> runStage;

	private final CompletableFuture<PullResponse> pullFuture;

	private final List<Record> records = new ArrayList<>();

	public BasicPullResponseHandler(CompletionStage<RunResponse> runStage, CompletableFuture<PullResponse> pullFuture) {
		this.runStage = runStage;
		this.pullFuture = pullFuture;
	}

	@Override
	public void onSuccess(Map<String, Value> metadata) {
		var hasMore = metadata.getOrDefault("has_more", BooleanValue.FALSE).asBoolean();
		var summary = (hasMore) ? null : MetadataExtractor.extractSummary(metadata);
		this.pullFuture.complete(new InternalPullResponse(Collections.unmodifiableList(this.records), summary));
	}

	@Override
	public void onFailure(Exception ex) {
		this.pullFuture.completeExceptionally(ex);
	}

	@Override
	public void onRecord(Value[] fields) {
		var runResponse = this.runStage.toCompletableFuture().join();
		this.records.add(Record.of(runResponse.keys(), fields));
	}

	private record InternalPullResponse(List<Record> records, ResultSummary summary) implements PullResponse {
		@Override
		public Optional<ResultSummary> resultSummary() {
			return Optional.ofNullable(this.summary);
		}
	}

}
