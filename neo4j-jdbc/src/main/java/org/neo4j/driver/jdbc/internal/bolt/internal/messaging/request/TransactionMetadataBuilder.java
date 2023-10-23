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
package org.neo4j.driver.jdbc.internal.bolt.internal.messaging.request;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.neo4j.driver.jdbc.internal.bolt.AccessMode;
import org.neo4j.driver.jdbc.internal.bolt.TransactionType;
import org.neo4j.driver.jdbc.internal.bolt.Value;
import org.neo4j.driver.jdbc.internal.bolt.Values;
import org.neo4j.driver.jdbc.internal.bolt.internal.util.Iterables;

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
			TransactionType transactionType, Set<String> bookmarks) {
		var bookmarksPresent = !bookmarks.isEmpty();
		var accessModePresent = mode == AccessMode.READ;

		if (!bookmarksPresent && !accessModePresent && transactionType == TransactionType.DEFAULT) {
			return Collections.emptyMap();
		}

		Map<String, Value> result = Iterables.newHashMapWithSize(5);

		if (bookmarksPresent) {
			result.put(BOOKMARKS_METADATA_KEY, Values.value(bookmarks));
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
