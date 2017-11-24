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
 * Created on 11/02/16
 */
package org.neo4j.jdbc.bolt;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.neo4j.driver.internal.types.InternalTypeSystem;
import org.neo4j.driver.internal.value.BooleanValue;
import org.neo4j.driver.internal.value.FloatValue;
import org.neo4j.driver.internal.value.IntegerValue;
import org.neo4j.driver.internal.value.ListValue;
import org.neo4j.driver.internal.value.NodeValue;
import org.neo4j.driver.internal.value.PathValue;
import org.neo4j.driver.internal.value.RelationshipValue;
import org.neo4j.driver.internal.value.StringValue;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Value;
import org.neo4j.driver.v1.exceptions.value.Uncoercible;
import org.neo4j.driver.v1.types.Node;
import org.neo4j.driver.v1.types.Path;
import org.neo4j.driver.v1.types.Relationship;
import org.neo4j.driver.v1.types.Type;
import org.neo4j.driver.v1.util.Pair;
import org.neo4j.jdbc.*;
import org.neo4j.jdbc.Neo4jStatement;
import org.neo4j.jdbc.impl.ListArray;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltNeo4jResultSet extends Neo4jResultSet implements Loggable {

	private StatementResult        iterator;
	private Neo4jResultSetMetaData metaData;
	private Record                 current;
	private List<String>           keys;
	private List<Type>             classes;
	private int                    type;
	private int                    concurrency;
	private int                    holdability;
	private boolean                wasNull;

	public static final int DEFAULT_TYPE        = TYPE_FORWARD_ONLY;
	public static final int DEFAULT_CONCURRENCY = CONCUR_READ_ONLY;
	public static final int DEFAULT_HOLDABILITY = CLOSE_CURSORS_AT_COMMIT;

	private boolean loggable  = false;
	private boolean flattened = false;

	private static final List<String> ACCEPTED_TYPES_FOR_FLATTENING = Arrays.asList("NODE", "RELATIONSHIP");
	private Neo4jStatement statement;

	private int flatten;

	private LinkedList<Record> prefetchedRecords = null;

	private static final String COLUMN_NOT_PRESENT = "Column not present in ResultSet";

	/**
	 * Default constructor for this class, if no params are given or if some params are missing it uses the defaults.
	 *
	 * @param statement The <code>Statement</code> this ResultSet comes from
	 * @param iterator  The <code>StatementResult</code> of this set
	 * @param params    At most three, type, concurrency and holdability.
	 *                  The defaults are <code>TYPE_FORWARD_ONLY</code>,
	 *                  <code>CONCUR_READ_ONLY</code>,
	 */
	public BoltNeo4jResultSet(Neo4jStatement statement, StatementResult iterator, int... params) {
		this.statement = statement;
		this.iterator = iterator;

		this.keys = new ArrayList<>();
		this.classes = new ArrayList<>();
		this.prefetchedRecords = new LinkedList<>();

		try {
			this.flatten = this.statement.getConnection().getFlattening();
		} catch (Exception e) {
			this.flatten = 0;
		}

		if (this.flatten != 0 && this.iterator != null && this.iterator.hasNext() && this.iterator.peek() != null && this.flatteningTypes(this.iterator)) {
			//Flatten the result
			this.flattenResultSet();
			this.flattened = true;
		} else if (this.iterator != null) {
			//Keys are exactly the ones returned from the iterator
			this.keys = this.iterator.keys();
			if (this.iterator.hasNext()) {
				for (Value value : this.iterator.peek().values()) {
					this.classes.add(value.type());
				}
			}
		}

		this.type = params.length > 0 ? params[0] : TYPE_FORWARD_ONLY;
		this.concurrency = params.length > 1 ? params[1] : CONCUR_READ_ONLY;
		this.holdability = params.length > 2 ? params[2] : CLOSE_CURSORS_AT_COMMIT;

		this.metaData = InstanceFactory.debug(BoltNeo4jResultSetMetaData.class, new BoltNeo4jResultSetMetaData(this.classes, this.keys), this.isLoggable());
	}

	private void flattenResultSet() {
		for (int i = 0; (this.flatten == -1 || i < this.flatten) && this.iterator.hasNext(); i++) {
			this.prefetchedRecords.add(this.iterator.next());
			this.flattenRecord(this.prefetchedRecords.getLast());
		}
	}

	private void flattenRecord(Record r) {
		for (Pair<String, Value> pair : r.fields()) {
			if (keys.indexOf(pair.key()) == -1) {
				keys.add(pair.key());
				classes.add(r.get(pair.key()).type());
			}
			Value val = r.get(pair.key());
			if (ACCEPTED_TYPES_FOR_FLATTENING.get(0).equals(pair.value().type().name())) {
				//Flatten node
				this.flattenNode(val.asNode(), pair.key());
			} else if (ACCEPTED_TYPES_FOR_FLATTENING.get(1).equals(pair.value().type().name())) {
				//Flatten relationship
				this.flattenRelationship(val.asRelationship(), pair.key());
			}
		}
	}

	private void flattenNode(Node node, String nodeKey) {
		if (keys.indexOf(nodeKey + ".id") == -1) {
			keys.add(nodeKey + ".id");
			classes.add(InternalTypeSystem.TYPE_SYSTEM.INTEGER());
			keys.add(nodeKey + ".labels");
			classes.add(InternalTypeSystem.TYPE_SYSTEM.LIST());
		}
		for (String key : node.keys()) {
			if (keys.indexOf(nodeKey + "." + key) == -1) {
				keys.add(nodeKey + "." + key);
				classes.add(node.get(key).type());
			}
		}
	}

	private void flattenRelationship(Relationship rel, String relationshipKey) {
		if (keys.indexOf(relationshipKey + ".id") == -1) {
			keys.add(relationshipKey + ".id");
			classes.add(InternalTypeSystem.TYPE_SYSTEM.INTEGER());
			keys.add(relationshipKey + ".type");
			classes.add(InternalTypeSystem.TYPE_SYSTEM.STRING());
		}
		for (String key : rel.keys()) {
			if (keys.indexOf(relationshipKey + "." + key) == -1) {
				keys.add(relationshipKey + "." + key);
				classes.add(rel.get(key).type());
			}
		}

	}

	private boolean flatteningTypes(StatementResult statementResult) {
		boolean result = true;

		for (Pair<String, Value> pair : statementResult.peek().fields()) {
			if (!ACCEPTED_TYPES_FOR_FLATTENING.contains(pair.value().type().name())) {
				result = false;
				break;
			}
		}

		return result;
	}

	@Override protected boolean innerNext() throws SQLException {
		if (this.iterator == null) {
			throw new SQLException("ResultCursor not initialized");
		}
		if (!this.prefetchedRecords.isEmpty()) {
			this.current = this.prefetchedRecords.pop();
		} else if (this.iterator.hasNext()) {
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
		this.isClosed = true;
	}

	@Override public boolean wasNull() throws SQLException {
		checkClosed();
		return this.wasNull;
	}

	@Override public String getString(int columnIndex) throws SQLException {
		checkClosed();
		Value value = this.fetchValueFromIndex(columnIndex);
		return this.getStringFromValue(value);
	}

	@Override public String getString(String columnLabel) throws SQLException {
		checkClosed();
		Value value = this.fetchValueFromLabel(columnLabel);
		return this.getStringFromValue(value);
	}

	private String getStringFromValue(Value value) {
		try {
			if (value.isNull()) {
				return null;
			} else {
				return value.asString();
			}
		} catch (Uncoercible e) {
			String result = null;
			if (value instanceof IntegerValue) {
				result = ((IntegerValue) value).asLiteralString();
			}	else if (value instanceof FloatValue) {
				result = ((FloatValue) value).asLiteralString();
			}	else if (value instanceof NodeValue) {
				result = this.convertNodeToString(value.asNode());
			} else if (value instanceof RelationshipValue) {
				result = this.convertRelationshipToString(value.asRelationship());
			} else if (value instanceof PathValue) {
				result = this.convertPathToString(value.asPath());
			}
			return result;
		}
	}

	@SuppressWarnings("rawtypes")
	private String convertToJSONProperty(String key, Object value) {
		String result = key == null ? "" : "\"" + key + "\":";

		if (value instanceof String) {
			result += "\"" + value + "\"";
		} else if (value instanceof Number) {
			result += value;
		} else if (value instanceof StringValue) {
			result += "\"" + ((StringValue) value).asString() + "\"";
		} else if (value instanceof IntegerValue) {
			result += Long.toString(((IntegerValue) value).asLong());
		} else if (value instanceof FloatValue) {
			result += Float.toString(((FloatValue) value).asFloat());
		} else if (value instanceof BooleanValue) {
			result += Boolean.toString(((BooleanValue) value).asBoolean());
		} else if (value instanceof ListValue) {
			result += "[";
			result += this.convertToJSONProperty(null, ((ListValue) value).asList());
			result += "]";
		} else if (value instanceof List) {
			String prefix = "";
			result += "[";
			for (Object obj : (List) value) {
				result += prefix + this.convertToJSONProperty(null, obj);
				prefix = ", ";
			}
			result += "]";
		} else if (value instanceof Iterable) {
			String prefix = "";
			result += "[";
			for (Object obj : (Iterable) value) {
				result += prefix + this.convertToJSONProperty(null, obj);
				prefix = ", ";
			}
			result += "]";
		}

		return result;
	}

	private String convertNodeToString(Node node) {
		String result = "{";

		result += this.convertToJSONProperty("id", node.id()) + ", ";

		result += this.convertToJSONProperty("labels", node.labels()) + (node.size() > 0 ? ", " : "");

		String prefix = "";
		for (String key : node.keys()) {
			result += prefix + this.convertToJSONProperty(key, node.get(key));
			prefix = ", ";
		}

		return result + "}";
	}

	private String convertRelationshipToString(Relationship rel) {
		String result = "{";

		result += this.convertToJSONProperty("id", rel.id()) + ", ";

		result += this.convertToJSONProperty("type", rel.type()) + ", ";

		result += this.convertToJSONProperty("startId", rel.startNodeId()) + ", ";
		result += this.convertToJSONProperty("endId", rel.endNodeId()) + (rel.size() > 0 ? ", " : "");

		String prefix = "";
		for (String key : rel.keys()) {
			result += prefix + this.convertToJSONProperty(key, rel.get(key));
			prefix = ", ";
		}

		return result + "}";
	}

	private String convertPathToString(Path path) {
		String result = "[";

		result += this.convertNodeToString(path.start());

		for (Path.Segment s : path) {
			result += ", " + this.convertRelationshipToString(s.relationship());
			result += ", " + this.convertNodeToString(s.end());
		}

		return result + "]";
	}

	@Override public boolean getBoolean(String columnLabel) throws SQLException {
		checkClosed();
		Value value = this.fetchValueFromLabel(columnLabel);
		return value.isNull() ? false : value.asBoolean();
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
			throw new SQLException(COLUMN_NOT_PRESENT, e);
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
			String[] labelKeys = label.split("\\.");
			value = this.fetchPropertyValue(labelKeys[0], labelKeys[1]);
		} else {
			//No value found
			throw new SQLException(COLUMN_NOT_PRESENT);
		}
		this.wasNull = value.isNull();
		return value;
	}

	private Value fetchValueFromIndex(int index) throws SQLException {
		Value value;
		if (this.flattened && index > 0 && index - 1 <= this.keys.size()) {
			//Requested value is to be considered from flattened results
			String[] indexKeys = this.keys.get(index - 1).split("\\.");
			if (indexKeys.length > 1) { //Requested value is a virtual column
				value = this.fetchPropertyValue(indexKeys[0], indexKeys[1]);
			} else {
				//Requested value is the node/relationship itself
				value = this.current.get(this.keys.get(index - 1));
			}
		} else if (index > 0 && index - 1 <= this.current.size()) {
			//Requested value is not flattened
			value = this.current.get(index - 1);
		} else {
			//No value found
			throw new SQLException(COLUMN_NOT_PRESENT);
		}
		this.wasNull = value.isNull();
		return value;
	}

	@Override public int getInt(String columnLabel) throws SQLException {
		checkClosed();
		Value value = this.fetchValueFromLabel(columnLabel);
		return value.isNull() ? 0 : value.asInt();
	}

	@Override public long getLong(String columnLabel) throws SQLException {
		checkClosed();
		Value value = this.fetchValueFromLabel(columnLabel);
		return value.isNull() ? 0 : value.asLong();
	}

	@Override public int findColumn(String columnLabel) throws SQLException {
		checkClosed();
		if (!this.keys.contains(columnLabel)) {
			throw new SQLException(COLUMN_NOT_PRESENT);
		}
		return this.keys.indexOf(columnLabel) + 1;
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

	@Override public boolean getBoolean(int columnIndex) throws SQLException {
		checkClosed();
		Value value = this.fetchValueFromIndex(columnIndex);
		return value.isNull() ? false : value.asBoolean();
	}

	@Override public int getInt(int columnIndex) throws SQLException {
		checkClosed();
		Value value = this.fetchValueFromIndex(columnIndex);
		return value.isNull() ? 0 : value.asInt();
	}

	@Override public long getLong(int columnIndex) throws SQLException {
		checkClosed();
		Value value = this.fetchValueFromIndex(columnIndex);
		return value.isNull() ? 0 : value.asLong();
	}

	@Override public float getFloat(String columnLabel) throws SQLException {
		checkClosed();
		Value value = this.fetchValueFromLabel(columnLabel);
		return value.isNull() ? 0 : value.asFloat();
	}

	@Override public float getFloat(int columnIndex) throws SQLException {
		checkClosed();
		Value value = this.fetchValueFromIndex(columnIndex);
		return value.isNull() ? 0 : value.asFloat();
	}

	@Override public short getShort(String columnLabel) throws SQLException {
		checkClosed();
		Value value = this.fetchValueFromLabel(columnLabel);
		return value.isNull() ? 0 : (short) value.asInt();
	}

	@Override public short getShort(int columnIndex) throws SQLException {
		checkClosed();
		Value value = this.fetchValueFromIndex(columnIndex);
		return value.isNull() ? 0 : (short) value.asInt();
	}

	@Override public double getDouble(int columnIndex) throws SQLException {
		checkClosed();
		Value value = this.fetchValueFromIndex(columnIndex);
		return value.isNull() ? 0 : value.asDouble();
	}

	@Override public Neo4jArray getArray(int columnIndex) throws SQLException {
		checkClosed();
		List<Object> list = this.fetchValueFromIndex(columnIndex).asList();
		return new ListArray(list, Neo4jArray.getObjectType(list.get(0)));
	}

	@SuppressWarnings("rawtypes")
	@Override public Neo4jArray getArray(String columnLabel) throws SQLException {
		checkClosed();
		List list = this.fetchValueFromLabel(columnLabel).asList();
		return new ListArray(list, Neo4jArray.getObjectType(list.get(0)));
	}

	@Override public double getDouble(String columnLabel) throws SQLException {
		checkClosed();
		Value value = this.fetchValueFromLabel(columnLabel);
		return value.isNull() ? 0 : value.asDouble();
	}

	@Override public Neo4jResultSetMetaData getMetaData() throws SQLException {
		return metaData;
	}

	private Object generateObject(Object obj) {
		if (obj instanceof Node) {
			Node node = (Node) obj;
			Map<String, Object> map = new HashMap<>();
			map.put("_id", node.id());
			map.put("_labels", node.labels());
			map.putAll(node.asMap());
			return map;
		}
		if (obj instanceof Relationship) {
			Relationship rel = (Relationship) obj;
			Map<String, Object> map = new HashMap<>(16);
			map.put("_id", rel.id());
			map.put("_type", rel.type());
			map.put("_startId", rel.startNodeId());
			map.put("_endId", rel.endNodeId());
			map.putAll(rel.asMap());
			return map;
		}
		if (obj instanceof Path) {
			Path path = (Path) obj;
			List<Object> list = new ArrayList<>(path.length());
			list.add(this.generateObject(path.start()));
			for (Path.Segment segment : path) {
				list.add(this.generateObject(segment.relationship()));
				list.add(this.generateObject(segment.end()));
			}
			return list;
		}
		return obj;
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

	@Override public boolean isLoggable() {
		return this.loggable;
	}

	@Override public void setLoggable(boolean loggable) {
		this.loggable = loggable;
	}

	@Override public java.sql.Statement getStatement() throws SQLException {
		return statement;
	}
}
