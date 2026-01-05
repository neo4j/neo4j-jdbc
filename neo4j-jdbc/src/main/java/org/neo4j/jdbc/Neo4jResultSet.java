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

import java.sql.ResultSet;

import org.neo4j.jdbc.events.ResultSetListener;
import org.neo4j.jdbc.values.Record;

/**
 * A Neo4j specific extension of a {@link ResultSet}. It may be referred to for use with
 * {@link #unwrap(Class)} to access specific Neo4j functionality.
 *
 * @author Michael J. Simons
 * @since 6.3.0
 */
public sealed interface Neo4jResultSet extends ResultSet permits ResultSetImpl {

	/**
	 * Adds a listener to this statement that gets notified on starts and finish of
	 * iteration and whenever a new batch is pulled from the database.
	 * @param resultSetListener the lister to add to this result set
	 * @since 6.3.0
	 */
	void addListener(ResultSetListener resultSetListener);

	/**
	 * {@return the current record if any}
	 * @since 6.10.0
	 */
	Record getCurrentRecord();

}
