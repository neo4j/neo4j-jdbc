/**
 * Copyright (c) 2004-2015 LARUS Business Automation Srl
 * <p>
 * All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * <p>
 * Created on 03/02/16
 */
package it.neo4j.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.Calendar;
import java.util.Map;

/**
 * @author AgileLARUS
 *
 * @since 3.0.0
 */
public abstract class CallableStatement implements java.sql.CallableStatement {
	/**
	 * Registers the OUT parameter in ordinal position
	 * <code>parameterIndex</code> to the JDBC type
	 * <code>sqlType</code>.  All OUT parameters must be registered
	 * before a stored procedure is executed.
	 * <p>
	 * The JDBC type specified by <code>sqlType</code> for an OUT
	 * parameter determines the Java type that must be used
	 * in the <code>get</code> method to read the value of that parameter.
	 * <p>
	 * If the JDBC type expected to be returned to this output parameter
	 * is specific to this particular database, <code>sqlType</code>
	 * should be <code>java.sql.Types.OTHER</code>.  The method
	 * {@link #getObject} retrieves the value.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2,
	 *                       and so on
	 * @param sqlType        the JDBC type code defined by <code>java.sql.Types</code>.
	 *                       If the parameter is of JDBC type <code>NUMERIC</code>
	 *                       or <code>DECIMAL</code>, the version of
	 *                       <code>registerOutParameter</code> that accepts a scale value
	 *                       should be used.
	 * @throws SQLException                    if the parameterIndex is not valid;
	 *                                         if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if <code>sqlType</code> is
	 *                                         a <code>ARRAY</code>, <code>BLOB</code>, <code>CLOB</code>,
	 *                                         <code>DATALINK</code>, <code>JAVA_OBJECT</code>, <code>NCHAR</code>,
	 *                                         <code>NCLOB</code>, <code>NVARCHAR</code>, <code>LONGNVARCHAR</code>,
	 *                                         <code>REF</code>, <code>ROWID</code>, <code>SQLXML</code>
	 *                                         or  <code>STRUCT</code> data type and the JDBC driver does not support
	 *                                         this data type
	 * @see Types
	 */
	@Override public void registerOutParameter(int parameterIndex, int sqlType) throws SQLException {

	}

	/**
	 * Registers the parameter in ordinal position
	 * <code>parameterIndex</code> to be of JDBC type
	 * <code>sqlType</code>. All OUT parameters must be registered
	 * before a stored procedure is executed.
	 * <p>
	 * The JDBC type specified by <code>sqlType</code> for an OUT
	 * parameter determines the Java type that must be used
	 * in the <code>get</code> method to read the value of that parameter.
	 * <p>
	 * This version of <code>registerOutParameter</code> should be
	 * used when the parameter is of JDBC type <code>NUMERIC</code>
	 * or <code>DECIMAL</code>.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2,
	 *                       and so on
	 * @param sqlType        the SQL type code defined by <code>java.sql.Types</code>.
	 * @param scale          the desired number of digits to the right of the
	 *                       decimal point.  It must be greater than or equal to zero.
	 * @throws SQLException                    if the parameterIndex is not valid;
	 *                                         if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if <code>sqlType</code> is
	 *                                         a <code>ARRAY</code>, <code>BLOB</code>, <code>CLOB</code>,
	 *                                         <code>DATALINK</code>, <code>JAVA_OBJECT</code>, <code>NCHAR</code>,
	 *                                         <code>NCLOB</code>, <code>NVARCHAR</code>, <code>LONGNVARCHAR</code>,
	 *                                         <code>REF</code>, <code>ROWID</code>, <code>SQLXML</code>
	 *                                         or  <code>STRUCT</code> data type and the JDBC driver does not support
	 *                                         this data type
	 * @see Types
	 */
	@Override public void registerOutParameter(int parameterIndex, int sqlType, int scale) throws SQLException {

	}

	/**
	 * Retrieves whether the last OUT parameter read had the value of
	 * SQL <code>NULL</code>.  Note that this method should be called only after
	 * calling a getter method; otherwise, there is no value to use in
	 * determining whether it is <code>null</code> or not.
	 *
	 * @return <code>true</code> if the last parameter read was SQL
	 * <code>NULL</code>; <code>false</code> otherwise
	 * @throws SQLException if a database access error occurs or
	 *                      this method is called on a closed <code>CallableStatement</code>
	 */
	@Override public boolean wasNull() throws SQLException {
		return false;
	}

	/**
	 * Retrieves the value of the designated JDBC <code>CHAR</code>,
	 * <code>VARCHAR</code>, or <code>LONGVARCHAR</code> parameter as a
	 * <code>String</code> in the Java programming language.
	 * <p>
	 * For the fixed-length type JDBC <code>CHAR</code>,
	 * the <code>String</code> object
	 * returned has exactly the same value the SQL
	 * <code>CHAR</code> value had in the
	 * database, including any padding added by the database.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2,
	 *                       and so on
	 * @return the parameter value. If the value is SQL <code>NULL</code>,
	 * the result
	 * is <code>null</code>.
	 * @throws SQLException if the parameterIndex is not valid;
	 *                      if a database access error occurs or
	 *                      this method is called on a closed <code>CallableStatement</code>
	 * @see #setString
	 */
	@Override public String getString(int parameterIndex) throws SQLException {
		return null;
	}

	/**
	 * Retrieves the value of the designated JDBC <code>BIT</code>
	 * or <code>BOOLEAN</code> parameter as a
	 * <code>boolean</code> in the Java programming language.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2,
	 *                       and so on
	 * @return the parameter value.  If the value is SQL <code>NULL</code>,
	 * the result is <code>false</code>.
	 * @throws SQLException if the parameterIndex is not valid;
	 *                      if a database access error occurs or
	 *                      this method is called on a closed <code>CallableStatement</code>
	 * @see #setBoolean
	 */
	@Override public boolean getBoolean(int parameterIndex) throws SQLException {
		return false;
	}

	/**
	 * Retrieves the value of the designated JDBC <code>TINYINT</code> parameter
	 * as a <code>byte</code> in the Java programming language.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2,
	 *                       and so on
	 * @return the parameter value.  If the value is SQL <code>NULL</code>, the result
	 * is <code>0</code>.
	 * @throws SQLException if the parameterIndex is not valid;
	 *                      if a database access error occurs or
	 *                      this method is called on a closed <code>CallableStatement</code>
	 * @see #setByte
	 */
	@Override public byte getByte(int parameterIndex) throws SQLException {
		return 0;
	}

	/**
	 * Retrieves the value of the designated JDBC <code>SMALLINT</code> parameter
	 * as a <code>short</code> in the Java programming language.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2,
	 *                       and so on
	 * @return the parameter value.  If the value is SQL <code>NULL</code>, the result
	 * is <code>0</code>.
	 * @throws SQLException if the parameterIndex is not valid;
	 *                      if a database access error occurs or
	 *                      this method is called on a closed <code>CallableStatement</code>
	 * @see #setShort
	 */
	@Override public short getShort(int parameterIndex) throws SQLException {
		return 0;
	}

	/**
	 * Retrieves the value of the designated JDBC <code>INTEGER</code> parameter
	 * as an <code>int</code> in the Java programming language.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2,
	 *                       and so on
	 * @return the parameter value.  If the value is SQL <code>NULL</code>, the result
	 * is <code>0</code>.
	 * @throws SQLException if the parameterIndex is not valid;
	 *                      if a database access error occurs or
	 *                      this method is called on a closed <code>CallableStatement</code>
	 * @see #setInt
	 */
	@Override public int getInt(int parameterIndex) throws SQLException {
		return 0;
	}

	/**
	 * Retrieves the value of the designated JDBC <code>BIGINT</code> parameter
	 * as a <code>long</code> in the Java programming language.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2,
	 *                       and so on
	 * @return the parameter value.  If the value is SQL <code>NULL</code>, the result
	 * is <code>0</code>.
	 * @throws SQLException if the parameterIndex is not valid;
	 *                      if a database access error occurs or
	 *                      this method is called on a closed <code>CallableStatement</code>
	 * @see #setLong
	 */
	@Override public long getLong(int parameterIndex) throws SQLException {
		return 0;
	}

	/**
	 * Retrieves the value of the designated JDBC <code>FLOAT</code> parameter
	 * as a <code>float</code> in the Java programming language.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2,
	 *                       and so on
	 * @return the parameter value.  If the value is SQL <code>NULL</code>, the result
	 * is <code>0</code>.
	 * @throws SQLException if the parameterIndex is not valid;
	 *                      if a database access error occurs or
	 *                      this method is called on a closed <code>CallableStatement</code>
	 * @see #setFloat
	 */
	@Override public float getFloat(int parameterIndex) throws SQLException {
		return 0;
	}

	/**
	 * Retrieves the value of the designated JDBC <code>DOUBLE</code> parameter as a <code>double</code>
	 * in the Java programming language.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2,
	 *                       and so on
	 * @return the parameter value.  If the value is SQL <code>NULL</code>, the result
	 * is <code>0</code>.
	 * @throws SQLException if the parameterIndex is not valid;
	 *                      if a database access error occurs or
	 *                      this method is called on a closed <code>CallableStatement</code>
	 * @see #setDouble
	 */
	@Override public double getDouble(int parameterIndex) throws SQLException {
		return 0;
	}

	/**
	 * Retrieves the value of the designated JDBC <code>NUMERIC</code> parameter as a
	 * <code>java.math.BigDecimal</code> object with <i>scale</i> digits to
	 * the right of the decimal point.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2,
	 *                       and so on
	 * @param scale          the number of digits to the right of the decimal point
	 * @return the parameter value.  If the value is SQL <code>NULL</code>, the result
	 * is <code>null</code>.
	 * @throws SQLException                    if the parameterIndex is not valid;
	 *                                         if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see #setBigDecimal
	 * @deprecated use <code>getBigDecimal(int parameterIndex)</code>
	 * or <code>getBigDecimal(String parameterName)</code>
	 */
	@Override public BigDecimal getBigDecimal(int parameterIndex, int scale) throws SQLException {
		return null;
	}

	/**
	 * Retrieves the value of the designated JDBC <code>BINARY</code> or
	 * <code>VARBINARY</code> parameter as an array of <code>byte</code>
	 * values in the Java programming language.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2,
	 *                       and so on
	 * @return the parameter value.  If the value is SQL <code>NULL</code>, the result
	 * is <code>null</code>.
	 * @throws SQLException if the parameterIndex is not valid;
	 *                      if a database access error occurs or
	 *                      this method is called on a closed <code>CallableStatement</code>
	 * @see #setBytes
	 */
	@Override public byte[] getBytes(int parameterIndex) throws SQLException {
		return new byte[0];
	}

	/**
	 * Retrieves the value of the designated JDBC <code>DATE</code> parameter as a
	 * <code>java.sql.Date</code> object.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2,
	 *                       and so on
	 * @return the parameter value.  If the value is SQL <code>NULL</code>, the result
	 * is <code>null</code>.
	 * @throws SQLException if the parameterIndex is not valid;
	 *                      if a database access error occurs or
	 *                      this method is called on a closed <code>CallableStatement</code>
	 * @see #setDate
	 */
	@Override public Date getDate(int parameterIndex) throws SQLException {
		return null;
	}

	/**
	 * Retrieves the value of the designated JDBC <code>TIME</code> parameter as a
	 * <code>java.sql.Time</code> object.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2,
	 *                       and so on
	 * @return the parameter value.  If the value is SQL <code>NULL</code>, the result
	 * is <code>null</code>.
	 * @throws SQLException if the parameterIndex is not valid;
	 *                      if a database access error occurs or
	 *                      this method is called on a closed <code>CallableStatement</code>
	 * @see #setTime
	 */
	@Override public Time getTime(int parameterIndex) throws SQLException {
		return null;
	}

	/**
	 * Retrieves the value of the designated JDBC <code>TIMESTAMP</code> parameter as a
	 * <code>java.sql.Timestamp</code> object.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2,
	 *                       and so on
	 * @return the parameter value.  If the value is SQL <code>NULL</code>, the result
	 * is <code>null</code>.
	 * @throws SQLException if the parameterIndex is not valid;
	 *                      if a database access error occurs or
	 *                      this method is called on a closed <code>CallableStatement</code>
	 * @see #setTimestamp
	 */
	@Override public Timestamp getTimestamp(int parameterIndex) throws SQLException {
		return null;
	}

	/**
	 * Retrieves the value of the designated parameter as an <code>Object</code>
	 * in the Java programming language. If the value is an SQL <code>NULL</code>,
	 * the driver returns a Java <code>null</code>.
	 * <p>
	 * This method returns a Java object whose type corresponds to the JDBC
	 * type that was registered for this parameter using the method
	 * <code>registerOutParameter</code>.  By registering the target JDBC
	 * type as <code>java.sql.Types.OTHER</code>, this method can be used
	 * to read database-specific abstract data types.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2,
	 *                       and so on
	 * @return A <code>java.lang.Object</code> holding the OUT parameter value
	 * @throws SQLException if the parameterIndex is not valid;
	 *                      if a database access error occurs or
	 *                      this method is called on a closed <code>CallableStatement</code>
	 * @see Types
	 * @see #setObject
	 */
	@Override public Object getObject(int parameterIndex) throws SQLException {
		return null;
	}

	/**
	 * Retrieves the value of the designated JDBC <code>NUMERIC</code> parameter as a
	 * <code>java.math.BigDecimal</code> object with as many digits to the
	 * right of the decimal point as the value contains.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2,
	 *                       and so on
	 * @return the parameter value in full precision.  If the value is
	 * SQL <code>NULL</code>, the result is <code>null</code>.
	 * @throws SQLException if the parameterIndex is not valid;
	 *                      if a database access error occurs or
	 *                      this method is called on a closed <code>CallableStatement</code>
	 * @see #setBigDecimal
	 * @since 1.2
	 */
	@Override public BigDecimal getBigDecimal(int parameterIndex) throws SQLException {
		return null;
	}

	/**
	 * Returns an object representing the value of OUT parameter
	 * <code>parameterIndex</code> and uses <code>map</code> for the custom
	 * mapping of the parameter value.
	 * <p>
	 * This method returns a Java object whose type corresponds to the
	 * JDBC type that was registered for this parameter using the method
	 * <code>registerOutParameter</code>.  By registering the target
	 * JDBC type as <code>java.sql.Types.OTHER</code>, this method can
	 * be used to read database-specific abstract data types.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, and so on
	 * @param map            the mapping from SQL type names to Java classes
	 * @return a <code>java.lang.Object</code> holding the OUT parameter value
	 * @throws SQLException                    if the parameterIndex is not valid;
	 *                                         if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see #setObject
	 * @since 1.2
	 */
	@Override public Object getObject(int parameterIndex, Map<String, Class<?>> map) throws SQLException {
		return null;
	}

	/**
	 * Retrieves the value of the designated JDBC <code>REF(&lt;structured-type&gt;)</code>
	 * parameter as a {@link Ref} object in the Java programming language.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2,
	 *                       and so on
	 * @return the parameter value as a <code>Ref</code> object in the
	 * Java programming language.  If the value was SQL <code>NULL</code>, the value
	 * <code>null</code> is returned.
	 * @throws SQLException                    if the parameterIndex is not valid;
	 *                                         if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @since 1.2
	 */
	@Override public Ref getRef(int parameterIndex) throws SQLException {
		return null;
	}

	/**
	 * Retrieves the value of the designated JDBC <code>BLOB</code> parameter as a
	 * {@link Blob} object in the Java programming language.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, and so on
	 * @return the parameter value as a <code>Blob</code> object in the
	 * Java programming language.  If the value was SQL <code>NULL</code>, the value
	 * <code>null</code> is returned.
	 * @throws SQLException                    if the parameterIndex is not valid;
	 *                                         if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @since 1.2
	 */
	@Override public Blob getBlob(int parameterIndex) throws SQLException {
		return null;
	}

	/**
	 * Retrieves the value of the designated JDBC <code>CLOB</code> parameter as a
	 * <code>java.sql.Clob</code> object in the Java programming language.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, and
	 *                       so on
	 * @return the parameter value as a <code>Clob</code> object in the
	 * Java programming language.  If the value was SQL <code>NULL</code>, the
	 * value <code>null</code> is returned.
	 * @throws SQLException                    if the parameterIndex is not valid;
	 *                                         if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @since 1.2
	 */
	@Override public Clob getClob(int parameterIndex) throws SQLException {
		return null;
	}

	/**
	 * Retrieves the value of the designated JDBC <code>ARRAY</code> parameter as an
	 * {@link Array} object in the Java programming language.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, and
	 *                       so on
	 * @return the parameter value as an <code>Array</code> object in
	 * the Java programming language.  If the value was SQL <code>NULL</code>, the
	 * value <code>null</code> is returned.
	 * @throws SQLException                    if the parameterIndex is not valid;
	 *                                         if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @since 1.2
	 */
	@Override public Array getArray(int parameterIndex) throws SQLException {
		return null;
	}

	/**
	 * Retrieves the value of the designated JDBC <code>DATE</code> parameter as a
	 * <code>java.sql.Date</code> object, using
	 * the given <code>Calendar</code> object
	 * to construct the date.
	 * With a <code>Calendar</code> object, the driver
	 * can calculate the date taking into account a custom timezone and locale.
	 * If no <code>Calendar</code> object is specified, the driver uses the
	 * default timezone and locale.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2,
	 *                       and so on
	 * @param cal            the <code>Calendar</code> object the driver will use
	 *                       to construct the date
	 * @return the parameter value.  If the value is SQL <code>NULL</code>, the result
	 * is <code>null</code>.
	 * @throws SQLException if the parameterIndex is not valid;
	 *                      if a database access error occurs or
	 *                      this method is called on a closed <code>CallableStatement</code>
	 * @see #setDate
	 * @since 1.2
	 */
	@Override public Date getDate(int parameterIndex, Calendar cal) throws SQLException {
		return null;
	}

	/**
	 * Retrieves the value of the designated JDBC <code>TIME</code> parameter as a
	 * <code>java.sql.Time</code> object, using
	 * the given <code>Calendar</code> object
	 * to construct the time.
	 * With a <code>Calendar</code> object, the driver
	 * can calculate the time taking into account a custom timezone and locale.
	 * If no <code>Calendar</code> object is specified, the driver uses the
	 * default timezone and locale.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2,
	 *                       and so on
	 * @param cal            the <code>Calendar</code> object the driver will use
	 *                       to construct the time
	 * @return the parameter value; if the value is SQL <code>NULL</code>, the result
	 * is <code>null</code>.
	 * @throws SQLException if the parameterIndex is not valid;
	 *                      if a database access error occurs or
	 *                      this method is called on a closed <code>CallableStatement</code>
	 * @see #setTime
	 * @since 1.2
	 */
	@Override public Time getTime(int parameterIndex, Calendar cal) throws SQLException {
		return null;
	}

	/**
	 * Retrieves the value of the designated JDBC <code>TIMESTAMP</code> parameter as a
	 * <code>java.sql.Timestamp</code> object, using
	 * the given <code>Calendar</code> object to construct
	 * the <code>Timestamp</code> object.
	 * With a <code>Calendar</code> object, the driver
	 * can calculate the timestamp taking into account a custom timezone and locale.
	 * If no <code>Calendar</code> object is specified, the driver uses the
	 * default timezone and locale.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2,
	 *                       and so on
	 * @param cal            the <code>Calendar</code> object the driver will use
	 *                       to construct the timestamp
	 * @return the parameter value.  If the value is SQL <code>NULL</code>, the result
	 * is <code>null</code>.
	 * @throws SQLException if the parameterIndex is not valid;
	 *                      if a database access error occurs or
	 *                      this method is called on a closed <code>CallableStatement</code>
	 * @see #setTimestamp
	 * @since 1.2
	 */
	@Override public Timestamp getTimestamp(int parameterIndex, Calendar cal) throws SQLException {
		return null;
	}

	/**
	 * Registers the designated output parameter.
	 * This version of
	 * the method <code>registerOutParameter</code>
	 * should be used for a user-defined or <code>REF</code> output parameter.  Examples
	 * of user-defined types include: <code>STRUCT</code>, <code>DISTINCT</code>,
	 * <code>JAVA_OBJECT</code>, and named array types.
	 * <p>
	 * All OUT parameters must be registered
	 * before a stored procedure is executed.
	 * <p>  For a user-defined parameter, the fully-qualified SQL
	 * type name of the parameter should also be given, while a <code>REF</code>
	 * parameter requires that the fully-qualified type name of the
	 * referenced type be given.  A JDBC driver that does not need the
	 * type code and type name information may ignore it.   To be portable,
	 * however, applications should always provide these values for
	 * user-defined and <code>REF</code> parameters.
	 * <p>
	 * Although it is intended for user-defined and <code>REF</code> parameters,
	 * this method may be used to register a parameter of any JDBC type.
	 * If the parameter does not have a user-defined or <code>REF</code> type, the
	 * <i>typeName</i> parameter is ignored.
	 * <p>
	 * <P><B>Note:</B> When reading the value of an out parameter, you
	 * must use the getter method whose Java type corresponds to the
	 * parameter's registered SQL type.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2,...
	 * @param sqlType        a value from {@link Types}
	 * @param typeName       the fully-qualified name of an SQL structured type
	 * @throws SQLException                    if the parameterIndex is not valid;
	 *                                         if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if <code>sqlType</code> is
	 *                                         a <code>ARRAY</code>, <code>BLOB</code>, <code>CLOB</code>,
	 *                                         <code>DATALINK</code>, <code>JAVA_OBJECT</code>, <code>NCHAR</code>,
	 *                                         <code>NCLOB</code>, <code>NVARCHAR</code>, <code>LONGNVARCHAR</code>,
	 *                                         <code>REF</code>, <code>ROWID</code>, <code>SQLXML</code>
	 *                                         or  <code>STRUCT</code> data type and the JDBC driver does not support
	 *                                         this data type
	 * @see Types
	 * @since 1.2
	 */
	@Override public void registerOutParameter(int parameterIndex, int sqlType, String typeName) throws SQLException {

	}

