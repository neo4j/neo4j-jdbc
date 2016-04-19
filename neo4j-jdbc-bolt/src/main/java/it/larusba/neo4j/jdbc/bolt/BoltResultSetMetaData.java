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

import it.larusba.neo4j.jdbc.Loggable;
import it.larusba.neo4j.jdbc.ResultSetMetaData;
import org.neo4j.driver.internal.types.InternalTypeSystem;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.types.Type;

import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltResultSetMetaData extends ResultSetMetaData implements Loggable {

	private StatementResult iterator = null;
	private boolean         loggable = false;

	/**
	 * Default constructor with result iterator and list of column name.
	 *
	 * @param iterator The result iterator
	 * @param keys     List of column name (ie. key)
	 */
	BoltResultSetMetaData(StatementResult iterator, List<String> keys) {
		super(keys);
		this.iterator = iterator;
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

	@Override public int getColumnType(int column) throws SQLException {
		Type type = this.iterator.peek().get(column - 1).type();

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
			resultType = Types.FLOAT;
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

		return resultType;
	}

	@Override public String getColumnTypeName(int column) throws SQLException {
		return this.iterator.peek().get(column - 1).type().name();
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
