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

import it.larusba.neo4j.jdbc.Array;
import it.larusba.neo4j.jdbc.ResultSet;
import it.larusba.neo4j.jdbc.ResultSetMetaData;
import it.larusba.neo4j.jdbc.impl.ListArray;
import org.neo4j.driver.internal.InternalNode;
import org.neo4j.driver.internal.InternalPath;
import org.neo4j.driver.internal.InternalRelationship;
import org.neo4j.driver.internal.value.IntegerValue;
import org.neo4j.driver.internal.value.ListValue;
import org.neo4j.driver.internal.value.StringValue;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Value;
import org.neo4j.driver.v1.types.Node;
import org.neo4j.driver.v1.types.Path;

import java.sql.SQLException;
import java.util.*;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltResultSet extends ResultSet implements Loggable {

	private StatementResult iterator;
	private Record          current;
	private List<String>    keys;
	private boolean closed = false;
	private int type;
	private int concurrency;
	private int holdability;
	private boolean wasNull;

	public static final int DEFAULT_TYPE        = TYPE_FORWARD_ONLY;
	public static final int DEFAULT_CONCURRENCY = CONCUR_READ_ONLY;
	public static final int DEFAULT_HOLDABILITY = CLOSE_CURSORS_AT_COMMIT;

	private boolean loggable  = false;
	private boolean flattened = false;

	public static final List<String> ACCEPTED_TYPES_FOR_FLATTENING = Arrays.asList("NODE", "RELATIONSHIP");

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

		this.keys = new ArrayList<>();

		if (this.iterator != null && this.iterator.hasNext() && this.iterator.peek() != null
				&& this.iterator.peek().fields().stream().filter(pair -> ACCEPTED_TYPES_FOR_FLATTENING.contains(pair.value().type().name())).count()
				== this.iterator.keys().size()) {
			//Flatten the result
			this.iterator.peek().fields().forEach(pair -> {
				keys.add(pair.key());
				if (ACCEPTED_TYPES_FOR_FLATTENING.get(0).equals(pair.value().type().name())) {
					keys.add(pair.key() + ".id");
					keys.add(pair.key() + ".labels");
					pair.value().asNode().keys().forEach(key -> keys.add(pair.key() + "." + key));
				} else if (ACCEPTED_TYPES_FOR_FLATTENING.get(1).equals(pair.value().type().name())) {
					keys.add(pair.key() + ".id");
					keys.add(pair.key() + ".type");
					pair.value().asRelationship().keys().forEach(key -> keys.add(pair.key() + "." + key));
				}
			});
			this.flattened = true;
		} else if (this.iterator != null) {
			//Keys are exactly the ones returned from the iterator
			this.keys = this.iterator.keys();
		}

		this.type = params.length > 0 ? params[0] : TYPE_FORWARD_ONLY;
		this.concurrency = params.length > 1 ? params[1] : CONCUR_READ_ONLY;
		this.holdability = params.length > 2 ? params[2] : CLOSE_CURSORS_AT_COMMIT;
	}

	private void checkClosed() throws SQLException {
		if (this.closed) {
			throw new SQLException("ResultSet was already closed");
		}
	}

	@Override public boolean next() throws SQLException {
		if (this.iterator == null) {
			throw new SQLException("ResultCursor not initialized");
		}
		if (this.iterator.hasNext()) {
			this.current = this.iterator.next();
		} else {
			this.current = null;
		}
		return this.current != null;
	}

	@Override public void close() throws SQLException {
		if (this.iterator == null) {
			throw new SQLException("ResultCursor not initialized");
		}
		this.closed = true;
	}

	@Override public boolean wasNull() throws SQLException {
		checkClosed();
		return this.wasNull;
	}

	@Override public String getString(String columnLabel) throws SQLException {
		checkClosed();
		return this.fetchValueFromLabel(columnLabel).asString();
	}

	@Override public boolean getBoolean(String columnLabel) throws SQLException {
		checkClosed();
		return this.fetchValueFromLabel(columnLabel).asBoolean();
	}

	private Value fetchPropertyValue(String key, String property) throws SQLException {
		Value value;
		try {
			if ("id".equals(property)) {
				//id requested
				value = new IntegerValue(this.current.get(key).asEntity().id());
			} else if ("labels".equals(property)) {
				//node's labels requested
				Node node = this.current.get(key).asNode();
				List<Value> values = new ArrayList<>();
				for (String label : node.labels()) {
					values.add(new StringValue(label));
				}
				value = new ListValue(values.toArray(new Value[values.size()]));
			} else if ("type".equals(property)) {
				//Relationship's type requested
				value = new StringValue(this.current.get(key).asRelationship().type());
			} else {
				//Property requested
				value = this.current.get(key).get(property);
			}
		} catch (Exception e) {
			throw new SQLException("Column not present in ResultSet");
		}
		return value;
	}

	private Value fetchValueFromLabel(String label) throws SQLException {
		Value value;
		if (this.current.containsKey(label)) {
			//Requested value is not flattened
			value = this.current.get(label);
		} else if (this.flattened && this.keys.contains(label)) {
			//Requested value is flattened
			String[] keys = label.split("\\.");
			value = this.fetchPropertyValue(keys[0], keys[1]);
		} else {
			//No value found
			throw new SQLException("Column not present in ResultSet");
		}
		this.wasNull = value.isNull();
		return value;
	}

	private Value fetchValueFromIndex(int index) throws SQLException {
		Value value;
		if (this.flattened && index > 0 && index - 1 <= this.keys.size()) {
			//Requested value is to be considered from flattened results
			String[] keys = this.keys.get(index - 1).split("\\.");
			if (keys.length > 1) { //Requested value is a virtual column
				value = this.fetchPropertyValue(keys[0], keys[1]);
			} else {
				//Requested value is the node/relationship itself
				value = this.current.get(index - 1);
			}
		} else if (index > 0 && index - 1 <= this.current.size()) {
			//Requested value is not flattened
			value = this.current.get(index - 1);
		} else {
			//No value found
			throw new SQLException("Column not present in ResultSet");
		}
		this.wasNull = value.isNull();
		return value;
	}

	@Override public int getInt(String columnLabel) throws SQLException {
		checkClosed();
		return this.fetchValueFromLabel(columnLabel).asInt();
	}

	@Override public long getLong(String columnLabel) throws SQLException {
		checkClosed();
		return this.fetchValueFromLabel(columnLabel).asLong();
	}

	@Override public int findColumn(String columnLabel) throws SQLException {
		checkClosed();
		if (!this.iterator.keys().contains(columnLabel)) {
			//if (!this.current.containsKey(columnLabel)) {
			throw new SQLException("Column not present in ResultSet");
		}
		return this.iterator.keys().indexOf(columnLabel) + 1;
	}

	@Override public int getType() throws SQLException {
		checkClosed();
		return this.type;
	}

	@Override public int getConcurrency() throws SQLException {
		checkClosed();
		return this.concurrency;
	}

	@Override public int getHoldability() throws SQLException {
		checkClosed();
		return this.holdability;
	}

	@Override public String getString(int columnIndex) throws SQLException {
		checkClosed();
		return this.fetchValueFromIndex(columnIndex).asString();
	}

	@Override public boolean getBoolean(int columnIndex) throws SQLException {
		checkClosed();
		return this.fetchValueFromIndex(columnIndex).asBoolean();
	}

	@Override public int getInt(int columnIndex) throws SQLException {
		checkClosed();
		return this.fetchValueFromIndex(columnIndex).asInt();
	}

	@Override public long getLong(int columnIndex) throws SQLException {
		checkClosed();
		return this.fetchValueFromIndex(columnIndex).asLong();
	}

	@Override public float getFloat(String columnLabel) throws SQLException {
		checkClosed();
		return this.fetchValueFromLabel(columnLabel).asFloat();
	}

	@Override public float getFloat(int columnIndex) throws SQLException {
		checkClosed();
		return this.fetchValueFromIndex(columnIndex).asFloat();
	}

	@Override public short getShort(String columnLabel) throws SQLException {
		checkClosed();
		return (short) this.fetchValueFromLabel(columnLabel).asInt();
	}

	@Override public short getShort(int columnIndex) throws SQLException {
		checkClosed();
		return (short) this.fetchValueFromIndex(columnIndex).asInt();
	}

	@Override public double getDouble(int columnIndex) throws SQLException {
		checkClosed();
		return this.fetchValueFromIndex(columnIndex).asDouble();
	}

	@Override public Array getArray(int columnIndex) throws SQLException {
		checkClosed();
		List<Object> list = this.fetchValueFromIndex(columnIndex).asList();
		return new ListArray(list, Array.getObjectType(list.get(0)));
	}

	@Override public Array getArray(String columnLabel) throws SQLException {
		checkClosed();
		List list = this.fetchValueFromLabel(columnLabel).asList();
		return new ListArray(list, Array.getObjectType(list.get(0)));
	}

	@Override public double getDouble(String columnLabel) throws SQLException {
		checkClosed();
		return this.fetchValueFromLabel(columnLabel).asDouble();
	}

	@Override public ResultSetMetaData getMetaData() throws SQLException {
		return InstanceFactory.debug(BoltResultSetMetaData.class, new BoltResultSetMetaData(this.iterator, this.keys), this.isLoggable());
	}

	private Object generateObject(Object obj) {
		Object result = obj;
		if (obj.getClass().equals(InternalNode.class)) {
			InternalNode node = (InternalNode) obj;
			HashMap<String, Object> map = new HashMap<>();
			map.put("_id", node.id());
			map.put("_labels", node.labels());
			node.keys().forEach(key -> map.put(key, node.get(key).asObject()));
			result = map;
		}
		if (obj.getClass().equals(InternalRelationship.class)) {
			InternalRelationship rel = (InternalRelationship) obj;
			HashMap<String, Object> map = new HashMap<>();
			map.put("_id", rel.id());
			map.put("_type", rel.type());
			map.put("_startId", rel.startNodeId());
			map.put("_endId", rel.endNodeId());
			rel.keys().forEach(key -> map.put(key, rel.get(key).asObject()));
			result = map;
		}
		if (obj.getClass().equals(InternalPath.class)) {
			InternalPath path = (InternalPath) obj;
			List<Object> list = new ArrayList<Object>();
			list.add(this.generateObject(path.start()));
			Iterator<Path.Segment> it = path.iterator();
			while (it.hasNext()) {
				Path.Segment segment = it.next();
				list.add(this.generateObject(segment.relationship()));
				list.add(this.generateObject(segment.end()));
			}
			result = list;
		}
		return result;
	}

	@Override public Object getObject(int columnIndex) throws SQLException {
		checkClosed();
		Object obj = this.fetchValueFromIndex(columnIndex).asObject();
		return this.generateObject(obj);
	}

	@Override public Object getObject(String columnLabel) throws SQLException {
		checkClosed();
		Object obj = this.fetchValueFromLabel(columnLabel).asObject();
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

	public StatementResult getIterator() {
		return this.iterator;
	}

	public List<String> getKeys() {
		return this.keys;
	}
}