	/**
	 * Registers the OUT parameter named
	 * <code>parameterName</code> to the JDBC type
	 * <code>sqlType</code>.  All OUT parameters must be registered
	 * before a stored procedure is executed.
	 * <p>
	 * The JDBC type specified by <code>sqlType</code> for an OUT
	 * parameter determines the Java type that must be used
	 * in the <code>get</code> method to read the value of that parameter.
	 * <p>
	 * If the JDBC type expected to be returned to this output parameter
	 * is specific to this particular database, <code>sqlType</code>
	 * should be <code>java.sql.Types.OTHER</code>.  The method
	 * {@link #getObject} retrieves the value.
	 *
	 * @param parameterName the name of the parameter
	 * @param sqlType       the JDBC type code defined by <code>java.sql.Types</code>.
	 *                      If the parameter is of JDBC type <code>NUMERIC</code>
	 *                      or <code>DECIMAL</code>, the version of
	 *                      <code>registerOutParameter</code> that accepts a scale value
	 *                      should be used.
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if <code>sqlType</code> is
	 *                                         a <code>ARRAY</code>, <code>BLOB</code>, <code>CLOB</code>,
	 *                                         <code>DATALINK</code>, <code>JAVA_OBJECT</code>, <code>NCHAR</code>,
	 *                                         <code>NCLOB</code>, <code>NVARCHAR</code>, <code>LONGNVARCHAR</code>,
	 *                                         <code>REF</code>, <code>ROWID</code>, <code>SQLXML</code>
	 *                                         or  <code>STRUCT</code> data type and the JDBC driver does not support
	 *                                         this data type or if the JDBC driver does not support
	 *                                         this method
	 * @see Types
	 * @since 1.4
	 */
	@Override public void registerOutParameter(String parameterName, int sqlType) throws SQLException {

	}

	/**
	 * Registers the parameter named
	 * <code>parameterName</code> to be of JDBC type
	 * <code>sqlType</code>.  All OUT parameters must be registered
	 * before a stored procedure is executed.
	 * <p>
	 * The JDBC type specified by <code>sqlType</code> for an OUT
	 * parameter determines the Java type that must be used
	 * in the <code>get</code> method to read the value of that parameter.
	 * <p>
	 * This version of <code>registerOutParameter</code> should be
	 * used when the parameter is of JDBC type <code>NUMERIC</code>
	 * or <code>DECIMAL</code>.
	 *
	 * @param parameterName the name of the parameter
	 * @param sqlType       SQL type code defined by <code>java.sql.Types</code>.
	 * @param scale         the desired number of digits to the right of the
	 *                      decimal point.  It must be greater than or equal to zero.
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if <code>sqlType</code> is
	 *                                         a <code>ARRAY</code>, <code>BLOB</code>, <code>CLOB</code>,
	 *                                         <code>DATALINK</code>, <code>JAVA_OBJECT</code>, <code>NCHAR</code>,
	 *                                         <code>NCLOB</code>, <code>NVARCHAR</code>, <code>LONGNVARCHAR</code>,
	 *                                         <code>REF</code>, <code>ROWID</code>, <code>SQLXML</code>
	 *                                         or  <code>STRUCT</code> data type and the JDBC driver does not support
	 *                                         this data type or if the JDBC driver does not support
	 *                                         this method
	 * @see Types
	 * @since 1.4
	 */
	@Override public void registerOutParameter(String parameterName, int sqlType, int scale) throws SQLException {

	}

	/**
	 * Registers the designated output parameter.  This version of
	 * the method <code>registerOutParameter</code>
	 * should be used for a user-named or REF output parameter.  Examples
	 * of user-named types include: STRUCT, DISTINCT, JAVA_OBJECT, and
	 * named array types.
	 * <p>
	 * All OUT parameters must be registered
	 * before a stored procedure is executed.
	 * <p>
	 * For a user-named parameter the fully-qualified SQL
	 * type name of the parameter should also be given, while a REF
	 * parameter requires that the fully-qualified type name of the
	 * referenced type be given.  A JDBC driver that does not need the
	 * type code and type name information may ignore it.   To be portable,
	 * however, applications should always provide these values for
	 * user-named and REF parameters.
	 * <p>
	 * Although it is intended for user-named and REF parameters,
	 * this method may be used to register a parameter of any JDBC type.
	 * If the parameter does not have a user-named or REF type, the
	 * typeName parameter is ignored.
	 * <p>
	 * <P><B>Note:</B> When reading the value of an out parameter, you
	 * must use the <code>getXXX</code> method whose Java type XXX corresponds to the
	 * parameter's registered SQL type.
	 *
	 * @param parameterName the name of the parameter
	 * @param sqlType       a value from {@link Types}
	 * @param typeName      the fully-qualified name of an SQL structured type
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if <code>sqlType</code> is
	 *                                         a <code>ARRAY</code>, <code>BLOB</code>, <code>CLOB</code>,
	 *                                         <code>DATALINK</code>, <code>JAVA_OBJECT</code>, <code>NCHAR</code>,
	 *                                         <code>NCLOB</code>, <code>NVARCHAR</code>, <code>LONGNVARCHAR</code>,
	 *                                         <code>REF</code>, <code>ROWID</code>, <code>SQLXML</code>
	 *                                         or  <code>STRUCT</code> data type and the JDBC driver does not support
	 *                                         this data type or if the JDBC driver does not support
	 *                                         this method
	 * @see Types
	 * @since 1.4
	 */
	@Override public void registerOutParameter(String parameterName, int sqlType, String typeName) throws SQLException {

	}

	/**
	 * Retrieves the value of the designated JDBC <code>DATALINK</code> parameter as a
	 * <code>java.net.URL</code> object.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2,...
	 * @return a <code>java.net.URL</code> object that represents the
	 * JDBC <code>DATALINK</code> value used as the designated
	 * parameter
	 * @throws SQLException                    if the parameterIndex is not valid;
	 *                                         if a database access error occurs,
	 *                                         this method is called on a closed <code>CallableStatement</code>,
	 *                                         or if the URL being returned is
	 *                                         not a valid URL on the Java platform
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see #setURL
	 * @since 1.4
	 */
	@Override public URL getURL(int parameterIndex) throws SQLException {
		return null;
	}

	/**
	 * Sets the designated parameter to the given <code>java.net.URL</code> object.
	 * The driver converts this to an SQL <code>DATALINK</code> value when
	 * it sends it to the database.
	 *
	 * @param parameterName the name of the parameter
	 * @param val           the parameter value
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs;
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 *                                         or if a URL is malformed
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see #getURL
	 * @since 1.4
	 */
	@Override public void setURL(String parameterName, URL val) throws SQLException {

	}

