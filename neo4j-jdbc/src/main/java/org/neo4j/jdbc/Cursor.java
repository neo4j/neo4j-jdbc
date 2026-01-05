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
package org.neo4j.jdbc;

import java.sql.SQLException;
import java.util.List;

import org.neo4j.jdbc.values.Record;

/**
 * A cursor abstracting away the iteration of either Bolt pull responses or any other
 * iterable structure.
 *
 * @author Michael J. Simons
 * @since 6.10.0
 */
interface Cursor {

	/**
	 * Creates a cursor based on a Bolt connection.
	 * @param transaction current transaction
	 * @param runResponse the initial response
	 * @param remainingRowAllowance maximum number of rows toe be retrieved
	 * @param fetchSize the fetch size to be used
	 * @param currentBatchResponse the initial response
	 * @param onNextBatch a callback that should be invoked when another batch is pulled
	 * @return a new cursor
	 */
	static Cursor of(Neo4jTransaction transaction, Neo4jTransaction.RunResponse runResponse, int remainingRowAllowance,
			int fetchSize, Neo4jTransaction.PullResponse currentBatchResponse, Runnable onNextBatch) {
		var records = currentBatchResponse.records();
		var currentBatch = records.iterator();
		return new BoltCursor(records.isEmpty() ? null : records.get(0), transaction, runResponse,
				remainingRowAllowance, fetchSize, currentBatchResponse, currentBatch, onNextBatch);
	}

	/**
	 * Creates a local, fully populated cursor.
	 * @param records a list of records
	 * @return a new cursor
	 */
	static Cursor of(List<Record> records) {
		return new LocalCursor(records);
	}

	/**
	 * Moves this cursor forward. If a new record could be fetched, this method returns
	 * {@literal true}.
	 * @return true if a new record was fetched
	 * @throws SQLException on any mischief that might happen
	 */
	boolean next() throws SQLException;

	/**
	 * {@return the latest record that was fetched by this cursor}
	 */
	Record getCurrentRecord();

	/**
	 * {@return the 1 based index of the current row this cursor points to}
	 */
	int getCurrentRowNum();

	/**
	 * {@return optional sample record of records that can be fetched by this cursor}
	 */
	Record getSampleRecord();

	/**
	 * {@return true if the current record was the last record}
	 */
	boolean isLast();

	/**
	 * Configures a new fetch size for this cursor.
	 * @param fetchSize new fetch size
	 */
	void setFetchSize(int fetchSize) throws SQLException;

	/**
	 * {@return fetch size of this cursor}
	 */
	int getFetchSize();

	/**
	 * Closes this cursor and cleans up all resources associated with it.
	 */
	default void close() throws SQLException {
	}

}
