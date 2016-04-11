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
 * Created on 23/03/16
 */
package it.larusba.neo4j.jdbc.http;

import it.larusba.neo4j.jdbc.ParameterMetaData;
import it.larusba.neo4j.jdbc.PreparedStatement;
import it.larusba.neo4j.jdbc.ResultSetMetaData;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class HttpPreparedStatement extends PreparedStatement implements Loggable {

	@Override
	public ResultSet executeQuery() throws SQLException {
		return null;
	}

	@Override public int executeUpdate() throws SQLException {
		return 0;
	}

	@Override
	public void setNull(int parameterIndex, int sqlType) throws SQLException {

	}

	@Override
	public void setBoolean(int parameterIndex, boolean x) throws SQLException {

	}

	@Override
	public void setShort(int parameterIndex, short x) throws SQLException {

	}

	@Override
	public void setInt(int parameterIndex, int x) throws SQLException {

	}

	@Override
	public void setLong(int parameterIndex, long x) throws SQLException {

	}

	@Override
	public void setFloat(int parameterIndex, float x) throws SQLException {

	}

	@Override
	public void setDouble(int parameterIndex, double x) throws SQLException {

	}

	@Override
	public void setString(int parameterIndex, String x) throws SQLException {

	}

	@Override
	public void clearParameters() throws SQLException {

	}

	@Override public boolean execute() throws SQLException {
		return false;
	}

	@Override public ResultSetMetaData getMetaData() throws SQLException {
		return null;
	}

	@Override
	public ParameterMetaData getParameterMetaData() throws SQLException {
		return null;
	}

	@Override
	public void close() throws SQLException {

	}

	@Override public ResultSet getResultSet() throws SQLException {
		return null;
	}

	@Override public int getUpdateCount() throws SQLException {
		return 0;
	}

	@Override
	public int getResultSetConcurrency() throws SQLException {
		return 0;
	}

	@Override
	public int getResultSetType() throws SQLException {
		return 0;
	}

	@Override
	public int getResultSetHoldability() throws SQLException {
		return 0;
	}

	@Override
	public boolean isClosed() throws SQLException {
		return false;
	}

	@Override
	public boolean isLoggable() {
		return false;
	}

	@Override
	public void setLoggable(boolean loggable) {

	}
}