	/**
	 * Sets the designated parameter to SQL <code>NULL</code>.
	 * <p>
	 * <P><B>Note:</B> You must specify the parameter's SQL type.
	 *
	 * @param parameterName the name of the parameter
	 * @param sqlType       the SQL type code defined in <code>java.sql.Types</code>
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @since 1.4
	 */
	@Override public void setNull(String parameterName, int sqlType) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given Java <code>boolean</code> value.
	 * The driver converts this
	 * to an SQL <code>BIT</code> or <code>BOOLEAN</code> value when it sends it to the database.
	 *
	 * @param parameterName the name of the parameter
	 * @param x             the parameter value
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see #getBoolean
	 * @since 1.4
	 */
	@Override public void setBoolean(String parameterName, boolean x) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given Java <code>byte</code> value.
	 * The driver converts this
	 * to an SQL <code>TINYINT</code> value when it sends it to the database.
	 *
	 * @param parameterName the name of the parameter
	 * @param x             the parameter value
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see #getByte
	 * @since 1.4
	 */
	@Override public void setByte(String parameterName, byte x) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given Java <code>short</code> value.
	 * The driver converts this
	 * to an SQL <code>SMALLINT</code> value when it sends it to the database.
	 *
	 * @param parameterName the name of the parameter
	 * @param x             the parameter value
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see #getShort
	 * @since 1.4
	 */
	@Override public void setShort(String parameterName, short x) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given Java <code>int</code> value.
	 * The driver converts this
	 * to an SQL <code>INTEGER</code> value when it sends it to the database.
	 *
	 * @param parameterName the name of the parameter
	 * @param x             the parameter value
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see #getInt
	 * @since 1.4
	 */
	@Override public void setInt(String parameterName, int x) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given Java <code>long</code> value.
	 * The driver converts this
	 * to an SQL <code>BIGINT</code> value when it sends it to the database.
	 *
	 * @param parameterName the name of the parameter
	 * @param x             the parameter value
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see #getLong
	 * @since 1.4
	 */
	@Override public void setLong(String parameterName, long x) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given Java <code>float</code> value.
	 * The driver converts this
	 * to an SQL <code>FLOAT</code> value when it sends it to the database.
	 *
	 * @param parameterName the name of the parameter
	 * @param x             the parameter value
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see #getFloat
	 * @since 1.4
	 */
	@Override public void setFloat(String parameterName, float x) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given Java <code>double</code> value.
	 * The driver converts this
	 * to an SQL <code>DOUBLE</code> value when it sends it to the database.
	 *
	 * @param parameterName the name of the parameter
	 * @param x             the parameter value
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see #getDouble
	 * @since 1.4
	 */
	@Override public void setDouble(String parameterName, double x) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given
	 * <code>java.math.BigDecimal</code> value.
	 * The driver converts this to an SQL <code>NUMERIC</code> value when
	 * it sends it to the database.
	 *
	 * @param parameterName the name of the parameter
	 * @param x             the parameter value
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see #getBigDecimal
	 * @since 1.4
	 */
	@Override public void setBigDecimal(String parameterName, BigDecimal x) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given Java <code>String</code> value.
	 * The driver converts this
	 * to an SQL <code>VARCHAR</code> or <code>LONGVARCHAR</code> value
	 * (depending on the argument's
	 * size relative to the driver's limits on <code>VARCHAR</code> values)
	 * when it sends it to the database.
	 *
	 * @param parameterName the name of the parameter
	 * @param x             the parameter value
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see #getString
	 * @since 1.4
	 */
	@Override public void setString(String parameterName, String x) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given Java array of bytes.
	 * The driver converts this to an SQL <code>VARBINARY</code> or
	 * <code>LONGVARBINARY</code> (depending on the argument's size relative
	 * to the driver's limits on <code>VARBINARY</code> values) when it sends
	 * it to the database.
	 *
	 * @param parameterName the name of the parameter
	 * @param x             the parameter value
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see #getBytes
	 * @since 1.4
	 */
	@Override public void setBytes(String parameterName, byte[] x) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given <code>java.sql.Date</code> value
	 * using the default time zone of the virtual machine that is running
	 * the application.
	 * The driver converts this
	 * to an SQL <code>DATE</code> value when it sends it to the database.
	 *
	 * @param parameterName the name of the parameter
	 * @param x             the parameter value
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see #getDate
	 * @since 1.4
	 */
	@Override public void setDate(String parameterName, Date x) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given <code>java.sql.Time</code> value.
	 * The driver converts this
	 * to an SQL <code>TIME</code> value when it sends it to the database.
	 *
	 * @param parameterName the name of the parameter
	 * @param x             the parameter value
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see #getTime
	 * @since 1.4
	 */
	@Override public void setTime(String parameterName, Time x) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given <code>java.sql.Timestamp</code> value.
	 * The driver
	 * converts this to an SQL <code>TIMESTAMP</code> value when it sends it to the
	 * database.
	 *
	 * @param parameterName the name of the parameter
	 * @param x             the parameter value
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see #getTimestamp
	 * @since 1.4
	 */
	@Override public void setTimestamp(String parameterName, Timestamp x) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given input stream, which will have
	 * the specified number of bytes.
	 * When a very large ASCII value is input to a <code>LONGVARCHAR</code>
	 * parameter, it may be more practical to send it via a
	 * <code>java.io.InputStream</code>. Data will be read from the stream
	 * as needed until end-of-file is reached.  The JDBC driver will
	 * do any necessary conversion from ASCII to the database char format.
	 * <p>
	 * <P><B>Note:</B> This stream object can either be a standard
	 * Java stream object or your own subclass that implements the
	 * standard interface.
	 *
	 * @param parameterName the name of the parameter
	 * @param x             the Java input stream that contains the ASCII parameter value
	 * @param length        the number of bytes in the stream
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @since 1.4
	 */
	@Override public void setAsciiStream(String parameterName, InputStream x, int length) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given input stream, which will have
	 * the specified number of bytes.
	 * When a very large binary value is input to a <code>LONGVARBINARY</code>
	 * parameter, it may be more practical to send it via a
	 * <code>java.io.InputStream</code> object. The data will be read from the stream
	 * as needed until end-of-file is reached.
	 * <p>
	 * <P><B>Note:</B> This stream object can either be a standard
	 * Java stream object or your own subclass that implements the
	 * standard interface.
	 *
	 * @param parameterName the name of the parameter
	 * @param x             the java input stream which contains the binary parameter value
	 * @param length        the number of bytes in the stream
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @since 1.4
	 */
	@Override public void setBinaryStream(String parameterName, InputStream x, int length) throws SQLException {

	}

	/**
	 * Sets the value of the designated parameter with the given object.
	 * <p>
	 * <p>The given Java object will be converted to the given targetSqlType
	 * before being sent to the database.
	 * <p>
	 * If the object has a custom mapping (is of a class implementing the
	 * interface <code>SQLData</code>),
	 * the JDBC driver should call the method <code>SQLData.writeSQL</code> to write it
	 * to the SQL data stream.
	 * If, on the other hand, the object is of a class implementing
	 * <code>Ref</code>, <code>Blob</code>, <code>Clob</code>,  <code>NClob</code>,
	 * <code>Struct</code>, <code>java.net.URL</code>,
	 * or <code>Array</code>, the driver should pass it to the database as a
	 * value of the corresponding SQL type.
	 * <p>
	 * Note that this method may be used to pass datatabase-
	 * specific abstract data types.
	 *
	 * @param parameterName the name of the parameter
	 * @param x             the object containing the input parameter value
	 * @param targetSqlType the SQL type (as defined in java.sql.Types) to be
	 *                      sent to the database. The scale argument may further qualify this type.
	 * @param scale         for java.sql.Types.DECIMAL or java.sql.Types.NUMERIC types,
	 *                      this is the number of digits after the decimal point.  For all other
	 *                      types, this value will be ignored.
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if
	 *                                         the JDBC driver does not support the specified targetSqlType
	 * @see Types
	 * @see #getObject
	 * @since 1.4
	 */
	@Override public void setObject(String parameterName, Object x, int targetSqlType, int scale) throws SQLException {

	}

	/**
	 * Sets the value of the designated parameter with the given object.
	 * <p>
	 * This method is similar to {@link #setObject(String parameterName,
	 * Object x, int targetSqlType, int scaleOrLength)},
	 * except that it assumes a scale of zero.
	 *
	 * @param parameterName the name of the parameter
	 * @param x             the object containing the input parameter value
	 * @param targetSqlType the SQL type (as defined in java.sql.Types) to be
	 *                      sent to the database
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if
	 *                                         the JDBC driver does not support the specified targetSqlType
	 * @see #getObject
	 * @since 1.4
	 */
	@Override public void setObject(String parameterName, Object x, int targetSqlType) throws SQLException {

	}

	/**
	 * Sets the value of the designated parameter with the given object.
	 * <p>
	 * <p>The JDBC specification specifies a standard mapping from
	 * Java <code>Object</code> types to SQL types.  The given argument
	 * will be converted to the corresponding SQL type before being
	 * sent to the database.
	 * <p>Note that this method may be used to pass datatabase-
	 * specific abstract data types, by using a driver-specific Java
	 * type.
	 * <p>
	 * If the object is of a class implementing the interface <code>SQLData</code>,
	 * the JDBC driver should call the method <code>SQLData.writeSQL</code>
	 * to write it to the SQL data stream.
	 * If, on the other hand, the object is of a class implementing
	 * <code>Ref</code>, <code>Blob</code>, <code>Clob</code>,  <code>NClob</code>,
	 * <code>Struct</code>, <code>java.net.URL</code>,
	 * or <code>Array</code>, the driver should pass it to the database as a
	 * value of the corresponding SQL type.
	 * <p>
	 * This method throws an exception if there is an ambiguity, for example, if the
	 * object is of a class implementing more than one of the interfaces named above.
	 * <p>
	 * <b>Note:</b> Not all databases allow for a non-typed Null to be sent to
	 * the backend. For maximum portability, the <code>setNull</code> or the
	 * <code>setObject(String parameterName, Object x, int sqlType)</code>
	 * method should be used
	 * instead of <code>setObject(String parameterName, Object x)</code>.
	 * <p>
	 *
	 * @param parameterName the name of the parameter
	 * @param x             the object containing the input parameter value
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs,
	 *                                         this method is called on a closed <code>CallableStatement</code> or if the given
	 *                                         <code>Object</code> parameter is ambiguous
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see #getObject
	 * @since 1.4
	 */
	@Override public void setObject(String parameterName, Object x) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given <code>Reader</code>
	 * object, which is the given number of characters long.
	 * When a very large UNICODE value is input to a <code>LONGVARCHAR</code>
	 * parameter, it may be more practical to send it via a
	 * <code>java.io.Reader</code> object. The data will be read from the stream
	 * as needed until end-of-file is reached.  The JDBC driver will
	 * do any necessary conversion from UNICODE to the database char format.
	 * <p>
	 * <P><B>Note:</B> This stream object can either be a standard
	 * Java stream object or your own subclass that implements the
	 * standard interface.
	 *
	 * @param parameterName the name of the parameter
	 * @param reader        the <code>java.io.Reader</code> object that
	 *                      contains the UNICODE data used as the designated parameter
	 * @param length        the number of characters in the stream
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @since 1.4
	 */
	@Override public void setCharacterStream(String parameterName, Reader reader, int length) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given <code>java.sql.Date</code> value,
	 * using the given <code>Calendar</code> object.  The driver uses
	 * the <code>Calendar</code> object to construct an SQL <code>DATE</code> value,
	 * which the driver then sends to the database.  With a
	 * a <code>Calendar</code> object, the driver can calculate the date
	 * taking into account a custom timezone.  If no
	 * <code>Calendar</code> object is specified, the driver uses the default
	 * timezone, which is that of the virtual machine running the application.
	 *
	 * @param parameterName the name of the parameter
	 * @param x             the parameter value
	 * @param cal           the <code>Calendar</code> object the driver will use
	 *                      to construct the date
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see #getDate
	 * @since 1.4
	 */
	@Override public void setDate(String parameterName, Date x, Calendar cal) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given <code>java.sql.Time</code> value,
	 * using the given <code>Calendar</code> object.  The driver uses
	 * the <code>Calendar</code> object to construct an SQL <code>TIME</code> value,
	 * which the driver then sends to the database.  With a
	 * a <code>Calendar</code> object, the driver can calculate the time
	 * taking into account a custom timezone.  If no
	 * <code>Calendar</code> object is specified, the driver uses the default
	 * timezone, which is that of the virtual machine running the application.
	 *
	 * @param parameterName the name of the parameter
	 * @param x             the parameter value
	 * @param cal           the <code>Calendar</code> object the driver will use
	 *                      to construct the time
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see #getTime
	 * @since 1.4
	 */
	@Override public void setTime(String parameterName, Time x, Calendar cal) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given <code>java.sql.Timestamp</code> value,
	 * using the given <code>Calendar</code> object.  The driver uses
	 * the <code>Calendar</code> object to construct an SQL <code>TIMESTAMP</code> value,
	 * which the driver then sends to the database.  With a
	 * a <code>Calendar</code> object, the driver can calculate the timestamp
	 * taking into account a custom timezone.  If no
	 * <code>Calendar</code> object is specified, the driver uses the default
	 * timezone, which is that of the virtual machine running the application.
	 *
	 * @param parameterName the name of the parameter
	 * @param x             the parameter value
	 * @param cal           the <code>Calendar</code> object the driver will use
	 *                      to construct the timestamp
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see #getTimestamp
	 * @since 1.4
	 */
	@Override public void setTimestamp(String parameterName, Timestamp x, Calendar cal) throws SQLException {

	}

	/**
	 * Sets the designated parameter to SQL <code>NULL</code>.
	 * This version of the method <code>setNull</code> should
	 * be used for user-defined types and REF type parameters.  Examples
	 * of user-defined types include: STRUCT, DISTINCT, JAVA_OBJECT, and
	 * named array types.
	 * <p>
	 * <P><B>Note:</B> To be portable, applications must give the
	 * SQL type code and the fully-qualified SQL type name when specifying
	 * a NULL user-defined or REF parameter.  In the case of a user-defined type
	 * the name is the type name of the parameter itself.  For a REF
	 * parameter, the name is the type name of the referenced type.
	 * <p>
	 * Although it is intended for user-defined and Ref parameters,
	 * this method may be used to set a null parameter of any JDBC type.
	 * If the parameter does not have a user-defined or REF type, the given
	 * typeName is ignored.
	 *
	 * @param parameterName the name of the parameter
	 * @param sqlType       a value from <code>java.sql.Types</code>
	 * @param typeName      the fully-qualified name of an SQL user-defined type;
	 *                      ignored if the parameter is not a user-defined type or
	 *                      SQL <code>REF</code> value
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @since 1.4
	 */
	@Override public void setNull(String parameterName, int sqlType, String typeName) throws SQLException {

	}

	/**
	 * Retrieves the value of a JDBC <code>CHAR</code>, <code>VARCHAR</code>,
	 * or <code>LONGVARCHAR</code> parameter as a <code>String</code> in
	 * the Java programming language.
	 * <p>
	 * For the fixed-length type JDBC <code>CHAR</code>,
	 * the <code>String</code> object
	 * returned has exactly the same value the SQL
	 * <code>CHAR</code> value had in the
	 * database, including any padding added by the database.
	 *
	 * @param parameterName the name of the parameter
	 * @return the parameter value. If the value is SQL <code>NULL</code>, the result
	 * is <code>null</code>.
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see #setString
	 * @since 1.4
	 */
	@Override public String getString(String parameterName) throws SQLException {
		return null;
	}

	/**
	 * Retrieves the value of a JDBC <code>BIT</code> or <code>BOOLEAN</code>
	 * parameter as a
	 * <code>boolean</code> in the Java programming language.
	 *
	 * @param parameterName the name of the parameter
	 * @return the parameter value.  If the value is SQL <code>NULL</code>, the result
	 * is <code>false</code>.
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see #setBoolean
	 * @since 1.4
	 */
	@Override public boolean getBoolean(String parameterName) throws SQLException {
		return false;
	}

	/**
	 * Retrieves the value of a JDBC <code>TINYINT</code> parameter as a <code>byte</code>
	 * in the Java programming language.
	 *
	 * @param parameterName the name of the parameter
	 * @return the parameter value.  If the value is SQL <code>NULL</code>, the result
	 * is <code>0</code>.
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see #setByte
	 * @since 1.4
	 */
	@Override public byte getByte(String parameterName) throws SQLException {
		return 0;
	}

	/**
	 * Retrieves the value of a JDBC <code>SMALLINT</code> parameter as a <code>short</code>
	 * in the Java programming language.
	 *
	 * @param parameterName the name of the parameter
	 * @return the parameter value.  If the value is SQL <code>NULL</code>, the result
	 * is <code>0</code>.
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see #setShort
	 * @since 1.4
	 */
	@Override public short getShort(String parameterName) throws SQLException {
		return 0;
	}

	/**
	 * Retrieves the value of a JDBC <code>INTEGER</code> parameter as an <code>int</code>
	 * in the Java programming language.
	 *
	 * @param parameterName the name of the parameter
	 * @return the parameter value.  If the value is SQL <code>NULL</code>,
	 * the result is <code>0</code>.
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see #setInt
	 * @since 1.4
	 */
	@Override public int getInt(String parameterName) throws SQLException {
		return 0;
	}

	/**
	 * Retrieves the value of a JDBC <code>BIGINT</code> parameter as a <code>long</code>
	 * in the Java programming language.
	 *
	 * @param parameterName the name of the parameter
	 * @return the parameter value.  If the value is SQL <code>NULL</code>,
	 * the result is <code>0</code>.
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see #setLong
	 * @since 1.4
	 */
	@Override public long getLong(String parameterName) throws SQLException {
		return 0;
	}

	/**
	 * Retrieves the value of a JDBC <code>FLOAT</code> parameter as a <code>float</code>
	 * in the Java programming language.
	 *
	 * @param parameterName the name of the parameter
	 * @return the parameter value.  If the value is SQL <code>NULL</code>,
	 * the result is <code>0</code>.
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see #setFloat
	 * @since 1.4
	 */
	@Override public float getFloat(String parameterName) throws SQLException {
		return 0;
	}

	/**
	 * Retrieves the value of a JDBC <code>DOUBLE</code> parameter as a <code>double</code>
	 * in the Java programming language.
	 *
	 * @param parameterName the name of the parameter
	 * @return the parameter value.  If the value is SQL <code>NULL</code>,
	 * the result is <code>0</code>.
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see #setDouble
	 * @since 1.4
	 */
	@Override public double getDouble(String parameterName) throws SQLException {
		return 0;
	}

	/**
	 * Retrieves the value of a JDBC <code>BINARY</code> or <code>VARBINARY</code>
	 * parameter as an array of <code>byte</code> values in the Java
	 * programming language.
	 *
	 * @param parameterName the name of the parameter
	 * @return the parameter value.  If the value is SQL <code>NULL</code>, the result is
	 * <code>null</code>.
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see #setBytes
	 * @since 1.4
	 */
	@Override public byte[] getBytes(String parameterName) throws SQLException {
		return new byte[0];
	}

	/**
	 * Retrieves the value of a JDBC <code>DATE</code> parameter as a
	 * <code>java.sql.Date</code> object.
	 *
	 * @param parameterName the name of the parameter
	 * @return the parameter value.  If the value is SQL <code>NULL</code>, the result
	 * is <code>null</code>.
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see #setDate
	 * @since 1.4
	 */
	@Override public Date getDate(String parameterName) throws SQLException {
		return null;
	}

	/**
	 * Retrieves the value of a JDBC <code>TIME</code> parameter as a
	 * <code>java.sql.Time</code> object.
	 *
	 * @param parameterName the name of the parameter
	 * @return the parameter value.  If the value is SQL <code>NULL</code>, the result
	 * is <code>null</code>.
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see #setTime
	 * @since 1.4
	 */
	@Override public Time getTime(String parameterName) throws SQLException {
		return null;
	}

	/**
	 * Retrieves the value of a JDBC <code>TIMESTAMP</code> parameter as a
	 * <code>java.sql.Timestamp</code> object.
	 *
	 * @param parameterName the name of the parameter
	 * @return the parameter value.  If the value is SQL <code>NULL</code>, the result
	 * is <code>null</code>.
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see #setTimestamp
	 * @since 1.4
	 */
	@Override public Timestamp getTimestamp(String parameterName) throws SQLException {
		return null;
	}

	/**
	 * Retrieves the value of a parameter as an <code>Object</code> in the Java
	 * programming language. If the value is an SQL <code>NULL</code>, the
	 * driver returns a Java <code>null</code>.
	 * <p>
	 * This method returns a Java object whose type corresponds to the JDBC
	 * type that was registered for this parameter using the method
	 * <code>registerOutParameter</code>.  By registering the target JDBC
	 * type as <code>java.sql.Types.OTHER</code>, this method can be used
	 * to read database-specific abstract data types.
	 *
	 * @param parameterName the name of the parameter
	 * @return A <code>java.lang.Object</code> holding the OUT parameter value.
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see Types
	 * @see #setObject
	 * @since 1.4
	 */
	@Override public Object getObject(String parameterName) throws SQLException {
		return null;
	}

	/**
	 * Retrieves the value of a JDBC <code>NUMERIC</code> parameter as a
	 * <code>java.math.BigDecimal</code> object with as many digits to the
	 * right of the decimal point as the value contains.
	 *
	 * @param parameterName the name of the parameter
	 * @return the parameter value in full precision.  If the value is
	 * SQL <code>NULL</code>, the result is <code>null</code>.
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter;  if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see #setBigDecimal
	 * @since 1.4
	 */
	@Override public BigDecimal getBigDecimal(String parameterName) throws SQLException {
		return null;
	}

	/**
	 * Returns an object representing the value of OUT parameter
	 * <code>parameterName</code> and uses <code>map</code> for the custom
	 * mapping of the parameter value.
	 * <p>
	 * This method returns a Java object whose type corresponds to the
	 * JDBC type that was registered for this parameter using the method
	 * <code>registerOutParameter</code>.  By registering the target
	 * JDBC type as <code>java.sql.Types.OTHER</code>, this method can
	 * be used to read database-specific abstract data types.
	 *
	 * @param parameterName the name of the parameter
	 * @param map           the mapping from SQL type names to Java classes
	 * @return a <code>java.lang.Object</code> holding the OUT parameter value
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see #setObject
	 * @since 1.4
	 */
	@Override public Object getObject(String parameterName, Map<String, Class<?>> map) throws SQLException {
		return null;
	}

	/**
	 * Retrieves the value of a JDBC <code>REF(&lt;structured-type&gt;)</code>
	 * parameter as a {@link Ref} object in the Java programming language.
	 *
	 * @param parameterName the name of the parameter
	 * @return the parameter value as a <code>Ref</code> object in the
	 * Java programming language.  If the value was SQL <code>NULL</code>,
	 * the value <code>null</code> is returned.
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @since 1.4
	 */
	@Override public Ref getRef(String parameterName) throws SQLException {
		return null;
	}

	/**
	 * Retrieves the value of a JDBC <code>BLOB</code> parameter as a
	 * {@link Blob} object in the Java programming language.
	 *
	 * @param parameterName the name of the parameter
	 * @return the parameter value as a <code>Blob</code> object in the
	 * Java programming language.  If the value was SQL <code>NULL</code>,
	 * the value <code>null</code> is returned.
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @since 1.4
	 */
	@Override public Blob getBlob(String parameterName) throws SQLException {
		return null;
	}

	/**
	 * Retrieves the value of a JDBC <code>CLOB</code> parameter as a
	 * <code>java.sql.Clob</code> object in the Java programming language.
	 *
	 * @param parameterName the name of the parameter
	 * @return the parameter value as a <code>Clob</code> object in the
	 * Java programming language.  If the value was SQL <code>NULL</code>,
	 * the value <code>null</code> is returned.
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @since 1.4
	 */
	@Override public Clob getClob(String parameterName) throws SQLException {
		return null;
	}

	/**
	 * Retrieves the value of a JDBC <code>ARRAY</code> parameter as an
	 * {@link Array} object in the Java programming language.
	 *
	 * @param parameterName the name of the parameter
	 * @return the parameter value as an <code>Array</code> object in
	 * Java programming language.  If the value was SQL <code>NULL</code>,
	 * the value <code>null</code> is returned.
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @since 1.4
	 */
	@Override public Array getArray(String parameterName) throws SQLException {
		return null;
	}

	/**
	 * Retrieves the value of a JDBC <code>DATE</code> parameter as a
	 * <code>java.sql.Date</code> object, using
	 * the given <code>Calendar</code> object
	 * to construct the date.
	 * With a <code>Calendar</code> object, the driver
	 * can calculate the date taking into account a custom timezone and locale.
	 * If no <code>Calendar</code> object is specified, the driver uses the
	 * default timezone and locale.
	 *
	 * @param parameterName the name of the parameter
	 * @param cal           the <code>Calendar</code> object the driver will use
	 *                      to construct the date
	 * @return the parameter value.  If the value is SQL <code>NULL</code>,
	 * the result is <code>null</code>.
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see #setDate
	 * @since 1.4
	 */
	@Override public Date getDate(String parameterName, Calendar cal) throws SQLException {
		return null;
	}

	/**
	 * Retrieves the value of a JDBC <code>TIME</code> parameter as a
	 * <code>java.sql.Time</code> object, using
	 * the given <code>Calendar</code> object
	 * to construct the time.
	 * With a <code>Calendar</code> object, the driver
	 * can calculate the time taking into account a custom timezone and locale.
	 * If no <code>Calendar</code> object is specified, the driver uses the
	 * default timezone and locale.
	 *
	 * @param parameterName the name of the parameter
	 * @param cal           the <code>Calendar</code> object the driver will use
	 *                      to construct the time
	 * @return the parameter value; if the value is SQL <code>NULL</code>, the result is
	 * <code>null</code>.
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see #setTime
	 * @since 1.4
	 */
	@Override public Time getTime(String parameterName, Calendar cal) throws SQLException {
		return null;
	}

	/**
	 * Retrieves the value of a JDBC <code>TIMESTAMP</code> parameter as a
	 * <code>java.sql.Timestamp</code> object, using
	 * the given <code>Calendar</code> object to construct
	 * the <code>Timestamp</code> object.
	 * With a <code>Calendar</code> object, the driver
	 * can calculate the timestamp taking into account a custom timezone and locale.
	 * If no <code>Calendar</code> object is specified, the driver uses the
	 * default timezone and locale.
	 *
	 * @param parameterName the name of the parameter
	 * @param cal           the <code>Calendar</code> object the driver will use
	 *                      to construct the timestamp
	 * @return the parameter value.  If the value is SQL <code>NULL</code>, the result is
	 * <code>null</code>.
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see #setTimestamp
	 * @since 1.4
	 */
	@Override public Timestamp getTimestamp(String parameterName, Calendar cal) throws SQLException {
		return null;
	}

	/**
	 * Retrieves the value of a JDBC <code>DATALINK</code> parameter as a
	 * <code>java.net.URL</code> object.
	 *
	 * @param parameterName the name of the parameter
	 * @return the parameter value as a <code>java.net.URL</code> object in the
	 * Java programming language.  If the value was SQL <code>NULL</code>, the
	 * value <code>null</code> is returned.
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs,
	 *                                         this method is called on a closed <code>CallableStatement</code>,
	 *                                         or if there is a problem with the URL
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see #setURL
	 * @since 1.4
	 */
	@Override public URL getURL(String parameterName) throws SQLException {
		return null;
	}

	/**
	 * Retrieves the value of the designated JDBC <code>ROWID</code> parameter as a
	 * <code>java.sql.RowId</code> object.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2,...
	 * @return a <code>RowId</code> object that represents the JDBC <code>ROWID</code>
	 * value is used as the designated parameter. If the parameter contains
	 * a SQL <code>NULL</code>, then a <code>null</code> value is returned.
	 * @throws SQLException                    if the parameterIndex is not valid;
	 *                                         if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @since 1.6
	 */
	@Override public RowId getRowId(int parameterIndex) throws SQLException {
		return null;
	}

	/**
	 * Retrieves the value of the designated JDBC <code>ROWID</code> parameter as a
	 * <code>java.sql.RowId</code> object.
	 *
	 * @param parameterName the name of the parameter
	 * @return a <code>RowId</code> object that represents the JDBC <code>ROWID</code>
	 * value is used as the designated parameter. If the parameter contains
	 * a SQL <code>NULL</code>, then a <code>null</code> value is returned.
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @since 1.6
	 */
	@Override public RowId getRowId(String parameterName) throws SQLException {
		return null;
	}

	/**
	 * Sets the designated parameter to the given <code>java.sql.RowId</code> object. The
	 * driver converts this to a SQL <code>ROWID</code> when it sends it to the
	 * database.
	 *
	 * @param parameterName the name of the parameter
	 * @param x             the parameter value
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @since 1.6
	 */
	@Override public void setRowId(String parameterName, RowId x) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given <code>String</code> object.
	 * The driver converts this to a SQL <code>NCHAR</code> or
	 * <code>NVARCHAR</code> or <code>LONGNVARCHAR</code>
	 *
	 * @param parameterName the name of the parameter to be set
	 * @param value         the parameter value
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if the driver does not support national
	 *                                         character sets;  if the driver can detect that a data conversion
	 *                                         error could occur; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @since 1.6
	 */
	@Override public void setNString(String parameterName, String value) throws SQLException {

	}

	/**
	 * Sets the designated parameter to a <code>Reader</code> object. The
	 * <code>Reader</code> reads the data till end-of-file is reached. The
	 * driver does the necessary conversion from Java character format to
	 * the national character set in the database.
	 *
	 * @param parameterName the name of the parameter to be set
	 * @param value         the parameter value
	 * @param length        the number of characters in the parameter data.
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if the driver does not support national
	 *                                         character sets;  if the driver can detect that a data conversion
	 *                                         error could occur; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @since 1.6
	 */
	@Override public void setNCharacterStream(String parameterName, Reader value, long length) throws SQLException {

	}

	/**
	 * Sets the designated parameter to a <code>java.sql.NClob</code> object. The object
	 * implements the <code>java.sql.NClob</code> interface. This <code>NClob</code>
	 * object maps to a SQL <code>NCLOB</code>.
	 *
	 * @param parameterName the name of the parameter to be set
	 * @param value         the parameter value
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if the driver does not support national
	 *                                         character sets;  if the driver can detect that a data conversion
	 *                                         error could occur; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @since 1.6
	 */
	@Override public void setNClob(String parameterName, NClob value) throws SQLException {

	}

	/**
	 * Sets the designated parameter to a <code>Reader</code> object.  The <code>reader</code> must contain  the number
	 * of characters specified by length otherwise a <code>SQLException</code> will be
	 * generated when the <code>CallableStatement</code> is executed.
	 * This method differs from the <code>setCharacterStream (int, Reader, int)</code> method
	 * because it informs the driver that the parameter value should be sent to
	 * the server as a <code>CLOB</code>.  When the <code>setCharacterStream</code> method is used, the
	 * driver may have to do extra work to determine whether the parameter
	 * data should be send to the server as a <code>LONGVARCHAR</code> or a <code>CLOB</code>
	 *
	 * @param parameterName the name of the parameter to be set
	 * @param reader        An object that contains the data to set the parameter value to.
	 * @param length        the number of characters in the parameter data.
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if the length specified is less than zero;
	 *                                         a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @since 1.6
	 */
	@Override public void setClob(String parameterName, Reader reader, long length) throws SQLException {

	}

	/**
	 * Sets the designated parameter to a <code>InputStream</code> object.  The <code>inputstream</code> must contain  the number
	 * of characters specified by length, otherwise a <code>SQLException</code> will be
	 * generated when the <code>CallableStatement</code> is executed.
	 * This method differs from the <code>setBinaryStream (int, InputStream, int)</code>
	 * method because it informs the driver that the parameter value should be
	 * sent to the server as a <code>BLOB</code>.  When the <code>setBinaryStream</code> method is used,
	 * the driver may have to do extra work to determine whether the parameter
	 * data should be sent to the server as a <code>LONGVARBINARY</code> or a <code>BLOB</code>
	 *
	 * @param parameterName the name of the parameter to be set
	 *                      the second is 2, ...
	 * @param inputStream   An object that contains the data to set the parameter
	 *                      value to.
	 * @param length        the number of bytes in the parameter data.
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if the length specified
	 *                                         is less than zero; if the number of bytes in the inputstream does not match
	 *                                         the specified length; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @since 1.6
	 */
	@Override public void setBlob(String parameterName, InputStream inputStream, long length) throws SQLException {

	}

	/**
	 * Sets the designated parameter to a <code>Reader</code> object.  The <code>reader</code> must contain  the number
	 * of characters specified by length otherwise a <code>SQLException</code> will be
	 * generated when the <code>CallableStatement</code> is executed.
	 * This method differs from the <code>setCharacterStream (int, Reader, int)</code> method
	 * because it informs the driver that the parameter value should be sent to
	 * the server as a <code>NCLOB</code>.  When the <code>setCharacterStream</code> method is used, the
	 * driver may have to do extra work to determine whether the parameter
	 * data should be send to the server as a <code>LONGNVARCHAR</code> or a <code>NCLOB</code>
	 *
	 * @param parameterName the name of the parameter to be set
	 * @param reader        An object that contains the data to set the parameter value to.
	 * @param length        the number of characters in the parameter data.
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if the length specified is less than zero;
	 *                                         if the driver does not support national
	 *                                         character sets;  if the driver can detect that a data conversion
	 *                                         error could occur; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @since 1.6
	 */
	@Override public void setNClob(String parameterName, Reader reader, long length) throws SQLException {

	}

	/**
	 * Retrieves the value of the designated JDBC <code>NCLOB</code> parameter as a
	 * <code>java.sql.NClob</code> object in the Java programming language.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, and
	 *                       so on
	 * @return the parameter value as a <code>NClob</code> object in the
	 * Java programming language.  If the value was SQL <code>NULL</code>, the
	 * value <code>null</code> is returned.
	 * @throws SQLException                    if the parameterIndex is not valid;
	 *                                         if the driver does not support national
	 *                                         character sets;  if the driver can detect that a data conversion
	 *                                         error could occur; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @since 1.6
	 */
	@Override public NClob getNClob(int parameterIndex) throws SQLException {
		return null;
	}

	/**
	 * Retrieves the value of a JDBC <code>NCLOB</code> parameter as a
	 * <code>java.sql.NClob</code> object in the Java programming language.
	 *
	 * @param parameterName the name of the parameter
	 * @return the parameter value as a <code>NClob</code> object in the
	 * Java programming language.  If the value was SQL <code>NULL</code>,
	 * the value <code>null</code> is returned.
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if the driver does not support national
	 *                                         character sets;  if the driver can detect that a data conversion
	 *                                         error could occur; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @since 1.6
	 */
	@Override public NClob getNClob(String parameterName) throws SQLException {
		return null;
	}

	/**
	 * Sets the designated parameter to the given <code>java.sql.SQLXML</code> object. The driver converts this to an
	 * <code>SQL XML</code> value when it sends it to the database.
	 *
	 * @param parameterName the name of the parameter
	 * @param xmlObject     a <code>SQLXML</code> object that maps an <code>SQL XML</code> value
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs;
	 *                                         this method is called on a closed <code>CallableStatement</code> or
	 *                                         the <code>java.xml.transform.Result</code>,
	 *                                         <code>Writer</code> or <code>OutputStream</code> has not been closed for the <code>SQLXML</code> object
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @since 1.6
	 */
	@Override public void setSQLXML(String parameterName, SQLXML xmlObject) throws SQLException {

	}

	/**
	 * Retrieves the value of the designated <code>SQL XML</code> parameter as a
	 * <code>java.sql.SQLXML</code> object in the Java programming language.
	 *
	 * @param parameterIndex index of the first parameter is 1, the second is 2, ...
	 * @return a <code>SQLXML</code> object that maps an <code>SQL XML</code> value
	 * @throws SQLException                    if the parameterIndex is not valid;
	 *                                         if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @since 1.6
	 */
	@Override public SQLXML getSQLXML(int parameterIndex) throws SQLException {
		return null;
	}

	/**
	 * Retrieves the value of the designated <code>SQL XML</code> parameter as a
	 * <code>java.sql.SQLXML</code> object in the Java programming language.
	 *
	 * @param parameterName the name of the parameter
	 * @return a <code>SQLXML</code> object that maps an <code>SQL XML</code> value
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @since 1.6
	 */
	@Override public SQLXML getSQLXML(String parameterName) throws SQLException {
		return null;
	}

	/**
	 * Retrieves the value of the designated <code>NCHAR</code>,
	 * <code>NVARCHAR</code>
	 * or <code>LONGNVARCHAR</code> parameter as
	 * a <code>String</code> in the Java programming language.
	 * <p>
	 * For the fixed-length type JDBC <code>NCHAR</code>,
	 * the <code>String</code> object
	 * returned has exactly the same value the SQL
	 * <code>NCHAR</code> value had in the
	 * database, including any padding added by the database.
	 *
	 * @param parameterIndex index of the first parameter is 1, the second is 2, ...
	 * @return a <code>String</code> object that maps an
	 * <code>NCHAR</code>, <code>NVARCHAR</code> or <code>LONGNVARCHAR</code> value
	 * @throws SQLException                    if the parameterIndex is not valid;
	 *                                         if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see #setNString
	 * @since 1.6
	 */
	@Override public String getNString(int parameterIndex) throws SQLException {
		return null;
	}

	/**
	 * Retrieves the value of the designated <code>NCHAR</code>,
	 * <code>NVARCHAR</code>
	 * or <code>LONGNVARCHAR</code> parameter as
	 * a <code>String</code> in the Java programming language.
	 * <p>
	 * For the fixed-length type JDBC <code>NCHAR</code>,
	 * the <code>String</code> object
	 * returned has exactly the same value the SQL
	 * <code>NCHAR</code> value had in the
	 * database, including any padding added by the database.
	 *
	 * @param parameterName the name of the parameter
	 * @return a <code>String</code> object that maps an
	 * <code>NCHAR</code>, <code>NVARCHAR</code> or <code>LONGNVARCHAR</code> value
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter;
	 *                                         if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @see #setNString
	 * @since 1.6
	 */
	@Override public String getNString(String parameterName) throws SQLException {
		return null;
	}

	/**
	 * Retrieves the value of the designated parameter as a
	 * <code>java.io.Reader</code> object in the Java programming language.
	 * It is intended for use when
	 * accessing  <code>NCHAR</code>,<code>NVARCHAR</code>
	 * and <code>LONGNVARCHAR</code> parameters.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, ...
	 * @return a <code>java.io.Reader</code> object that contains the parameter
	 * value; if the value is SQL <code>NULL</code>, the value returned is
	 * <code>null</code> in the Java programming language.
	 * @throws SQLException                    if the parameterIndex is not valid;
	 *                                         if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @since 1.6
	 */
	@Override public Reader getNCharacterStream(int parameterIndex) throws SQLException {
		return null;
	}

	/**
	 * Retrieves the value of the designated parameter as a
	 * <code>java.io.Reader</code> object in the Java programming language.
	 * It is intended for use when
	 * accessing  <code>NCHAR</code>,<code>NVARCHAR</code>
	 * and <code>LONGNVARCHAR</code> parameters.
	 *
	 * @param parameterName the name of the parameter
	 * @return a <code>java.io.Reader</code> object that contains the parameter
	 * value; if the value is SQL <code>NULL</code>, the value returned is
	 * <code>null</code> in the Java programming language
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @since 1.6
	 */
	@Override public Reader getNCharacterStream(String parameterName) throws SQLException {
		return null;
	}

	/**
	 * Retrieves the value of the designated parameter as a
	 * <code>java.io.Reader</code> object in the Java programming language.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, ...
	 * @return a <code>java.io.Reader</code> object that contains the parameter
	 * value; if the value is SQL <code>NULL</code>, the value returned is
	 * <code>null</code> in the Java programming language.
	 * @throws SQLException if the parameterIndex is not valid; if a database access error occurs or
	 *                      this method is called on a closed <code>CallableStatement</code>
	 * @since 1.6
	 */
	@Override public Reader getCharacterStream(int parameterIndex) throws SQLException {
		return null;
	}

	/**
	 * Retrieves the value of the designated parameter as a
	 * <code>java.io.Reader</code> object in the Java programming language.
	 *
	 * @param parameterName the name of the parameter
	 * @return a <code>java.io.Reader</code> object that contains the parameter
	 * value; if the value is SQL <code>NULL</code>, the value returned is
	 * <code>null</code> in the Java programming language
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @since 1.6
	 */
	@Override public Reader getCharacterStream(String parameterName) throws SQLException {
		return null;
	}

	/**
	 * Sets the designated parameter to the given <code>java.sql.Blob</code> object.
	 * The driver converts this to an SQL <code>BLOB</code> value when it
	 * sends it to the database.
	 *
	 * @param parameterName the name of the parameter
	 * @param x             a <code>Blob</code> object that maps an SQL <code>BLOB</code> value
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @since 1.6
	 */
	@Override public void setBlob(String parameterName, Blob x) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given <code>java.sql.Clob</code> object.
	 * The driver converts this to an SQL <code>CLOB</code> value when it
	 * sends it to the database.
	 *
	 * @param parameterName the name of the parameter
	 * @param x             a <code>Clob</code> object that maps an SQL <code>CLOB</code> value
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @since 1.6
	 */
	@Override public void setClob(String parameterName, Clob x) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given input stream, which will have
	 * the specified number of bytes.
	 * When a very large ASCII value is input to a <code>LONGVARCHAR</code>
	 * parameter, it may be more practical to send it via a
	 * <code>java.io.InputStream</code>. Data will be read from the stream
	 * as needed until end-of-file is reached.  The JDBC driver will
	 * do any necessary conversion from ASCII to the database char format.
	 * <p>
	 * <P><B>Note:</B> This stream object can either be a standard
	 * Java stream object or your own subclass that implements the
	 * standard interface.
	 *
	 * @param parameterName the name of the parameter
	 * @param x             the Java input stream that contains the ASCII parameter value
	 * @param length        the number of bytes in the stream
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @since 1.6
	 */
	@Override public void setAsciiStream(String parameterName, InputStream x, long length) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given input stream, which will have
	 * the specified number of bytes.
	 * When a very large binary value is input to a <code>LONGVARBINARY</code>
	 * parameter, it may be more practical to send it via a
	 * <code>java.io.InputStream</code> object. The data will be read from the stream
	 * as needed until end-of-file is reached.
	 * <p>
	 * <P><B>Note:</B> This stream object can either be a standard
	 * Java stream object or your own subclass that implements the
	 * standard interface.
	 *
	 * @param parameterName the name of the parameter
	 * @param x             the java input stream which contains the binary parameter value
	 * @param length        the number of bytes in the stream
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @since 1.6
	 */
	@Override public void setBinaryStream(String parameterName, InputStream x, long length) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given <code>Reader</code>
	 * object, which is the given number of characters long.
	 * When a very large UNICODE value is input to a <code>LONGVARCHAR</code>
	 * parameter, it may be more practical to send it via a
	 * <code>java.io.Reader</code> object. The data will be read from the stream
	 * as needed until end-of-file is reached.  The JDBC driver will
	 * do any necessary conversion from UNICODE to the database char format.
	 * <p>
	 * <P><B>Note:</B> This stream object can either be a standard
	 * Java stream object or your own subclass that implements the
	 * standard interface.
	 *
	 * @param parameterName the name of the parameter
	 * @param reader        the <code>java.io.Reader</code> object that
	 *                      contains the UNICODE data used as the designated parameter
	 * @param length        the number of characters in the stream
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @since 1.6
	 */
	@Override public void setCharacterStream(String parameterName, Reader reader, long length) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given input stream.
	 * When a very large ASCII value is input to a <code>LONGVARCHAR</code>
	 * parameter, it may be more practical to send it via a
	 * <code>java.io.InputStream</code>. Data will be read from the stream
	 * as needed until end-of-file is reached.  The JDBC driver will
	 * do any necessary conversion from ASCII to the database char format.
	 * <p>
	 * <P><B>Note:</B> This stream object can either be a standard
	 * Java stream object or your own subclass that implements the
	 * standard interface.
	 * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
	 * it might be more efficient to use a version of
	 * <code>setAsciiStream</code> which takes a length parameter.
	 *
	 * @param parameterName the name of the parameter
	 * @param x             the Java input stream that contains the ASCII parameter value
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
	 * @since 1.6
	 */
	@Override public void setAsciiStream(String parameterName, InputStream x) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given input stream.
	 * When a very large binary value is input to a <code>LONGVARBINARY</code>
	 * parameter, it may be more practical to send it via a
	 * <code>java.io.InputStream</code> object. The data will be read from the
	 * stream as needed until end-of-file is reached.
	 * <p>
	 * <P><B>Note:</B> This stream object can either be a standard
	 * Java stream object or your own subclass that implements the
	 * standard interface.
	 * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
	 * it might be more efficient to use a version of
	 * <code>setBinaryStream</code> which takes a length parameter.
	 *
	 * @param parameterName the name of the parameter
	 * @param x             the java input stream which contains the binary parameter value
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
	 * @since 1.6
	 */
	@Override public void setBinaryStream(String parameterName, InputStream x) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given <code>Reader</code>
	 * object.
	 * When a very large UNICODE value is input to a <code>LONGVARCHAR</code>
	 * parameter, it may be more practical to send it via a
	 * <code>java.io.Reader</code> object. The data will be read from the stream
	 * as needed until end-of-file is reached.  The JDBC driver will
	 * do any necessary conversion from UNICODE to the database char format.
	 * <p>
	 * <P><B>Note:</B> This stream object can either be a standard
	 * Java stream object or your own subclass that implements the
	 * standard interface.
	 * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
	 * it might be more efficient to use a version of
	 * <code>setCharacterStream</code> which takes a length parameter.
	 *
	 * @param parameterName the name of the parameter
	 * @param reader        the <code>java.io.Reader</code> object that contains the
	 *                      Unicode data
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
	 * @since 1.6
	 */
	@Override public void setCharacterStream(String parameterName, Reader reader) throws SQLException {

	}

	/**
	 * Sets the designated parameter to a <code>Reader</code> object. The
	 * <code>Reader</code> reads the data till end-of-file is reached. The
	 * driver does the necessary conversion from Java character format to
	 * the national character set in the database.
	 * <p>
	 * <P><B>Note:</B> This stream object can either be a standard
	 * Java stream object or your own subclass that implements the
	 * standard interface.
	 * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
	 * it might be more efficient to use a version of
	 * <code>setNCharacterStream</code> which takes a length parameter.
	 *
	 * @param parameterName the name of the parameter
	 * @param value         the parameter value
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if the driver does not support national
	 *                                         character sets;  if the driver can detect that a data conversion
	 *                                         error could occur; if a database access error occurs; or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
	 * @since 1.6
	 */
	@Override public void setNCharacterStream(String parameterName, Reader value) throws SQLException {

	}

	/**
	 * Sets the designated parameter to a <code>Reader</code> object.
	 * This method differs from the <code>setCharacterStream (int, Reader)</code> method
	 * because it informs the driver that the parameter value should be sent to
	 * the server as a <code>CLOB</code>.  When the <code>setCharacterStream</code> method is used, the
	 * driver may have to do extra work to determine whether the parameter
	 * data should be send to the server as a <code>LONGVARCHAR</code> or a <code>CLOB</code>
	 * <p>
	 * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
	 * it might be more efficient to use a version of
	 * <code>setClob</code> which takes a length parameter.
	 *
	 * @param parameterName the name of the parameter
	 * @param reader        An object that contains the data to set the parameter value to.
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or this method is called on
	 *                                         a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
	 * @since 1.6
	 */
	@Override public void setClob(String parameterName, Reader reader) throws SQLException {

	}

	/**
	 * Sets the designated parameter to a <code>InputStream</code> object.
	 * This method differs from the <code>setBinaryStream (int, InputStream)</code>
	 * method because it informs the driver that the parameter value should be
	 * sent to the server as a <code>BLOB</code>.  When the <code>setBinaryStream</code> method is used,
	 * the driver may have to do extra work to determine whether the parameter
	 * data should be send to the server as a <code>LONGVARBINARY</code> or a <code>BLOB</code>
	 * <p>
	 * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
	 * it might be more efficient to use a version of
	 * <code>setBlob</code> which takes a length parameter.
	 *
	 * @param parameterName the name of the parameter
	 * @param inputStream   An object that contains the data to set the parameter
	 *                      value to.
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
	 * @since 1.6
	 */
	@Override public void setBlob(String parameterName, InputStream inputStream) throws SQLException {

	}

	/**
	 * Sets the designated parameter to a <code>Reader</code> object.
	 * This method differs from the <code>setCharacterStream (int, Reader)</code> method
	 * because it informs the driver that the parameter value should be sent to
	 * the server as a <code>NCLOB</code>.  When the <code>setCharacterStream</code> method is used, the
	 * driver may have to do extra work to determine whether the parameter
	 * data should be send to the server as a <code>LONGNVARCHAR</code> or a <code>NCLOB</code>
	 * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
	 * it might be more efficient to use a version of
	 * <code>setNClob</code> which takes a length parameter.
	 *
	 * @param parameterName the name of the parameter
	 * @param reader        An object that contains the data to set the parameter value to.
	 * @throws SQLException                    if parameterName does not correspond to a named
	 *                                         parameter; if the driver does not support national character sets;
	 *                                         if the driver can detect that a data conversion
	 *                                         error could occur;  if a database access error occurs or
	 *                                         this method is called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
	 * @since 1.6
	 */
	@Override public void setNClob(String parameterName, Reader reader) throws SQLException {

	}

	/**
	 * <p>Returns an object representing the value of OUT parameter
	 * {@code parameterIndex} and will convert from the
	 * SQL type of the parameter to the requested Java data type, if the
	 * conversion is supported. If the conversion is not
	 * supported or null is specified for the type, a
	 * <code>SQLException</code> is thrown.
	 * <p>
	 * At a minimum, an implementation must support the conversions defined in
	 * Appendix B, Table B-3 and conversion of appropriate user defined SQL
	 * types to a Java type which implements {@code SQLData}, or {@code Struct}.
	 * Additional conversions may be supported and are vendor defined.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, and so on
	 * @param type           Class representing the Java data type to convert the
	 *                       designated parameter to.
	 * @return an instance of {@code type} holding the OUT parameter value
	 * @throws SQLException                    if conversion is not supported, type is null or
	 *                                         another error occurs. The getCause() method of the
	 *                                         exception may provide a more detailed exception, for example, if
	 *                                         a conversion error occurs
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @since 1.7
	 */
	@Override public <T> T getObject(int parameterIndex, Class<T> type) throws SQLException {
		return null;
	}

	/**
	 * <p>Returns an object representing the value of OUT parameter
	 * {@code parameterName} and will convert from the
	 * SQL type of the parameter to the requested Java data type, if the
	 * conversion is supported. If the conversion is not
	 * supported  or null is specified for the type, a
	 * <code>SQLException</code> is thrown.
	 * <p>
	 * At a minimum, an implementation must support the conversions defined in
	 * Appendix B, Table B-3 and conversion of appropriate user defined SQL
	 * types to a Java type which implements {@code SQLData}, or {@code Struct}.
	 * Additional conversions may be supported and are vendor defined.
	 *
	 * @param parameterName the name of the parameter
	 * @param type          Class representing the Java data type to convert
	 *                      the designated parameter to.
	 * @return an instance of {@code type} holding the OUT parameter
	 * value
	 * @throws SQLException                    if conversion is not supported, type is null or
	 *                                         another error occurs. The getCause() method of the
	 *                                         exception may provide a more detailed exception, for example, if
	 *                                         a conversion error occurs
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @since 1.7
	 */
	@Override public <T> T getObject(String parameterName, Class<T> type) throws SQLException {
		return null;
	}

	/**
	 * Executes the SQL query in this <code>PreparedStatement</code> object
	 * and returns the <code>ResultSet</code> object generated by the query.
	 *
	 * @return a <code>ResultSet</code> object that contains the data produced by the
	 * query; never <code>null</code>
	 * @throws SQLException        if a database access error occurs;
	 *                             this method is called on a closed  <code>PreparedStatement</code> or the SQL
	 *                             statement does not return a <code>ResultSet</code> object
	 * @throws SQLTimeoutException when the driver has determined that the
	 *                             timeout value that was specified by the {@code setQueryTimeout}
	 *                             method has been exceeded and has at least attempted to cancel
	 *                             the currently running {@code Statement}
	 */
	@Override public ResultSet executeQuery() throws SQLException {
		return null;
	}

	/**
	 * Executes the SQL statement in this <code>PreparedStatement</code> object,
	 * which must be an SQL Data Manipulation Language (DML) statement, such as <code>INSERT</code>, <code>UPDATE</code> or
	 * <code>DELETE</code>; or an SQL statement that returns nothing,
	 * such as a DDL statement.
	 *
	 * @return either (1) the row count for SQL Data Manipulation Language (DML) statements
	 * or (2) 0 for SQL statements that return nothing
	 * @throws SQLException        if a database access error occurs;
	 *                             this method is called on a closed  <code>PreparedStatement</code>
	 *                             or the SQL statement returns a <code>ResultSet</code> object
	 * @throws SQLTimeoutException when the driver has determined that the
	 *                             timeout value that was specified by the {@code setQueryTimeout}
	 *                             method has been exceeded and has at least attempted to cancel
	 *                             the currently running {@code Statement}
	 */
	@Override public int executeUpdate() throws SQLException {
		return 0;
	}

	/**
	 * Sets the designated parameter to SQL <code>NULL</code>.
	 * <p>
	 * <P><B>Note:</B> You must specify the parameter's SQL type.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, ...
	 * @param sqlType        the SQL type code defined in <code>java.sql.Types</code>
	 * @throws SQLException                    if parameterIndex does not correspond to a parameter
	 *                                         marker in the SQL statement; if a database access error occurs or
	 *                                         this method is called on a closed <code>PreparedStatement</code>
	 * @throws SQLFeatureNotSupportedException if <code>sqlType</code> is
	 *                                         a <code>ARRAY</code>, <code>BLOB</code>, <code>CLOB</code>,
	 *                                         <code>DATALINK</code>, <code>JAVA_OBJECT</code>, <code>NCHAR</code>,
	 *                                         <code>NCLOB</code>, <code>NVARCHAR</code>, <code>LONGNVARCHAR</code>,
	 *                                         <code>REF</code>, <code>ROWID</code>, <code>SQLXML</code>
	 *                                         or  <code>STRUCT</code> data type and the JDBC driver does not support
	 *                                         this data type
	 */
	@Override public void setNull(int parameterIndex, int sqlType) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given Java <code>boolean</code> value.
	 * The driver converts this
	 * to an SQL <code>BIT</code> or <code>BOOLEAN</code> value when it sends it to the database.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, ...
	 * @param x              the parameter value
	 * @throws SQLException if parameterIndex does not correspond to a parameter
	 *                      marker in the SQL statement;
	 *                      if a database access error occurs or
	 *                      this method is called on a closed <code>PreparedStatement</code>
	 */
	@Override public void setBoolean(int parameterIndex, boolean x) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given Java <code>byte</code> value.
	 * The driver converts this
	 * to an SQL <code>TINYINT</code> value when it sends it to the database.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, ...
	 * @param x              the parameter value
	 * @throws SQLException if parameterIndex does not correspond to a parameter
	 *                      marker in the SQL statement; if a database access error occurs or
	 *                      this method is called on a closed <code>PreparedStatement</code>
	 */
	@Override public void setByte(int parameterIndex, byte x) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given Java <code>short</code> value.
	 * The driver converts this
	 * to an SQL <code>SMALLINT</code> value when it sends it to the database.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, ...
	 * @param x              the parameter value
	 * @throws SQLException if parameterIndex does not correspond to a parameter
	 *                      marker in the SQL statement; if a database access error occurs or
	 *                      this method is called on a closed <code>PreparedStatement</code>
	 */
	@Override public void setShort(int parameterIndex, short x) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given Java <code>int</code> value.
	 * The driver converts this
	 * to an SQL <code>INTEGER</code> value when it sends it to the database.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, ...
	 * @param x              the parameter value
	 * @throws SQLException if parameterIndex does not correspond to a parameter
	 *                      marker in the SQL statement; if a database access error occurs or
	 *                      this method is called on a closed <code>PreparedStatement</code>
	 */
	@Override public void setInt(int parameterIndex, int x) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given Java <code>long</code> value.
	 * The driver converts this
	 * to an SQL <code>BIGINT</code> value when it sends it to the database.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, ...
	 * @param x              the parameter value
	 * @throws SQLException if parameterIndex does not correspond to a parameter
	 *                      marker in the SQL statement; if a database access error occurs or
	 *                      this method is called on a closed <code>PreparedStatement</code>
	 */
	@Override public void setLong(int parameterIndex, long x) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given Java <code>float</code> value.
	 * The driver converts this
	 * to an SQL <code>REAL</code> value when it sends it to the database.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, ...
	 * @param x              the parameter value
	 * @throws SQLException if parameterIndex does not correspond to a parameter
	 *                      marker in the SQL statement; if a database access error occurs or
	 *                      this method is called on a closed <code>PreparedStatement</code>
	 */
	@Override public void setFloat(int parameterIndex, float x) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given Java <code>double</code> value.
	 * The driver converts this
	 * to an SQL <code>DOUBLE</code> value when it sends it to the database.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, ...
	 * @param x              the parameter value
	 * @throws SQLException if parameterIndex does not correspond to a parameter
	 *                      marker in the SQL statement; if a database access error occurs or
	 *                      this method is called on a closed <code>PreparedStatement</code>
	 */
	@Override public void setDouble(int parameterIndex, double x) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given <code>java.math.BigDecimal</code> value.
	 * The driver converts this to an SQL <code>NUMERIC</code> value when
	 * it sends it to the database.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, ...
	 * @param x              the parameter value
	 * @throws SQLException if parameterIndex does not correspond to a parameter
	 *                      marker in the SQL statement; if a database access error occurs or
	 *                      this method is called on a closed <code>PreparedStatement</code>
	 */
	@Override public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given Java <code>String</code> value.
	 * The driver converts this
	 * to an SQL <code>VARCHAR</code> or <code>LONGVARCHAR</code> value
	 * (depending on the argument's
	 * size relative to the driver's limits on <code>VARCHAR</code> values)
	 * when it sends it to the database.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, ...
	 * @param x              the parameter value
	 * @throws SQLException if parameterIndex does not correspond to a parameter
	 *                      marker in the SQL statement; if a database access error occurs or
	 *                      this method is called on a closed <code>PreparedStatement</code>
	 */
	@Override public void setString(int parameterIndex, String x) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given Java array of bytes.  The driver converts
	 * this to an SQL <code>VARBINARY</code> or <code>LONGVARBINARY</code>
	 * (depending on the argument's size relative to the driver's limits on
	 * <code>VARBINARY</code> values) when it sends it to the database.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, ...
	 * @param x              the parameter value
	 * @throws SQLException if parameterIndex does not correspond to a parameter
	 *                      marker in the SQL statement; if a database access error occurs or
	 *                      this method is called on a closed <code>PreparedStatement</code>
	 */
	@Override public void setBytes(int parameterIndex, byte[] x) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given <code>java.sql.Date</code> value
	 * using the default time zone of the virtual machine that is running
	 * the application.
	 * The driver converts this
	 * to an SQL <code>DATE</code> value when it sends it to the database.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, ...
	 * @param x              the parameter value
	 * @throws SQLException if parameterIndex does not correspond to a parameter
	 *                      marker in the SQL statement; if a database access error occurs or
	 *                      this method is called on a closed <code>PreparedStatement</code>
	 */
	@Override public void setDate(int parameterIndex, Date x) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given <code>java.sql.Time</code> value.
	 * The driver converts this
	 * to an SQL <code>TIME</code> value when it sends it to the database.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, ...
	 * @param x              the parameter value
	 * @throws SQLException if parameterIndex does not correspond to a parameter
	 *                      marker in the SQL statement; if a database access error occurs or
	 *                      this method is called on a closed <code>PreparedStatement</code>
	 */
	@Override public void setTime(int parameterIndex, Time x) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given <code>java.sql.Timestamp</code> value.
	 * The driver
	 * converts this to an SQL <code>TIMESTAMP</code> value when it sends it to the
	 * database.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, ...
	 * @param x              the parameter value
	 * @throws SQLException if parameterIndex does not correspond to a parameter
	 *                      marker in the SQL statement; if a database access error occurs or
	 *                      this method is called on a closed <code>PreparedStatement</code>
	 */
	@Override public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given input stream, which will have
	 * the specified number of bytes.
	 * When a very large ASCII value is input to a <code>LONGVARCHAR</code>
	 * parameter, it may be more practical to send it via a
	 * <code>java.io.InputStream</code>. Data will be read from the stream
	 * as needed until end-of-file is reached.  The JDBC driver will
	 * do any necessary conversion from ASCII to the database char format.
	 * <p>
	 * <P><B>Note:</B> This stream object can either be a standard
	 * Java stream object or your own subclass that implements the
	 * standard interface.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, ...
	 * @param x              the Java input stream that contains the ASCII parameter value
	 * @param length         the number of bytes in the stream
	 * @throws SQLException if parameterIndex does not correspond to a parameter
	 *                      marker in the SQL statement; if a database access error occurs or
	 *                      this method is called on a closed <code>PreparedStatement</code>
	 */
	@Override public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given input stream, which
	 * will have the specified number of bytes.
	 * <p>
	 * When a very large Unicode value is input to a <code>LONGVARCHAR</code>
	 * parameter, it may be more practical to send it via a
	 * <code>java.io.InputStream</code> object. The data will be read from the
	 * stream as needed until end-of-file is reached.  The JDBC driver will
	 * do any necessary conversion from Unicode to the database char format.
	 * <p>
	 * The byte format of the Unicode stream must be a Java UTF-8, as defined in the
	 * Java Virtual Machine Specification.
	 * <p>
	 * <P><B>Note:</B> This stream object can either be a standard
	 * Java stream object or your own subclass that implements the
	 * standard interface.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, ...
	 * @param x              a <code>java.io.InputStream</code> object that contains the
	 *                       Unicode parameter value
	 * @param length         the number of bytes in the stream
	 * @throws SQLException                    if parameterIndex does not correspond to a parameter
	 *                                         marker in the SQL statement; if a database access error occurs or
	 *                                         this method is called on a closed <code>PreparedStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @deprecated Use {@code setCharacterStream}
	 */
	@Override public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given input stream, which will have
	 * the specified number of bytes.
	 * When a very large binary value is input to a <code>LONGVARBINARY</code>
	 * parameter, it may be more practical to send it via a
	 * <code>java.io.InputStream</code> object. The data will be read from the
	 * stream as needed until end-of-file is reached.
	 * <p>
	 * <P><B>Note:</B> This stream object can either be a standard
	 * Java stream object or your own subclass that implements the
	 * standard interface.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, ...
	 * @param x              the java input stream which contains the binary parameter value
	 * @param length         the number of bytes in the stream
	 * @throws SQLException if parameterIndex does not correspond to a parameter
	 *                      marker in the SQL statement; if a database access error occurs or
	 *                      this method is called on a closed <code>PreparedStatement</code>
	 */
	@Override public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {

	}

	/**
	 * Clears the current parameter values immediately.
	 * <P>In general, parameter values remain in force for repeated use of a
	 * statement. Setting a parameter value automatically clears its
	 * previous value.  However, in some cases it is useful to immediately
	 * release the resources used by the current parameter values; this can
	 * be done by calling the method <code>clearParameters</code>.
	 *
	 * @throws SQLException if a database access error occurs or
	 *                      this method is called on a closed <code>PreparedStatement</code>
	 */
	@Override public void clearParameters() throws SQLException {

	}

	/**
	 * Sets the value of the designated parameter with the given object.
	 * <p>
	 * This method is similar to {@link #setObject(int parameterIndex,
	 * Object x, int targetSqlType, int scaleOrLength)},
	 * except that it assumes a scale of zero.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, ...
	 * @param x              the object containing the input parameter value
	 * @param targetSqlType  the SQL type (as defined in java.sql.Types) to be
	 *                       sent to the database
	 * @throws SQLException                    if parameterIndex does not correspond to a parameter
	 *                                         marker in the SQL statement; if a database access error occurs or this
	 *                                         method is called on a closed PreparedStatement
	 * @throws SQLFeatureNotSupportedException if
	 *                                         the JDBC driver does not support the specified targetSqlType
	 * @see Types
	 */
	@Override public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {

	}

	/**
	 * <p>Sets the value of the designated parameter using the given object.
	 * <p>
	 * <p>The JDBC specification specifies a standard mapping from
	 * Java <code>Object</code> types to SQL types.  The given argument
	 * will be converted to the corresponding SQL type before being
	 * sent to the database.
	 * <p>
	 * <p>Note that this method may be used to pass datatabase-
	 * specific abstract data types, by using a driver-specific Java
	 * type.
	 * <p>
	 * If the object is of a class implementing the interface <code>SQLData</code>,
	 * the JDBC driver should call the method <code>SQLData.writeSQL</code>
	 * to write it to the SQL data stream.
	 * If, on the other hand, the object is of a class implementing
	 * <code>Ref</code>, <code>Blob</code>, <code>Clob</code>,  <code>NClob</code>,
	 * <code>Struct</code>, <code>java.net.URL</code>, <code>RowId</code>, <code>SQLXML</code>
	 * or <code>Array</code>, the driver should pass it to the database as a
	 * value of the corresponding SQL type.
	 * <p>
	 * <b>Note:</b> Not all databases allow for a non-typed Null to be sent to
	 * the backend. For maximum portability, the <code>setNull</code> or the
	 * <code>setObject(int parameterIndex, Object x, int sqlType)</code>
	 * method should be used
	 * instead of <code>setObject(int parameterIndex, Object x)</code>.
	 * <p>
	 * <b>Note:</b> This method throws an exception if there is an ambiguity, for example, if the
	 * object is of a class implementing more than one of the interfaces named above.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, ...
	 * @param x              the object containing the input parameter value
	 * @throws SQLException if parameterIndex does not correspond to a parameter
	 *                      marker in the SQL statement; if a database access error occurs;
	 *                      this method is called on a closed <code>PreparedStatement</code>
	 *                      or the type of the given object is ambiguous
	 */
	@Override public void setObject(int parameterIndex, Object x) throws SQLException {

	}

	/**
	 * Executes the SQL statement in this <code>PreparedStatement</code> object,
	 * which may be any kind of SQL statement.
	 * Some prepared statements return multiple results; the <code>execute</code>
	 * method handles these complex statements as well as the simpler
	 * form of statements handled by the methods <code>executeQuery</code>
	 * and <code>executeUpdate</code>.
	 * <p>
	 * The <code>execute</code> method returns a <code>boolean</code> to
	 * indicate the form of the first result.  You must call either the method
	 * <code>getResultSet</code> or <code>getUpdateCount</code>
	 * to retrieve the result; you must call <code>getMoreResults</code> to
	 * move to any subsequent result(s).
	 *
	 * @return <code>true</code> if the first result is a <code>ResultSet</code>
	 * object; <code>false</code> if the first result is an update
	 * count or there is no result
	 * @throws SQLException        if a database access error occurs;
	 *                             this method is called on a closed <code>PreparedStatement</code>
	 *                             or an argument is supplied to this method
	 * @throws SQLTimeoutException when the driver has determined that the
	 *                             timeout value that was specified by the {@code setQueryTimeout}
	 *                             method has been exceeded and has at least attempted to cancel
	 *                             the currently running {@code Statement}
	 * @see Statement#execute
	 * @see Statement#getResultSet
	 * @see Statement#getUpdateCount
	 * @see Statement#getMoreResults
	 */
	@Override public boolean execute() throws SQLException {
		return false;
	}

	/**
	 * Adds a set of parameters to this <code>PreparedStatement</code>
	 * object's batch of commands.
	 *
	 * @throws SQLException if a database access error occurs or
	 *                      this method is called on a closed <code>PreparedStatement</code>
	 * @see Statement#addBatch
	 * @since 1.2
	 */
	@Override public void addBatch() throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given <code>Reader</code>
	 * object, which is the given number of characters long.
	 * When a very large UNICODE value is input to a <code>LONGVARCHAR</code>
	 * parameter, it may be more practical to send it via a
	 * <code>java.io.Reader</code> object. The data will be read from the stream
	 * as needed until end-of-file is reached.  The JDBC driver will
	 * do any necessary conversion from UNICODE to the database char format.
	 * <p>
	 * <P><B>Note:</B> This stream object can either be a standard
	 * Java stream object or your own subclass that implements the
	 * standard interface.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, ...
	 * @param reader         the <code>java.io.Reader</code> object that contains the
	 *                       Unicode data
	 * @param length         the number of characters in the stream
	 * @throws SQLException if parameterIndex does not correspond to a parameter
	 *                      marker in the SQL statement; if a database access error occurs or
	 *                      this method is called on a closed <code>PreparedStatement</code>
	 * @since 1.2
	 */
	@Override public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given
	 * <code>REF(&lt;structured-type&gt;)</code> value.
	 * The driver converts this to an SQL <code>REF</code> value when it
	 * sends it to the database.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, ...
	 * @param x              an SQL <code>REF</code> value
	 * @throws SQLException                    if parameterIndex does not correspond to a parameter
	 *                                         marker in the SQL statement; if a database access error occurs or
	 *                                         this method is called on a closed <code>PreparedStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
	 * @since 1.2
	 */
	@Override public void setRef(int parameterIndex, Ref x) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given <code>java.sql.Blob</code> object.
	 * The driver converts this to an SQL <code>BLOB</code> value when it
	 * sends it to the database.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, ...
	 * @param x              a <code>Blob</code> object that maps an SQL <code>BLOB</code> value
	 * @throws SQLException                    if parameterIndex does not correspond to a parameter
	 *                                         marker in the SQL statement; if a database access error occurs or
	 *                                         this method is called on a closed <code>PreparedStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
	 * @since 1.2
	 */
	@Override public void setBlob(int parameterIndex, Blob x) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given <code>java.sql.Clob</code> object.
	 * The driver converts this to an SQL <code>CLOB</code> value when it
	 * sends it to the database.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, ...
	 * @param x              a <code>Clob</code> object that maps an SQL <code>CLOB</code> value
	 * @throws SQLException                    if parameterIndex does not correspond to a parameter
	 *                                         marker in the SQL statement; if a database access error occurs or
	 *                                         this method is called on a closed <code>PreparedStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
	 * @since 1.2
	 */
	@Override public void setClob(int parameterIndex, Clob x) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given <code>java.sql.Array</code> object.
	 * The driver converts this to an SQL <code>ARRAY</code> value when it
	 * sends it to the database.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, ...
	 * @param x              an <code>Array</code> object that maps an SQL <code>ARRAY</code> value
	 * @throws SQLException                    if parameterIndex does not correspond to a parameter
	 *                                         marker in the SQL statement; if a database access error occurs or
	 *                                         this method is called on a closed <code>PreparedStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
	 * @since 1.2
	 */
	@Override public void setArray(int parameterIndex, Array x) throws SQLException {

	}

	/**
	 * Retrieves a <code>ResultSetMetaData</code> object that contains
	 * information about the columns of the <code>ResultSet</code> object
	 * that will be returned when this <code>PreparedStatement</code> object
	 * is executed.
	 * <p>
	 * Because a <code>PreparedStatement</code> object is precompiled, it is
	 * possible to know about the <code>ResultSet</code> object that it will
	 * return without having to execute it.  Consequently, it is possible
	 * to invoke the method <code>getMetaData</code> on a
	 * <code>PreparedStatement</code> object rather than waiting to execute
	 * it and then invoking the <code>ResultSet.getMetaData</code> method
	 * on the <code>ResultSet</code> object that is returned.
	 * <p>
	 * <B>NOTE:</B> Using this method may be expensive for some drivers due
	 * to the lack of underlying DBMS support.
	 *
	 * @return the description of a <code>ResultSet</code> object's columns or
	 * <code>null</code> if the driver cannot return a
	 * <code>ResultSetMetaData</code> object
	 * @throws SQLException                    if a database access error occurs or
	 *                                         this method is called on a closed <code>PreparedStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 * @since 1.2
	 */
	@Override public ResultSetMetaData getMetaData() throws SQLException {
		return null;
	}

	/**
	 * Sets the designated parameter to the given <code>java.sql.Date</code> value,
	 * using the given <code>Calendar</code> object.  The driver uses
	 * the <code>Calendar</code> object to construct an SQL <code>DATE</code> value,
	 * which the driver then sends to the database.  With
	 * a <code>Calendar</code> object, the driver can calculate the date
	 * taking into account a custom timezone.  If no
	 * <code>Calendar</code> object is specified, the driver uses the default
	 * timezone, which is that of the virtual machine running the application.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, ...
	 * @param x              the parameter value
	 * @param cal            the <code>Calendar</code> object the driver will use
	 *                       to construct the date
	 * @throws SQLException if parameterIndex does not correspond to a parameter
	 *                      marker in the SQL statement; if a database access error occurs or
	 *                      this method is called on a closed <code>PreparedStatement</code>
	 * @since 1.2
	 */
	@Override public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given <code>java.sql.Time</code> value,
	 * using the given <code>Calendar</code> object.  The driver uses
	 * the <code>Calendar</code> object to construct an SQL <code>TIME</code> value,
	 * which the driver then sends to the database.  With
	 * a <code>Calendar</code> object, the driver can calculate the time
	 * taking into account a custom timezone.  If no
	 * <code>Calendar</code> object is specified, the driver uses the default
	 * timezone, which is that of the virtual machine running the application.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, ...
	 * @param x              the parameter value
	 * @param cal            the <code>Calendar</code> object the driver will use
	 *                       to construct the time
	 * @throws SQLException if parameterIndex does not correspond to a parameter
	 *                      marker in the SQL statement; if a database access error occurs or
	 *                      this method is called on a closed <code>PreparedStatement</code>
	 * @since 1.2
	 */
	@Override public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given <code>java.sql.Timestamp</code> value,
	 * using the given <code>Calendar</code> object.  The driver uses
	 * the <code>Calendar</code> object to construct an SQL <code>TIMESTAMP</code> value,
	 * which the driver then sends to the database.  With a
	 * <code>Calendar</code> object, the driver can calculate the timestamp
	 * taking into account a custom timezone.  If no
	 * <code>Calendar</code> object is specified, the driver uses the default
	 * timezone, which is that of the virtual machine running the application.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, ...
	 * @param x              the parameter value
	 * @param cal            the <code>Calendar</code> object the driver will use
	 *                       to construct the timestamp
	 * @throws SQLException if parameterIndex does not correspond to a parameter
	 *                      marker in the SQL statement; if a database access error occurs or
	 *                      this method is called on a closed <code>PreparedStatement</code>
	 * @since 1.2
	 */
	@Override public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {

	}

	/**
	 * Sets the designated parameter to SQL <code>NULL</code>.
	 * This version of the method <code>setNull</code> should
	 * be used for user-defined types and REF type parameters.  Examples
	 * of user-defined types include: STRUCT, DISTINCT, JAVA_OBJECT, and
	 * named array types.
	 * <p>
	 * <P><B>Note:</B> To be portable, applications must give the
	 * SQL type code and the fully-qualified SQL type name when specifying
	 * a NULL user-defined or REF parameter.  In the case of a user-defined type
	 * the name is the type name of the parameter itself.  For a REF
	 * parameter, the name is the type name of the referenced type.  If
	 * a JDBC driver does not need the type code or type name information,
	 * it may ignore it.
	 * <p>
	 * Although it is intended for user-defined and Ref parameters,
	 * this method may be used to set a null parameter of any JDBC type.
	 * If the parameter does not have a user-defined or REF type, the given
	 * typeName is ignored.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, ...
	 * @param sqlType        a value from <code>java.sql.Types</code>
	 * @param typeName       the fully-qualified name of an SQL user-defined type;
	 *                       ignored if the parameter is not a user-defined type or REF
	 * @throws SQLException                    if parameterIndex does not correspond to a parameter
	 *                                         marker in the SQL statement; if a database access error occurs or
	 *                                         this method is called on a closed <code>PreparedStatement</code>
	 * @throws SQLFeatureNotSupportedException if <code>sqlType</code> is
	 *                                         a <code>ARRAY</code>, <code>BLOB</code>, <code>CLOB</code>,
	 *                                         <code>DATALINK</code>, <code>JAVA_OBJECT</code>, <code>NCHAR</code>,
	 *                                         <code>NCLOB</code>, <code>NVARCHAR</code>, <code>LONGNVARCHAR</code>,
	 *                                         <code>REF</code>, <code>ROWID</code>, <code>SQLXML</code>
	 *                                         or  <code>STRUCT</code> data type and the JDBC driver does not support
	 *                                         this data type or if the JDBC driver does not support this method
	 * @since 1.2
	 */
	@Override public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given <code>java.net.URL</code> value.
	 * The driver converts this to an SQL <code>DATALINK</code> value
	 * when it sends it to the database.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, ...
	 * @param x              the <code>java.net.URL</code> object to be set
	 * @throws SQLException                    if parameterIndex does not correspond to a parameter
	 *                                         marker in the SQL statement; if a database access error occurs or
	 *                                         this method is called on a closed <code>PreparedStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
	 * @since 1.4
	 */
	@Override public void setURL(int parameterIndex, URL x) throws SQLException {

	}

	/**
	 * Retrieves the number, types and properties of this
	 * <code>PreparedStatement</code> object's parameters.
	 *
	 * @return a <code>ParameterMetaData</code> object that contains information
	 * about the number, types and properties for each
	 * parameter marker of this <code>PreparedStatement</code> object
	 * @throws SQLException if a database access error occurs or
	 *                      this method is called on a closed <code>PreparedStatement</code>
	 * @see ParameterMetaData
	 * @since 1.4
	 */
	@Override public java.sql.ParameterMetaData getParameterMetaData() throws SQLException {
		return null;
	}

	/**
	 * Sets the designated parameter to the given <code>java.sql.RowId</code> object. The
	 * driver converts this to a SQL <code>ROWID</code> value when it sends it
	 * to the database
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, ...
	 * @param x              the parameter value
	 * @throws SQLException                    if parameterIndex does not correspond to a parameter
	 *                                         marker in the SQL statement; if a database access error occurs or
	 *                                         this method is called on a closed <code>PreparedStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
	 * @since 1.6
	 */
	@Override public void setRowId(int parameterIndex, RowId x) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given <code>String</code> object.
	 * The driver converts this to a SQL <code>NCHAR</code> or
	 * <code>NVARCHAR</code> or <code>LONGNVARCHAR</code> value
	 * (depending on the argument's
	 * size relative to the driver's limits on <code>NVARCHAR</code> values)
	 * when it sends it to the database.
	 *
	 * @param parameterIndex of the first parameter is 1, the second is 2, ...
	 * @param value          the parameter value
	 * @throws SQLException                    if parameterIndex does not correspond to a parameter
	 *                                         marker in the SQL statement; if the driver does not support national
	 *                                         character sets;  if the driver can detect that a data conversion
	 *                                         error could occur; if a database access error occurs; or
	 *                                         this method is called on a closed <code>PreparedStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
	 * @since 1.6
	 */
	@Override public void setNString(int parameterIndex, String value) throws SQLException {

	}

	/**
	 * Sets the designated parameter to a <code>Reader</code> object. The
	 * <code>Reader</code> reads the data till end-of-file is reached. The
	 * driver does the necessary conversion from Java character format to
	 * the national character set in the database.
	 *
	 * @param parameterIndex of the first parameter is 1, the second is 2, ...
	 * @param value          the parameter value
	 * @param length         the number of characters in the parameter data.
	 * @throws SQLException                    if parameterIndex does not correspond to a parameter
	 *                                         marker in the SQL statement; if the driver does not support national
	 *                                         character sets;  if the driver can detect that a data conversion
	 *                                         error could occur; if a database access error occurs; or
	 *                                         this method is called on a closed <code>PreparedStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
	 * @since 1.6
	 */
	@Override public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {

	}

	/**
	 * Sets the designated parameter to a <code>java.sql.NClob</code> object. The driver converts this to a
	 * SQL <code>NCLOB</code> value when it sends it to the database.
	 *
	 * @param parameterIndex of the first parameter is 1, the second is 2, ...
	 * @param value          the parameter value
	 * @throws SQLException                    if parameterIndex does not correspond to a parameter
	 *                                         marker in the SQL statement; if the driver does not support national
	 *                                         character sets;  if the driver can detect that a data conversion
	 *                                         error could occur; if a database access error occurs; or
	 *                                         this method is called on a closed <code>PreparedStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
	 * @since 1.6
	 */
	@Override public void setNClob(int parameterIndex, NClob value) throws SQLException {

	}

	/**
	 * Sets the designated parameter to a <code>Reader</code> object.  The reader must contain  the number
	 * of characters specified by length otherwise a <code>SQLException</code> will be
	 * generated when the <code>PreparedStatement</code> is executed.
	 * This method differs from the <code>setCharacterStream (int, Reader, int)</code> method
	 * because it informs the driver that the parameter value should be sent to
	 * the server as a <code>CLOB</code>.  When the <code>setCharacterStream</code> method is used, the
	 * driver may have to do extra work to determine whether the parameter
	 * data should be sent to the server as a <code>LONGVARCHAR</code> or a <code>CLOB</code>
	 *
	 * @param parameterIndex index of the first parameter is 1, the second is 2, ...
	 * @param reader         An object that contains the data to set the parameter value to.
	 * @param length         the number of characters in the parameter data.
	 * @throws SQLException                    if parameterIndex does not correspond to a parameter
	 *                                         marker in the SQL statement; if a database access error occurs; this method is called on
	 *                                         a closed <code>PreparedStatement</code> or if the length specified is less than zero.
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
	 * @since 1.6
	 */
	@Override public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {

	}

	/**
	 * Sets the designated parameter to a <code>InputStream</code> object.  The inputstream must contain  the number
	 * of characters specified by length otherwise a <code>SQLException</code> will be
	 * generated when the <code>PreparedStatement</code> is executed.
	 * This method differs from the <code>setBinaryStream (int, InputStream, int)</code>
	 * method because it informs the driver that the parameter value should be
	 * sent to the server as a <code>BLOB</code>.  When the <code>setBinaryStream</code> method is used,
	 * the driver may have to do extra work to determine whether the parameter
	 * data should be sent to the server as a <code>LONGVARBINARY</code> or a <code>BLOB</code>
	 *
	 * @param parameterIndex index of the first parameter is 1,
	 *                       the second is 2, ...
	 * @param inputStream    An object that contains the data to set the parameter
	 *                       value to.
	 * @param length         the number of bytes in the parameter data.
	 * @throws SQLException                    if parameterIndex does not correspond to a parameter
	 *                                         marker in the SQL statement; if a database access error occurs;
	 *                                         this method is called on a closed <code>PreparedStatement</code>;
	 *                                         if the length specified
	 *                                         is less than zero or if the number of bytes in the inputstream does not match
	 *                                         the specified length.
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
	 * @since 1.6
	 */
	@Override public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {

	}

	/**
	 * Sets the designated parameter to a <code>Reader</code> object.  The reader must contain  the number
	 * of characters specified by length otherwise a <code>SQLException</code> will be
	 * generated when the <code>PreparedStatement</code> is executed.
	 * This method differs from the <code>setCharacterStream (int, Reader, int)</code> method
	 * because it informs the driver that the parameter value should be sent to
	 * the server as a <code>NCLOB</code>.  When the <code>setCharacterStream</code> method is used, the
	 * driver may have to do extra work to determine whether the parameter
	 * data should be sent to the server as a <code>LONGNVARCHAR</code> or a <code>NCLOB</code>
	 *
	 * @param parameterIndex index of the first parameter is 1, the second is 2, ...
	 * @param reader         An object that contains the data to set the parameter value to.
	 * @param length         the number of characters in the parameter data.
	 * @throws SQLException                    if parameterIndex does not correspond to a parameter
	 *                                         marker in the SQL statement; if the length specified is less than zero;
	 *                                         if the driver does not support national character sets;
	 *                                         if the driver can detect that a data conversion
	 *                                         error could occur;  if a database access error occurs or
	 *                                         this method is called on a closed <code>PreparedStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
	 * @since 1.6
	 */
	@Override public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given <code>java.sql.SQLXML</code> object.
	 * The driver converts this to an
	 * SQL <code>XML</code> value when it sends it to the database.
	 * <p>
	 *
	 * @param parameterIndex index of the first parameter is 1, the second is 2, ...
	 * @param xmlObject      a <code>SQLXML</code> object that maps an SQL <code>XML</code> value
	 * @throws SQLException                    if parameterIndex does not correspond to a parameter
	 *                                         marker in the SQL statement; if a database access error occurs;
	 *                                         this method is called on a closed <code>PreparedStatement</code>
	 *                                         or the <code>java.xml.transform.Result</code>,
	 *                                         <code>Writer</code> or <code>OutputStream</code> has not been closed for
	 *                                         the <code>SQLXML</code> object
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
	 * @since 1.6
	 */
	@Override public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {

	}

	/**
	 * <p>Sets the value of the designated parameter with the given object.
	 * <p>
	 * If the second argument is an <code>InputStream</code> then the stream must contain
	 * the number of bytes specified by scaleOrLength.  If the second argument is a
	 * <code>Reader</code> then the reader must contain the number of characters specified
	 * by scaleOrLength. If these conditions are not true the driver will generate a
	 * <code>SQLException</code> when the prepared statement is executed.
	 * <p>
	 * <p>The given Java object will be converted to the given targetSqlType
	 * before being sent to the database.
	 * <p>
	 * If the object has a custom mapping (is of a class implementing the
	 * interface <code>SQLData</code>),
	 * the JDBC driver should call the method <code>SQLData.writeSQL</code> to
	 * write it to the SQL data stream.
	 * If, on the other hand, the object is of a class implementing
	 * <code>Ref</code>, <code>Blob</code>, <code>Clob</code>,  <code>NClob</code>,
	 * <code>Struct</code>, <code>java.net.URL</code>,
	 * or <code>Array</code>, the driver should pass it to the database as a
	 * value of the corresponding SQL type.
	 * <p>
	 * <p>Note that this method may be used to pass database-specific
	 * abstract data types.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, ...
	 * @param x              the object containing the input parameter value
	 * @param targetSqlType  the SQL type (as defined in java.sql.Types) to be
	 *                       sent to the database. The scale argument may further qualify this type.
	 * @param scaleOrLength  for <code>java.sql.Types.DECIMAL</code>
	 *                       or <code>java.sql.Types.NUMERIC types</code>,
	 *                       this is the number of digits after the decimal point. For
	 *                       Java Object types <code>InputStream</code> and <code>Reader</code>,
	 *                       this is the length
	 *                       of the data in the stream or reader.  For all other types,
	 *                       this value will be ignored.
	 * @throws SQLException                    if parameterIndex does not correspond to a parameter
	 *                                         marker in the SQL statement; if a database access error occurs;
	 *                                         this method is called on a closed <code>PreparedStatement</code> or
	 *                                         if the Java Object specified by x is an InputStream
	 *                                         or Reader object and the value of the scale parameter is less
	 *                                         than zero
	 * @throws SQLFeatureNotSupportedException if
	 *                                         the JDBC driver does not support the specified targetSqlType
	 * @see Types
	 */
	@Override public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given input stream, which will have
	 * the specified number of bytes.
	 * When a very large ASCII value is input to a <code>LONGVARCHAR</code>
	 * parameter, it may be more practical to send it via a
	 * <code>java.io.InputStream</code>. Data will be read from the stream
	 * as needed until end-of-file is reached.  The JDBC driver will
	 * do any necessary conversion from ASCII to the database char format.
	 * <p>
	 * <P><B>Note:</B> This stream object can either be a standard
	 * Java stream object or your own subclass that implements the
	 * standard interface.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, ...
	 * @param x              the Java input stream that contains the ASCII parameter value
	 * @param length         the number of bytes in the stream
	 * @throws SQLException if parameterIndex does not correspond to a parameter
	 *                      marker in the SQL statement; if a database access error occurs or
	 *                      this method is called on a closed <code>PreparedStatement</code>
	 * @since 1.6
	 */
	@Override public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given input stream, which will have
	 * the specified number of bytes.
	 * When a very large binary value is input to a <code>LONGVARBINARY</code>
	 * parameter, it may be more practical to send it via a
	 * <code>java.io.InputStream</code> object. The data will be read from the
	 * stream as needed until end-of-file is reached.
	 * <p>
	 * <P><B>Note:</B> This stream object can either be a standard
	 * Java stream object or your own subclass that implements the
	 * standard interface.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, ...
	 * @param x              the java input stream which contains the binary parameter value
	 * @param length         the number of bytes in the stream
	 * @throws SQLException if parameterIndex does not correspond to a parameter
	 *                      marker in the SQL statement; if a database access error occurs or
	 *                      this method is called on a closed <code>PreparedStatement</code>
	 * @since 1.6
	 */
	@Override public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given <code>Reader</code>
	 * object, which is the given number of characters long.
	 * When a very large UNICODE value is input to a <code>LONGVARCHAR</code>
	 * parameter, it may be more practical to send it via a
	 * <code>java.io.Reader</code> object. The data will be read from the stream
	 * as needed until end-of-file is reached.  The JDBC driver will
	 * do any necessary conversion from UNICODE to the database char format.
	 * <p>
	 * <P><B>Note:</B> This stream object can either be a standard
	 * Java stream object or your own subclass that implements the
	 * standard interface.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, ...
	 * @param reader         the <code>java.io.Reader</code> object that contains the
	 *                       Unicode data
	 * @param length         the number of characters in the stream
	 * @throws SQLException if parameterIndex does not correspond to a parameter
	 *                      marker in the SQL statement; if a database access error occurs or
	 *                      this method is called on a closed <code>PreparedStatement</code>
	 * @since 1.6
	 */
	@Override public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given input stream.
	 * When a very large ASCII value is input to a <code>LONGVARCHAR</code>
	 * parameter, it may be more practical to send it via a
	 * <code>java.io.InputStream</code>. Data will be read from the stream
	 * as needed until end-of-file is reached.  The JDBC driver will
	 * do any necessary conversion from ASCII to the database char format.
	 * <p>
	 * <P><B>Note:</B> This stream object can either be a standard
	 * Java stream object or your own subclass that implements the
	 * standard interface.
	 * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
	 * it might be more efficient to use a version of
	 * <code>setAsciiStream</code> which takes a length parameter.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, ...
	 * @param x              the Java input stream that contains the ASCII parameter value
	 * @throws SQLException                    if parameterIndex does not correspond to a parameter
	 *                                         marker in the SQL statement; if a database access error occurs or
	 *                                         this method is called on a closed <code>PreparedStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
	 * @since 1.6
	 */
	@Override public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given input stream.
	 * When a very large binary value is input to a <code>LONGVARBINARY</code>
	 * parameter, it may be more practical to send it via a
	 * <code>java.io.InputStream</code> object. The data will be read from the
	 * stream as needed until end-of-file is reached.
	 * <p>
	 * <P><B>Note:</B> This stream object can either be a standard
	 * Java stream object or your own subclass that implements the
	 * standard interface.
	 * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
	 * it might be more efficient to use a version of
	 * <code>setBinaryStream</code> which takes a length parameter.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, ...
	 * @param x              the java input stream which contains the binary parameter value
	 * @throws SQLException                    if parameterIndex does not correspond to a parameter
	 *                                         marker in the SQL statement; if a database access error occurs or
	 *                                         this method is called on a closed <code>PreparedStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
	 * @since 1.6
	 */
	@Override public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given <code>Reader</code>
	 * object.
	 * When a very large UNICODE value is input to a <code>LONGVARCHAR</code>
	 * parameter, it may be more practical to send it via a
	 * <code>java.io.Reader</code> object. The data will be read from the stream
	 * as needed until end-of-file is reached.  The JDBC driver will
	 * do any necessary conversion from UNICODE to the database char format.
	 * <p>
	 * <P><B>Note:</B> This stream object can either be a standard
	 * Java stream object or your own subclass that implements the
	 * standard interface.
	 * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
	 * it might be more efficient to use a version of
	 * <code>setCharacterStream</code> which takes a length parameter.
	 *
	 * @param parameterIndex the first parameter is 1, the second is 2, ...
	 * @param reader         the <code>java.io.Reader</code> object that contains the
	 *                       Unicode data
	 * @throws SQLException                    if parameterIndex does not correspond to a parameter
	 *                                         marker in the SQL statement; if a database access error occurs or
	 *                                         this method is called on a closed <code>PreparedStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
	 * @since 1.6
	 */
	@Override public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {

	}

	/**
	 * Sets the designated parameter to a <code>Reader</code> object. The
	 * <code>Reader</code> reads the data till end-of-file is reached. The
	 * driver does the necessary conversion from Java character format to
	 * the national character set in the database.
	 * <p>
	 * <P><B>Note:</B> This stream object can either be a standard
	 * Java stream object or your own subclass that implements the
	 * standard interface.
	 * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
	 * it might be more efficient to use a version of
	 * <code>setNCharacterStream</code> which takes a length parameter.
	 *
	 * @param parameterIndex of the first parameter is 1, the second is 2, ...
	 * @param value          the parameter value
	 * @throws SQLException                    if parameterIndex does not correspond to a parameter
	 *                                         marker in the SQL statement; if the driver does not support national
	 *                                         character sets;  if the driver can detect that a data conversion
	 *                                         error could occur; if a database access error occurs; or
	 *                                         this method is called on a closed <code>PreparedStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
	 * @since 1.6
	 */
	@Override public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {

	}

	/**
	 * Sets the designated parameter to a <code>Reader</code> object.
	 * This method differs from the <code>setCharacterStream (int, Reader)</code> method
	 * because it informs the driver that the parameter value should be sent to
	 * the server as a <code>CLOB</code>.  When the <code>setCharacterStream</code> method is used, the
	 * driver may have to do extra work to determine whether the parameter
	 * data should be sent to the server as a <code>LONGVARCHAR</code> or a <code>CLOB</code>
	 * <p>
	 * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
	 * it might be more efficient to use a version of
	 * <code>setClob</code> which takes a length parameter.
	 *
	 * @param parameterIndex index of the first parameter is 1, the second is 2, ...
	 * @param reader         An object that contains the data to set the parameter value to.
	 * @throws SQLException                    if parameterIndex does not correspond to a parameter
	 *                                         marker in the SQL statement; if a database access error occurs; this method is called on
	 *                                         a closed <code>PreparedStatement</code>or if parameterIndex does not correspond to a parameter
	 *                                         marker in the SQL statement
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
	 * @since 1.6
	 */
	@Override public void setClob(int parameterIndex, Reader reader) throws SQLException {

	}

	/**
	 * Sets the designated parameter to a <code>InputStream</code> object.
	 * This method differs from the <code>setBinaryStream (int, InputStream)</code>
	 * method because it informs the driver that the parameter value should be
	 * sent to the server as a <code>BLOB</code>.  When the <code>setBinaryStream</code> method is used,
	 * the driver may have to do extra work to determine whether the parameter
	 * data should be sent to the server as a <code>LONGVARBINARY</code> or a <code>BLOB</code>
	 * <p>
	 * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
	 * it might be more efficient to use a version of
	 * <code>setBlob</code> which takes a length parameter.
	 *
	 * @param parameterIndex index of the first parameter is 1,
	 *                       the second is 2, ...
	 * @param inputStream    An object that contains the data to set the parameter
	 *                       value to.
	 * @throws SQLException                    if parameterIndex does not correspond to a parameter
	 *                                         marker in the SQL statement; if a database access error occurs;
	 *                                         this method is called on a closed <code>PreparedStatement</code> or
	 *                                         if parameterIndex does not correspond
	 *                                         to a parameter marker in the SQL statement,
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
	 * @since 1.6
	 */
	@Override public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {

	}

	/**
	 * Sets the designated parameter to a <code>Reader</code> object.
	 * This method differs from the <code>setCharacterStream (int, Reader)</code> method
	 * because it informs the driver that the parameter value should be sent to
	 * the server as a <code>NCLOB</code>.  When the <code>setCharacterStream</code> method is used, the
	 * driver may have to do extra work to determine whether the parameter
	 * data should be sent to the server as a <code>LONGNVARCHAR</code> or a <code>NCLOB</code>
	 * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
	 * it might be more efficient to use a version of
	 * <code>setNClob</code> which takes a length parameter.
	 *
	 * @param parameterIndex index of the first parameter is 1, the second is 2, ...
	 * @param reader         An object that contains the data to set the parameter value to.
	 * @throws SQLException                    if parameterIndex does not correspond to a parameter
	 *                                         marker in the SQL statement;
	 *                                         if the driver does not support national character sets;
	 *                                         if the driver can detect that a data conversion
	 *                                         error could occur;  if a database access error occurs or
	 *                                         this method is called on a closed <code>PreparedStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
	 * @since 1.6
	 */
	@Override public void setNClob(int parameterIndex, Reader reader) throws SQLException {

	}

	/**
	 * Executes the given SQL statement, which returns a single
	 * <code>ResultSet</code> object.
	 * <p>
	 * <strong>Note:</strong>This method cannot be called on a
	 * <code>PreparedStatement</code> or <code>CallableStatement</code>.
	 *
	 * @param sql an SQL statement to be sent to the database, typically a
	 *            static SQL <code>SELECT</code> statement
	 * @return a <code>ResultSet</code> object that contains the data produced
	 * by the given query; never <code>null</code>
	 * @throws SQLException        if a database access error occurs,
	 *                             this method is called on a closed <code>Statement</code>, the given
	 *                             SQL statement produces anything other than a single
	 *                             <code>ResultSet</code> object, the method is called on a
	 *                             <code>PreparedStatement</code> or <code>CallableStatement</code>
	 * @throws SQLTimeoutException when the driver has determined that the
	 *                             timeout value that was specified by the {@code setQueryTimeout}
	 *                             method has been exceeded and has at least attempted to cancel
	 *                             the currently running {@code Statement}
	 */
	@Override public ResultSet executeQuery(String sql) throws SQLException {
		return null;
	}

	/**
	 * Executes the given SQL statement, which may be an <code>INSERT</code>,
	 * <code>UPDATE</code>, or <code>DELETE</code> statement or an
	 * SQL statement that returns nothing, such as an SQL DDL statement.
	 * <p>
	 * <strong>Note:</strong>This method cannot be called on a
	 * <code>PreparedStatement</code> or <code>CallableStatement</code>.
	 *
	 * @param sql an SQL Data Manipulation Language (DML) statement, such as <code>INSERT</code>, <code>UPDATE</code> or
	 *            <code>DELETE</code>; or an SQL statement that returns nothing,
	 *            such as a DDL statement.
	 * @return either (1) the row count for SQL Data Manipulation Language (DML) statements
	 * or (2) 0 for SQL statements that return nothing
	 * @throws SQLException        if a database access error occurs,
	 *                             this method is called on a closed <code>Statement</code>, the given
	 *                             SQL statement produces a <code>ResultSet</code> object, the method is called on a
	 *                             <code>PreparedStatement</code> or <code>CallableStatement</code>
	 * @throws SQLTimeoutException when the driver has determined that the
	 *                             timeout value that was specified by the {@code setQueryTimeout}
	 *                             method has been exceeded and has at least attempted to cancel
	 *                             the currently running {@code Statement}
	 */
	@Override public int executeUpdate(String sql) throws SQLException {
		return 0;
	}

	/**
	 * Releases this <code>Statement</code> object's database
	 * and JDBC resources immediately instead of waiting for
	 * this to happen when it is automatically closed.
	 * It is generally good practice to release resources as soon as
	 * you are finished with them to avoid tying up database
	 * resources.
	 * <p>
	 * Calling the method <code>close</code> on a <code>Statement</code>
	 * object that is already closed has no effect.
	 * <p>
	 * <B>Note:</B>When a <code>Statement</code> object is
	 * closed, its current <code>ResultSet</code> object, if one exists, is
	 * also closed.
	 *
	 * @throws SQLException if a database access error occurs
	 */
	@Override public void close() throws SQLException {

	}

	/**
	 * Retrieves the maximum number of bytes that can be
	 * returned for character and binary column values in a <code>ResultSet</code>
	 * object produced by this <code>Statement</code> object.
	 * This limit applies only to  <code>BINARY</code>, <code>VARBINARY</code>,
	 * <code>LONGVARBINARY</code>, <code>CHAR</code>, <code>VARCHAR</code>,
	 * <code>NCHAR</code>, <code>NVARCHAR</code>, <code>LONGNVARCHAR</code>
	 * and <code>LONGVARCHAR</code> columns.  If the limit is exceeded, the
	 * excess data is silently discarded.
	 *
	 * @return the current column size limit for columns storing character and
	 * binary values; zero means there is no limit
	 * @throws SQLException if a database access error occurs or
	 *                      this method is called on a closed <code>Statement</code>
	 * @see #setMaxFieldSize
	 */
	@Override public int getMaxFieldSize() throws SQLException {
		return 0;
	}

	/**
	 * Sets the limit for the maximum number of bytes that can be returned for
	 * character and binary column values in a <code>ResultSet</code>
	 * object produced by this <code>Statement</code> object.
	 * <p>
	 * This limit applies
	 * only to <code>BINARY</code>, <code>VARBINARY</code>,
	 * <code>LONGVARBINARY</code>, <code>CHAR</code>, <code>VARCHAR</code>,
	 * <code>NCHAR</code>, <code>NVARCHAR</code>, <code>LONGNVARCHAR</code> and
	 * <code>LONGVARCHAR</code> fields.  If the limit is exceeded, the excess data
	 * is silently discarded. For maximum portability, use values
	 * greater than 256.
	 *
	 * @param max the new column size limit in bytes; zero means there is no limit
	 * @throws SQLException if a database access error occurs,
	 *                      this method is called on a closed <code>Statement</code>
	 *                      or the condition {@code max >= 0} is not satisfied
	 * @see #getMaxFieldSize
	 */
	@Override public void setMaxFieldSize(int max) throws SQLException {

	}

	/**
	 * Retrieves the maximum number of rows that a
	 * <code>ResultSet</code> object produced by this
	 * <code>Statement</code> object can contain.  If this limit is exceeded,
	 * the excess rows are silently dropped.
	 *
	 * @return the current maximum number of rows for a <code>ResultSet</code>
	 * object produced by this <code>Statement</code> object;
	 * zero means there is no limit
	 * @throws SQLException if a database access error occurs or
	 *                      this method is called on a closed <code>Statement</code>
	 * @see #setMaxRows
	 */
	@Override public int getMaxRows() throws SQLException {
		return 0;
	}

	/**
	 * Sets the limit for the maximum number of rows that any
	 * <code>ResultSet</code> object  generated by this <code>Statement</code>
	 * object can contain to the given number.
	 * If the limit is exceeded, the excess
	 * rows are silently dropped.
	 *
	 * @param max the new max rows limit; zero means there is no limit
	 * @throws SQLException if a database access error occurs,
	 *                      this method is called on a closed <code>Statement</code>
	 *                      or the condition {@code max >= 0} is not satisfied
	 * @see #getMaxRows
	 */
	@Override public void setMaxRows(int max) throws SQLException {

	}

	/**
	 * Sets escape processing on or off.
	 * If escape scanning is on (the default), the driver will do
	 * escape substitution before sending the SQL statement to the database.
	 * <p>
	 * The {@code Connection} and {@code DataSource} property
	 * {@code escapeProcessing} may be used to change the default escape processing
	 * behavior.  A value of true (the default) enables escape Processing for
	 * all {@code Statement} objects. A value of false disables escape processing
	 * for all {@code Statement} objects.  The {@code setEscapeProcessing}
	 * method may be used to specify the escape processing behavior for an
	 * individual {@code Statement} object.
	 * <p>
	 * Note: Since prepared statements have usually been parsed prior
	 * to making this call, disabling escape processing for
	 * <code>PreparedStatements</code> objects will have no effect.
	 *
	 * @param enable <code>true</code> to enable escape processing;
	 *               <code>false</code> to disable it
	 * @throws SQLException if a database access error occurs or
	 *                      this method is called on a closed <code>Statement</code>
	 */
	@Override public void setEscapeProcessing(boolean enable) throws SQLException {

	}

	/**
	 * Retrieves the number of seconds the driver will
	 * wait for a <code>Statement</code> object to execute.
	 * If the limit is exceeded, a
	 * <code>SQLException</code> is thrown.
	 *
	 * @return the current query timeout limit in seconds; zero means there is
	 * no limit
	 * @throws SQLException if a database access error occurs or
	 *                      this method is called on a closed <code>Statement</code>
	 * @see #setQueryTimeout
	 */
	@Override public int getQueryTimeout() throws SQLException {
		return 0;
	}

	/**
	 * Sets the number of seconds the driver will wait for a
	 * <code>Statement</code> object to execute to the given number of seconds.
	 * By default there is no limit on the amount of time allowed for a running
	 * statement to complete. If the limit is exceeded, an
	 * <code>SQLTimeoutException</code> is thrown.
	 * A JDBC driver must apply this limit to the <code>execute</code>,
	 * <code>executeQuery</code> and <code>executeUpdate</code> methods.
	 * <p>
	 * <strong>Note:</strong> JDBC driver implementations may also apply this
	 * limit to {@code ResultSet} methods
	 * (consult your driver vendor documentation for details).
	 * <p>
	 * <strong>Note:</strong> In the case of {@code Statement} batching, it is
	 * implementation defined as to whether the time-out is applied to
	 * individual SQL commands added via the {@code addBatch} method or to
	 * the entire batch of SQL commands invoked by the {@code executeBatch}
	 * method (consult your driver vendor documentation for details).
	 *
	 * @param seconds the new query timeout limit in seconds; zero means
	 *                there is no limit
	 * @throws SQLException if a database access error occurs,
	 *                      this method is called on a closed <code>Statement</code>
	 *                      or the condition {@code seconds >= 0} is not satisfied
	 * @see #getQueryTimeout
	 */
	@Override public void setQueryTimeout(int seconds) throws SQLException {

	}

	/**
	 * Cancels this <code>Statement</code> object if both the DBMS and
	 * driver support aborting an SQL statement.
	 * This method can be used by one thread to cancel a statement that
	 * is being executed by another thread.
	 *
	 * @throws SQLException                    if a database access error occurs or
	 *                                         this method is called on a closed <code>Statement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method
	 */
	@Override public void cancel() throws SQLException {

	}

	/**
	 * Retrieves the first warning reported by calls on this <code>Statement</code> object.
	 * Subsequent <code>Statement</code> object warnings will be chained to this
	 * <code>SQLWarning</code> object.
	 * <p>
	 * <p>The warning chain is automatically cleared each time
	 * a statement is (re)executed. This method may not be called on a closed
	 * <code>Statement</code> object; doing so will cause an <code>SQLException</code>
	 * to be thrown.
	 * <p>
	 * <P><B>Note:</B> If you are processing a <code>ResultSet</code> object, any
	 * warnings associated with reads on that <code>ResultSet</code> object
	 * will be chained on it rather than on the <code>Statement</code>
	 * object that produced it.
	 *
	 * @return the first <code>SQLWarning</code> object or <code>null</code>
	 * if there are no warnings
	 * @throws SQLException if a database access error occurs or
	 *                      this method is called on a closed <code>Statement</code>
	 */
	@Override public SQLWarning getWarnings() throws SQLException {
		return null;
	}

	/**
	 * Clears all the warnings reported on this <code>Statement</code>
	 * object. After a call to this method,
	 * the method <code>getWarnings</code> will return
	 * <code>null</code> until a new warning is reported for this
	 * <code>Statement</code> object.
	 *
	 * @throws SQLException if a database access error occurs or
	 *                      this method is called on a closed <code>Statement</code>
	 */
	@Override public void clearWarnings() throws SQLException {

	}

	/**
	 * Sets the SQL cursor name to the given <code>String</code>, which
	 * will be used by subsequent <code>Statement</code> object
	 * <code>execute</code> methods. This name can then be
	 * used in SQL positioned update or delete statements to identify the
	 * current row in the <code>ResultSet</code> object generated by this
	 * statement.  If the database does not support positioned update/delete,
	 * this method is a noop.  To insure that a cursor has the proper isolation
	 * level to support updates, the cursor's <code>SELECT</code> statement
	 * should have the form <code>SELECT FOR UPDATE</code>.  If
	 * <code>FOR UPDATE</code> is not present, positioned updates may fail.
	 * <p>
	 * <P><B>Note:</B> By definition, the execution of positioned updates and
	 * deletes must be done by a different <code>Statement</code> object than
	 * the one that generated the <code>ResultSet</code> object being used for
	 * positioning. Also, cursor names must be unique within a connection.
	 *
	 * @param name the new cursor name, which must be unique within
	 *             a connection
	 * @throws SQLException                    if a database access error occurs or
	 *                                         this method is called on a closed <code>Statement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
	 */
	@Override public void setCursorName(String name) throws SQLException {

	}

	/**
	 * Executes the given SQL statement, which may return multiple results.
	 * In some (uncommon) situations, a single SQL statement may return
	 * multiple result sets and/or update counts.  Normally you can ignore
	 * this unless you are (1) executing a stored procedure that you know may
	 * return multiple results or (2) you are dynamically executing an
	 * unknown SQL string.
	 * <p>
	 * The <code>execute</code> method executes an SQL statement and indicates the
	 * form of the first result.  You must then use the methods
	 * <code>getResultSet</code> or <code>getUpdateCount</code>
	 * to retrieve the result, and <code>getMoreResults</code> to
	 * move to any subsequent result(s).
	 * <p>
	 * <strong>Note:</strong>This method cannot be called on a
	 * <code>PreparedStatement</code> or <code>CallableStatement</code>.
	 *
	 * @param sql any SQL statement
	 * @return <code>true</code> if the first result is a <code>ResultSet</code>
	 * object; <code>false</code> if it is an update count or there are
	 * no results
	 * @throws SQLException        if a database access error occurs,
	 *                             this method is called on a closed <code>Statement</code>,
	 *                             the method is called on a
	 *                             <code>PreparedStatement</code> or <code>CallableStatement</code>
	 * @throws SQLTimeoutException when the driver has determined that the
	 *                             timeout value that was specified by the {@code setQueryTimeout}
	 *                             method has been exceeded and has at least attempted to cancel
	 *                             the currently running {@code Statement}
	 * @see #getResultSet
	 * @see #getUpdateCount
	 * @see #getMoreResults
	 */
	@Override public boolean execute(String sql) throws SQLException {
		return false;
	}

	/**
	 * Retrieves the current result as a <code>ResultSet</code> object.
	 * This method should be called only once per result.
	 *
	 * @return the current result as a <code>ResultSet</code> object or
	 * <code>null</code> if the result is an update count or there are no more results
	 * @throws SQLException if a database access error occurs or
	 *                      this method is called on a closed <code>Statement</code>
	 * @see #execute
	 */
	@Override public ResultSet getResultSet() throws SQLException {
		return null;
	}

	/**
	 * Retrieves the current result as an update count;
	 * if the result is a <code>ResultSet</code> object or there are no more results, -1
	 * is returned. This method should be called only once per result.
	 *
	 * @return the current result as an update count; -1 if the current result is a
	 * <code>ResultSet</code> object or there are no more results
	 * @throws SQLException if a database access error occurs or
	 *                      this method is called on a closed <code>Statement</code>
	 * @see #execute
	 */
	@Override public int getUpdateCount() throws SQLException {
		return 0;
	}

	/**
	 * Moves to this <code>Statement</code> object's next result, returns
	 * <code>true</code> if it is a <code>ResultSet</code> object, and
	 * implicitly closes any current <code>ResultSet</code>
	 * object(s) obtained with the method <code>getResultSet</code>.
	 * <p>
	 * <P>There are no more results when the following is true:
	 * <PRE>{@code
	 * // stmt is a Statement object
	 * ((stmt.getMoreResults() == false) && (stmt.getUpdateCount() == -1))
	 * }</PRE>
	 *
	 * @return <code>true</code> if the next result is a <code>ResultSet</code>
	 * object; <code>false</code> if it is an update count or there are
	 * no more results
	 * @throws SQLException if a database access error occurs or
	 *                      this method is called on a closed <code>Statement</code>
	 * @see #execute
	 */
	@Override public boolean getMoreResults() throws SQLException {
		return false;
	}

	/**
	 * Gives the driver a hint as to the direction in which
	 * rows will be processed in <code>ResultSet</code>
	 * objects created using this <code>Statement</code> object.  The
	 * default value is <code>ResultSet.FETCH_FORWARD</code>.
	 * <p>
	 * Note that this method sets the default fetch direction for
	 * result sets generated by this <code>Statement</code> object.
	 * Each result set has its own methods for getting and setting
	 * its own fetch direction.
	 *
	 * @param direction the initial direction for processing rows
	 * @throws SQLException if a database access error occurs,
	 *                      this method is called on a closed <code>Statement</code>
	 *                      or the given direction
	 *                      is not one of <code>ResultSet.FETCH_FORWARD</code>,
	 *                      <code>ResultSet.FETCH_REVERSE</code>, or <code>ResultSet.FETCH_UNKNOWN</code>
	 * @see #getFetchDirection
	 * @since 1.2
	 */
	@Override public void setFetchDirection(int direction) throws SQLException {

	}

	/**
	 * Retrieves the direction for fetching rows from
	 * database tables that is the default for result sets
	 * generated from this <code>Statement</code> object.
	 * If this <code>Statement</code> object has not set
	 * a fetch direction by calling the method <code>setFetchDirection</code>,
	 * the return value is implementation-specific.
	 *
	 * @return the default fetch direction for result sets generated
	 * from this <code>Statement</code> object
	 * @throws SQLException if a database access error occurs or
	 *                      this method is called on a closed <code>Statement</code>
	 * @see #setFetchDirection
	 * @since 1.2
	 */
	@Override public int getFetchDirection() throws SQLException {
		return 0;
	}

	/**
	 * Gives the JDBC driver a hint as to the number of rows that should
	 * be fetched from the database when more rows are needed for
	 * <code>ResultSet</code> objects generated by this <code>Statement</code>.
	 * If the value specified is zero, then the hint is ignored.
	 * The default value is zero.
	 *
	 * @param rows the number of rows to fetch
	 * @throws SQLException if a database access error occurs,
	 *                      this method is called on a closed <code>Statement</code> or the
	 *                      condition {@code rows >= 0} is not satisfied.
	 * @see #getFetchSize
	 * @since 1.2
	 */
	@Override public void setFetchSize(int rows) throws SQLException {

	}

	/**
	 * Retrieves the number of result set rows that is the default
	 * fetch size for <code>ResultSet</code> objects
	 * generated from this <code>Statement</code> object.
	 * If this <code>Statement</code> object has not set
	 * a fetch size by calling the method <code>setFetchSize</code>,
	 * the return value is implementation-specific.
	 *
	 * @return the default fetch size for result sets generated
	 * from this <code>Statement</code> object
	 * @throws SQLException if a database access error occurs or
	 *                      this method is called on a closed <code>Statement</code>
	 * @see #setFetchSize
	 * @since 1.2
	 */
	@Override public int getFetchSize() throws SQLException {
		return 0;
	}

	/**
	 * Retrieves the result set concurrency for <code>ResultSet</code> objects
	 * generated by this <code>Statement</code> object.
	 *
	 * @return either <code>ResultSet.CONCUR_READ_ONLY</code> or
	 * <code>ResultSet.CONCUR_UPDATABLE</code>
	 * @throws SQLException if a database access error occurs or
	 *                      this method is called on a closed <code>Statement</code>
	 * @since 1.2
	 */
	@Override public int getResultSetConcurrency() throws SQLException {
		return 0;
	}

	/**
	 * Retrieves the result set type for <code>ResultSet</code> objects
	 * generated by this <code>Statement</code> object.
	 *
	 * @return one of <code>ResultSet.TYPE_FORWARD_ONLY</code>,
	 * <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or
	 * <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
	 * @throws SQLException if a database access error occurs or
	 *                      this method is called on a closed <code>Statement</code>
	 * @since 1.2
	 */
	@Override public int getResultSetType() throws SQLException {
		return 0;
	}

	/**
	 * Adds the given SQL command to the current list of commands for this
	 * <code>Statement</code> object. The commands in this list can be
	 * executed as a batch by calling the method <code>executeBatch</code>.
	 * <p>
	 * <strong>Note:</strong>This method cannot be called on a
	 * <code>PreparedStatement</code> or <code>CallableStatement</code>.
	 *
	 * @param sql typically this is a SQL <code>INSERT</code> or
	 *            <code>UPDATE</code> statement
	 * @throws SQLException if a database access error occurs,
	 *                      this method is called on a closed <code>Statement</code>, the
	 *                      driver does not support batch updates, the method is called on a
	 *                      <code>PreparedStatement</code> or <code>CallableStatement</code>
	 * @see #executeBatch
	 * @see DatabaseMetaData#supportsBatchUpdates
	 * @since 1.2
	 */
	@Override public void addBatch(String sql) throws SQLException {

	}

	/**
	 * Empties this <code>Statement</code> object's current list of
	 * SQL commands.
	 * <p>
	 *
	 * @throws SQLException if a database access error occurs,
	 *                      this method is called on a closed <code>Statement</code> or the
	 *                      driver does not support batch updates
	 * @see #addBatch
	 * @see DatabaseMetaData#supportsBatchUpdates
	 * @since 1.2
	 */
	@Override public void clearBatch() throws SQLException {

	}

	/**
	 * Submits a batch of commands to the database for execution and
	 * if all commands execute successfully, returns an array of update counts.
	 * The <code>int</code> elements of the array that is returned are ordered
	 * to correspond to the commands in the batch, which are ordered
	 * according to the order in which they were added to the batch.
	 * The elements in the array returned by the method <code>executeBatch</code>
	 * may be one of the following:
	 * <OL>
	 * <LI>A number greater than or equal to zero -- indicates that the
	 * command was processed successfully and is an update count giving the
	 * number of rows in the database that were affected by the command's
	 * execution
	 * <LI>A value of <code>SUCCESS_NO_INFO</code> -- indicates that the command was
	 * processed successfully but that the number of rows affected is
	 * unknown
	 * <p>
	 * If one of the commands in a batch update fails to execute properly,
	 * this method throws a <code>BatchUpdateException</code>, and a JDBC
	 * driver may or may not continue to process the remaining commands in
	 * the batch.  However, the driver's behavior must be consistent with a
	 * particular DBMS, either always continuing to process commands or never
	 * continuing to process commands.  If the driver continues processing
	 * after a failure, the array returned by the method
	 * <code>BatchUpdateException.getUpdateCounts</code>
	 * will contain as many elements as there are commands in the batch, and
	 * at least one of the elements will be the following:
	 * <p>
	 * <LI>A value of <code>EXECUTE_FAILED</code> -- indicates that the command failed
	 * to execute successfully and occurs only if a driver continues to
	 * process commands after a command fails
	 * </OL>
	 * <p>
	 * The possible implementations and return values have been modified in
	 * the Java 2 SDK, Standard Edition, version 1.3 to
	 * accommodate the option of continuing to process commands in a batch
	 * update after a <code>BatchUpdateException</code> object has been thrown.
	 *
	 * @return an array of update counts containing one element for each
	 * command in the batch.  The elements of the array are ordered according
	 * to the order in which commands were added to the batch.
	 * @throws SQLException        if a database access error occurs,
	 *                             this method is called on a closed <code>Statement</code> or the
	 *                             driver does not support batch statements. Throws {@link BatchUpdateException}
	 *                             (a subclass of <code>SQLException</code>) if one of the commands sent to the
	 *                             database fails to execute properly or attempts to return a result set.
	 * @throws SQLTimeoutException when the driver has determined that the
	 *                             timeout value that was specified by the {@code setQueryTimeout}
	 *                             method has been exceeded and has at least attempted to cancel
	 *                             the currently running {@code Statement}
	 * @see #addBatch
	 * @see DatabaseMetaData#supportsBatchUpdates
	 * @since 1.2
	 */
	@Override public int[] executeBatch() throws SQLException {
		return new int[0];
	}

	/**
	 * Retrieves the <code>Connection</code> object
	 * that produced this <code>Statement</code> object.
	 *
	 * @return the connection that produced this statement
	 * @throws SQLException if a database access error occurs or
	 *                      this method is called on a closed <code>Statement</code>
	 * @since 1.2
	 */
	@Override public Connection getConnection() throws SQLException {
		return null;
	}

	/**
	 * Moves to this <code>Statement</code> object's next result, deals with
	 * any current <code>ResultSet</code> object(s) according  to the instructions
	 * specified by the given flag, and returns
	 * <code>true</code> if the next result is a <code>ResultSet</code> object.
	 * <p>
	 * <P>There are no more results when the following is true:
	 * <PRE>{@code
	 * // stmt is a Statement object
	 * ((stmt.getMoreResults(current) == false) && (stmt.getUpdateCount() == -1))
	 * }</PRE>
	 *
	 * @param current one of the following <code>Statement</code>
	 *                constants indicating what should happen to current
	 *                <code>ResultSet</code> objects obtained using the method
	 *                <code>getResultSet</code>:
	 *                <code>Statement.CLOSE_CURRENT_RESULT</code>,
	 *                <code>Statement.KEEP_CURRENT_RESULT</code>, or
	 *                <code>Statement.CLOSE_ALL_RESULTS</code>
	 * @return <code>true</code> if the next result is a <code>ResultSet</code>
	 * object; <code>false</code> if it is an update count or there are no
	 * more results
	 * @throws SQLException                    if a database access error occurs,
	 *                                         this method is called on a closed <code>Statement</code> or the argument
	 *                                         supplied is not one of the following:
	 *                                         <code>Statement.CLOSE_CURRENT_RESULT</code>,
	 *                                         <code>Statement.KEEP_CURRENT_RESULT</code> or
	 *                                         <code>Statement.CLOSE_ALL_RESULTS</code>
	 * @throws SQLFeatureNotSupportedException if
	 *                                         <code>DatabaseMetaData.supportsMultipleOpenResults</code> returns
	 *                                         <code>false</code> and either
	 *                                         <code>Statement.KEEP_CURRENT_RESULT</code> or
	 *                                         <code>Statement.CLOSE_ALL_RESULTS</code> are supplied as
	 *                                         the argument.
	 * @see #execute
	 * @since 1.4
	 */
	@Override public boolean getMoreResults(int current) throws SQLException {
		return false;
	}

	/**
	 * Retrieves any auto-generated keys created as a result of executing this
	 * <code>Statement</code> object. If this <code>Statement</code> object did
	 * not generate any keys, an empty <code>ResultSet</code>
	 * object is returned.
	 * <p>
	 * <p><B>Note:</B>If the columns which represent the auto-generated keys were not specified,
	 * the JDBC driver implementation will determine the columns which best represent the auto-generated keys.
	 *
	 * @return a <code>ResultSet</code> object containing the auto-generated key(s)
	 * generated by the execution of this <code>Statement</code> object
	 * @throws SQLException                    if a database access error occurs or
	 *                                         this method is called on a closed <code>Statement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
	 * @since 1.4
	 */
	@Override public ResultSet getGeneratedKeys() throws SQLException {
		return null;
	}

	/**
	 * Executes the given SQL statement and signals the driver with the
	 * given flag about whether the
	 * auto-generated keys produced by this <code>Statement</code> object
	 * should be made available for retrieval.  The driver will ignore the
	 * flag if the SQL statement
	 * is not an <code>INSERT</code> statement, or an SQL statement able to return
	 * auto-generated keys (the list of such statements is vendor-specific).
	 * <p>
	 * <strong>Note:</strong>This method cannot be called on a
	 * <code>PreparedStatement</code> or <code>CallableStatement</code>.
	 *
	 * @param sql               an SQL Data Manipulation Language (DML) statement, such as <code>INSERT</code>, <code>UPDATE</code> or
	 *                          <code>DELETE</code>; or an SQL statement that returns nothing,
	 *                          such as a DDL statement.
	 * @param autoGeneratedKeys a flag indicating whether auto-generated keys
	 *                          should be made available for retrieval;
	 *                          one of the following constants:
	 *                          <code>Statement.RETURN_GENERATED_KEYS</code>
	 *                          <code>Statement.NO_GENERATED_KEYS</code>
	 * @return either (1) the row count for SQL Data Manipulation Language (DML) statements
	 * or (2) 0 for SQL statements that return nothing
	 * @throws SQLException                    if a database access error occurs,
	 *                                         this method is called on a closed <code>Statement</code>, the given
	 *                                         SQL statement returns a <code>ResultSet</code> object,
	 *                                         the given constant is not one of those allowed, the method is called on a
	 *                                         <code>PreparedStatement</code> or <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method with a constant of Statement.RETURN_GENERATED_KEYS
	 * @throws SQLTimeoutException             when the driver has determined that the
	 *                                         timeout value that was specified by the {@code setQueryTimeout}
	 *                                         method has been exceeded and has at least attempted to cancel
	 *                                         the currently running {@code Statement}
	 * @since 1.4
	 */
	@Override public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
		return 0;
	}

	/**
	 * Executes the given SQL statement and signals the driver that the
	 * auto-generated keys indicated in the given array should be made available
	 * for retrieval.   This array contains the indexes of the columns in the
	 * target table that contain the auto-generated keys that should be made
	 * available. The driver will ignore the array if the SQL statement
	 * is not an <code>INSERT</code> statement, or an SQL statement able to return
	 * auto-generated keys (the list of such statements is vendor-specific).
	 * <p>
	 * <strong>Note:</strong>This method cannot be called on a
	 * <code>PreparedStatement</code> or <code>CallableStatement</code>.
	 *
	 * @param sql           an SQL Data Manipulation Language (DML) statement, such as <code>INSERT</code>, <code>UPDATE</code> or
	 *                      <code>DELETE</code>; or an SQL statement that returns nothing,
	 *                      such as a DDL statement.
	 * @param columnIndexes an array of column indexes indicating the columns
	 *                      that should be returned from the inserted row
	 * @return either (1) the row count for SQL Data Manipulation Language (DML) statements
	 * or (2) 0 for SQL statements that return nothing
	 * @throws SQLException                    if a database access error occurs,
	 *                                         this method is called on a closed <code>Statement</code>, the SQL
	 *                                         statement returns a <code>ResultSet</code> object,the second argument
	 *                                         supplied to this method is not an
	 *                                         <code>int</code> array whose elements are valid column indexes, the method is called on a
	 *                                         <code>PreparedStatement</code> or <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
	 * @throws SQLTimeoutException             when the driver has determined that the
	 *                                         timeout value that was specified by the {@code setQueryTimeout}
	 *                                         method has been exceeded and has at least attempted to cancel
	 *                                         the currently running {@code Statement}
	 * @since 1.4
	 */
	@Override public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
		return 0;
	}

	/**
	 * Executes the given SQL statement and signals the driver that the
	 * auto-generated keys indicated in the given array should be made available
	 * for retrieval.   This array contains the names of the columns in the
	 * target table that contain the auto-generated keys that should be made
	 * available. The driver will ignore the array if the SQL statement
	 * is not an <code>INSERT</code> statement, or an SQL statement able to return
	 * auto-generated keys (the list of such statements is vendor-specific).
	 * <p>
	 * <strong>Note:</strong>This method cannot be called on a
	 * <code>PreparedStatement</code> or <code>CallableStatement</code>.
	 *
	 * @param sql         an SQL Data Manipulation Language (DML) statement, such as <code>INSERT</code>, <code>UPDATE</code> or
	 *                    <code>DELETE</code>; or an SQL statement that returns nothing,
	 *                    such as a DDL statement.
	 * @param columnNames an array of the names of the columns that should be
	 *                    returned from the inserted row
	 * @return either the row count for <code>INSERT</code>, <code>UPDATE</code>,
	 * or <code>DELETE</code> statements, or 0 for SQL statements
	 * that return nothing
	 * @throws SQLException                    if a database access error occurs,
	 *                                         this method is called on a closed <code>Statement</code>, the SQL
	 *                                         statement returns a <code>ResultSet</code> object, the
	 *                                         second argument supplied to this method is not a <code>String</code> array
	 *                                         whose elements are valid column names, the method is called on a
	 *                                         <code>PreparedStatement</code> or <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
	 * @throws SQLTimeoutException             when the driver has determined that the
	 *                                         timeout value that was specified by the {@code setQueryTimeout}
	 *                                         method has been exceeded and has at least attempted to cancel
	 *                                         the currently running {@code Statement}
	 * @since 1.4
	 */
	@Override public int executeUpdate(String sql, String[] columnNames) throws SQLException {
		return 0;
	}

	/**
	 * Executes the given SQL statement, which may return multiple results,
	 * and signals the driver that any
	 * auto-generated keys should be made available
	 * for retrieval.  The driver will ignore this signal if the SQL statement
	 * is not an <code>INSERT</code> statement, or an SQL statement able to return
	 * auto-generated keys (the list of such statements is vendor-specific).
	 * <p>
	 * In some (uncommon) situations, a single SQL statement may return
	 * multiple result sets and/or update counts.  Normally you can ignore
	 * this unless you are (1) executing a stored procedure that you know may
	 * return multiple results or (2) you are dynamically executing an
	 * unknown SQL string.
	 * <p>
	 * The <code>execute</code> method executes an SQL statement and indicates the
	 * form of the first result.  You must then use the methods
	 * <code>getResultSet</code> or <code>getUpdateCount</code>
	 * to retrieve the result, and <code>getMoreResults</code> to
	 * move to any subsequent result(s).
	 * <p>
	 * <strong>Note:</strong>This method cannot be called on a
	 * <code>PreparedStatement</code> or <code>CallableStatement</code>.
	 *
	 * @param sql               any SQL statement
	 * @param autoGeneratedKeys a constant indicating whether auto-generated
	 *                          keys should be made available for retrieval using the method
	 *                          <code>getGeneratedKeys</code>; one of the following constants:
	 *                          <code>Statement.RETURN_GENERATED_KEYS</code> or
	 *                          <code>Statement.NO_GENERATED_KEYS</code>
	 * @return <code>true</code> if the first result is a <code>ResultSet</code>
	 * object; <code>false</code> if it is an update count or there are
	 * no results
	 * @throws SQLException                    if a database access error occurs,
	 *                                         this method is called on a closed <code>Statement</code>, the second
	 *                                         parameter supplied to this method is not
	 *                                         <code>Statement.RETURN_GENERATED_KEYS</code> or
	 *                                         <code>Statement.NO_GENERATED_KEYS</code>,
	 *                                         the method is called on a
	 *                                         <code>PreparedStatement</code> or <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
	 *                                         this method with a constant of Statement.RETURN_GENERATED_KEYS
	 * @throws SQLTimeoutException             when the driver has determined that the
	 *                                         timeout value that was specified by the {@code setQueryTimeout}
	 *                                         method has been exceeded and has at least attempted to cancel
	 *                                         the currently running {@code Statement}
	 * @see #getResultSet
	 * @see #getUpdateCount
	 * @see #getMoreResults
	 * @see #getGeneratedKeys
	 * @since 1.4
	 */
	@Override public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
		return false;
	}

	/**
	 * Executes the given SQL statement, which may return multiple results,
	 * and signals the driver that the
	 * auto-generated keys indicated in the given array should be made available
	 * for retrieval.  This array contains the indexes of the columns in the
	 * target table that contain the auto-generated keys that should be made
	 * available.  The driver will ignore the array if the SQL statement
	 * is not an <code>INSERT</code> statement, or an SQL statement able to return
	 * auto-generated keys (the list of such statements is vendor-specific).
	 * <p>
	 * Under some (uncommon) situations, a single SQL statement may return
	 * multiple result sets and/or update counts.  Normally you can ignore
	 * this unless you are (1) executing a stored procedure that you know may
	 * return multiple results or (2) you are dynamically executing an
	 * unknown SQL string.
	 * <p>
	 * The <code>execute</code> method executes an SQL statement and indicates the
	 * form of the first result.  You must then use the methods
	 * <code>getResultSet</code> or <code>getUpdateCount</code>
	 * to retrieve the result, and <code>getMoreResults</code> to
	 * move to any subsequent result(s).
	 * <p>
	 * <strong>Note:</strong>This method cannot be called on a
	 * <code>PreparedStatement</code> or <code>CallableStatement</code>.
	 *
	 * @param sql           any SQL statement
	 * @param columnIndexes an array of the indexes of the columns in the
	 *                      inserted row that should be  made available for retrieval by a
	 *                      call to the method <code>getGeneratedKeys</code>
	 * @return <code>true</code> if the first result is a <code>ResultSet</code>
	 * object; <code>false</code> if it is an update count or there
	 * are no results
	 * @throws SQLException                    if a database access error occurs,
	 *                                         this method is called on a closed <code>Statement</code>, the
	 *                                         elements in the <code>int</code> array passed to this method
	 *                                         are not valid column indexes, the method is called on a
	 *                                         <code>PreparedStatement</code> or <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
	 * @throws SQLTimeoutException             when the driver has determined that the
	 *                                         timeout value that was specified by the {@code setQueryTimeout}
	 *                                         method has been exceeded and has at least attempted to cancel
	 *                                         the currently running {@code Statement}
	 * @see #getResultSet
	 * @see #getUpdateCount
	 * @see #getMoreResults
	 * @since 1.4
	 */
	@Override public boolean execute(String sql, int[] columnIndexes) throws SQLException {
		return false;
	}

	/**
	 * Executes the given SQL statement, which may return multiple results,
	 * and signals the driver that the
	 * auto-generated keys indicated in the given array should be made available
	 * for retrieval. This array contains the names of the columns in the
	 * target table that contain the auto-generated keys that should be made
	 * available.  The driver will ignore the array if the SQL statement
	 * is not an <code>INSERT</code> statement, or an SQL statement able to return
	 * auto-generated keys (the list of such statements is vendor-specific).
	 * <p>
	 * In some (uncommon) situations, a single SQL statement may return
	 * multiple result sets and/or update counts.  Normally you can ignore
	 * this unless you are (1) executing a stored procedure that you know may
	 * return multiple results or (2) you are dynamically executing an
	 * unknown SQL string.
	 * <p>
	 * The <code>execute</code> method executes an SQL statement and indicates the
	 * form of the first result.  You must then use the methods
	 * <code>getResultSet</code> or <code>getUpdateCount</code>
	 * to retrieve the result, and <code>getMoreResults</code> to
	 * move to any subsequent result(s).
	 * <p>
	 * <strong>Note:</strong>This method cannot be called on a
	 * <code>PreparedStatement</code> or <code>CallableStatement</code>.
	 *
	 * @param sql         any SQL statement
	 * @param columnNames an array of the names of the columns in the inserted
	 *                    row that should be made available for retrieval by a call to the
	 *                    method <code>getGeneratedKeys</code>
	 * @return <code>true</code> if the next result is a <code>ResultSet</code>
	 * object; <code>false</code> if it is an update count or there
	 * are no more results
	 * @throws SQLException                    if a database access error occurs,
	 *                                         this method is called on a closed <code>Statement</code>,the
	 *                                         elements of the <code>String</code> array passed to this
	 *                                         method are not valid column names, the method is called on a
	 *                                         <code>PreparedStatement</code> or <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
	 * @throws SQLTimeoutException             when the driver has determined that the
	 *                                         timeout value that was specified by the {@code setQueryTimeout}
	 *                                         method has been exceeded and has at least attempted to cancel
	 *                                         the currently running {@code Statement}
	 * @see #getResultSet
	 * @see #getUpdateCount
	 * @see #getMoreResults
	 * @see #getGeneratedKeys
	 * @since 1.4
	 */
	@Override public boolean execute(String sql, String[] columnNames) throws SQLException {
		return false;
	}

	/**
	 * Retrieves the result set holdability for <code>ResultSet</code> objects
	 * generated by this <code>Statement</code> object.
	 *
	 * @return either <code>ResultSet.HOLD_CURSORS_OVER_COMMIT</code> or
	 * <code>ResultSet.CLOSE_CURSORS_AT_COMMIT</code>
	 * @throws SQLException if a database access error occurs or
	 *                      this method is called on a closed <code>Statement</code>
	 * @since 1.4
	 */
	@Override public int getResultSetHoldability() throws SQLException {
		return 0;
	}

	/**
	 * Retrieves whether this <code>Statement</code> object has been closed. A <code>Statement</code> is closed if the
	 * method close has been called on it, or if it is automatically closed.
	 *
	 * @return true if this <code>Statement</code> object is closed; false if it is still open
	 * @throws SQLException if a database access error occurs
	 * @since 1.6
	 */
	@Override public boolean isClosed() throws SQLException {
		return false;
	}

	/**
	 * Requests that a <code>Statement</code> be pooled or not pooled.  The value
	 * specified is a hint to the statement pool implementation indicating
	 * whether the application wants the statement to be pooled.  It is up to
	 * the statement pool manager as to whether the hint is used.
	 * <p>
	 * The poolable value of a statement is applicable to both internal
	 * statement caches implemented by the driver and external statement caches
	 * implemented by application servers and other applications.
	 * <p>
	 * By default, a <code>Statement</code> is not poolable when created, and
	 * a <code>PreparedStatement</code> and <code>CallableStatement</code>
	 * are poolable when created.
	 * <p>
	 *
	 * @param poolable requests that the statement be pooled if true and
	 *                 that the statement not be pooled if false
	 *                 <p>
	 * @throws SQLException if this method is called on a closed
	 *                      <code>Statement</code>
	 *                      <p>
	 * @since 1.6
	 */
	@Override public void setPoolable(boolean poolable) throws SQLException {

	}

	/**
	 * Returns a  value indicating whether the <code>Statement</code>
	 * is poolable or not.
	 * <p>
	 *
	 * @return <code>true</code> if the <code>Statement</code>
	 * is poolable; <code>false</code> otherwise
	 * <p>
	 * @throws SQLException if this method is called on a closed
	 *                      <code>Statement</code>
	 *                      <p>
	 * @see java.sql.Statement#setPoolable(boolean) setPoolable(boolean)
	 * @since 1.6
	 * <p>
	 */
	@Override public boolean isPoolable() throws SQLException {
		return false;
	}

	/**
	 * Specifies that this {@code Statement} will be closed when all its
	 * dependent result sets are closed. If execution of the {@code Statement}
	 * does not produce any result sets, this method has no effect.
	 * <p>
	 * <strong>Note:</strong> Multiple calls to {@code closeOnCompletion} do
	 * not toggle the effect on this {@code Statement}. However, a call to
	 * {@code closeOnCompletion} does effect both the subsequent execution of
	 * statements, and statements that currently have open, dependent,
	 * result sets.
	 *
	 * @throws SQLException if this method is called on a closed
	 *                      {@code Statement}
	 * @since 1.7
	 */
	@Override public void closeOnCompletion() throws SQLException {

	}

	/**
	 * Returns a value indicating whether this {@code Statement} will be
	 * closed when all its dependent result sets are closed.
	 *
	 * @return {@code true} if the {@code Statement} will be closed when all
	 * of its dependent result sets are closed; {@code false} otherwise
	 * @throws SQLException if this method is called on a closed
	 *                      {@code Statement}
	 * @since 1.7
	 */
	@Override public boolean isCloseOnCompletion() throws SQLException {
		return false;
	}

	/**
	 * Returns an object that implements the given interface to allow access to
	 * non-standard methods, or standard methods not exposed by the proxy.
	 * <p>
	 * If the receiver implements the interface then the result is the receiver
	 * or a proxy for the receiver. If the receiver is a wrapper
	 * and the wrapped object implements the interface then the result is the
	 * wrapped object or a proxy for the wrapped object. Otherwise return the
	 * the result of calling <code>unwrap</code> recursively on the wrapped object
	 * or a proxy for that result. If the receiver is not a
	 * wrapper and does not implement the interface, then an <code>SQLException</code> is thrown.
	 *
	 * @param iface A Class defining an interface that the result must implement.
	 * @return an object that implements the interface. May be a proxy for the actual implementing object.
	 * @throws SQLException If no object found that implements the interface
	 * @since 1.6
	 */
	@Override public <T> T unwrap(Class<T> iface) throws SQLException {
		return null;
	}

	/**
	 * Returns true if this either implements the interface argument or is directly or indirectly a wrapper
	 * for an object that does. Returns false otherwise. If this implements the interface then return true,
	 * else if this is a wrapper then return the result of recursively calling <code>isWrapperFor</code> on the wrapped
	 * object. If this does not implement the interface and is not a wrapper, return false.
	 * This method should be implemented as a low-cost operation compared to <code>unwrap</code> so that
	 * callers can use this method to avoid expensive <code>unwrap</code> calls that may fail. If this method
	 * returns true then calling <code>unwrap</code> with the same argument should succeed.
	 *
	 * @param iface a Class defining an interface.
	 * @return true if this implements the interface or directly or indirectly wraps an object that does.
	 * @throws SQLException if an error occurs while determining whether this is a wrapper
	 *                      for an object with the given interface.
	 * @since 1.6
	 */
	@Override public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return false;
	}
}
