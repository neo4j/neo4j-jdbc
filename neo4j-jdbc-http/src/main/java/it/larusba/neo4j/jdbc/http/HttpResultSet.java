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
package it.larusba.neo4j.jdbc.http;

import it.larusba.neo4j.jdbc.Array;
import it.larusba.neo4j.jdbc.ResultSet;
import it.larusba.neo4j.jdbc.ResultSetMetaData;

import java.sql.SQLException;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class HttpResultSet extends ResultSet implements Loggable {

    @Override
    public boolean next() throws SQLException {
        return false;
    }

    @Override
    public void close() throws SQLException {

    }

	@Override public boolean wasNull() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override
	public String getString(int columnIndex) throws SQLException {
		return null;
	}

    @Override
    public boolean getBoolean(int columnIndex) throws SQLException {
        return false;
    }

    @Override
    public short getShort(int columnIndex) throws SQLException {
        return 0;
    }

    @Override
    public int getInt(int columnIndex) throws SQLException {
        return 0;
    }

    @Override
    public long getLong(int columnIndex) throws SQLException {
        return 0;
    }

    @Override
    public float getFloat(int columnIndex) throws SQLException {
        return 0;
    }

    @Override
    public double getDouble(int columnIndex) throws SQLException {
        return 0;
    }

    @Override
    public Array getArray(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public Array getArray(String columnLabel) throws SQLException {
        return null;
    }

    @Override
    public String getString(String columnLabel) throws SQLException {
        return null;
    }

    @Override
    public boolean getBoolean(String columnLabel) throws SQLException {
        return false;
    }

    @Override
    public short getShort(String columnLabel) throws SQLException {
        return 0;
    }

    @Override
    public int getInt(String columnLabel) throws SQLException {
        return 0;
    }

    @Override
    public long getLong(String columnLabel) throws SQLException {
        return 0;
    }

    @Override
    public float getFloat(String columnLabel) throws SQLException {
        return 0;
    }

    @Override
    public double getDouble(String columnLabel) throws SQLException {
        return 0;
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return null;
    }

    @Override
    public Object getObject(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public Object getObject(String columnLabel) throws SQLException {
        return null;
    }

    @Override
    public int findColumn(String columnLabel) throws SQLException {
        return 0;
    }

    @Override
    public int getType() throws SQLException {
        return 0;
    }

    @Override
    public int getConcurrency() throws SQLException {
        return 0;
    }

    @Override
    public int getHoldability() throws SQLException {
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
