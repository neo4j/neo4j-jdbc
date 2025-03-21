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
 * Defines a listener on relevant {@link org.neo4j.jdbc.Neo4jDriver} events.
 *
 * @author Michael J. Simons
 * @since 6.3.0
 */
public interface DriverListener {

	/**
	 * Will be called on any newly opened connection.
	 * @param event the corresponding event
	 */
	default void connectionOpened(ConnectionOpenedEvent event) {
	}

	/**
	 * Will be called when a connection is closed or aborted.
	 * @param event the corresponding event
	 */
	default void connectionClosed(ConnectionClosedEvent event) {
	}

}
