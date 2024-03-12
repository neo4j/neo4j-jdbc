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
package org.neo4j.jdbc.internal.bolt.internal.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public final class Iterables {

	private Iterables() {
	}

	private static final float DEFAULT_HASH_MAP_LOAD_FACTOR = 0.75F;

	public static int count(Iterable<?> it) {
		if (it instanceof Collection) {
			return ((Collection<?>) it).size();
		}
		var size = 0;
		for (Object ignored : it) {
			size++;
		}
		return size;
	}

	public static <T> List<T> asList(Iterable<T> it) {
		if (it instanceof List) {
			return (List<T>) it;
		}
		List<T> list = new ArrayList<>();
		for (var t : it) {
			list.add(t);
		}
		return list;
	}

	public static <T> T single(Iterable<T> it) {
		var iterator = it.iterator();
		if (!iterator.hasNext()) {
			throw new IllegalArgumentException("Given iterable is empty");
		}
		var result = iterator.next();
		if (iterator.hasNext()) {
			throw new IllegalArgumentException("Given iterable contains more than one element: " + it);
		}
		return result;
	}

	public static <A, B> Iterable<B> map(final Iterable<A> it, final Function<A, B> f) {
		return () -> {
			final var aIterator = it.iterator();
			return new Iterator<>() {
				@Override
				public boolean hasNext() {
					return aIterator.hasNext();
				}

				@Override
				public B next() {
					return f.apply(aIterator.next());
				}

				@Override
				public void remove() {
					aIterator.remove();
				}
			};
		};
	}

	public static <K, V> HashMap<K, V> newHashMapWithSize(int expectedSize) {
		return new HashMap<>(hashMapCapacity(expectedSize));
	}

	@SuppressWarnings("squid:S3518") // Complaining about division by zero, which cannot
										// happen
	private static int hashMapCapacity(int expectedSize) {
		if (expectedSize < 3) {
			if (expectedSize < 0) {
				throw new IllegalArgumentException("Illegal map size: " + expectedSize);
			}
			return expectedSize + 1;
		}
		return (int) (expectedSize / DEFAULT_HASH_MAP_LOAD_FACTOR + 1.0F);
	}

}
