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
package org.neo4j.jdbc.authn.spi;

import java.util.Map;

/**
 * An interface for custom authentications based on a map of values.
 *
 * @author Michael J. Simons
 * @since 6.6.0
 */
public non-sealed interface CustomAuthentication extends Authentication {

	/**
	 * Converts this token into a map. Usual keys that the server expects are
	 * {@code scheme}, {@code principal} and {@code credentials}.
	 * @return a map to be handled by the Neo4j server, must not be {@literal null}
	 */
	default Map<String, Object> toMap() {
		return Map.of();
	}

}
