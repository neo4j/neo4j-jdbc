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
 * Created on 23/03/16
 */
package org.neo4j.jdbc.bolt;

import org.neo4j.jdbc.*;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.summary.SummaryCounters;

import java.sql.BatchUpdateException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static java.util.Arrays.*;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltPreparedStatement extends PreparedStatement implements Loggable {

	private Transaction transaction;
	private int[]       rsParams;
	private boolean loggable = false;
	private List<HashMap<String, Object>> batchParameters;

	public BoltPreparedStatement(BoltConnection connection, String rawStatement, int... rsParams) {
		super(connection, rawStatement);
		this.transaction = connection.getTransaction();
		this.rsParams = rsParams;
		this.batchParameters = new ArrayList<>();
	}

	@Override public ResultSet executeQuery() throws SQLException {
		this.checkClosed();
		StatementResult result = executeInternal();

		this.currentResultSet = InstanceFactory.debug(BoltResultSet.class, new BoltResultSet(this,result, this.rsParams), this.isLoggable());
		this.currentUpdateCount = -1;
		return currentResultSet;
	}

	@Override public int executeUpdate() throws SQLException {
		this.checkClosed();
		StatementResult result = executeInternal();

		SummaryCounters stats = result.consume().counters();
		this.currentUpdateCount = stats.nodesCreated() + stats.nodesDeleted() + stats.relationshipsCreated() + stats.relationshipsDeleted();
		this.currentResultSet = null;
		return this.currentUpdateCount;
	}

	@Override public boolean execute() throws SQLException {
		boolean result = false;
		if (statement.contains("DELETE") || statement.contains("MERGE") || statement.contains("CREATE") || statement.contains("delete") || statement
				.contains("merge") || statement.contains("create")) {
			this.executeUpdate();
		} else {
			this.executeQuery();
			result = true;
		}

		return result;
	}

	private StatementResult executeInternal() throws SQLException {
		this.checkClosed();

		StatementResult result;
		if (this.getConnection().getAutoCommit()) {
			Transaction t = ((BoltConnection) this.getConnection()).getSession().beginTransaction();
			result = t.run(this.statement, this.parameters);
			t.success();
			t.close();
		} else {
			result = ((BoltConnection) this.getConnection()).getTransaction().run(this.statement, this.parameters);
		}

		return result;
	}

	@Override public ParameterMetaData getParameterMetaData() throws SQLException {
		this.checkClosed();
		ParameterMetaData pmd = new BoltParameterMetaData(this);
		return pmd;
	}

	@Override public ResultSetMetaData getMetaData() throws SQLException {
		return this.currentResultSet == null ? null : (ResultSetMetaData) this.currentResultSet.getMetaData();
		/*return InstanceFactory.debug(BoltResultSetMetaData.class,
				new BoltResultSetMetaData(((BoltResultSet) this.currentResultSet).getIterator(), ((BoltResultSet) this.currentResultSet).getKeys()),
				this.isLoggable());*/
	}

	@Override public int getResultSetConcurrency() throws SQLException {
		this.checkClosed();
		if (currentResultSet != null) {
			return currentResultSet.getConcurrency();
		}
		if (this.rsParams.length > 1) {
			return this.rsParams[1];
		}
		return BoltResultSet.DEFAULT_CONCURRENCY;
	}

	@Override public int getResultSetType() throws SQLException {
		this.checkClosed();
		if (currentResultSet != null) {
			return currentResultSet.getType();
		}
		if (this.rsParams.length > 0) {
			return this.rsParams[0];
		}
		return BoltResultSet.DEFAULT_TYPE;
	}

	@Override public int getResultSetHoldability() throws SQLException {
		this.checkClosed();
		if (currentResultSet != null) {
			return currentResultSet.getHoldability();
		}
		if (this.rsParams.length > 2) {
			return this.rsParams[2];
		}
		return BoltResultSet.DEFAULT_HOLDABILITY;
	}

	/*-------------------*/
	/*       Batch       */
	/*-------------------*/

	@Override public void addBatch() throws SQLException {
		this.checkClosed();
		this.batchParameters.add(new HashMap<>(this.parameters));
		this.parameters.clear();
	}

	@Override public void clearBatch() throws SQLException {
		this.checkClosed();
		this.batchParameters.clear();
	}

	@Override public int[] executeBatch() throws SQLException {
		this.checkClosed();
		int[] result = new int[0];

		try {
			for (Map<String, Object> parameter : this.batchParameters) {
				StatementResult res;
				if(this.connection.getAutoCommit()) {
					res = ((BoltConnection) this.connection).getSession().run(this.statement, parameter);
				} else {
					res = ((BoltConnection) this.connection).getTransaction().run(this.statement, parameter);
				}
				SummaryCounters count = res.consume().counters();
				result = copyOf(result, result.length + 1);
				result[result.length - 1] = count.nodesCreated() + count.nodesDeleted();
			}
		} catch (Exception e) {
			throw new BatchUpdateException(result, e);
		}

		return result;
	}

	/*--------------------*/
	/*       Logger       */
	/*--------------------*/

	@Override public boolean isLoggable() {
		return this.loggable;
	}

	@Override public void setLoggable(boolean loggable) {
		this.loggable = loggable;
	}
}
