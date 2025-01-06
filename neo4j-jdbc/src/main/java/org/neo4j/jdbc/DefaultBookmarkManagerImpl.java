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
package org.neo4j.jdbc;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Responsible for storing, updating and retrieving the bookmarks of Neo4j's transaction.
 * The original class appeared for the first time in
 * <a href="https://github.com/spring-projects/spring-data-neo4j/tree/6.0.0">Spring Data
 * Neo4j 6</a> by the same author, under the same license.
 *
 * @author Michael J. Simons
 */
final class DefaultBookmarkManagerImpl implements BookmarkManager {

	private final Set<String> bookmarks = new HashSet<>();

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	private final Lock read = this.lock.readLock();

	private final Lock write = this.lock.writeLock();

	@Override
	public <T> Set<T> getBookmarks(Function<String, T> transformer) {

		try {
			this.read.lock();
			return this.bookmarks.stream().map(transformer).collect(Collectors.toUnmodifiableSet());
		}
		finally {
			this.read.unlock();
		}
	}

	@Override
	public <T> void updateBookmarks(Function<T, String> transformer, Collection<T> usedBookmarks,
			Collection<T> newBookmarks) {

		Objects.requireNonNull(transformer, "A function for deriving a String value from a bookmark is required");
		Objects.requireNonNull(newBookmarks, "New bookmarks might not be null");

		try {
			this.write.lock();
			if (usedBookmarks != null) {
				usedBookmarks.forEach(b -> this.bookmarks.remove(transformer.apply(b)));
			}
			newBookmarks.forEach(b -> this.bookmarks.add(transformer.apply(b)));
		}
		finally {
			this.write.unlock();
		}
	}

}
