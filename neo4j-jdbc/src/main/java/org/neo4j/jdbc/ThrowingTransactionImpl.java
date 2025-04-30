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
package org.neo4j.jdbc;

import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Map;

import org.neo4j.jdbc.Neo4jException.GQLError;

/**
 * Will throw on anything modifying run and pull responses, default to no-op for
 * commiting, failing and rollback.
 *
 * @author Neo4j Drivers Team
 */
final class ThrowingTransactionImpl implements Neo4jTransaction {

	private State state = State.READY;

	@Override
	public RunAndPullResponses runAndPull(String query, Map<String, Object> parameters, int fetchSize, int timeout)
			throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public DiscardResponse runAndDiscard(String query, Map<String, Object> parameters, int timeout, boolean commit)
			throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public PullResponse pull(RunResponse runResponse, long request) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void commit() throws SQLException {
		if (this.state != State.READY) {
			throw new Neo4jException(
					GQLError.$2DN01.withTemplatedMessage("Cannot commit in %s state".formatted(this.state)));
		}
		this.state = State.COMMITTED;
	}

	@Override
	public void rollback() throws SQLException {
		if (this.state != State.READY) {
			throw new Neo4jException(
					GQLError.$40N01.withTemplatedMessage("Cannot rollback in %s state".formatted(this.state)));
		}
		this.state = State.ROLLEDBACK;
	}

	@Override
	public void fail(SQLException exception) throws SQLException {
		if (this.state != State.READY) {
			throw new Neo4jException(
					GQLError.$2DN03.withTemplatedMessage("Cannot fail in %s state".formatted(this.state)));
		}
		this.state = State.FAILED;
	}

	@Override
	public boolean isAutoCommit() {
		return false;
	}

	@Override
	public State getState() {
		return this.state;
	}

}
