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
package org.neo4j.jdbc;

import java.sql.Driver;
import java.util.Collection;

/**
 * Neo4j specific extensions to a {@link Driver}.
 *
 * @author Michael J. Simons
 * @since 6.0.0
 */
public sealed interface Neo4jDriverExtensions extends Driver permits Neo4jDriver {

	/**
	 * Retrieves the bookmarks currently known to this driver. The order is not important
	 * and the collection can be passed as is to another instance of the Neo4j JDBC Driver
	 * via {@link #addBookmarks(Collection)} as is.
	 * @return the collection of {@link Bookmark bookmarks} known to this driver.
	 */
	Collection<Bookmark> getCurrentBookmarks();

	/**
	 * Adds bookmarks to this driver instance to be used when opening a new connection.
	 * The collection does not need to be sorted nor does it need to consist of unique
	 * items. The driver and more important, Neo4j cluster will figure the latest bookmark
	 * for you.
	 * @param bookmarks a list of bookmarks
	 */
	void addBookmarks(Collection<Bookmark> bookmarks);

}
