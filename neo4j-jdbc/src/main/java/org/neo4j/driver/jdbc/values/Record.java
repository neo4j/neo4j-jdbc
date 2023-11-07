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
package org.neo4j.driver.jdbc.values;

import java.util.List;

/**
 * Container for Cypher result values.
 *
 * @author Neo4j Drivers Team
 * @since 1.0.0
 */
public interface Record extends MapAccessorWithDefaultValue {

	/**
	 * Retrieve the keys of the underlying map.
	 * @return all field keys in order
	 */
	@Override
	List<String> keys();

	/**
	 * Retrieve the values of the underlying map.
	 * @return all field keys in order
	 */
	@Override
	List<Value> values();

	/**
	 * Retrieve the index of the field with the given key.
	 * @param key the give key
	 * @return the index of the field as used by {@link #get(int)}
	 * @throws java.util.NoSuchElementException if the given key is not from
	 * {@link #keys()}
	 */
	int index(String key);

	Value get(int index);

}
