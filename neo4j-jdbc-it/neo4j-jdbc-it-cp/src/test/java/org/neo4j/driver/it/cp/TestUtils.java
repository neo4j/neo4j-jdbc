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
package org.neo4j.driver.it.cp;

import org.testcontainers.containers.Neo4jContainer;

final class TestUtils {

	private TestUtils() {
	}

	/**
	 * {@return a Neo4j testcontainer configured to the needs of the integration tests}.
	 */
	@SuppressWarnings("resource")
	static Neo4jContainer<?> getNeo4jContainer() {
		return new Neo4jContainer<>(System.getProperty("neo4j-jdbc.default-neo4j-image"))
			.withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
			.waitingFor(Neo4jContainer.WAIT_FOR_BOLT) // The HTTP wait strategy used by
														// default seems not to work in
														// native image, bolt must be
														// sufficed.
			.withReuse(true);
	}

}
