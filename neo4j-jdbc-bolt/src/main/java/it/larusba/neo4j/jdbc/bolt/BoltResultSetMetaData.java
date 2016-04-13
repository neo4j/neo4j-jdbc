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
import org.neo4j.driver.internal.types.InternalTypeSystem;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.types.Type;
import org.omg.CORBA.INTERNAL;

import java.sql.SQLException;
import java.sql.Types;
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

	@Override public int getColumnDisplaySize(int column) throws SQLException {
		int type = this.getColumnType(column);
		int value = 0;
		if (type == Types.VARCHAR) {
			value = 40;
		} else if (type == Types.INTEGER) {
			value = 10;
		} else if (type == Types.BOOLEAN) {
			value = 5;
		} else if (type == Types.FLOAT) {
			value = 15;
		} else if (type == Types.JAVA_OBJECT) {
			value = 60;
		}
		return value;
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

	@Override public int getColumnType(int column) throws SQLException {
		Type type = this.iterator.peek().get(column - 1).type();

		int resultType = 0;

		if(InternalTypeSystem.TYPE_SYSTEM.STRING().equals(type)){
			resultType = Types.VARCHAR;
		}
		if(InternalTypeSystem.TYPE_SYSTEM.INTEGER().equals(type)){
			resultType = Types.INTEGER;
		}
		if(InternalTypeSystem.TYPE_SYSTEM.BOOLEAN().equals(type)){
			resultType = Types.BOOLEAN;
		}
		if(InternalTypeSystem.TYPE_SYSTEM.FLOAT().equals(type)){
			resultType = Types.FLOAT;
		}
		if(InternalTypeSystem.TYPE_SYSTEM.NODE().equals(type)){
			resultType = Types.JAVA_OBJECT;
		}
		if(InternalTypeSystem.TYPE_SYSTEM.RELATIONSHIP().equals(type)){
			resultType = Types.JAVA_OBJECT;
		}
		if(InternalTypeSystem.TYPE_SYSTEM.PATH().equals(type)){
			resultType = Types.JAVA_OBJECT;
		}

		return resultType;
	}

	@Override public String getColumnTypeName(int column) throws SQLException {
		return this.iterator.peek().get(column - 1).type().name();
	}

	@Override public boolean isLoggable() {
		return this.loggable;
	}

	@Override public void setLoggable(boolean loggable) {
		this.loggable = loggable;
	}
}
