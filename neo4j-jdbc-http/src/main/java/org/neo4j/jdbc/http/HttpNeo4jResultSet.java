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
 * Created on 15/4/2016
 */
package org.neo4j.jdbc.http;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.neo4j.jdbc.Neo4jArray;
import org.neo4j.jdbc.Neo4jResultSet;
import org.neo4j.jdbc.Neo4jResultSetMetaData;
import org.neo4j.jdbc.Neo4jStatement;
import org.neo4j.jdbc.http.driver.Neo4jResult;
import org.neo4j.jdbc.impl.ListArray;

import java.sql.SQLDataException;
import java.sql.SQLException;
import java.util.*;

/**
 * ResultSet for the HTTP connector.
 * This is a wrapper of Neo4jResult.
 */
public class HttpNeo4jResultSet extends Neo4jResultSet {

	/**
	 * Jackson mapper to make toString() method for complex object.
	 */
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	/**
	 * Neo4j query result.
	 */
	Neo4jResult result;

	/**
	 * Cursor position of this iterator.
	 */
	private int row = -1;

	/**
	 * The current row of the iterator.
	 */
	private List<Object> currentRow;

	/**
	 * Statement that have produced this ResultSet.
	 */
	private Neo4jStatement statement;

	private static final String INVALID_COLUMN = "Column %s is invalid";
	private static final String UNDEFINED_COLUMN = "Column %s is not defined";
	private static final String COLUMN_NOT_ARRAY = "Column %s is not an array";

	static {
		OBJECT_MAPPER.configure(DeserializationFeature.USE_LONG_FOR_INTS, true);
	}

	/**
	 * Default constructor.
	 *
	 * @param statement Statement of this resultset.
	 * @param result    A Neo4j query result.
	 */
	public HttpNeo4jResultSet(Neo4jStatement statement, Neo4jResult result) {
		super(statement);
		this.statement = statement;
		this.result = result;
		this.row = -1;
	}

	/**
	 * Retrieve the object that match the asked column.
	 *
	 * @param column Index of the column to retrieve
	 * @return
	 * @throws SQLDataException
	 */
	private Object get(int column) throws SQLDataException {

		if (column < 1 || column > result.getColumns().size()) {
			throw new SQLDataException(String.format(INVALID_COLUMN, column));
		}

		Object value = currentRow.get(column - 1);

		if (value == null) {
			wasNull = true;
		} else {
			wasNull = false;
		}
		return value;
	}

