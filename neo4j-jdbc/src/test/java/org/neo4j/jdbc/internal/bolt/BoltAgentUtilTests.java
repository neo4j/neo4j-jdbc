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

import org.junit.jupiter.api.Test;

import static com.github.stefanbirkner.systemlambda.SystemLambda.restoreSystemProperties;
import static org.assertj.core.api.Assertions.assertThat;

class BoltAgentUtilTests {

	@Test
	void factoryMethodShouldWork() throws Exception {

		restoreSystemProperties(() -> {
			System.setProperty("os.name", "foo");
			System.setProperty("os.version", "bar");
			System.setProperty("os.arch", "bazbar");

			System.setProperty("java.version", "1.4");
			System.setProperty("java.vm.vendor", "ms");
			System.setProperty("java.vm.name", "fake");
			System.clearProperty("java.vm.version");

			var whatever = BoltAdapters.newAgent("whatever");
			assertThat(whatever.product()).isEqualTo("neo4j-jdbc/whatever");
			assertThat(whatever.platform()).isEqualTo("foo; bar; bazbar");
			assertThat(whatever.language()).isEqualTo("Java/1.4");
			assertThat(whatever.languageDetails()).isEqualTo("ms; fake");
		});
	}

}
