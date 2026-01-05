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
package org.neo4j.jdbc.translator.spi;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LRUCacheTests {

	@Test
	void shouldRemoveEldest() {
		var cache = new LRUCache<Integer, String>(2);
		for (int i = 0; i < 4; ++i) {
			cache.put(i, Integer.toString(i));
		}
		assertThat(cache).containsExactlyInAnyOrderEntriesOf(Map.of(2, "2", 3, "3"));
	}

	@Test
	void shouldFlush() {
		var cache = new LRUCache<Integer, String>(2);
		cache.put(1, "eins");
		assertThat(cache).containsEntry(1, "eins");
		cache.flush();
		assertThat(cache).isEmpty();
	}

}
