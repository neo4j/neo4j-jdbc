/**
 * Copyright (c) 2004-2016 LARUS Business Automation [http://www.larus-ba.it]
 * <p>
 * This file is part of the "LARUS Integration Framework for Neo4j".
 * <p>
 * The "LARUS Integration Framework for Neo4j" is free software: you can
 * redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * Created on 11/02/16
 */
package it.neo4j.jdbc.bolt;

import it.neo4j.jdbc.ResultSet;
import org.neo4j.driver.v1.ResultCursor;

import java.sql.SQLException;
import java.util.Collections;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltResultSet extends ResultSet {

	private ResultCursor cursor;

	public BoltResultSet(ResultCursor cursor) {
		this.cursor = cursor;
	}

	@Override public boolean next() throws SQLException {
		if (this.cursor == null) {
			throw new SQLException("ResultCursor not initialized");
		}
		if (this.cursor.position() == this.cursor.size() - 1) {
			return false;
		} else {
			return this.cursor.next();
		}
	}

	@Override public String getString(String columnLabel) throws SQLException {
		if (!cursor.containsKey(columnLabel)) {
			throw new SQLException("Column not present in ResultSet");
		}
		return cursor.value(columnLabel).asString();
	}

	@Override public String getString(int columnIndex) throws SQLException {
		return null;
	}

	@Override public boolean previous() throws SQLException {
		return false;
	}

	@Override public boolean first() throws SQLException {
		return false;
	}

	@Override public boolean last() throws SQLException {
		return false;
	}

}
