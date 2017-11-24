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
 * Created on 30/03/16
 */
package org.neo4j.jdbc.impl;

import org.neo4j.jdbc.Neo4jArray;

import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Objects;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class ListArray extends Neo4jArray {

	private List list;
	private int  type;
	private static final String TYPE_NOT_SUPPORTED = "Type %s not supported";


	public ListArray(List list, int type) {
		this.list = list;
		this.type = type;
	}

	@Override public String getBaseTypeName() throws SQLException {
		String name;

		if (!TYPES_SUPPORTED.contains(this.type)) {
			throw new SQLException(String.format(TYPE_NOT_SUPPORTED, this.type));
		}

		switch (this.type) {
			case Types.VARCHAR:
				name = "VARCHAR";
				break;
			case Types.INTEGER:
				name = "INTEGER";
				break;
			case Types.BOOLEAN:
				name = "BOOLEAN";
				break;
			case Types.DOUBLE:
				name = "DOUBLE";
				break;
			case Types.JAVA_OBJECT:
				name = "JAVA_OBJECT";
				break;
			default:
				throw new SQLException(String.format(TYPE_NOT_SUPPORTED, this.type));
		}

		return name;
	}

	@Override public int getBaseType() throws SQLException {
		if (!TYPES_SUPPORTED.contains(this.type)) {
			throw new SQLException(String.format(TYPE_NOT_SUPPORTED, this.type));
		}
		return this.type;
	}

	@Override public Object getArray() throws SQLException {
		if(!TYPES_SUPPORTED.contains(this.type)) {
			throw new SQLException(String.format(TYPE_NOT_SUPPORTED, this.type));
		}
		Object result;

		try {
			switch (this.type) {
				case Types.VARCHAR:
					result = this.list.toArray(new String[this.list.size()]);
					break;
				case Types.INTEGER:
					result = this.list.toArray(new Long[this.list.size()]);
					break;
				case Types.BOOLEAN:
					result = this.list.toArray(new Boolean[this.list.size()]);
					break;
				case Types.DOUBLE:
					result = this.list.toArray(new Double[this.list.size()]);
					break;
				case Types.JAVA_OBJECT:
					result = this.list.toArray(new Object[this.list.size()]);
					break;
				default:
					throw new SQLException(String.format(TYPE_NOT_SUPPORTED, this.type));
			}
		} catch (ArrayStoreException e){
			throw new SQLException(e);
		}

		return result;
	}

	@Override public boolean equals(Object o){
		return o instanceof ListArray && this.list.equals(((ListArray)o).list);
	}

	@Override public int hashCode() {
		return Objects.hash(list, type);
	}
}
