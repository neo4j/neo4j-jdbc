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
 * Created on 11/02/16
 */
package it.neo4j.jdbc.bolt;

import it.neo4j.jdbc.ResultSet;
import org.mockito.Mockito;
import org.neo4j.driver.internal.InternalNode;
import org.neo4j.driver.internal.InternalRelationship;
import org.neo4j.driver.v1.ResultCursor;

import java.sql.SQLException;
import java.util.HashMap;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltResultSet extends ResultSet {

	private ResultCursor cursor;
	private boolean closed = false;
	private int type;
	private int concurrency;
	private int holdability;

	private boolean debug = false;

	public static BoltResultSet instantiate(ResultCursor cursor, boolean debug, int... params) {
		BoltResultSet boltResultSet = null;
		if (debug) {
			boltResultSet = Mockito.mock(BoltResultSet.class,
					Mockito.withSettings().useConstructor().outerInstance(cursor).outerInstance(params).verboseLogging()
							.defaultAnswer(Mockito.CALLS_REAL_METHODS));
			boltResultSet.debug = debug;
		} else {
			boltResultSet = new BoltResultSet(cursor, params);
		}

		return boltResultSet;
	}

	/**
	 * Default constructor for this class, if no params are given or if some params are missing it uses the defaults.
	 *
	 * @param cursor The <code>ResultCursor</code> of this set
	 * @param params At most three, type, concurrency and holdability.
	 *               The defaults are <code>TYPE_FORWARD_ONLY</code>,
	 *               <code>CONCUR_READ_ONLY</code>,
	 *               <code>CLOSE_CURSORS_AT_COMMIT</code>.
	 */
	public BoltResultSet(ResultCursor cursor, int... params) {
		this.cursor = cursor;
		int paramsQty = params.length;
		if (paramsQty > 0) {
			this.type = params[0];
		} else {
			this.type = TYPE_FORWARD_ONLY;
		}
		if (paramsQty > 1) {
			this.concurrency = params[1];
		} else {
			this.concurrency = CONCUR_READ_ONLY;
		}
		if (paramsQty > 1) {
			this.holdability = params[1];
		} else {
			this.holdability = CLOSE_CURSORS_AT_COMMIT;
		}
	}

	@Override public boolean next() throws SQLException {
		if (this.cursor == null) {
			throw new SQLException("ResultCursor not initialized");
		}
		if (this.cursor.atEnd()) {
			return false;
		} else {
			return this.cursor.next();
		}
	}

	@Override public void close() throws SQLException {
		if (this.cursor == null) {
			throw new SQLException("ResultCursor not initialized");
		}
		if (!this.closed) {
			this.cursor.close();
			this.closed = true;
		}
	}

	@Override public String getString(String columnLabel) throws SQLException {
		if (this.closed) {
			throw new SQLException("ResultSet was already closed");
		}
		if (!this.cursor.containsKey(columnLabel)) {
			throw new SQLException("Column not present in ResultSet");
		}
		return this.cursor.get(columnLabel).asString();
	}

	@Override public int getInt(String columnLabel) throws SQLException {
		if (this.closed) {
			throw new SQLException("ResultSet was already closed");
		}
		if (!this.cursor.containsKey(columnLabel)) {
			throw new SQLException("Column not present in ResultSet");
		}
		return this.cursor.get(columnLabel).asInt();
	}

	@Override public int findColumn(String columnLabel) throws SQLException {
		if (this.closed) {
			throw new SQLException("ResultSet was already closed");
		}
		if (!this.cursor.containsKey(columnLabel)) {
			throw new SQLException("Column not present in ResultSet");
		}
		this.cursor.next();
		return this.cursor.index(columnLabel);
	}

	@Override public String getString(int columnIndex) throws SQLException {
		if (this.closed) {
			throw new SQLException("ResultSet was already closed");
		}
		if (columnIndex - 1 > this.cursor.size()) {
			throw new SQLException("Column not present in ResultSet");
		}
		return this.cursor.get(columnIndex - 1).asString();
	}

	@Override public int getInt(int columnIndex) throws SQLException {
		if (this.closed) {
			throw new SQLException("ResultSet was already closed");
		}
		if (columnIndex - 1 > this.cursor.size()) {
			throw new SQLException("Column not present in ResultSet");
		}
		return this.cursor.get(columnIndex - 1).asInt();
	}

	@Override public float getFloat(String columnLabel) throws SQLException {
		if (this.closed) {
			throw new SQLException("ResultSet was already closed");
		}
		if (!this.cursor.containsKey(columnLabel)) {
			throw new SQLException("Column not present in ResultSet");
		}
		return this.cursor.get(columnLabel).asFloat();
	}

	@Override public float getFloat(int columnIndex) throws SQLException {
		if (this.closed) {
			throw new SQLException("ResultSet was already closed");
		}
		if (columnIndex - 1 > this.cursor.size()) {
			throw new SQLException("Column not present in ResultSet");
		}
		return this.cursor.get(columnIndex - 1).asFloat();
	}

	@Override public short getShort(String columnLabel) throws SQLException {
		if (this.closed) {
			throw new SQLException("ResultSet was already closed");
		}
		if (!this.cursor.containsKey(columnLabel)) {
			throw new SQLException("Column not present in ResultSet");
		}
		return (short) this.cursor.get(columnLabel).asInt();
	}

	@Override public short getShort(int columnIndex) throws SQLException {
		if (this.closed) {
			throw new SQLException("ResultSet was already closed");
		}
		if (columnIndex - 1 > this.cursor.size()) {
			throw new SQLException("Column not present in ResultSet");
		}
		return (short) this.cursor.get(columnIndex - 1).asInt();
	}

	@Override public double getDouble(int columnIndex) throws SQLException {
		if (this.closed) {
			throw new SQLException("ResultSet was already closed");
		}
		if (columnIndex - 1 > this.cursor.size()) {
			throw new SQLException("Column not present in ResultSet");
		}
		return this.cursor.get(columnIndex - 1).asDouble();
	}

	@Override public double getDouble(String columnLabel) throws SQLException {
		if (this.closed) {
			throw new SQLException("ResultSet was already closed");
		}
		if (!this.cursor.containsKey(columnLabel)) {
			throw new SQLException("Column not present in ResultSet");
		}
		return this.cursor.get(columnLabel).asDouble();
	}

	private Object generateObject(Object obj) {
		if (obj.getClass().equals(InternalNode.class)) {
			InternalNode node = (InternalNode) obj;
			HashMap<String, Object> map = new HashMap<>();
			map.put("_id", node.identity().asLong());
			map.put("_labels", node.labels());
			node.properties().forEach(property -> map.put(property.key(), property.value().asObject()));
			return map;
		}
		if (obj.getClass().equals(InternalRelationship.class)) {
			InternalRelationship rel = (InternalRelationship) obj;
			HashMap<String, Object> map = new HashMap<>();
			map.put("_id", rel.identity().asLong());
			map.put("_type", rel.type());
			rel.properties().forEach(property -> map.put(property.key(), property.value().asObject()));
			return map;
		}
		return obj;
	}

	@Override public Object getObject(int columnIndex) throws SQLException {
		if (this.closed) {
			throw new SQLException("ResultSet was already closed");
		}
		if (columnIndex - 1 > this.cursor.size()) {
			throw new SQLException("Column not present in ResultSet");
		}
		Object obj = this.cursor.get(columnIndex - 1).asObject();
		return this.generateObject(obj);
	}

	@Override public Object getObject(String columnLabel) throws SQLException {
		if (this.closed) {
			throw new SQLException("ResultSet was already closed");
		}
		if (!this.cursor.containsKey(columnLabel)) {
			throw new SQLException("Column not present in ResultSet");
		}
		Object obj = this.cursor.get(columnLabel).asObject();
		return this.generateObject(obj);
	}

	@Override public boolean isClosed() throws SQLException {
		return this.closed;
	}
}
