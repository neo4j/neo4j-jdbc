/**
 * Copyright (c) 2016 LARUS Business Automation [http://www.larus-ba.it]
 * <p>
 * This file is part of the "LARUS Integration Framework for Neo4j".
 * <p>
 * The "LARUS Integration Framework for Neo4j" is licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * Created on 17/02/16
 */
package it.neo4j.jdbc.bolt;

import it.neo4j.jdbc.ResultSetMetaData;
import org.neo4j.driver.v1.StatementResult;

import java.sql.SQLException;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltResultSetMetaData extends ResultSetMetaData {

	StatementResult iterator = null;
	boolean         debug    = false;

	BoltResultSetMetaData(StatementResult iterator, boolean debug) {
		this.iterator = iterator;
		this.debug = debug;
	}

	BoltResultSetMetaData(StatementResult iterator) {
		this(iterator, false);
	}

	@Override public int getColumnCount() throws SQLException {
		if (this.iterator == null) {
			throw new SQLException("The ResultCursor is null");
		}
		return this.iterator.keys().size();
	}

	@Override public String getColumnLabel(int column) throws SQLException {
		return this.getColumnName(column);
	}

	@Override public String getColumnName(int column) throws SQLException {
		if (this.iterator == null) {
			throw new SQLException("The ResultCursor is null");
		}
		if (column > this.iterator.keys().size() || column < 1) {
			throw new SQLException("Column out of range");
		}
		return this.iterator.keys().get(column - 1);
	}

	@Override public String getCatalogName(int column) throws SQLException {
		return ""; //not applicable
	}
}
