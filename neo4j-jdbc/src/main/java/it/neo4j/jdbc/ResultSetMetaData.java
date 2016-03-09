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
 * Created on 03/02/16
 */
package it.neo4j.jdbc;

import java.sql.SQLException;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public abstract class ResultSetMetaData implements java.sql.ResultSetMetaData {

	@Override public abstract int getColumnCount() throws SQLException;

	@Override public boolean isAutoIncrement(int column) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean isCaseSensitive(int column) throws SQLException {
		//TODO check if is String
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean isSearchable(int column) throws SQLException {
		if(column <= 0 || column > this.getColumnCount()){
			return false;
		}
		return true;
	}

	@Override public boolean isCurrency(int column) throws SQLException {
		//TODO check if all fields have two decimal digits
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public int isNullable(int column) throws SQLException {
		//TODO false
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean isSigned(int column) throws SQLException {
		//TODO true if int/short/long
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public int getColumnDisplaySize(int column) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public String getColumnLabel(int column) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public abstract String getColumnName(int column) throws SQLException;

	@Override public String getSchemaName(int column) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override public int getPrecision(int column) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public int getScale(int column) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public String getTableName(int column) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public String getCatalogName(int column) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public int getColumnType(int column) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public String getColumnTypeName(int column) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean isReadOnly(int column) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean isWritable(int column) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean isDefinitelyWritable(int column) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public String getColumnClassName(int column) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public <T> T unwrap(Class<T> iface) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean isWrapperFor(Class<?> iface) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}
}
