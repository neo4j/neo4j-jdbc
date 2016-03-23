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
package it.larusba.neo4j.jdbc.bolt;

import it.larusba.neo4j.jdbc.ResultSet;
import org.neo4j.driver.internal.InternalNode;
import org.neo4j.driver.internal.InternalRelationship;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.StatementResult;

import java.sql.SQLException;
import java.util.HashMap;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltResultSet extends ResultSet implements Loggable {

	private StatementResult iterator;
	private Record          current;
	private boolean closed = false;
	private int type;
	private int concurrency;
	private int holdability;

	private boolean loggable = false;

	/**
	 * Default constructor for this class, if no params are given or if some params are missing it uses the defaults.
	 *
	 * @param iterator The <code>StatementResult</code> of this set
	 * @param params   At most three, type, concurrency and holdability.
	 *                 The defaults are <code>TYPE_FORWARD_ONLY</code>,
	 *                 <code>CONCUR_READ_ONLY</code>,
	 *                 <code>CLOSE_CURSORS_AT_COMMIT</code>.
	 */
	public BoltResultSet(StatementResult iterator, int... params) {
		this.iterator = iterator;

		this.type = params.length > 0 ? params[0] : TYPE_FORWARD_ONLY;
		this.concurrency = params.length > 1 ? params[1] : CONCUR_READ_ONLY;
		this.holdability = params.length > 2 ? params[2] : CLOSE_CURSORS_AT_COMMIT;
	}

	@Override public boolean next() throws SQLException {
		if (this.iterator == null) {
			throw new SQLException("ResultCursor not initialized");
		}
		return (this.current = this.iterator.next()) != null;
	}

	@Override public void close() throws SQLException {
		if (this.iterator == null) {
			throw new SQLException("ResultCursor not initialized");
		}
		this.closed = true;
	}

	@Override public String getString(String columnLabel) throws SQLException {
		if (this.closed) {
			throw new SQLException("ResultSet was already closed");
		}
		if (!this.current.containsKey(columnLabel)) {
			throw new SQLException("Column not present in ResultSet");
		}
		return this.current.get(columnLabel).asString();
	}

	@Override public boolean getBoolean(String columnLabel) throws SQLException {
		if (this.closed) {
			throw new SQLException("ResultSet was already closed");
		}
		if (!this.current.containsKey(columnLabel)) {
			throw new SQLException("Column not present in ResultSet");
		}
		return this.current.get(columnLabel).asBoolean();
	}

	@Override public int getInt(String columnLabel) throws SQLException {
		if (this.closed) {
			throw new SQLException("ResultSet was already closed");
		}
		if (!this.current.containsKey(columnLabel)) {
			throw new SQLException("Column not present in ResultSet");
		}
		return this.current.get(columnLabel).asInt();
	}

	@Override public long getLong(String columnLabel) throws SQLException {
		if (this.closed) {
			throw new SQLException("ResultSet was already closed");
		}
		if (!this.current.containsKey(columnLabel)) {
			throw new SQLException("Column not present in ResultSet");
		}
		return this.current.get(columnLabel).asLong();
	}

	@Override public int findColumn(String columnLabel) throws SQLException {
		if (this.closed) {
			throw new SQLException("ResultSet was already closed");
		}
		if (!this.iterator.keys().contains(columnLabel)) {
			//if (!this.current.containsKey(columnLabel)) {
			throw new SQLException("Column not present in ResultSet");
		}
		return this.iterator.keys().indexOf(columnLabel) + 1;
	}

	@Override public String getString(int columnIndex) throws SQLException {
		if (this.closed) {
			throw new SQLException("ResultSet was already closed");
		}
		if (columnIndex - 1 > this.current.size()) {
			throw new SQLException("Column not present in ResultSet");
		}
		return this.current.get(columnIndex - 1).asString();
	}

	@Override public boolean getBoolean(int columnIndex) throws SQLException {
		if (this.closed) {
			throw new SQLException("ResultSet was already closed");
		}
		if (columnIndex - 1 > this.current.size()) {
			throw new SQLException("Column not present in ResultSet");
		}
		return this.current.get(columnIndex - 1).asBoolean();
	}

	@Override public int getInt(int columnIndex) throws SQLException {
		if (this.closed) {
			throw new SQLException("ResultSet was already closed");
		}
		if (columnIndex - 1 > this.current.size()) {
			throw new SQLException("Column not present in ResultSet");
		}
		return this.current.get(columnIndex - 1).asInt();
	}

	@Override public long getLong(int columnIndex) throws SQLException {
		if (this.closed) {
			throw new SQLException("ResultSet was already closed");
		}
		if (columnIndex - 1 > this.current.size()) {
			throw new SQLException("Column not present in ResultSet");
		}
		return this.current.get(columnIndex - 1).asLong();
	}

	@Override public float getFloat(String columnLabel) throws SQLException {
		if (this.closed) {
			throw new SQLException("ResultSet was already closed");
		}
		if (!this.current.containsKey(columnLabel)) {
			throw new SQLException("Column not present in ResultSet");
		}
		return this.current.get(columnLabel).asFloat();
	}

	@Override public float getFloat(int columnIndex) throws SQLException {
		if (this.closed) {
			throw new SQLException("ResultSet was already closed");
		}
		if (columnIndex - 1 > this.current.size()) {
			throw new SQLException("Column not present in ResultSet");
		}
		return this.current.get(columnIndex - 1).asFloat();
	}

	@Override public short getShort(String columnLabel) throws SQLException {
		if (this.closed) {
			throw new SQLException("ResultSet was already closed");
		}
		if (!this.current.containsKey(columnLabel)) {
			throw new SQLException("Column not present in ResultSet");
		}
		return (short) this.current.get(columnLabel).asInt();
	}

	@Override public short getShort(int columnIndex) throws SQLException {
		if (this.closed) {
			throw new SQLException("ResultSet was already closed");
		}
		if (columnIndex - 1 > this.current.size()) {
			throw new SQLException("Column not present in ResultSet");
		}
		return (short) this.current.get(columnIndex - 1).asInt();
	}

	@Override public double getDouble(int columnIndex) throws SQLException {
		if (this.closed) {
			throw new SQLException("ResultSet was already closed");
		}
		if (columnIndex - 1 > this.current.size()) {
			throw new SQLException("Column not present in ResultSet");
		}
		return this.current.get(columnIndex - 1).asDouble();
	}

	@Override public double getDouble(String columnLabel) throws SQLException {
		if (this.closed) {
			throw new SQLException("ResultSet was already closed");
		}
		if (!this.current.containsKey(columnLabel)) {
			throw new SQLException("Column not present in ResultSet");
		}
		return this.current.get(columnLabel).asDouble();
	}

	private Object generateObject(Object obj) {
		if (obj.getClass().equals(InternalNode.class)) {
			InternalNode node = (InternalNode) obj;
			HashMap<String, Object> map = new HashMap<>();
			map.put("_id", node.id());
			map.put("_labels", node.labels());
			node.keys().forEach(key -> map.put(key, node.get(key).asObject()));
			return map;
		}
		if (obj.getClass().equals(InternalRelationship.class)) {
			InternalRelationship rel = (InternalRelationship) obj;
			HashMap<String, Object> map = new HashMap<>();
			map.put("_id", rel.id());
			map.put("_type", rel.type());
			rel.keys().forEach(key -> map.put(key, rel.get(key).asObject()));
			return map;
		}
		return obj;
	}

	@Override public Object getObject(int columnIndex) throws SQLException {
		if (this.closed) {
			throw new SQLException("ResultSet was already closed");
		}
		if (columnIndex - 1 > this.current.size()) {
			throw new SQLException("Column not present in ResultSet");
		}
		Object obj = this.current.get(columnIndex - 1).asObject();
		return this.generateObject(obj);
	}

	@Override public Object getObject(String columnLabel) throws SQLException {
		if (this.closed) {
			throw new SQLException("ResultSet was already closed");
		}
		if (!this.current.containsKey(columnLabel)) {
			throw new SQLException("Column not present in ResultSet");
		}
		Object obj = this.current.get(columnLabel).asObject();
		return this.generateObject(obj);
	}

	@Override public boolean isClosed() throws SQLException {
		return this.closed;
	}

	@Override public boolean isLoggable() {
		return this.loggable;
	}

	@Override public void setLoggable(boolean loggable) {
		this.loggable = loggable;
	}
}
