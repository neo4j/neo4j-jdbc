/*
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
package org.neo4j.jdbc.bolt;

import org.neo4j.driver.internal.types.InternalTypeSystem;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.exceptions.NoSuchRecordException;
import org.neo4j.driver.v1.types.Type;
import org.neo4j.jdbc.Loggable;
import org.neo4j.jdbc.ResultSetMetaData;

import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltResultSetMetaData extends ResultSetMetaData implements Loggable {

	private StatementResult iterator = null;
	private boolean         loggable = false;
	private Type[] columnType;

	/**
	 * Default constructor with result iterator and list of column name.
	 *
	 * @param iterator The result iterator
	 * @param keys     List of column name (ie. key)
	 */
	BoltResultSetMetaData(StatementResult iterator, List<String> keys) {
		super(keys);
		this.iterator = iterator;
		this.columnType = new Type[this.keys.size() + 1];

		// we init columnType with the first record
		// in case first == last record
		for (int i = 1; i <= this.keys.size(); i++) {
			columnType[i] = this.getColumnDriverTypeOrDefault(i, InternalTypeSystem.TYPE_SYSTEM.STRING());
		}
	}

	/**
	 * Override the default implementation.
	 * If the result iterator is not initialized, we throw an exception.
	 */
	@Override public int getColumnCount() throws SQLException {
		if (this.iterator == null) {
			throw new SQLException("ResultCursor not initialized");
		}
		return this.keys.size();
	}

	@Override
	public String getColumnClassName(int column) throws SQLException {
		Type type = this.getColumnDriverTypeOrDefault(column, columnType[column]);

		if (InternalTypeSystem.TYPE_SYSTEM.STRING().equals(type)) {
			return String.class.getName();
		}
		if (InternalTypeSystem.TYPE_SYSTEM.INTEGER().equals(type)) {
			return Long.class.getName();
		}
		if (InternalTypeSystem.TYPE_SYSTEM.BOOLEAN().equals(type)) {
			return Boolean.class.getName();
		}
		if (InternalTypeSystem.TYPE_SYSTEM.FLOAT().equals(type)) {
			return Double.class.getName();
		}
		if (InternalTypeSystem.TYPE_SYSTEM.NODE().equals(type)) {
			return Object.class.getName();
		}
		if (InternalTypeSystem.TYPE_SYSTEM.RELATIONSHIP().equals(type)) {
			return Object.class.getName();
		}
		if (InternalTypeSystem.TYPE_SYSTEM.PATH().equals(type)) {
			return Object.class.getName();
		}
		if (InternalTypeSystem.TYPE_SYSTEM.MAP().equals(type)) {
			return Map.class.getName();
		}
		if (InternalTypeSystem.TYPE_SYSTEM.ANY().equals(type)) {
			return Object.class.getName();
		}
		if (InternalTypeSystem.TYPE_SYSTEM.NULL().equals(type)) {
			return null;
		}
		if (InternalTypeSystem.TYPE_SYSTEM.LIST().equals(type)) {
			return List.class.getName();
		}

		return Object.class.getName();
	}

	@Override public int getColumnType(int column) throws SQLException {
		Type type = this.getColumnDriverTypeOrDefault(column, columnType[column]);
		int resultType = 0;

		if (InternalTypeSystem.TYPE_SYSTEM.STRING().equals(type)) {
			resultType = Types.VARCHAR;
		}
		if (InternalTypeSystem.TYPE_SYSTEM.INTEGER().equals(type)) {
			resultType = Types.INTEGER;
		}
		if (InternalTypeSystem.TYPE_SYSTEM.BOOLEAN().equals(type)) {
			resultType = Types.BOOLEAN;
		}
		if (InternalTypeSystem.TYPE_SYSTEM.FLOAT().equals(type)) {
			resultType = Types.NUMERIC;
		}
		if (InternalTypeSystem.TYPE_SYSTEM.NODE().equals(type)) {
			resultType = Types.JAVA_OBJECT;
		}
		if (InternalTypeSystem.TYPE_SYSTEM.RELATIONSHIP().equals(type)) {
			resultType = Types.JAVA_OBJECT;
		}
		if (InternalTypeSystem.TYPE_SYSTEM.PATH().equals(type)) {
			resultType = Types.JAVA_OBJECT;
		}
		if (InternalTypeSystem.TYPE_SYSTEM.MAP().equals(type)) {
			resultType = Types.JAVA_OBJECT;
		}
		if (InternalTypeSystem.TYPE_SYSTEM.ANY().equals(type)) {
			resultType = Types.JAVA_OBJECT;
		}
		if (InternalTypeSystem.TYPE_SYSTEM.NULL().equals(type)) {
			resultType = Types.NULL;
		}
		if (InternalTypeSystem.TYPE_SYSTEM.LIST().equals(type)) {
			resultType = Types.ARRAY;
		}

		return resultType;
	}

	@Override public String getColumnTypeName(int column) throws SQLException {
		Type type = this.getColumnDriverTypeOrDefault(column, columnType[column]);
		return type.name();
	}

	/**
	 * Return the driver column type from the next record if it's possible.
	 * If there is no `next record`, this method return the specify default type.
	 *
	 * @param column index of the JDBC column (start from 1)
	 * @param def The default type
	 * @return Driver type of the column
	 */
	private Type getColumnDriverTypeOrDefault(int column, Type def) {
		// Default type
		Type type = def;
		try {
			type = this.iterator.peek().get(column - 1).type();
		} catch (NoSuchRecordException e) {
			// Silent exception !
			// here there is no next record (case for the last record)
		}

		return type;
	}

	/*--------------------*/
	/*       Logger       */
	/*--------------------*/

	@Override public boolean isLoggable() {
		return this.loggable;
	}

	@Override public void setLoggable(boolean loggable) {
		this.loggable = loggable;
	}
}
