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
 * Created on 06/04/16
 */
package org.neo4j.jdbc.impl;

import org.neo4j.jdbc.Neo4jArray;
import org.neo4j.jdbc.Neo4jResultSet;
import org.neo4j.jdbc.Neo4jResultSetMetaData;
import org.neo4j.jdbc.utils.Neo4jInvocationHandler;

import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
//FIXME tests needed
public class ListNeo4jResultSet extends Neo4jResultSet {

	private List<List<Object>> list;
	private int     index  = -1;
	private boolean closed = false;
	private List<String> keys;

	private ListNeo4jResultSet(List<List<Object>> list, List<String> keys) {
		super(null);
		this.list = list;
		this.keys = keys;
	}

	public static ResultSet newInstance(boolean debug, List<List<Object>> list, List<String> keys) {
		ResultSet rs = new ListNeo4jResultSet(list, keys);
		return (ResultSet) Proxy
				.newProxyInstance(ListNeo4jResultSet.class.getClassLoader(), new Class[] { ResultSet.class }, new Neo4jInvocationHandler(rs, debug));
	}

	@Override protected boolean innerNext() throws SQLException {
		return ++this.index < this.list.size();
	}

	@Override public void close() throws SQLException {
		this.closed = true;
	}

	@Override public boolean wasNull() throws SQLException {
		return false;
	}

	private Object get(int columnIndex) throws SQLException {
		if (this.isClosed()) {
			throw new SQLException("ResultSet already closed");
		}
		if(this.index >= this.list.size()){
			throw new SQLException("ResultSet has no more rows");
		}
		if (columnIndex < 1 || columnIndex > this.list.get(this.index).size()) {
			throw new SQLException("columnIndex out of bounds");
		}
		if(this.index == -1 ){
			throw new SQLException("ResultSet not pointing to existing row");
		}
		return this.list.get(index).get(columnIndex - 1);
	}

	private Object get(String columnLabel) throws SQLException {
		if (this.isClosed()) {
			throw new SQLException("ResultSet already closed");
		}
		if(this.index >= this.list.size()){
			throw new SQLException("ResultSet has no more rows");
		}
		if (this.keys.indexOf(columnLabel) == -1) {
			throw new SQLException("columnIndex out of bounds");
		}
		if(this.index == -1 ){
			throw new SQLException("ResultSet not pointing to existing row");
		}
		return this.list.get(index).get(this.keys.indexOf(columnLabel));
	}

	@Override public String getString(int columnIndex) throws SQLException {
		Object value = this.get(columnIndex);
		return value == null ? null : value.toString();
	}

	@Override public boolean getBoolean(int columnIndex) throws SQLException {
		return false;
	}

	@Override public short getShort(int columnIndex) throws SQLException {
		return 0;
	}

	@Override public int getInt(int columnIndex) throws SQLException {
		return 0;
	}

	@Override public long getLong(int columnIndex) throws SQLException {
		return 0;
	}

	@Override public float getFloat(int columnIndex) throws SQLException {
		return 0;
	}

	@Override public double getDouble(int columnIndex) throws SQLException {
		return 0;
	}

	@Override public Neo4jArray getArray(int columnIndex) throws SQLException {
		return null;
	}

	@Override public Neo4jArray getArray(String columnLabel) throws SQLException {
		return null;
	}

	@Override public String getString(String columnLabel) throws SQLException {
		Object value = this.get(columnLabel);
		return value == null ? null : value.toString();
	}

	@Override public boolean getBoolean(String columnLabel) throws SQLException {
		return false;
	}

	@Override public short getShort(String columnLabel) throws SQLException {
		return 0;
	}

	@Override public int getInt(String columnLabel) throws SQLException {
		return 0;
	}

	@Override public long getLong(String columnLabel) throws SQLException {
		return 0;
	}

	@Override public float getFloat(String columnLabel) throws SQLException {
		return 0;
	}

	@Override public double getDouble(String columnLabel) throws SQLException {
		return 0;
	}

	@Override public Neo4jResultSetMetaData getMetaData() throws SQLException {
		return new ListNeo4jResultSetMetaData(this.keys);
	}

	@Override public Object getObject(int columnIndex) throws SQLException {
		return this.get(columnIndex);
	}

	@Override public Object getObject(String columnLabel) throws SQLException {
		return this.get(columnLabel);
	}

	@Override public int findColumn(String columnLabel) throws SQLException {
		return this.keys.indexOf(columnLabel) + 1;
	}

	@Override public int getType() throws SQLException {
		return 0;
	}

	@Override public int getConcurrency() throws SQLException {
		return 0;
	}

	@Override public int getHoldability() throws SQLException {
		return 0;
	}

	@Override public boolean isClosed() throws SQLException {
		return this.closed;
	}
}
