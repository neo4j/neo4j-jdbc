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

import java.util.Optional;

public record BoltAgent(String product, String platform, String language, String languageDetails) {

	public static BoltAgent of(String driverVersion) {
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

}
