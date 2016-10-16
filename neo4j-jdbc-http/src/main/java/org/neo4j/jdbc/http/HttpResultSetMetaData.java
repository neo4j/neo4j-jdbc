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

import java.sql.Array;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;

import org.neo4j.jdbc.Loggable;
import org.neo4j.jdbc.ResultSetMetaData;
import org.neo4j.jdbc.http.driver.Neo4jResult;

public class HttpResultSetMetaData extends ResultSetMetaData implements Loggable {

	private boolean      loggable = false;

	private Neo4jResult result;
	
	/**
	 * Default constructor.
	 */
	HttpResultSetMetaData(Neo4jResult result) {
		super(result.columns);
		this.result = result;
	}

	@SuppressWarnings("unchecked")
	@Override public int getColumnType(int column) throws SQLException {
		final Object object = ((List<Object>) this.result.rows.get(0).get("row")).get(column - 1);

		if (object == null) {
			return Types.NULL;
		}
		if (object instanceof String) {
			return Types.VARCHAR;
		}
		if (object instanceof Integer || object instanceof Long) {
			return Types.INTEGER;
		}
		if (object instanceof Boolean) {
			return Types.BOOLEAN;
		}
		if (object instanceof Float || object instanceof Double) {
			return Types.FLOAT;
		}
		if (object instanceof Map) {
			return Types.JAVA_OBJECT;
		}
		if (object instanceof List) {
			return Types.ARRAY;
		}

		return Types.JAVA_OBJECT;
	}

	@SuppressWarnings("unchecked")
	@Override public String getColumnClassName(int column) throws SQLException {
		final Object object = ((List<Object>) this.result.rows.get(0).get("row")).get(column - 1);
		
		if (object == null) {
			return null;
		}
		if (object instanceof String) {
			return String.class.getName();
		}
		if (object instanceof Integer || object instanceof Long) {
			return Long.class.getName();
		}
		if (object instanceof Boolean) {
			return Boolean.class.getName();
		}
		if (object instanceof Float || object instanceof Double) {
			return Double.class.getName();
		}
		if (object instanceof Map) {
			return Map.class.getName();
		}
		if (object instanceof List) {
			return Array.class.getName();
		}

		return Object.class.getName();
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

