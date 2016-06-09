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

import org.neo4j.jdbc.Array;

import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class ListArray extends Array {

	private List list;
	private int  type;



	public ListArray(List list, int type) {
		this.list = list;
		this.type = type;
	}

	@Override public String getBaseTypeName() throws SQLException {
		String name;

		if (!TYPES_SUPPORTED.contains(this.type)) {
			throw new SQLException("Type " + this.type + " not supported");
		}

		if (this.type == Types.VARCHAR) {
			name = "VARCHAR";
		} else if (this.type == Types.INTEGER) {
			name = "INTEGER";
		} else if (this.type == Types.BOOLEAN) {
			name = "BOOLEAN";
		} else if (this.type == Types.DOUBLE) {
			name = "DOUBLE";
		} else if (this.type == Types.JAVA_OBJECT) {
			name = "JAVA_OBJECT";
		} else {
			throw new SQLException("Type " + this.type + " not supported");
		}

		return name;
	}

	@Override public int getBaseType() throws SQLException {
		if (!TYPES_SUPPORTED.contains(this.type)) {
			throw new SQLException("Type " + this.type + " not supported");
		}
		return this.type;
	}

	@Override public Object getArray() throws SQLException {
		if(!TYPES_SUPPORTED.contains(this.type)) {
			throw new SQLException("Type " + this.type + " not supported");
		}
		Object result;

		try {
			if (this.type == Types.VARCHAR) {
				result = this.list.toArray(new String[this.list.size()]);
			} else if (this.type == Types.INTEGER) {
				result = this.list.toArray(new Long[this.list.size()]);
			} else if (this.type == Types.BOOLEAN) {
				result = this.list.toArray(new Boolean[this.list.size()]);
			} else if (this.type == Types.DOUBLE) {
				result = this.list.toArray(new Double[this.list.size()]);
			} else if (this.type == Types.JAVA_OBJECT) {
				result = this.list.toArray(new Object[this.list.size()]);
			} else {
				throw new SQLException("Type " + this.type + " not supported");
			}
		} catch (ArrayStoreException e){
			throw new SQLException(e);
		}

		return result;
	}
}
