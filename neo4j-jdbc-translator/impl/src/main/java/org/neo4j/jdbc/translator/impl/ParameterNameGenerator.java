/*
 * Copyright (c) 2023-2026 "Neo4j,"
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
package org.neo4j.jdbc.translator.impl;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Produces parameter names.
 *
 * @author Michael J. Simons
 */
final class ParameterNameGenerator {

	private final AtomicInteger parameterIndex = new AtomicInteger(0);

	/**
	 * Retrieves a new index.
	 * @return a new name
	 */
	String newIndex() {
		return Integer.toString(this.parameterIndex.incrementAndGet());
	}

	/**
	 * Retrieves a new name based derived from {@code name}.
	 * @param name the name to derive the final name from
	 * @return a new name
	 */
	String newIndex(String name) {
		if (name == null) {
			return null;
		}

		try {
			this.parameterIndex.accumulateAndGet(Integer.parseInt(name), Math::max);
		}
		catch (NumberFormatException ignored) {
			// This is fine but sonar things otherwise.
		}

		return name;
	}

}
