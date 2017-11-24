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
 * Created on 03/02/16
 */
package org.neo4j.jdbc;

import org.neo4j.jdbc.utils.ExceptionBuilder;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public abstract class Neo4jResultSetMetaData implements java.sql.ResultSetMetaData {

	/**
	 * List of column of the ResultSet
	 */
	protected final List<String> keys;

	/**
	 * Default constructor with the list of column.
	 *
	 * @param keys List of column of the ResultSet
	 */
	protected Neo4jResultSetMetaData(List<String> keys) {
		if (keys != null) {
			this.keys = keys;
		} else {
			this.keys = new ArrayList<>();
		}
	}

	/*------------------------------------*/
	/*       Default implementation       */
	/*------------------------------------*/

	@Override public int getColumnCount() throws SQLException {
		int result = 0;
		// just a preventing test for mockito
		// otherwise it's not needed
		if (this.keys != null) {
			result = this.keys.size();
		}
		return result;
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

	@Override public boolean isAutoIncrement(int column) throws SQLException {
		return false;
	}

	@Override public boolean isSearchable(int column) throws SQLException {
		if (column <= 0 || column > this.getColumnCount()) {
			return false;
		}
		return true;
	}

	@Override public boolean isCurrency(int column) throws SQLException {
		return false;
	}

	@Override public int isNullable(int column) throws SQLException {
		return columnNoNulls;
	}

	@Override public boolean isSigned(int column) throws SQLException {
		return false;
	}

	@Override public int getPrecision(int column) throws SQLException {
		return 0;
	}

	@Override public int getScale(int column) throws SQLException {
		return 0;
	}

	@Override public <T> T unwrap(Class<T> iface) throws SQLException {
		return Wrapper.unwrap(iface, this);
	}

	@Override public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return Wrapper.isWrapperFor(iface, this.getClass());
	}

	/**
	 * By default, every field are string ...
	 */
	@Override public int getColumnType(int column) throws SQLException {
		return Types.VARCHAR;
	}
	
	/**
	 * By default, every field are string ...
	 */
	@Override public String getColumnTypeName(int column) throws SQLException {
		return "String";
	}

	/**
	 * By default, every field are string ...
	 */
	@Override public String getColumnClassName(int column) throws SQLException {
		return String.class.getName();
	}
	
	/**
	 * PLANNED FOR REL 3.1
	 */
	@Override public String getTableName(int column) throws SQLException {
		return ""; //not applicable
	}

	/**
	 * PLANNED FOR REL 3.1
	 */
	@Override public String getSchemaName(int column) throws SQLException {
		return ""; //not applicable
	}

	@Override public boolean isCaseSensitive(int column) throws SQLException {
		return true;
	}

	@Override public boolean isReadOnly(int column) throws SQLException {
		return false;
	}

	/*---------------------------------*/
	/*       Not implemented yet       */
	/*---------------------------------*/

	@Override public boolean isWritable(int column) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public boolean isDefinitelyWritable(int column) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

}
