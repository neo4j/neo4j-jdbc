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
 * Created on 19/02/16
 */
package it.larusba.neo4j.jdbc.http;

import it.larusba.neo4j.jdbc.Statement;

import java.sql.ResultSet;
import java.sql.SQLException;

public class HttpStatement extends Statement implements Loggable {

    @Override
    public ResultSet executeQuery(String cypher) throws SQLException {
        return null;
    }

    @Override
    public int executeUpdate(String cypher) throws SQLException {
        return 0;
    }

    @Override
    public void close() throws SQLException {

    }

	@Override public int getMaxRows() throws SQLException {
		return 0;
	}

	@Override public void setMaxRows(int max) throws SQLException {

	}

	@Override public boolean execute(String sql) throws SQLException {
		return false;
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
