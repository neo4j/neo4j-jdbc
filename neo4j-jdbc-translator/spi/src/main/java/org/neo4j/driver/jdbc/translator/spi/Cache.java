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
package org.neo4j.driver.jdbc.translator.spi;

import java.util.function.Function;

/**
 * A cache that can be used to store results of successful translations.
 *
 * @param <K> the type of the key
 * @param <V> the type of the value
 * @author Michael J. Simons
 */
public interface Cache<K, V> {

	/**
	 * Creates new cache instance. The default implementation is not thread-safe and must
	 * be wrapped appropriate.
	 * @param capacity the requested capacity
	 * @param <K> the type of the key
	 * @param <V> the type of the value
	 * @return a non-thread-safe instance
	 */
	static <K, V> Cache<K, V> getInstance(int capacity) {
		return new LRUCache<>(capacity);
	}

	/**
	 * Gets the value corresponding to the key {@code key} or computes a new value for the
	 * key.
	 * @param key the key of the requested value
	 * @param mappingFunction a function to compute the new value
	 * @return the existing value or the newly computed value
	 */
	V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction);

	/**
	 * Checks whether the given key is contained in the cache.
	 * @param key the key to check
	 * @return {@literal true} if this cache contains the key
	 */
	boolean containsKey(Object key);

	/**
	 * Retrieves a value from the cache.
	 * @param key the key of the value
	 * @return the value or {@literal null} if there's no such value
	 */
	V get(Object key);

	/**
	 * Sets the key {@code key} to the new value.
	 * @param key the key to update
	 * @param value the new value
	 * @return the previous value or {@literal null} if there's no previous value
	 */
	V put(K key, V value);

	/**
	 * Flushes all entries of the cache.
	 */
	void flush();

}
