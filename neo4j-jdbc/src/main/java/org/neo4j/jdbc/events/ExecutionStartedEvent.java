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
package org.neo4j.jdbc.events;

/**
 * Will be fired before the execution of a statement happens.
 *
 * @param id a generated id to correlate this event to the corresponding
 * {@link ExecutionEndedEvent end event}
 * @param statement the statement to be executed. This will always the original statement
 * passed to execute, not a potentially translated one.
 * @param type the type of the execution (as defined by the JDBC spec)
 * @author Michael J. Simons
 * @since 6.3.0
 */
public record ExecutionStartedEvent(String id, String statement, ExecutionType type) {

	public enum ExecutionType {

		/**
		 * Used with {@link java.sql.Statement#execute(String)} and overloads.
		 */
		PLAIN,
		/**
		 * Used with {@link java.sql.Statement#executeQuery(String)} and overloads.
		 */
		QUERY,
		/**
		 * Used with {@link java.sql.Statement#executeUpdate(String)} and overloads.
		 */
		UPDATE

	}
}
