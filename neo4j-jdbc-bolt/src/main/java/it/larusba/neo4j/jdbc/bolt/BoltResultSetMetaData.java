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
package it.larusba.neo4j.jdbc.bolt;

import it.larusba.neo4j.jdbc.ResultSetMetaData;
import org.neo4j.driver.v1.StatementResult;

import java.sql.SQLException;
import java.util.List;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltResultSetMetaData extends ResultSetMetaData implements Loggable {

	StatementResult iterator = null;
	private boolean      loggable = false;
	private List<String> keys     = null;

	BoltResultSetMetaData(StatementResult iterator, List<String> keys) {
		this.iterator = iterator;
		this.keys = keys;
	}

	@Override public int getColumnCount() throws SQLException {
		if (this.iterator == null) {
			throw new SQLException("ResultCursor not initialized");
		}
		return this.keys.size();
	}

	@Override public String getColumnLabel(int column) throws SQLException {
		return this.getColumnName(column);
	}

	@Override public String getColumnName(int column) throws SQLException {
		if (this.keys == null || column > this.keys.size() || column <= 0) {
			throw new SQLException("Column out of range");
		}
		return this.keys.get(column - 1);
	}

	@Override public String getCatalogName(int column) throws SQLException {
		return ""; //not applicable
	}

	@Override public boolean isLoggable() {
		return this.loggable;
	}

	@Override public void setLoggable(boolean loggable) {
		this.loggable = loggable;
	}
}
