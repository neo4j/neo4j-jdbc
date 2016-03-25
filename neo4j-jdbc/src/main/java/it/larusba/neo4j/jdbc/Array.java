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
 * Created on 30/03/16
 */
package it.larusba.neo4j.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public abstract class Array implements java.sql.Array {

	public static List<Integer> TYPES_SUPPORTED   = Arrays.asList(Types.VARCHAR, Types.INTEGER, Types.BOOLEAN, Types.DOUBLE, Types.JAVA_OBJECT);
	public static List<Integer> TYPES_UNSUPPORTED = Arrays
			.asList(Types.ARRAY, Types.BIGINT, Types.BINARY, Types.BIT, Types.BLOB, Types.CHAR, Types.CLOB, Types.DATALINK, Types.DATE, Types.DECIMAL,
					Types.DISTINCT, Types.FLOAT, Types.LONGNVARCHAR, Types.LONGVARBINARY, Types.NCHAR, Types.NCLOB, Types.NUMERIC, Types.NVARCHAR, Types.OTHER,
					Types.REAL, Types.REF, Types.REF_CURSOR, Types.ROWID, Types.SMALLINT, Types.SQLXML, Types.STRUCT, Types.TIME, Types.TIME_WITH_TIMEZONE,
					Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE, Types.TINYINT, Types.VARBINARY);

	@Override public String getBaseTypeName() throws SQLException {
		throw new SQLFeatureNotSupportedException("Feature not supported");
	}

	@Override public int getBaseType() throws SQLException {
		throw new SQLFeatureNotSupportedException("Feature not supported");
	}

	@Override public Object getArray() throws SQLException {
		throw new SQLFeatureNotSupportedException("Feature not supported");
	}

	@Override public Object getArray(Map<String, Class<?>> map) throws SQLException {
		throw new SQLFeatureNotSupportedException("Feature not supported");
	}

	@Override public Object getArray(long index, int count) throws SQLException {
		throw new SQLFeatureNotSupportedException("Feature not supported");
	}

	@Override public Object getArray(long index, int count, Map<String, Class<?>> map) throws SQLException {
		throw new SQLFeatureNotSupportedException("Feature not supported");
	}

	@Override public java.sql.ResultSet getResultSet() throws SQLException {
		throw new SQLFeatureNotSupportedException("Feature not supported");
	}

	@Override public ResultSet getResultSet(Map<String, Class<?>> map) throws SQLException {
		throw new SQLFeatureNotSupportedException("Feature not supported");
	}

	@Override public ResultSet getResultSet(long index, int count) throws SQLException {
		throw new SQLFeatureNotSupportedException("Feature not supported");
	}

	@Override public ResultSet getResultSet(long index, int count, Map<String, Class<?>> map) throws SQLException {
		throw new SQLFeatureNotSupportedException("Feature not supported");
	}

	@Override public void free() throws SQLException {
		throw new SQLFeatureNotSupportedException("Feature not supported");
	}

	public static int getObjectType(Object obj){
		int type;

		if(obj instanceof String){
			type = Types.VARCHAR;
		} else if(obj instanceof Long){
			type = Types.INTEGER;
		} else if(obj instanceof Boolean) {
			type = Types.BOOLEAN;
		} else if(obj instanceof Double){
			type = Types.DOUBLE;
		} else {
			type = Types.JAVA_OBJECT;
		}

		return type;
	}
}
