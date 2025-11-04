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
import java.util.Iterator;

import org.neo4j.jdbc.Neo4jTransaction.PullResponse;
import org.neo4j.jdbc.Neo4jTransaction.RunResponse;
import org.neo4j.jdbc.values.Record;

/**
 * A Bolt response based {@link Cursor} implementation.
 *
 * @author Michael J. Simons
 * @since 6.10.0
 */
final class BoltCursor extends AbstractCursor {

	private final Neo4jTransaction transaction;

	private final RunResponse runResponse;

	private final Runnable onNextBatch;

	private int fetchSize;

	private int remainingRowAllowance;

	private Iterator<Record> currentBatch;

	private PullResponse currentBatchResponse;

	BoltCursor(Record sampleRecord, Neo4jTransaction transaction, RunResponse runResponse, int remainingRowAllowance,
			int fetchSize, PullResponse currentBatchResponse, Iterator<Record> currentBatch, Runnable onNextBatch) {
		super(sampleRecord);

		this.transaction = transaction;
		this.runResponse = runResponse;
		this.onNextBatch = onNextBatch;
		this.fetchSize = fetchSize;

		this.remainingRowAllowance = remainingRowAllowance;
		this.currentBatchResponse = currentBatchResponse;
		this.currentBatch = currentBatch;
	}

	@Override
	public boolean next() throws SQLException {
		if (this.remainingRowAllowance == 0) {
			return false;
		}
		if (this.currentBatch.hasNext()) {
			return pullNext();
		}
		if (this.currentBatchResponse.hasMore()) {
			this.currentBatchResponse = this.transaction.pull(this.runResponse, calculateFetchSize());
			this.currentBatch = this.currentBatchResponse.records().iterator();
			this.onNextBatch.run();
			return pullNext();
		}
		this.currentRecord = null;
		return false;
	}

	@Override
	public boolean isLast() {
		return this.currentBatch.hasNext() || this.currentBatchResponse.hasMore();
	}

	@Override
	public int getFetchSize() {
		return this.fetchSize;
	}

	@Override
	public void setFetchSize(int fetchSize) throws SQLException {
		if (fetchSize <= 0) {
			throw new Neo4jException(Neo4jException.GQLError.$22N02.withTemplatedMessage("fetch size", fetchSize));
		}
		this.fetchSize = fetchSize;
	}

	@Override
	public void close() throws SQLException {
		if (this.transaction.isAutoCommit() && this.transaction.isRunnable()) {
			this.transaction.commit();
		}
	}

	private boolean pullNext() {
		this.currentRecord = this.currentBatch.next();
		++this.currentRowNum;
		decrementRemainingRowAllowance();
		return this.currentRecord != null;
	}

	private int calculateFetchSize() {
		return (this.remainingRowAllowance > 0) ? Math.min(this.remainingRowAllowance, this.fetchSize) : this.fetchSize;
	}

	private void decrementRemainingRowAllowance() {
		if (this.remainingRowAllowance > 0) {
			this.remainingRowAllowance--;
		}
	}

}
