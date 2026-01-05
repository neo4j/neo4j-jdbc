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

/**
 * Defines a listener on a {@link org.neo4j.jdbc.Neo4jConnection}.
 *
 * @author Michael J. Simons
 * @since 6.3.0
 */
public interface ConnectionListener {

	/**
	 * Will be called when a new statement is opened.
	 * @param event the corresponding event
	 */
	default void onStatementCreated(StatementCreatedEvent event) {
	}

	/**
	 * Will be called when a statement is closed.
	 * @param event the corresponding event
	 */
	default void onStatementClosed(StatementClosedEvent event) {
	}

	/**
	 * Will be called when a translation has been cached.
	 * @param event the corresponding event
	 */
	default void onTranslationCached(TranslationCachedEvent event) {
	}

	/**
	 * Will be called when a new authentication has been acquired.
	 * @param event some information about the event
	 * @since 6.6.0
	 */
	default void onNewAuthentication(NewAuthenticationEvent event) {
	}

	/**
	 * This event will be fired when a statement has been created.
	 *
	 * @param uri The URL of the Neo4j instance towards the statement has been opened too.
	 * @param statementType the actual type of the statement as defined in the JDBC spec.
	 * @param statement the actual statement that has been created
	 */
	record StatementCreatedEvent(URI uri, Class<? extends Statement> statementType, Statement statement) {
	}

	/**
	 * Will be fired when a statement has been closed.
	 *
	 * @param uri the URL of the statement that was closed.
	 * @param statementType the actual type of the statement as defined in the JDBC spec.
	 */
	record StatementClosedEvent(URI uri, Class<? extends Statement> statementType) {
	}

	/**
	 * This event will be fired when a translation has been cached.
	 *
	 * @param cacheSize the size of the cache
	 */
	record TranslationCachedEvent(int cacheSize) {
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
