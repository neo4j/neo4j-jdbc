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
import org.neo4j.driver.v1.types.Type;
import org.neo4j.jdbc.Neo4jResultSetMetaData;
import org.neo4j.jdbc.utils.Neo4jInvocationHandler;

import java.lang.reflect.Proxy;
import java.sql.Array;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltNeo4jResultSetMetaData extends Neo4jResultSetMetaData {

	private Type[] columnType;
	private static final Map<Type, Class> INTERNAL_TYPE_TO_CLASS_MAP = new HashMap<>();
	private static final Map<Type, Integer> INTERNAL_TYPE_TO_SQL_TYPES_MAP = new HashMap<>();

	static {
		INTERNAL_TYPE_TO_CLASS_MAP.put(InternalTypeSystem.TYPE_SYSTEM.STRING(), String.class);
		INTERNAL_TYPE_TO_CLASS_MAP.put(InternalTypeSystem.TYPE_SYSTEM.INTEGER(), Long.class);
		INTERNAL_TYPE_TO_CLASS_MAP.put(InternalTypeSystem.TYPE_SYSTEM.BOOLEAN(), Boolean.class);
		INTERNAL_TYPE_TO_CLASS_MAP.put(InternalTypeSystem.TYPE_SYSTEM.FLOAT(), Double.class);
		INTERNAL_TYPE_TO_CLASS_MAP.put(InternalTypeSystem.TYPE_SYSTEM.NODE(), Object.class);
		INTERNAL_TYPE_TO_CLASS_MAP.put(InternalTypeSystem.TYPE_SYSTEM.RELATIONSHIP(), Object.class);
		INTERNAL_TYPE_TO_CLASS_MAP.put(InternalTypeSystem.TYPE_SYSTEM.PATH(), Object.class);
		INTERNAL_TYPE_TO_CLASS_MAP.put(InternalTypeSystem.TYPE_SYSTEM.MAP(), Map.class);
		INTERNAL_TYPE_TO_CLASS_MAP.put(InternalTypeSystem.TYPE_SYSTEM.ANY(), Object.class);
		INTERNAL_TYPE_TO_CLASS_MAP.put(InternalTypeSystem.TYPE_SYSTEM.LIST(), Array.class);
		INTERNAL_TYPE_TO_CLASS_MAP.put(InternalTypeSystem.TYPE_SYSTEM.NUMBER(), Double.class); // to be decided
		INTERNAL_TYPE_TO_CLASS_MAP.put(InternalTypeSystem.TYPE_SYSTEM.DATE(), java.sql.Date.class);
		INTERNAL_TYPE_TO_CLASS_MAP.put(InternalTypeSystem.TYPE_SYSTEM.DATE_TIME(), java.sql.Timestamp.class);
		INTERNAL_TYPE_TO_CLASS_MAP.put(InternalTypeSystem.TYPE_SYSTEM.LOCAL_DATE_TIME(), java.sql.Timestamp.class);
		INTERNAL_TYPE_TO_CLASS_MAP.put(InternalTypeSystem.TYPE_SYSTEM.TIME(), java.sql.Time.class);
		INTERNAL_TYPE_TO_CLASS_MAP.put(InternalTypeSystem.TYPE_SYSTEM.LOCAL_TIME(), java.sql.Time.class);

		INTERNAL_TYPE_TO_SQL_TYPES_MAP.put(InternalTypeSystem.TYPE_SYSTEM.STRING(), Types.VARCHAR);
		INTERNAL_TYPE_TO_SQL_TYPES_MAP.put(InternalTypeSystem.TYPE_SYSTEM.INTEGER(), Types.INTEGER);
		INTERNAL_TYPE_TO_SQL_TYPES_MAP.put(InternalTypeSystem.TYPE_SYSTEM.BOOLEAN(), Types.BOOLEAN);
		INTERNAL_TYPE_TO_SQL_TYPES_MAP.put(InternalTypeSystem.TYPE_SYSTEM.FLOAT(), Types.FLOAT);
		INTERNAL_TYPE_TO_SQL_TYPES_MAP.put(InternalTypeSystem.TYPE_SYSTEM.NODE(), Types.JAVA_OBJECT);
		INTERNAL_TYPE_TO_SQL_TYPES_MAP.put(InternalTypeSystem.TYPE_SYSTEM.RELATIONSHIP(), Types.JAVA_OBJECT);
		INTERNAL_TYPE_TO_SQL_TYPES_MAP.put(InternalTypeSystem.TYPE_SYSTEM.PATH(), Types.JAVA_OBJECT);
		INTERNAL_TYPE_TO_SQL_TYPES_MAP.put(InternalTypeSystem.TYPE_SYSTEM.MAP(), Types.JAVA_OBJECT);
		INTERNAL_TYPE_TO_SQL_TYPES_MAP.put(InternalTypeSystem.TYPE_SYSTEM.ANY(), Types.JAVA_OBJECT);
		INTERNAL_TYPE_TO_SQL_TYPES_MAP.put(InternalTypeSystem.TYPE_SYSTEM.NULL(), Types.NULL);
		INTERNAL_TYPE_TO_SQL_TYPES_MAP.put(InternalTypeSystem.TYPE_SYSTEM.LIST(), Types.ARRAY);
		INTERNAL_TYPE_TO_SQL_TYPES_MAP.put(InternalTypeSystem.TYPE_SYSTEM.NUMBER(), Types.FLOAT); // to be decided
		INTERNAL_TYPE_TO_SQL_TYPES_MAP.put(InternalTypeSystem.TYPE_SYSTEM.DATE(), Types.DATE);
		INTERNAL_TYPE_TO_SQL_TYPES_MAP.put(InternalTypeSystem.TYPE_SYSTEM.DATE_TIME(), Types.TIMESTAMP_WITH_TIMEZONE);
		INTERNAL_TYPE_TO_SQL_TYPES_MAP.put(InternalTypeSystem.TYPE_SYSTEM.LOCAL_DATE_TIME(), Types.TIMESTAMP);
		INTERNAL_TYPE_TO_SQL_TYPES_MAP.put(InternalTypeSystem.TYPE_SYSTEM.TIME(), Types.TIME);
		INTERNAL_TYPE_TO_SQL_TYPES_MAP.put(InternalTypeSystem.TYPE_SYSTEM.LOCAL_TIME(), Types.TIME);
	}

	/**
	 * Default constructor with result iterator and list of column name.
	 *
	 * @param types  List of types
	 * @param keys     List of column name (ie. key)
	 */
	private BoltNeo4jResultSetMetaData(List<Type> types, List<String> keys) {
		super(keys);
		this.columnType = types.toArray(new Type[this.keys.size() + 1]);
	}

	public static ResultSetMetaData newInstance(boolean debug, List<Type> types, List<String> keys) {
		ResultSetMetaData rsmd = new BoltNeo4jResultSetMetaData(types, keys);
		return (ResultSetMetaData) Proxy
				.newProxyInstance(BoltNeo4jResultSetMetaData.class.getClassLoader(), new Class[] { ResultSetMetaData.class }, new Neo4jInvocationHandler(rsmd, debug));
	}

	/**
	 * Override the default implementation.
	 * If the result iterator is not initialized, we throw an exception.
	 */
	@Override public int getColumnCount() throws SQLException {
		return this.keys.size();
	}

	@Override public String getColumnClassName(int column) throws SQLException {
		Type type = this.columnType[column - 1];
		if (InternalTypeSystem.TYPE_SYSTEM.NULL().equals(type)) {
			return null;
		}
		if(INTERNAL_TYPE_TO_CLASS_MAP.containsKey(type)){
			return INTERNAL_TYPE_TO_CLASS_MAP.get(type).getName();
		}
		return Object.class.getName();
	}

	@Override public int getColumnType(int column) throws SQLException {
		Type type = this.columnType[column - 1];
		if(INTERNAL_TYPE_TO_SQL_TYPES_MAP.containsKey(type)){
			return INTERNAL_TYPE_TO_SQL_TYPES_MAP.get(type);
		}
		return Types.JAVA_OBJECT;
	}

	@Override public String getColumnTypeName(int column) throws SQLException {
		if(column > this.getColumnCount()){
			throw new SQLException("Column index out of bound");
		}
		Type type = this.columnType[column - 1];
		return type.name();
	}
}
