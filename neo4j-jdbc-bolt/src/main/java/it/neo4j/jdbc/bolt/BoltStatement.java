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
package it.neo4j.jdbc.bolt;

import it.neo4j.jdbc.Statement;
import org.mockito.Mockito;
import org.neo4j.driver.v1.ResultCursor;
import org.neo4j.driver.v1.Transaction;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltStatement extends Statement {

	private BoltConnection connection;
	private Transaction    transaction;
	private boolean debug = false;

	public static BoltStatement instantiate(BoltConnection connection, boolean debug) {
		BoltStatement boltStatement = null;

		if (debug) {
			boltStatement = Mockito.mock(BoltStatement.class,
					Mockito.withSettings().useConstructor().outerInstance(connection).verboseLogging().defaultAnswer(Mockito.CALLS_REAL_METHODS));
			boltStatement.debug = debug;
		} else {
			boltStatement = new BoltStatement(connection);
		}

		return boltStatement;
	}

	/**
	 * @param connection
	 */
	public BoltStatement(BoltConnection connection) {
		this.connection = connection;
		this.transaction = connection.getTransaction();
	}

	//Mustn't return null
	@Override public ResultSet executeQuery(String sql) throws SQLException {
		if (connection.isClosed()) {
			throw new SQLException("Connection already closed");
		}
		ResultCursor cur;
		if (connection.getAutoCommit()) {
			Transaction t = this.connection.getSession().beginTransaction();
			cur = t.run(sql);
			t.success();
			t.close();
		} else {
			cur = this.connection.getTransaction().run(sql);
		}
		return BoltResultSet.istantiate(cur, this.debug);
	}

	@Override public int executeUpdate(String sql) throws SQLException {
		throw new UnsupportedOperationException();
	}
}
