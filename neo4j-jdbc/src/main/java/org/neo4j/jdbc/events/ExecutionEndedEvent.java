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

import java.net.URI;
import java.time.Duration;

/**
 * Will be fired at the end of the execution of a {@link org.neo4j.jdbc.Neo4jStatement} or
 * subtypes thereof.
 *
 * @param id a generated id to correlate this event to the corresponding
 * {@link ExecutionStartedEvent start event}
 * @param uri The URL of the Neo4j instance that was queried
 * @param state information about the outcome of the execution (success or not)
 * @param executionMode the mode of the execution (as defined by the JDBC spec)
 * @param elapsedTime the elapsed time between sending the original statement to the
 * database and the end of the first pull or discard. The elapsed time won't take the full
 * materialization of any corresponding {@link java.sql.ResultSet} into account.
 * @author Michael J. Simons
 * @since 6.3.0
 */
public record ExecutionEndedEvent(String id, URI uri, State state, ExecutionMode executionMode, Duration elapsedTime) {

	/**
	 * Possible state of a statement execution.
	 */
	public enum State {

		/**
		 * The statement was executed successfully.
		 */
		SUCCESSFUL,
		/**
		 * Execution failed.
		 */
		FAILED

	}
}
