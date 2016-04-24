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
 * Created on 15/4/2016
 */
package it.larusba.neo4j.jdbc.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.larusba.neo4j.jdbc.Array;
import it.larusba.neo4j.jdbc.Loggable;
import it.larusba.neo4j.jdbc.ResultSet;
import it.larusba.neo4j.jdbc.ResultSetMetaData;
import it.larusba.neo4j.jdbc.http.driver.Neo4jResult;
import it.larusba.neo4j.jdbc.impl.ListArray;

import java.sql.SQLDataException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ResultSet for the HTTP connector.
 * This is a wrapper of Neo4jResult.
 */
public class HttpResultSet extends ResultSet implements Loggable {

	/**
	 * Jackson mapper to make toString() method for complex object.
	 */
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	/**
	 * Neo4j query result.
	 */
	protected Neo4jResult result;

	/**
	 * Cursor position of this iterator.
	 */
	private int row = -1;

	/**
	 * The current row of the iterator.
	 */
	private List<Object> currentRow;

	/**
	 * Is the last read column was null.
	 */
	private boolean wasNull = false;

	private boolean loggable;
	private boolean isClosed = false;

	/**
	 * Default constructor.
	 *
	 * @param result A Neo4j query result.
	 */
	public HttpResultSet(Neo4jResult result) {
		this.result = result;
		this.row = -1;
	}

	/**
	 * Check if this connection is closed or not.
	 * If it's closed, then we throw a SQLException, otherwise we do nothing.
	 *
	 * @throws SQLException
	 */
	private void checkClosed() throws SQLException {
		if (isClosed()) {
			throw new SQLException("Connection is closed.");
		}
	}

	/**
	 * Retrieve the object that match the asked column.
	 *
	 * @param column Index of the column to retrieve
	 * @return
	 * @throws SQLDataException
	 */
	private Object get(int column) throws SQLDataException {
		if (column < 1 || column > result.columns.size()) {
			throw new SQLDataException("Column " + column + " is invalid");
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

	@Override public boolean next() throws SQLException {
		checkClosed();
		row++;
		if (row < result.rows.size()) {
			currentRow = (List<Object>) result.rows.get(row).get("row");
			return true;
		} else {
			currentRow = null;
			return false;
		}
	}

	@Override public void close() throws SQLException {
		checkClosed();
		result = null;
		row = -1;
		isClosed = true;
		currentRow = null;
	}

	@Override public boolean wasNull() throws SQLException {
		checkClosed();
		return this.wasNull;
	}

	@Override public ResultSetMetaData getMetaData() throws SQLException {
		return new HttpResultSetMetaData(result);
	}

	@Override public String getString(int columnIndex) throws SQLException {
		String object = null;

		checkClosed();
		Object value = get(columnIndex);

		if (value != null) {
			final Class<?> type = value.getClass();

			if (String.class.equals(type)) {
				object = (String) value;
			} else {
				if (type.isPrimitive() || Number.class.isAssignableFrom(type)) {
					object = value.toString();
				} else {
					try {
						object = OBJECT_MAPPER.writeValueAsString(value);
					} catch (Exception e) {
						throw new SQLException("Couldn't convert value " + value + " of type " + type + " to JSON " + e.getMessage());
					}
				}
			}
		}
		return object;
	}

	@Override public boolean getBoolean(int columnIndex) throws SQLException {
		checkClosed();
		Boolean result = (Boolean) get(columnIndex);
		if(result == null)
			return false;
		else
			return result;
	}

	@Override public short getShort(int columnIndex) throws SQLException {
		checkClosed();
		if(getNumber(columnIndex) == null)
			return 0;
		else
			return getNumber(columnIndex).shortValue();
	}

	@Override public int getInt(int columnIndex) throws SQLException {
		checkClosed();
		if(getNumber(columnIndex) == null)
			return 0;
		else
			return getNumber(columnIndex).intValue();
	}

	@Override public long getLong(int columnIndex) throws SQLException {
		checkClosed();
		if(getNumber(columnIndex)== null)
			return 0;
		else
			return getNumber(columnIndex).longValue();
	}

	@Override public float getFloat(int columnIndex) throws SQLException {
		checkClosed();
		if(getNumber(columnIndex) == null)
			return 0;
		else
			return getNumber(columnIndex).floatValue();
	}

	@Override public double getDouble(int columnIndex) throws SQLException {
		checkClosed();
		if(getNumber(columnIndex) == null)
			return 0;
		else
			return getNumber(columnIndex).doubleValue();
	}

	@Override public Array getArray(int columnIndex) throws SQLException {
		checkClosed();
		// Default list for null array
		List result = new ArrayList<String>();
		Object obj = get(columnIndex);
		if (obj != null) {
			if (!obj.getClass().isArray()) {
				throw new SQLException("Column " + columnIndex + " is not an Array");
			}

			result = Arrays.asList((Array) obj);
		}
		return new ListArray(result, Array.getObjectType(result.get(0)));
	}

	@Override public Object getObject(int columnIndex) throws SQLException {
		checkClosed();
		return get(columnIndex);
	}

	@Override public String getString(String columnLabel) throws SQLException {
		return getString(findColumn(columnLabel));
	}

	@Override public boolean getBoolean(String columnLabel) throws SQLException {
		return getBoolean(findColumn(columnLabel));
	}

	@Override public short getShort(String columnLabel) throws SQLException {
		return getShort(findColumn(columnLabel));
	}

	@Override public int getInt(String columnLabel) throws SQLException {
		return getInt(findColumn(columnLabel));
	}

	@Override public long getLong(String columnLabel) throws SQLException {
		return getLong(findColumn(columnLabel));
	}

	@Override public float getFloat(String columnLabel) throws SQLException {
		return getFloat(findColumn(columnLabel));
	}

	@Override public double getDouble(String columnLabel) throws SQLException {
		return getDouble(findColumn(columnLabel));
	}

	@Override public Array getArray(String columnLabel) throws SQLException {
		return getArray(findColumn(columnLabel));
	}

	@Override public Object getObject(String columnLabel) throws SQLException {
		return getObject(findColumn(columnLabel));
	}

	@Override public int findColumn(String columnLabel) throws SQLException {
		checkClosed();

		// The indexOf return -1 if not found
		int index = -1;
		if (columnLabel != null) {
			index = result.columns.indexOf(columnLabel);
		}

		// To respect the specification
		if (index == -1) {
			throw new SQLException("Column " + columnLabel + " is not defined");
		}

		// here we make +1 because column index for JDBC start at 1, not 0 like an arraylist
		return index + 1;
	}

	@Override public int getType() throws SQLException {
		checkClosed();
		return TYPE_FORWARD_ONLY;
	}

	@Override public int getConcurrency() throws SQLException {
		checkClosed();
		return CONCUR_READ_ONLY;
	}

	@Override public int getHoldability() throws SQLException {
		checkClosed();
		return CLOSE_CURSORS_AT_COMMIT;
	}

	@Override public boolean isClosed() throws SQLException {
		return this.isClosed;
	}

	@Override public boolean isLoggable() {
		return this.loggable;
	}

	@Override public void setLoggable(boolean loggable) {
		this.loggable = loggable;
	}

}
