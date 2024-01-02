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
package org.neo4j.driver.jdbc.internal.bolt.value;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import org.neo4j.driver.jdbc.values.Record;
import org.neo4j.driver.jdbc.values.Value;

final class ValueUtils {

	private ValueUtils() {
		throw new UnsupportedOperationException();
	}

	static <T> Map<String, T> map(Record record, Function<Value, T> mapFunction) {
		var size = record.size();
		switch (size) {
			case 0 -> {
				return Collections.emptyMap();
			}
			case 1 -> {
				return Collections.singletonMap(record.keys().get(0), mapFunction.apply(record.get(0)));
			}
			default -> {
				Map<String, T> map = new LinkedHashMap<>(size);
				var keys = record.keys();
				for (var i = 0; i < size; i++) {
					map.put(keys.get(i), mapFunction.apply(record.get(i)));
				}
				return Collections.unmodifiableMap(map);
			}
		}
	}

}
