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

import java.sql.Driver;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Properties;

/**
 * Neo4j specific extensions to a {@link Driver}.
 *
 * @author Michael J. Simons
 * @since 6.0.0
 */
public sealed interface Neo4jDriverExtensions extends Driver, Neo4jMetadataWriter permits Neo4jDriver {

	/**
	 * Retrieves the bookmarks currently known to this driver.
	 * @param url the URL of the dbms for which the bookmarks are to be retrieved
	 * @return the collection of {@link Bookmark bookmarks} known to this driver for the
	 * given url.
	 * @throws SQLException in all error cases
	 * @see #getCurrentBookmarks(String, Properties)
	 */
	default Collection<Bookmark> getCurrentBookmarks(String url) throws SQLException {
		return getCurrentBookmarks(url, new Properties());
	}

	/**
	 * Retrieves the bookmarks currently known to this driver. The order is not important
	 * and the collection can be passed as is to another instance of the Neo4j JDBC Driver
	 * via {@link #addBookmarks(String, Collection)} as is.
	 * <p>
	 * Take note that the combination of {@code url} and {@code info} must be the same as
	 * originally used during connect.
	 * @param url the URL of the dbms for which the bookmarks are to be retrieved
	 * @param info additional properties that might have been specified on
	 * {@link #connect(String, Properties)}, can be {@literal null}
	 * @return the collection of {@link Bookmark bookmarks} known to this driver for the
	 * given url.
	 * @throws SQLException in all error cases
	 */
	Collection<Bookmark> getCurrentBookmarks(String url, Properties info) throws SQLException;

	/**
	 * Adds bookmarks to this driver instance to be used when opening a new connection to
	 * the same url.
	 * @param url the URL of the dbms for which the bookmarks are to be set
	 * @param bookmarks a list of bookmarks
	 * @throws SQLException in all error cases
	 * @see #addBookmarks(String, Properties, Collection)
	 */
	default void addBookmarks(String url, Collection<Bookmark> bookmarks) throws SQLException {
		this.addBookmarks(url, new Properties(), bookmarks);
	}

	/**
	 * Adds bookmarks to this driver instance to be used when opening a new connection to
	 * the same url. The collection does not need to be sorted nor does it need to consist
	 * of unique items. The driver and more important, Neo4j cluster will figure the
	 * latest bookmark for you.
	 * <p>
	 * Take note that the combination of {@code url} and {@code info} must be the same as
	 * originally used during connect.
	 * @param url the URL of the dbms for which the bookmarks are to be set
	 * @param info additional properties that might have been specified on
	 * {@link #connect(String, Properties)}, can be {@literal null}
	 * @param bookmarks a list of bookmarks
	 * @throws SQLException in all error cases
	 */
	void addBookmarks(String url, Properties info, Collection<Bookmark> bookmarks) throws SQLException;

}
