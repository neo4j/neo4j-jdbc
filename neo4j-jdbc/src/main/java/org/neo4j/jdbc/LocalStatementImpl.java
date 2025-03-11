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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.neo4j.jdbc.Neo4jTransaction.PullResponse;
import org.neo4j.jdbc.Neo4jTransaction.RunResponse;

/**
 * This is intended as almost a no-op implementation of a statement that we mainly return
 * from metadata calls.
 *
 * @author Conor Watson
 */
final class LocalStatementImpl extends StatementImpl {

	private final Connection connection;

	private final RunResponse runResponse;

	private final PullResponse pullResponse;

	private boolean closeOnCompletion;

	private boolean closed;

	private final AtomicBoolean resultSetAcquired = new AtomicBoolean(false);

	LocalStatementImpl(Connection connection, RunResponse runResponse, PullResponse pullResponse) {
		this.connection = connection;
		this.runResponse = runResponse;
		this.pullResponse = pullResponse;
	}

	@Override
	public ResultSet executeQuery(String sql) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public int executeUpdate(String sql) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void close() {
		this.closed = true;
	}

	@Override
	public void cancel() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public boolean execute(String sql) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public ResultSet getResultSet() throws SQLException {

		if (!this.resultSetAcquired.compareAndSet(false, true)) {
			throw new SQLException("Result set has already been acquired");
		}
		return new ResultSetImpl(this, new ThrowingTransactionImpl(), this.runResponse, this.pullResponse, -1, -1, -1);
	}

	@Override
	public int getUpdateCount() throws SQLException {
		assertIsOpen();
		return -1;
	}

	@Override
	public boolean getMoreResults() {
		return !this.resultSetAcquired.get();
	}

	@Override
	public void setFetchSize(int rows) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public int getFetchSize() {
		return -1;
	}

	@Override
	public int[] executeBatch() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Connection getConnection() {
		return this.connection;
	}

	@Override
	public boolean getMoreResults(int current) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public ResultSet getGeneratedKeys() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public int executeUpdate(String sql, String[] columnNames) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public boolean execute(String sql, int[] columnIndexes) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public boolean execute(String sql, String[] columnNames) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public int getResultSetHoldability() throws SQLException {
		assertIsOpen();
		return ResultSet.CLOSE_CURSORS_AT_COMMIT;
	}

	@Override
	public boolean isClosed() {
		return this.closed;
	}

	// While this statement or more specific its result set does not complete as it is
	// always "complete" and therefor
	// closing on completion can't happen, we decided to provide these implementations
	// essentially as no-ops for better
	// quality of life for people implementing any flow that sets {@code
	// closeOnCompletion}.
	@Override
	public void closeOnCompletion() {
		this.closeOnCompletion = true;
	}

	@Override
	public boolean isCloseOnCompletion() {
		return this.closeOnCompletion;
	}

	protected void assertIsOpen() throws SQLException {
		if (this.closed) {
			throw new SQLException("The statement set is closed");
		}
	}

}
