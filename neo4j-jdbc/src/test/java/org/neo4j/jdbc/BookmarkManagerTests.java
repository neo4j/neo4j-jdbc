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
package org.neo4j.jdbc;

import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * @author Michael J. Simons
 */
class BookmarkManagerTests {

	@Nested
	class ForDefaultImpl {

		@Test
		void mustUpdateAndReturnBookmarks() {
			var bookmarkManager = new DefaultBookmarkManagerImpl();
			var current = bookmarkManager.getBookmarks(Function.identity());
			assertThat(current).isEmpty();
			bookmarkManager.updateBookmarks(Function.identity(), List.of(), List.of("test"));
			current = bookmarkManager.getBookmarks(Function.identity());
			assertThat(current).containsExactly("test");
			bookmarkManager.updateBookmarks(Function.identity(), List.of("test"), List.of());
			current = bookmarkManager.getBookmarks(Function.identity());
			assertThat(current).isEmpty();
		}

	}

	@Nested
	class ForVoid {

		@Test
		void mustNotUpdateAndReturnBookmarks() {
			var bookmarkManager = new NoopBookmarkManagerImpl();
			var current = bookmarkManager.getBookmarks(Function.identity());
			assertThat(current).isEmpty();
			bookmarkManager.updateBookmarks(Function.identity(), List.of(), List.of("test"));
			current = bookmarkManager.getBookmarks(Function.identity());
			assertThat(current).isEmpty();
			bookmarkManager.updateBookmarks(Function.identity(), List.of("test"), List.of());
			current = bookmarkManager.getBookmarks(Function.identity());
			assertThat(current).isEmpty();
		}

	}

	@Nested
	class Shared {

		@ParameterizedTest
		@ValueSource(classes = { DefaultBookmarkManagerImpl.class, NoopBookmarkManagerImpl.class })
		void shouldRejectNullNewBookmarks(Class<? extends BookmarkManager> type) throws Exception {
			BookmarkManager bookmarkManager = type.getDeclaredConstructor().newInstance();
			Function<Bookmark, String> transformer = Bookmark::value;
			List<Bookmark> used = List.of();
			assertThatNullPointerException().isThrownBy(() -> bookmarkManager.updateBookmarks(transformer, used, null))
				.withMessage("New bookmarks might not be null");
		}

		@ParameterizedTest
		@ValueSource(classes = { DefaultBookmarkManagerImpl.class, NoopBookmarkManagerImpl.class })
		void nullOrEmptyUsedBookmarksAreFine(Class<? extends BookmarkManager> type) throws Exception {
			BookmarkManager bookmarkManager = type.getDeclaredConstructor().newInstance();
			Function<Bookmark, String> transformer = Bookmark::value;
			List<Bookmark> newBookmarks = List.of();
			assertThatNoException().isThrownBy(() -> bookmarkManager.updateBookmarks(transformer, null, newBookmarks));
		}

		@ParameterizedTest
		@ValueSource(classes = { DefaultBookmarkManagerImpl.class, NoopBookmarkManagerImpl.class })
		void transformerIsRequired(Class<? extends BookmarkManager> type) throws Exception {
			BookmarkManager bookmarkManager = type.getDeclaredConstructor().newInstance();
			List<Bookmark> used = List.of();
			List<Bookmark> newBookmarks = List.of();
			assertThatNullPointerException().isThrownBy(() -> bookmarkManager.updateBookmarks(null, used, newBookmarks))
				.withMessage("A function for deriving a String value from a bookmark is required");
		}

	}

}
