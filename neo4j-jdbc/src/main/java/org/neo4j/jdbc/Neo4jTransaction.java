/*
 * Copyright (c) 2023-2024 "Neo4j,"
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

import java.sql.SQLException;
import java.util.Map;

import org.neo4j.jdbc.internal.bolt.response.DiscardResponse;
import org.neo4j.jdbc.internal.bolt.response.PullResponse;
import org.neo4j.jdbc.internal.bolt.response.RunResponse;

/**
 * A transaction that manages a Bolt transaction on the network level.
 * <p>
 * This interface represents common interactions with Bolt transaction and, therefore, has
 * synchronous methods to represent those.
 *
 * @author Neo4j Drivers Team
 */
interface Neo4jTransaction {

	RunAndPullResponses runAndPull(String query, Map<String, Object> parameters, int fetchSize, int timeout)
			throws SQLException;

	DiscardResponse runAndDiscard(String query, Map<String, Object> parameters, int timeout, boolean commit)
			throws SQLException;

	PullResponse pull(RunResponse runResponse, long request) throws SQLException;

	void commit() throws SQLException;

	void rollback() throws SQLException;

	void fail(SQLException exception) throws SQLException;

	boolean isAutoCommit();

	default boolean isRunnable() {
		return switch (this.getState()) {
			case NEW, READY -> true;
			case OPEN_FAILED, FAILED, COMMITTED, ROLLEDBACK -> false;
		};
	}

	default boolean isOpen() {
		return switch (this.getState()) {
			case NEW, READY, OPEN_FAILED -> true;
			case FAILED, COMMITTED, ROLLEDBACK -> false;
		};
	}

	State getState();

	record RunAndPullResponses(RunResponse runResponse, PullResponse pullResponse) {
	}

	enum State {

		/**
		 * A new unused transaction.
		 */
		NEW,
		/**
		 * A used transaction that is ready for further requests.
		 */
		READY,
		/**
		 * A failed transaction that must be explicitly rolled back. Auto commit
		 * transactions do not support this state and transition directly to the
		 * {@link #FAILED} state on failure.
		 */
		OPEN_FAILED,
		/**
		 * A failed transaction that is no longer manageable.
		 */
		FAILED,
		/**
		 * A successfully committed transaction.
		 */
		COMMITTED,
		/**
		 * A successfully rolled back transaction.
		 */
		ROLLEDBACK

	}

}
