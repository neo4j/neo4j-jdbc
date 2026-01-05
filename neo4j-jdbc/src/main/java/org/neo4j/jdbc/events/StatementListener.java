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
package org.neo4j.jdbc.events;

import java.net.URI;
import java.sql.Statement;
import java.time.Duration;

/**
 * Defines a listener on a {@link org.neo4j.jdbc.Neo4jStatement} and subtypes. A statement
 * may also send notifications on various events that happens only once and not in a pair
 * of begin and end, too.
 *
 * @author Michael J. Simons
 * @since 6.3.0
 */
public interface StatementListener extends Neo4jEventListener {

	/**
	 * Will be called when the execution of a statement is about to start.
	 * @param event the corresponding event
	 */
	default void onExecutionStarted(ExecutionStartedEvent event) {
	}

	/**
	 * Will be called when the execution of a statement has finished (after bolt run,
	 * before any full materializing of a result set).
	 * @param event the corresponding event
	 */
	default void onExecutionEnded(ExecutionEndedEvent event) {
	}

	/**
	 * Will be fired before the execution of a statement happens.
	 *
	 * @param id a generated id to correlate this event to the corresponding
	 * {@link ExecutionEndedEvent end event}
	 * @param uri The URL of the Neo4j instance that was queried
	 * @param statementType the actual type of the statement as defined in the JDBC spec.
	 * @param executionMode the mode of the execution
	 * @param statement the statement to be executed. This will always the original
	 * statement passed to execute, not a potentially translated one
	 */
	record ExecutionStartedEvent(String id, URI uri, Class<? extends Statement> statementType,
			ExecutionMode executionMode, String statement) {

		/**
		 * The mode how a statement is executed (plain, without any immediate visible
		 * results or updates, as a query or as an update statement).
		 */
		public enum ExecutionMode {

			/**
			 * Used with {@link Statement#execute(String)} and overloads.
			 */
			PLAIN,
			/**
			 * Used with {@link Statement#executeQuery(String)} and overloads.
			 */
			QUERY,
			/**
			 * Used with {@link Statement#executeUpdate(String)} and overloads.
			 */
			UPDATE

		}
	}

	/**
	 * Will be fired at the end of the execution of a
	 * {@link org.neo4j.jdbc.Neo4jStatement} or subtypes thereof.
	 *
	 * @param id a generated id to correlate this event to the corresponding
	 * {@link ExecutionStartedEvent start event}
	 * @param uri the URL of the Neo4j instance that was queried
	 * @param state information about the outcome of the execution (success or not)
	 * @param elapsedTime the elapsed time between sending the original statement to the
	 * database and the end of the first pull or discard. The elapsed time won't take the
	 * full materialization of any corresponding {@link java.sql.ResultSet} into account.
	 */
	record ExecutionEndedEvent(String id, URI uri, State state, Duration elapsedTime) {

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

}
