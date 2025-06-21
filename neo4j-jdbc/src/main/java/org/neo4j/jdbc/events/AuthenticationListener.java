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

/**
 * Defines a listener for events related to authentication.
 *
 * @author Michael J. Simons
 * @since 6.6.0
 */
public interface AuthenticationListener {

	/**
	 * Will be called when a new authentication has been acquired.
	 * @param event some information about the event
	 */
	default void onNewAuthentication(NewAuthenticationEvent event) {
	}

	/**
	 * Will be fired after the Neo4j-JDBC driver has acquired a new authentication.
	 *
	 * @param uri the URL of the Neo4j instance that was queried
	 * @param state the state of the new authentication
	 */
	record NewAuthenticationEvent(URI uri, State state) {

		/**
		 * Information about the state of the authentication.
		 */
		public enum State {

			/**
			 * Value indicating a brand-new authentication.
			 */
			NEW,
			/**
			 * Value indicating a refreshed authentication.
			 */
			REFRESHED

		}
	}

}