	/**
	 * Retrieve a Numeric object from the currentRow that correspond to the column index.
	 *
	 * @param columnIndex Index of the column
	 * @return 0 if null, otherwise a number
	 * @throws SQLException If the object cannot be cast to Number
	 */
	private Number getNumber(int columnIndex) throws SQLException {

		Number num = null;
		Object value = get(columnIndex);

		if (value != null) {
			if (value instanceof Number) {
				num = (Number) value;
			} else {
				throw new SQLDataException("Value is not a number" + value);

			}
		}
		return num;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected boolean innerNext() throws SQLException {

		checkClosed();
		row++;
		if (row < result.getRows().size()) {
			currentRow = (List<Object>) result.getRows().get(row).get("row");
			return true;
		} else {
			currentRow = null;
			return false;
		}
	}

	@Override
	public void close() throws SQLException {

		result = null;
		row = -1;
		isClosed = true;
		currentRow = null;
	}

	@Override
	public boolean wasNull() throws SQLException {

		checkClosed();
		return this.wasNull;
	}

	@Override
	public Neo4jResultSetMetaData getMetaData() throws SQLException {

		return new HttpNeo4jResultSetMetaData(result);
	}

	@Override
	public String getString(int columnIndex) throws SQLException {

		String object = null;

		checkClosed();
		Object value = get(columnIndex);

		if (value != null) {
			final Class<?> type = value.getClass();

			object = getObjectByTypeAndValue(type, value);
		}
		return object;
	}

	private String getObjectByTypeAndValue(Class<?> type, Object value) throws SQLException {
		String object;
		if (String.class.equals(type)) {
			object = (String) value;
		} else {
			if (type.isPrimitive() || Number.class.isAssignableFrom(type)) {
				object = value.toString();
			} else {
				try {
					object = OBJECT_MAPPER.writeValueAsString(value);
				} catch (Exception e) {
					throw new SQLException("Couldn't convert value " + value + " of type " + type + " to JSON " + e.getMessage(), e);
				}
			}
		}
		return object;
	}

	@Override
	public boolean getBoolean(int columnIndex) throws SQLException {

		checkClosed();
		Boolean hasResults = (Boolean) get(columnIndex);
		if (hasResults == null)
			return false;
		else
			return hasResults;
	}

	@Override
	public short getShort(int columnIndex) throws SQLException {

		checkClosed();
		if (getNumber(columnIndex) == null)
			return 0;
		else
			return getNumber(columnIndex).shortValue();
	}

	@Override
	public int getInt(int columnIndex) throws SQLException {

		checkClosed();
		if (getNumber(columnIndex) == null)
			return 0;
		else
			return getNumber(columnIndex).intValue();
	}

	@Override
	public long getLong(int columnIndex) throws SQLException {

		checkClosed();
		if (getNumber(columnIndex) == null)
			return 0;
		else
			return getNumber(columnIndex).longValue();
	}

	@Override
	public float getFloat(int columnIndex) throws SQLException {

		checkClosed();
		if (getNumber(columnIndex) == null)
			return 0;
		else
			return getNumber(columnIndex).floatValue();
	}

	@Override
	public double getDouble(int columnIndex) throws SQLException {

		checkClosed();
		if (getNumber(columnIndex) == null)
			return 0;
		else
			return getNumber(columnIndex).doubleValue();
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Neo4jArray getArray(int columnIndex) throws SQLException {

		checkClosed();
		// Default list for null array
		List results = new ArrayList<String>();
		Object obj = get(columnIndex);
		if (obj != null) {
			if (obj.getClass().isArray()){
				results = Arrays.asList((Neo4jArray) obj);
			}else if (obj instanceof List){
				results = (List)obj;
			}else{
				throw new SQLException(String.format(COLUMN_NOT_ARRAY, columnIndex));
			}
		}

		Object objType = (results.isEmpty())?new Object():results.get(0);

		return new ListArray(results, Neo4jArray.getObjectType(objType));
	}

	@Override
	public Object getObject(int columnIndex) throws SQLException {

		checkClosed();
		Object obj = get(columnIndex);
		return convertValue(obj);
	}

	private Map<String, Object> convertPoint(Map<String, Object> objMap){

		Map<String, Object> converted = new HashMap<>();

		Map<String, Object> crs = (Map<String, Object>) objMap.get("crs");

		Long srid = (Long) crs.get("srid");

		List<Double> coordinates = (List<Double>) objMap.get("coordinates");

		converted.put("srid",srid.intValue());
		converted.put("crs",crs.get("name"));

		converted.put("x",coordinates.get(0));
		converted.put("y",coordinates.get(1));
		if(coordinates.size() > 2){
			converted.put("z",coordinates.get(2));
		}

		if (srid == 4326 || srid == 4979){
			converted.put("longitude",coordinates.get(0));
			converted.put("latitude",coordinates.get(1));
			if(coordinates.size() > 2){
				converted.put("height",coordinates.get(2));
			}
		}

		return converted;
	}

	private Object convertValue(Object obj) {
		if (obj instanceof Map) {
			Map<String, Object> objMap = (Map<String, Object>) obj;

			if (objMap.containsKey("type") && "Point".equals(objMap.get("type"))) {
				return convertPoint(objMap);
			} else {
				Map<String, Object> converted = new HashMap<>(objMap.size());
				for (Map.Entry<String, Object> entry : objMap.entrySet()) {
					converted.put(entry.getKey(), convertValue(entry.getValue()));
				}
				return converted;
			}
		}
		if (obj instanceof List) {
			List<Object> objList = (List) obj;
			List<Object> converted = new ArrayList<>(objList.size());
			for (Object o : objList) {
				converted.add(convertValue(o));
			}
			return converted;
		}

		return obj;
	}

	@Override
	public String getString(String columnLabel) throws SQLException {

		return getString(findColumn(columnLabel));
	}

	@Override
	public boolean getBoolean(String columnLabel) throws SQLException {

		return getBoolean(findColumn(columnLabel));
	}

	@Override
	public short getShort(String columnLabel) throws SQLException {

		return getShort(findColumn(columnLabel));
	}

	@Override
	public int getInt(String columnLabel) throws SQLException {

		return getInt(findColumn(columnLabel));
	}

	@Override
	public long getLong(String columnLabel) throws SQLException {

		return getLong(findColumn(columnLabel));
	}

	@Override
	public float getFloat(String columnLabel) throws SQLException {

		return getFloat(findColumn(columnLabel));
	}

	@Override
	public double getDouble(String columnLabel) throws SQLException {

		return getDouble(findColumn(columnLabel));
	}

	@Override
	public Neo4jArray getArray(String columnLabel) throws SQLException {

		return getArray(findColumn(columnLabel));
	}

	@Override
	public Object getObject(String columnLabel) throws SQLException {

		return getObject(findColumn(columnLabel));
	}

	@Override
	public int findColumn(String columnLabel) throws SQLException {

		checkClosed();

		// The indexOf return -1 if not found
		int index = -1;
		if (columnLabel != null) {
			index = result.getColumns().indexOf(columnLabel);
		}

		// To respect the specification
		if (index == -1) {
			throw new SQLException(String.format(UNDEFINED_COLUMN, columnLabel));
		}

		// here we make +1 because column index for JDBC start at 1, not 0 like an arraylist
		return index + 1;
	}

	@Override
	public int getType() throws SQLException {

		checkClosed();
		return TYPE_FORWARD_ONLY;
	}

	@Override
	public int getConcurrency() throws SQLException {

		checkClosed();
		return CONCUR_READ_ONLY;
	}

	@Override
	public int getHoldability() throws SQLException {

		checkClosed();
		return CLOSE_CURSORS_AT_COMMIT;
	}

	@Override
	public boolean isClosed() throws SQLException {

		return this.isClosed;
	}

	@Override
	public java.sql.Statement getStatement() throws SQLException {

		return statement;
	}
}
