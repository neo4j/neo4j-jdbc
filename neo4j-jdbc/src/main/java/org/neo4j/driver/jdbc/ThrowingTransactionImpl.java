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
package org.neo4j.driver.jdbc;

import java.sql.SQLException;
import java.util.Map;

import org.neo4j.driver.jdbc.internal.bolt.response.DiscardResponse;
import org.neo4j.driver.jdbc.internal.bolt.response.PullResponse;
import org.neo4j.driver.jdbc.internal.bolt.response.RunResponse;

class ThrowingTransactionImpl implements Neo4jTransaction {

	@Override
	public RunAndPullResponses runAndPull(String query, Map<String, Object> parameters, int fetchSize, int timeout)
			throws SQLException {
		throw new SQLException("Unsupported operation.");
	}

	@Override
	public DiscardResponse runAndDiscard(String query, Map<String, Object> parameters, int timeout, boolean commit)
			throws SQLException {
		throw new SQLException("Unsupported operation.");
	}

	@Override
	public PullResponse pull(RunResponse runResponse, long request) throws SQLException {
		throw new SQLException("Unsupported operation.");
	}

	@Override
	public void commit() throws SQLException {
		throw new SQLException("Unsupported operation.");
	}

	@Override
	public void rollback() throws SQLException {
		throw new SQLException("Unsupported operation.");
	}

	@Override
	public void fail(SQLException exception) {

	}

	@Override
	public boolean isAutoCommit() {
		return false;
	}

	@Override
	public State getState() {
		return State.ROLLEDBACK;
	}

}
