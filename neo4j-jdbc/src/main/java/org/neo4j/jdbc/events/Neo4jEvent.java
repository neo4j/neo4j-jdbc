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

import java.util.Map;

/**
 * A generic Neo4j specific event that happens once (in contrast for example to a pair of
 * {@link ConnectionListener.StatementCreatedEvent statement opened} and
 * {@link ConnectionListener.StatementClosedEvent statement closed events}).
 *
 * @param type the type of this event
 * @param payload an optional payload, never {@literal null}, but maybe empty
 * @author Michael J. Simons
 * @since 6.3.0
 */
public record Neo4jEvent(Type type, Map<String, Object> payload) {

	/**
	 * Creates a new event with a given type and payload.
	 * @param type the type of the event
	 * @param payload the payload ({@literal null} is safe to use)
	 */
	public Neo4jEvent {
		payload = (payload != null) ? Map.copyOf(payload) : Map.of();
	}

	/**
	 * Creates a new event with a given type and an empty payload.
	 * @param type the type of the event
	 */
	public Neo4jEvent(Type type) {
		this(type, null);
	}

	/**
	 * Possible types of a {@link Neo4jEvent}.
	 */
	public enum Type {

		/**
		 * Fired after the driver applied all SQL translators.
		 */
		SQL_PROCESSED("sqlProcessed"),
		/**
		 * Fired after a bolt transaction has been required.
		 */
		TRANSACTION_ACQUIRED("transactionAcquired"),
		/**
		 * Fired after the first run and initial pull response has been acquired.
		 */
		RUN_AND_PULL_RESPONSE_ACQUIRED("runAndPullResponseAcquired"),
		/**
		 * Fired after the first run request has been sent and the pull response has been
		 * discarded.
		 */
		DISCARD_RESPONSE_ACQUIRED("discardResponseAcquired"),
		/**
		 * Fired after another batch of records has been pulled over bolt.
		 */
		PULLED_NEXT_BATCH("pulledNextBatch");

		Type(String value) {
			this.value = value;
		}

		private final String value;

		@Override
		public String toString() {
			return this.value;
		}

	}
}
