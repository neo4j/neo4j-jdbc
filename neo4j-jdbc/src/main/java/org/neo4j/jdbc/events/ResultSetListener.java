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

/**
 * Defines a listener on a {@link org.neo4j.jdbc.Neo4jResultSet} and subtypes. A result
 * set may also send notifications on various events that happens only once and not in a
 * pair of begin and end, too.
 *
 * @author Michael J. Simons
 * @since 6.3.0
 */
public interface ResultSetListener extends Neo4jEventListener {

	/**
	 * Will be called when the iteration of a result set is about to start.
	 * @param event the event carrying the necessary details
	 */
	default void onIterationStarted(IterationStartedEvent event) {
	}

	/**
	 * Will be called when the iteration of a result set is done.
	 * @param event the event carrying the necessary details
	 */
	default void onIterationDone(IterationDoneEvent event) {
	}

	/**
	 * An event that is fired when the iteration of a
	 * {@link org.neo4j.jdbc.Neo4jResultSet} has begun, that is when it's cursor has been
	 * moved from before the first row onto the first row.
	 *
	 * @param id a generated id to correlate this event to the corresponding
	 * {@link IterationDoneEvent end event}
	 * @author Michael J. Simons
	 * @since 6.3.0
	 */
	record IterationStartedEvent(String id) {
	}

	/**
	 * This event is fired when a {@link org.neo4j.jdbc.Neo4jResultSet} has been fully
	 * iterated, hence it's cursor is positioned after the last row or when it's closed.
	 *
	 * @param id a generated id to correlate this event to the corresponding
	 * {@link IterationStartedEvent end event}
	 * @param exhausted will be {@literal true} if the result set has been fully iterated,
	 * i.e. the position is now after the last row
	 * @author Michael J. Simons
	 * @since 6.3.0
	 */
	record IterationDoneEvent(String id, boolean exhausted) {
	}

}
