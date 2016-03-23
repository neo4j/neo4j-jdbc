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
package it.larusba.neo4j.jdbc.bolt;

import it.larusba.neo4j.jdbc.Statement;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.summary.SummaryCounters;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltStatement extends Statement implements Loggable {

	private BoltConnection connection;
	private Transaction    transaction;
	private int[]          rsParams;
	private ResultSet      currentResultSet;
	private boolean        closed;

	private boolean loggable = false;

	/**
	 * Default Constructor
	 *
	 * @param connection
	 * @param rsParams   The params (type, concurrency and holdability) used to create a new ResultSet
	 */
	public BoltStatement(BoltConnection connection, int... rsParams) {
		this.connection = connection;
		this.transaction = connection.getTransaction();
		this.rsParams = rsParams;
		this.currentResultSet = null;
		this.closed = false;
	}

	//Mustn't return null
	@Override public ResultSet executeQuery(String sql) throws SQLException {
		if (this.isClosed()) {
			throw new SQLException("Statement already closed");
		}
		if (connection.isClosed()) {
			throw new SQLException("Connection already closed");
		}
		StatementResult result;
		if (connection.getAutoCommit()) {
			Transaction t = this.connection.getSession().beginTransaction();
			result = t.run(sql);
			t.success();
			t.close();
		} else {
			result = this.connection.getTransaction().run(sql);
		}
		this.currentResultSet = InstanceFactory.debug(BoltResultSet.class, new BoltResultSet(result, this.rsParams), this.isLoggable());
		return currentResultSet;
	}

	@Override public int executeUpdate(String sql) throws SQLException {
		if (this.isClosed()) {
			throw new SQLException("Statement already closed");
		}
		if (connection.isClosed()) {
			throw new SQLException("Connection already closed");
		}
		StatementResult result;
		if (connection.getAutoCommit()) {
			Transaction t = this.connection.getSession().beginTransaction();
			result = t.run(sql);
			t.success();
			t.close();
		} else {
			result = this.connection.getTransaction().run(sql);
		}

		SummaryCounters stats = result.consume().counters();
		return stats.nodesCreated() + stats.nodesDeleted() + stats.relationshipsCreated() + stats.relationshipsDeleted();
	}

	@Override public void close() throws SQLException {
		if (this.closed) {
			return;
		}
		if (this.currentResultSet != null) {
			this.currentResultSet.close();
		}
		if (this.transaction != null) {
			this.transaction.failure();
			this.transaction.close();
		}
		this.closed = true;
	}

	@Override public boolean isClosed() throws SQLException {
		return closed;
	}

	@Override public boolean isLoggable() {
		return this.loggable;
	}

	@Override public void setLoggable(boolean loggable) {
		this.loggable = loggable;
	}
}
