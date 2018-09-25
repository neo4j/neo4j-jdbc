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

import org.neo4j.driver.internal.InternalIsoDuration;
import org.neo4j.driver.internal.types.InternalTypeSystem;
import org.neo4j.driver.internal.value.*;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Value;
import org.neo4j.driver.v1.exceptions.value.Uncoercible;
import org.neo4j.driver.v1.types.*;
import org.neo4j.driver.v1.util.Pair;
import org.neo4j.jdbc.Neo4jArray;
import org.neo4j.jdbc.Neo4jConnection;
import org.neo4j.jdbc.Neo4jResultSet;
import org.neo4j.jdbc.impl.ListArray;
import org.neo4j.jdbc.utils.Neo4jInvocationHandler;
import org.neo4j.jdbc.utils.ObjectConverter;

import java.lang.reflect.Proxy;
import java.sql.Date;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltNeo4jResultSet extends Neo4jResultSet {

	private StatementResult   iterator;
	private ResultSetMetaData metaData;
	private Record            current;
	private List<String>      keys;
	private List<Type>        classes;

	private boolean flattened = false;

	private static final List<String> ACCEPTED_TYPES_FOR_FLATTENING = Arrays.asList("NODE", "RELATIONSHIP");

	private int flatten;

	private LinkedList<Record> prefetchedRecords = null;

	/**
	 * Default constructor for this class, if no params are given or if some params are missing it uses the defaults.
	 *
	 * @param statement The <code>Statement</code> this ResultSet comes from
	 * @param iterator  The <code>StatementResult</code> of this set
	 * @param params    At most three, type, concurrency and holdability.
	 *                  The defaults are <code>TYPE_FORWARD_ONLY</code>,
	 *                  <code>CONCUR_READ_ONLY</code>,
	 */
	private BoltNeo4jResultSet(Statement statement, StatementResult iterator, int... params) {
		super(statement, params);
		this.iterator = iterator;

		this.keys = new ArrayList<>();
		this.classes = new ArrayList<>();
		this.prefetchedRecords = new LinkedList<>();

		try {
			this.flatten = ((Neo4jConnection) this.statement.getConnection()).getFlattening();
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


		this.metaData = BoltNeo4jResultSetMetaData.newInstance(false, this.classes, this.keys);
	}

	public static ResultSet newInstance(boolean debug, Statement statement, StatementResult iterator, int... params) {
		ResultSet rs = new BoltNeo4jResultSet(statement, iterator, params);
		return (ResultSet) Proxy
				.newProxyInstance(BoltNeo4jResultSet.class.getClassLoader(), new Class[] { ResultSet.class }, new Neo4jInvocationHandler(rs, debug));
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
				result = ((IntegerValue) value).toString();
			} else if (value instanceof FloatValue) {
				result = ((FloatValue) value).toString();
			} else if (value instanceof NodeValue) {
				result = this.convertNodeToString(value.asNode());
			} else if (value instanceof RelationshipValue) {
				result = this.convertRelationshipToString(value.asRelationship());
			} else if (value instanceof PathValue) {
				result = this.convertPathToString(value.asPath());
			}
			return result;
		}
	}

	@SuppressWarnings("rawtypes") private String convertToJSONProperty(String key, Object value) {
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
			result += Double.toString(((FloatValue) value).asDouble());
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
		return !value.isNull() && value.asBoolean();
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
		return !value.isNull() && value.asBoolean();
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
		Object obj = (list.isEmpty())?new Object():list.get(0);

		return new ListArray(list, Neo4jArray.getObjectType(obj));
	}

	@SuppressWarnings("rawtypes") @Override public Neo4jArray getArray(String columnLabel) throws SQLException {
		checkClosed();
		List list = this.fetchValueFromLabel(columnLabel).asList();
		Object obj = (list.isEmpty())?new Object():list.get(0);

		return new ListArray(list, Neo4jArray.getObjectType(obj));
	}

	@Override public double getDouble(String columnLabel) throws SQLException {
		checkClosed();
		Value value = this.fetchValueFromLabel(columnLabel);
		return value.isNull() ? 0 : value.asDouble();
	}

	@Override public ResultSetMetaData getMetaData() throws SQLException {
		return metaData;
	}

	/**
	 * It hides the neo4j type with standard Java type (or sql java type)
	 * @param value
	 * @return
	 */
	private Object convertObject(Object value) {
		Object converted = value;
		if (value instanceof Point) {
			converted = pointToMap((Point) value);
		}
		if (value instanceof List) {
			converted = convertList((List) value);
		}
		if (value instanceof ZonedDateTime) {
			return zonedDateTimeToTimestamp((ZonedDateTime) value);
		}
		if (value instanceof LocalDateTime) {
			return localDateTimeToTimestamp((LocalDateTime) value);
		}
		if (value instanceof LocalDate) {
			return localDateToDate((LocalDate) value);
		}
		if (value instanceof OffsetTime){
			return offsetTimeToTime((OffsetTime) value);
		}
		if (value instanceof LocalTime){
			return localTimeToTime((LocalTime) value);
		}
		if (value instanceof InternalIsoDuration){
			return durationToMap((InternalIsoDuration)value);
		}

		return converted;
	}

	/**
	 * It builds a new List with the items converted into java type
	 * @param list
	 * @return
	 */
	private List convertList(List list) {
		List converted = new ArrayList(list.size());

		for (Object o : list) {
			converted.add(convertObject(o));
		}

		return converted;
	}

	/**
	 * It transforms a Neo4j Point (Spatial) into a java Map
	 * @param point
	 * @return
	 */
	private Map<String, Object> pointToMap(Point point) {
		Map<String, Object> map = new HashMap<>();
		map.put("srid",point.srid());
		map.put("x", point.x());
		map.put("y", point.y());

		switch(point.srid()){
			case 7203:
				map.put("crs","cartesian");
				break;
			case 9157:
				map.put("crs","cartesian-3d");
				map.put("z", point.z());
				break;
			case 4326:
				map.put("crs","wgs-84");
				map.put("longitude", point.x());
				map.put("latitude", point.y());
				break;
			case 4979:
				map.put("crs","wgs-84-3d");
				map.put("longitude", point.x());
				map.put("latitude", point.y());
				map.put("height",point.z());
				map.put("z", point.z());
				break;
		}

		return map;
	}

	/**
	 * It build a new Map with the same keys but with pure java type, instead of Neo4j types
	 * @param fields
	 * @return
	 */
	private Map<String, Object> convertFields(Map<String, Object> fields){
		Map<String, Object> converted = new HashMap();

		Set<Map.Entry<String, Object>> entrySet = fields.entrySet();

		for (Map.Entry<String, Object> entry : entrySet) {
			converted.put(entry.getKey(), convertObject(entry.getValue()));
		}

		return converted;
	}

	private Object generateObject(Object obj) {
		if (obj instanceof Node) {
			Node node = (Node) obj;
			Map<String, Object> map = new HashMap<>();
			map.put("_id", node.id());
			map.put("_labels", node.labels());
			map.putAll(convertFields(node.asMap()));
			return map;
		}
		if (obj instanceof Relationship) {
			Relationship rel = (Relationship) obj;
			Map<String, Object> map = new HashMap<>(16);
			map.put("_id", rel.id());
			map.put("_type", rel.type());
			map.put("_startId", rel.startNodeId());
			map.put("_endId", rel.endNodeId());
			map.putAll(convertFields(rel.asMap()));
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

		return convertObject(obj);
	}

	/**
	 * Convert a InternalIsoDuration to a Map with the same fields you can get with cypher
	 * @param obj
	 * @return
	 */
	private Map<String, Object> durationToMap(InternalIsoDuration obj) {
		Map<String, Object> converted = new HashMap<>(16);

		converted.put("duration", obj.toString());
		converted.put("months", obj.months());
		converted.put("days", obj.days());
		converted.put("seconds", obj.seconds());
		converted.put("nanoseconds", obj.nanoseconds());

		return converted;
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

	@Override public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
		try {
			return getObject(type, ()-> this.fetchValueFromLabel(columnLabel), () -> this.getObject(columnLabel));
		} catch (Exception e) {
			throw new SQLException(e);
		}
	}

	/**
	 * Check if the argument is a internal data used by neo4j
	 * @param type
	 * @return
	 */
	private boolean isNeo4jDatatype(Class type){
		return ObjectValueAdapter.class.isAssignableFrom(type);
	}

	private <T> T getObject (Class<T> type, Callable fetch, Callable getObject) throws Exception {
		checkClosed();
		if (type == null) {
			throw new SQLException("Type to cast cannot be null");
		}

		if (isNeo4jDatatype(type)){
			return (T) fetch.call();
		}

		if (type == ZonedDateTime.class){
			DateTimeValue value = (DateTimeValue) fetch.call();
			return (T) value.asZonedDateTime();
		}
		else if (type == LocalDateTime.class){
			LocalDateTimeValue value = (LocalDateTimeValue) fetch.call();
			return (T) value.asLocalDateTime();
		}
		else if (type == IsoDuration.class){
			DurationValue value = (DurationValue) fetch.call();
			return (T) value.asIsoDuration();
		}
		else if (type == LocalDate.class){
			DateValue value = (DateValue) fetch.call();
			return (T) value.asLocalDate();
		}
		else if (type == LocalTime.class){
			LocalTimeValue value = (LocalTimeValue) fetch.call();
			return (T) value.asLocalTime();
		}
		else if (type == OffsetTime.class){
			TimeValue value = (TimeValue) fetch.call();
			return (T) value.asOffsetTime();
		}
		else if (type == Point.class){
			PointValue value = (PointValue) fetch.call();
			return (T) value.asPoint();
		}
		else {
			Object obj = getObject.call();
			T ret;
			try {
				ret = ObjectConverter.convert(obj, type);
			} catch (Exception e) {
				throw new SQLException(e);
			}
			return ret;
		}
	}

	@Override public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
		try {
			return getObject(type, ()-> this.fetchValueFromIndex(columnIndex), () -> this.getObject(columnIndex));
		} catch (Exception e) {
			throw new SQLException(e);
		}
	}

	@Override public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
		checkClosed();
		Object obj = this.getObject(columnLabel);
		String fromClass = obj.getClass().getCanonicalName();
		Class<?> toClass = map.get(fromClass);
		if(toClass == null) {
			throw new SQLException(String.format("Mapping for class: %s not found", fromClass));
		}
		Object ret;
		try {
			ret = ObjectConverter.convert(obj, toClass);
		} catch (Exception e) {
			throw new SQLException(e);
		}
		return ret;
	}

	@Override public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
		checkClosed();
		Object obj = this.getObject(columnIndex);
		Object ret;
		try {
			ret = ObjectConverter.convert(obj, map.get(obj.getClass().toString()));
		} catch (Exception e) {
			throw new SQLException(e);
		}
		return ret;
	}

	@Override public Statement getStatement() {
		return statement;
	}

	/**
	 * Convert Value to sql.Time
	 * @param value
	 * @return
	 */
	private Time valueToTime(Value value){
		return valueToTime(value, Calendar.getInstance());
	}

	/**
	 * Convert Value to sql.Time with timezone
	 * @param value
	 * @return
	 */
	private Time valueToTime(Value value, Calendar cal){
		if (value.isNull()){
			return null;
		}

		if (value instanceof TimeValue){
			ZoneOffset zoneOffset = ZoneOffset.ofTotalSeconds(cal.get(Calendar.ZONE_OFFSET) / 1000);
			TimeValue timeValue = (TimeValue) value;
			OffsetTime offsetTime = timeValue.asOffsetTime().withOffsetSameInstant(zoneOffset);
			return offsetTimeToTime(offsetTime);
		}

		if (value instanceof LocalTimeValue){
			return localTimeToTime(((LocalTimeValue)value).asLocalTime());
		}

		return null;
	}

	/**
	 * Convert LocalTime to sql.Time
	 * @param localTime
	 * @return
	 */
	private Time localTimeToTime(LocalTime localTime) {
		Time time = Time.valueOf(localTime);
		time.setTime(time.getTime()+localTime.getNano() / 1000_000L);
		return time;
	}

	/**
	 * Convert OffsetTime to sql.Time
	 * @param offsetTime
	 * @return
	 */
	private Time offsetTimeToTime(OffsetTime offsetTime) {
		return localTimeToTime(offsetTime.toLocalTime());
	}


	/**
	 * Convert Value to sql.Date
	 * @param value
	 * @return
	 */
	private Date valueToDate(Value value){
		if (value.isNull()){
			return null;
		}

		if (value instanceof DateValue){
			return localDateToDate(((DateValue)value).asLocalDate());
		}

		return null;
	}

	/**
	 * Convert a LocalDate to sql.Date
	 * @param localDate
	 * @return
	 */
	private Date localDateToDate(LocalDate localDate) {
		return Date.valueOf(localDate);
	}

	/**
	 * Convert Value to Timestamp with system timezone
	 * @param value
	 * @return
	 */
	private Timestamp valueToTimestamp(Value value){
		return valueToTimestamp(value, ZoneId.systemDefault() );
	}

	/**
	 * Convert Value to Timestamp with timezone
	 * @param value
	 * @return
	 */
	private Timestamp valueToTimestamp(Value value, ZoneId zone) {
		if (value.isNull()){
			return null;
		}

		if (value instanceof DateTimeValue){
			return zonedDateTimeToTimestamp(value.asZonedDateTime().withZoneSameInstant(zone));
		}

		if (value instanceof LocalDateTimeValue){
			return localDateTimeToTimestamp(value.asLocalDateTime());
		}

		return null;
	}

	/**
	 * Convert a LocalDataTime to sql.Timestamp
	 * @param ldt
	 * @return
	 */
	private Timestamp localDateTimeToTimestamp(LocalDateTime ldt) {
		return Timestamp.valueOf(ldt);
	}

	/**
	 * Convert a ZonedDateTime to sql.Timestamp
	 * @param zdt
	 * @return
	 */
	private Timestamp zonedDateTimeToTimestamp(ZonedDateTime zdt){
		return new Timestamp(zdt.toInstant().toEpochMilli());
	}

	@Override public Timestamp getTimestamp(int columnIndex) throws SQLException {
		checkClosed();
		Value value = this.fetchValueFromIndex(columnIndex);
		return valueToTimestamp(value);
	}

	@Override public Timestamp getTimestamp(String columnLabel) throws SQLException {
		checkClosed();
		Value value = this.fetchValueFromLabel(columnLabel);
		return valueToTimestamp(value);
	}

	@Override public Date getDate(int columnIndex) throws SQLException {
		checkClosed();
		Value value = this.fetchValueFromIndex(columnIndex);
		return valueToDate(value);
	}

	@Override public Date getDate(String columnLabel) throws SQLException {
		checkClosed();
		Value value = this.fetchValueFromLabel(columnLabel);
		return valueToDate(value);
	}

	@Override public Time getTime(int columnIndex) throws SQLException {
		checkClosed();
		Value value = this.fetchValueFromIndex(columnIndex);
		return valueToTime(value);
	}

	@Override public Time getTime(String columnLabel) throws SQLException {
		checkClosed();
		Value value = this.fetchValueFromLabel(columnLabel);
		return valueToTime(value);
	}

	@Override
	public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
		checkClosed();
		Value value = this.fetchValueFromIndex(columnIndex);
		return valueToTimestamp(value, cal.getTimeZone().toZoneId());
	}

	@Override
	public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
		checkClosed();
		Value value = this.fetchValueFromLabel(columnLabel);
		return valueToTimestamp(value, cal.getTimeZone().toZoneId());
	}

	@Override
	public Time getTime(int columnIndex, Calendar cal) throws SQLException {
		checkClosed();
		Value value = this.fetchValueFromIndex(columnIndex);
		return valueToTime(value,cal);
	}

	@Override
	public Time getTime(String columnLabel, Calendar cal) throws SQLException {
		checkClosed();
		Value value = this.fetchValueFromLabel(columnLabel);
		return valueToTime(value,cal);
	}
}
