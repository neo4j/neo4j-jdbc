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
 * Defines a listener that can react to very detailed events, such as when a batch of
 * records has been pulled from the database. It's well suited to build tracing and
 * similar features
 *
 * @author Michael J. Simons
 * @since 6.3.0
 */
public interface Neo4jEventListener {

	/**
	 * Reacts on a generic {@link Neo4jEvent}, which will carry all relevant details.
	 * @param event the event to react on
	 */
	default void on(Neo4jEvent event) {
	}

}
