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
import java.util.Iterator;
import java.util.List;

import org.neo4j.jdbc.values.Record;

/**
 * A cursor based on a fully fetched list of {@link Record records}.
 *
 * @author Michael J. Simons
 */
final class LocalCursor extends AbstractCursor {

	private final Iterator<Record> iterator;

	LocalCursor(List<Record> records) {
		super(records.isEmpty() ? null : records.get(0));
		this.iterator = records.iterator();
	}

	@Override
	public boolean next() throws SQLException {
		var hasNext = this.iterator.hasNext();
		if (hasNext) {
			super.currentRecord = this.iterator.next();
			++super.currentRowNum;
		}
		return hasNext;
	}

	@Override
	public boolean isLast() {
		return this.iterator.hasNext();
	}

	@Override
	public void setFetchSize(int fetchSize) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public int getFetchSize() {
		return Integer.MAX_VALUE;
	}

}
