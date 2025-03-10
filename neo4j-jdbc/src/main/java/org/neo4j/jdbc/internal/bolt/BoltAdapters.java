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
package org.neo4j.jdbc.internal.bolt;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.neo4j.bolt.connection.BoltAgent;
import org.neo4j.bolt.connection.LoggingProvider;
import org.neo4j.bolt.connection.SummaryCounters;
import org.neo4j.bolt.connection.values.Value;
import org.neo4j.bolt.connection.values.ValueFactory;
import org.neo4j.jdbc.values.Values;

/**
 * Adapters towards the Bolt Connection API.
 *
 * @author Michael J. Simons
 * @since 6.2.0
 */
public final class BoltAdapters {

	public static LoggingProvider newLoggingProvider() {
		return new LoggingProviderImpl();
	}

	public static ValueFactory getValueFactory() {
		return ValueFactoryImpl.INSTANCE;
	}

	public static BoltAgent newAgent(String driverVersion) {
		var platformBuilder = new StringBuilder();
		getProperty("os.name").ifPresent(value -> append(value, platformBuilder));
		getProperty("os.version").ifPresent(value -> append(value, platformBuilder));
		getProperty("os.arch").ifPresent(value -> append(value, platformBuilder));

		var language = getProperty("java.version").map(version -> "Java/" + version);

		var languageDetailsBuilder = new StringBuilder();
		getProperty("java.vm.vendor").ifPresent(value -> append(value, languageDetailsBuilder));
		getProperty("java.vm.name").ifPresent(value -> append(value, languageDetailsBuilder));
		getProperty("java.vm.version").ifPresent(value -> append(value, languageDetailsBuilder));

		return new BoltAgent(String.format("neo4j-jdbc/%s", driverVersion),
				platformBuilder.isEmpty() ? null : platformBuilder.toString(), language.orElse(null),
				languageDetailsBuilder.isEmpty() ? null : languageDetailsBuilder.toString());

	}

	private static Optional<String> getProperty(String key) {
		try {
			var value = System.getProperty(key);
			if (value != null) {
				value = value.trim();
			}
			return (value != null && !value.isEmpty()) ? Optional.of(value) : Optional.empty();
		}
		catch (SecurityException exception) {
			return Optional.empty();
		}
	}

	private static void append(String value, StringBuilder builder) {
		if (value != null && !value.isEmpty()) {
			var separator = builder.isEmpty() ? "" : "; ";
			builder.append(separator).append(value);
		}
	}

	public static Map<String, Value> adaptMap(Map<String, Object> map) {
		if (map == null) {
			return null;
		}

		var result = new HashMap<String, Value>(map.size());
		for (var entry : map.entrySet()) {
			var boltValue = ValueFactoryImpl.asBoltValue(Values.value(entry.getValue()));
			result.put(entry.getKey(), boltValue);
		}
		return Collections.unmodifiableMap(result);
	}

	public static SummaryCounters newSummaryCounters(Value countersValue) {
		if (countersValue == null) {
			return SummaryCountersImpl.EMPTY_STATS;
		}
		return new SummaryCountersImpl(counterValue(countersValue, "nodes-created"),
				counterValue(countersValue, "nodes-deleted"), counterValue(countersValue, "relationships-created"),
				counterValue(countersValue, "relationships-deleted"), counterValue(countersValue, "properties-set"),
				counterValue(countersValue, "labels-added"), counterValue(countersValue, "labels-removed"),
				counterValue(countersValue, "indexes-added"), counterValue(countersValue, "indexes-removed"),
				counterValue(countersValue, "constraints-added"), counterValue(countersValue, "constraints-removed"),
				counterValue(countersValue, "system-updates"));
	}

	private static int counterValue(Value countersValue, String name) {
		var value = countersValue.get(name);
		return value.isNull() ? 0 : (int) value.asLong();
	}

	private BoltAdapters() {
	}

}
