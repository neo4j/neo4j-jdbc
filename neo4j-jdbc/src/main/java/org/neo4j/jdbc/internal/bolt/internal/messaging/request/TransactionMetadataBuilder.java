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
package org.neo4j.jdbc.internal.bolt.internal.messaging.request;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.neo4j.jdbc.internal.bolt.AccessMode;
import org.neo4j.jdbc.internal.bolt.TransactionType;
import org.neo4j.jdbc.internal.bolt.internal.util.Iterables;
import org.neo4j.jdbc.values.Value;
import org.neo4j.jdbc.values.Values;

public final class TransactionMetadataBuilder {

	private static final String BOOKMARKS_METADATA_KEY = "bookmarks";

	private static final String DATABASE_NAME_KEY = "db";

	private static final String TX_TIMEOUT_METADATA_KEY = "tx_timeout";

	private static final String TX_METADATA_METADATA_KEY = "tx_metadata";

	private static final String MODE_KEY = "mode";

	private static final String MODE_READ_VALUE = "r";

	private static final String IMPERSONATED_USER_KEY = "imp_user";

	private static final String TX_TYPE_KEY = "tx_type";

	private TransactionMetadataBuilder() {
	}

	public static Map<String, Value> buildMetadata(String databaseName, AccessMode mode,
			TransactionType transactionType, Set<String> bookmarks, Map<String, Object> txMetadata) {
		var databaseNamePresent = databaseName != null;
		var bookmarksPresent = !bookmarks.isEmpty();
		var txMetadataPresent = txMetadata != null && !txMetadata.isEmpty();
		var accessModePresent = mode == AccessMode.READ;

		if (!databaseNamePresent && !bookmarksPresent && !accessModePresent
				&& transactionType == TransactionType.DEFAULT) {
			return Collections.emptyMap();
		}

		Map<String, Value> result = Iterables.newHashMapWithSize(5);

		if (bookmarksPresent) {
			result.put(BOOKMARKS_METADATA_KEY, Values.value(bookmarks));
		}
		if (txMetadataPresent) {
			result.put(TX_METADATA_METADATA_KEY, Values.value(txMetadata));
		}
		if (accessModePresent) {
			result.put(MODE_KEY, Values.value(MODE_READ_VALUE));
		}
		switch (transactionType) {
			case DEFAULT:
				break;
			case UNCONSTRAINED:
				result.put(TX_TYPE_KEY, Values.value("IMPLICIT"));
		}

		result.put(DATABASE_NAME_KEY, Values.value(databaseName));
		return result;
	}

}
