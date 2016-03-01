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
 * Created on 11/02/16
 */
package it.neo4j.jdbc.bolt;

import it.neo4j.jdbc.ResultSet;
import org.neo4j.driver.v1.ResultCursor;

import java.sql.SQLException;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltResultSet extends ResultSet {

	private ResultCursor cursor;

	public BoltResultSet(ResultCursor cursor) {
		this.cursor = cursor;
	}

	@Override public boolean next() throws SQLException {
		if (this.cursor == null) {
			throw new SQLException("ResultCursor not initialized");
		}
		if (this.cursor.position() == this.cursor.size()) {
			return false;
		} else {
			return this.cursor.next();
		}
	}

	@Override public String getString(String columnLabel) throws SQLException {
		if (!this.cursor.containsKey(columnLabel)) {
			throw new SQLException("Column not present in ResultSet");
		}
		return this.cursor.get(columnLabel).asString();
	}

	@Override public int getInt(String columnLabel) throws SQLException {
		if (!this.cursor.containsKey(columnLabel)) {
			throw new SQLException("Column not present in ResultSet");
		}
		return this.cursor.get(columnLabel).asInt();
	}

	@Override public int findColumn(String columnLabel) throws SQLException {
		if (!this.cursor.containsKey(columnLabel)) {
			throw new SQLException("Column not present in ResultSet");
		}
		this.cursor.next();
		return this.cursor.index(columnLabel);
	}

	@Override public String getString(int columnIndex) throws SQLException {
		if (columnIndex - 1 > this.cursor.size()) {
			throw new SQLException("Column not present in ResultSet");
		}
		return this.cursor.get(columnIndex - 1).asString();
	}

	@Override public int getInt(int columnIndex) throws SQLException {
		if (columnIndex - 1 > this.cursor.size()) {
			throw new SQLException("Column not present in ResultSet");
		}
		return this.cursor.get(columnIndex - 1).asInt();
	}

	@Override public float getFloat(String columnLabel) throws SQLException {
		if (!this.cursor.containsKey(columnLabel)) {
			throw new SQLException("Column not present in ResultSet");
		}
		return this.cursor.get(columnLabel).asFloat();
	}

	@Override public float getFloat(int columnIndex) throws SQLException {
		if (columnIndex - 1 > this.cursor.size()) {
			throw new SQLException("Column not present in ResultSet");
		}
		return this.cursor.get(columnIndex - 1).asFloat();
	}

	@Override public short getShort(String columnLabel) throws SQLException {
		if (!this.cursor.containsKey(columnLabel)) {
			throw new SQLException("Column not present in ResultSet");
		}
		return (short) this.cursor.get(columnLabel).asInt();
	}

	@Override public short getShort(int columnIndex) throws SQLException {
		if (columnIndex - 1 > this.cursor.size()) {
			throw new SQLException("Column not present in ResultSet");
		}
		return (short) this.cursor.get(columnIndex - 1).asInt();
	}

	@Override public double getDouble(int columnIndex) throws SQLException {
		if (columnIndex - 1 > this.cursor.size()) {
			throw new SQLException("Column not present in ResultSet");
		}
		return this.cursor.get(columnIndex - 1).asDouble();
	}

	@Override public double getDouble(String columnLabel) throws SQLException {
		if (!this.cursor.containsKey(columnLabel)) {
			throw new SQLException("Column not present in ResultSet");
		}
		return this.cursor.get(columnLabel).asDouble();
	}

	@Override public boolean previous() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override public boolean first() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override public boolean last() throws SQLException {
		throw new UnsupportedOperationException();
	}

}
