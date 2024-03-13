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
package org.neo4j.jdbc.internal.bolt.internal.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.neo4j.jdbc.internal.bolt.exception.BoltException;
import org.neo4j.jdbc.internal.bolt.internal.response.InternalResultSummary;
import org.neo4j.jdbc.internal.bolt.internal.response.InternalSummaryCounters;
import org.neo4j.jdbc.internal.bolt.response.ResultSummary;
import org.neo4j.jdbc.values.Value;

public final class MetadataExtractor {

	public static final int ABSENT_QUERY_ID = -1;

	private static final String UNEXPECTED_TYPE_MSG_FMT = "Unexpected query type '%s', consider updating the driver";

	private final String resultAvailableAfterMetadataKey;

	private final String resultConsumedAfterMetadataKey;

	public MetadataExtractor(String resultAvailableAfterMetadataKey, String resultConsumedAfterMetadataKey) {
		this.resultAvailableAfterMetadataKey = resultAvailableAfterMetadataKey;
		this.resultConsumedAfterMetadataKey = resultConsumedAfterMetadataKey;
	}

	public static Value extractServer(Map<String, Value> metadata) {
		var versionValue = metadata.get("server");
		if (versionValue == null || versionValue.isNull()) {
			throw new BoltException("Server provides no product identifier");
		}
		var serverAgent = versionValue.asString();
		if (!serverAgent.startsWith("Neo4j/")) {
			throw new BoltException("Server does not identify as a genuine Neo4j instance: '" + serverAgent + "'");
		}
		return versionValue;
	}

	private static int counterValue(Value countersValue, String name) {
		var value = countersValue.get(name);
		return value.isNull() ? 0 : value.asInt();
	}

	private static long extractResultConsumedAfter(Map<String, Value> metadata, String key) {
		var resultConsumedAfterValue = metadata.get(key);
		if (resultConsumedAfterValue != null) {
			return resultConsumedAfterValue.asLong();
		}
		return -1;
	}

	public static Set<String> extractBoltPatches(Map<String, Value> metadata) {
		var boltPatch = metadata.get("patch_bolt");
		if (boltPatch != null && !boltPatch.isNull()) {
			return new HashSet<>(boltPatch.asList(Value::asString));
		}
		else {
			return Collections.emptySet();
		}
	}

	public static long extractQueryId(Map<String, Value> metadata) {
		var queryId = metadata.get("qid");
		if (queryId != null) {
			return queryId.asLong();
		}
		return ABSENT_QUERY_ID;
	}

	public static List<String> extractQueryKeys(Map<String, Value> metadata) {
		var keysValue = metadata.get("fields");
		if (keysValue != null) {
			if (!keysValue.isEmpty()) {
				var keys = new ArrayList<String>(keysValue.size());
				for (var value : keysValue.values()) {
					keys.add(value.asString());
				}

				return Collections.unmodifiableList(keys);
			}
		}
		return Collections.emptyList();
	}

	public static String extractBookmark(Map<String, Value> metadata) {
		var bookmarkValue = metadata.get("bookmark");
		return (bookmarkValue != null) ? bookmarkValue.asString() : null;
	}

	public static ResultSummary extractSummary(Map<String, Value> metadata) {
		return new InternalResultSummary(extractCounters(metadata));
	}

	private static InternalSummaryCounters extractCounters(Map<String, Value> metadata) {
		var countersValue = metadata.get("stats");
		if (countersValue != null) {
			return new InternalSummaryCounters(counterValue(countersValue, "nodes-created"),
					counterValue(countersValue, "nodes-deleted"), counterValue(countersValue, "relationships-created"),
					counterValue(countersValue, "relationships-deleted"), counterValue(countersValue, "properties-set"),
					counterValue(countersValue, "labels-added"), counterValue(countersValue, "labels-removed"),
					counterValue(countersValue, "indexes-added"), counterValue(countersValue, "indexes-removed"),
					counterValue(countersValue, "constraints-added"),
					counterValue(countersValue, "constraints-removed"), counterValue(countersValue, "system-updates"));
		}
		else {
			return new InternalSummaryCounters(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		}
	}

}
