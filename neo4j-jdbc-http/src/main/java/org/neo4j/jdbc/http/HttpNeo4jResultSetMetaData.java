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
 * Created on 15/4/2016
 */
package org.neo4j.jdbc.http;

import org.neo4j.jdbc.Neo4jResultSetMetaData;
import org.neo4j.jdbc.http.driver.Neo4jResult;

import java.sql.Array;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpNeo4jResultSetMetaData extends Neo4jResultSetMetaData {

	private Neo4jResult result;

	private static final Map<Class, Class> CLASSES_ASSOCIATIONS = new HashMap<>();
	private static final Map<Class, Integer> TYPES_ASSOCIATIONS = new HashMap<>();

	static {
		CLASSES_ASSOCIATIONS.put(String.class, String.class);
		CLASSES_ASSOCIATIONS.put(Integer.class, Long.class);
		CLASSES_ASSOCIATIONS.put(Long.class, Long.class);
		CLASSES_ASSOCIATIONS.put(Boolean.class, Boolean.class);
		CLASSES_ASSOCIATIONS.put(Float.class, Double.class);
		CLASSES_ASSOCIATIONS.put(Double.class, Double.class);
		CLASSES_ASSOCIATIONS.put(Map.class, Map.class);
		CLASSES_ASSOCIATIONS.put(List.class, Array.class);

		TYPES_ASSOCIATIONS.put(String.class, Types.VARCHAR);
		TYPES_ASSOCIATIONS.put(Integer.class, Types.INTEGER);
		TYPES_ASSOCIATIONS.put(Long.class, Types.INTEGER);
		TYPES_ASSOCIATIONS.put(Boolean.class, Types.BOOLEAN);
		TYPES_ASSOCIATIONS.put(Float.class, Types.FLOAT);
		TYPES_ASSOCIATIONS.put(Double.class, Types.FLOAT);
		TYPES_ASSOCIATIONS.put(Map.class, Types.JAVA_OBJECT);
		TYPES_ASSOCIATIONS.put(List.class, Types.ARRAY);
	}

	/**
	 * Default constructor.
	 */
	HttpNeo4jResultSetMetaData(Neo4jResult result) {
		super(result.getColumns());
		this.result = result;
	}

	@SuppressWarnings("unchecked") @Override public int getColumnType(int column) throws SQLException {
		final Object object = extractObject(column);

		if (object == null) {
			return Types.NULL;
		}
		for (Map.Entry<Class, Integer> entry : TYPES_ASSOCIATIONS.entrySet()) {
			if (entry.getKey().isInstance(object)) {
				return entry.getValue();
			}
		}

		return Types.JAVA_OBJECT;
	}

	@SuppressWarnings("unchecked") @Override public String getColumnClassName(int column) throws SQLException {
		final Object object = extractObject(column);

		if (object == null) {
			return null;
		}
		for (Map.Entry<Class, Class> entry : CLASSES_ASSOCIATIONS.entrySet()) {
			if (entry.getKey().isInstance(object)) {
				return entry.getValue().getName();
			}
		}
		return Object.class.getName();
	}

	/**
	 * Safe method to get the object at column index. Null if cannot extract it.
	 * @param column
	 * @return
	 */
	private Object extractObject(int column) {
		if(this.result == null){
			return null;
		}

		if(this.result.getRows() == null || this.result.getRows().isEmpty()){
			return null;
		}

		Map map = this.result.getRows().get(0);

		if (!map.containsKey("row")){
			return null;
		}

		Object row = map.get("row");

		if (! (row instanceof List)){
			return null;
		}

		List<Object> rowList = (List<Object>) row;

		if (rowList.size() < column){
			return null;
		}

		return rowList.get(column - 1);
	}

}

