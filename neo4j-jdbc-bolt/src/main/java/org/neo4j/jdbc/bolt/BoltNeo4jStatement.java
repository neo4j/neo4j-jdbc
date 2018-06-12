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
 * Created on 19/02/16
 */
package org.neo4j.jdbc.bolt;

import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.summary.ResultSummary;
import org.neo4j.driver.v1.summary.SummaryCounters;
import org.neo4j.jdbc.Neo4jStatement;
import org.neo4j.jdbc.bolt.impl.BoltNeo4jConnectionImpl;
import org.neo4j.jdbc.utils.Neo4jInvocationHandler;

import java.lang.reflect.Proxy;
import java.sql.BatchUpdateException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltNeo4jStatement extends Neo4jStatement {

	/**
	 * Default Constructor
	 *
	 * @param connection The connection used for sharing the transaction between statements
	 * @param rsParams   The params (type, concurrency and holdability) used to create a new ResultSet
	 */
	private BoltNeo4jStatement(BoltNeo4jConnectionImpl connection, int... rsParams) {
		super(connection);
		this.resultSetParams = rsParams;
		this.batchStatements = new ArrayList<>();
	}

	public static Statement newInstance(boolean debug, BoltNeo4jConnectionImpl connection, int... rsParams) {
		Statement statement = new BoltNeo4jStatement(connection, rsParams);
		((Neo4jStatement) statement).setDebug(debug);
		return (Statement) Proxy.newProxyInstance(BoltNeo4jStatement.class.getClassLoader(), new Class[] { Statement.class },
				new Neo4jInvocationHandler(statement, debug));
	}

	@Override public ResultSet executeQuery(String sql) throws SQLException {
		StatementResult result = executeInternal(sql);

		this.currentResultSet = BoltNeo4jResultSet.newInstance(this.hasDebug(), this, result, this.resultSetParams);
		this.currentUpdateCount = -1;
		return this.currentResultSet;
	}

	@Override public int executeUpdate(String sql) throws SQLException {
		StatementResult result = executeInternal(sql);
		SummaryCounters stats = result.consume().counters();
		this.currentUpdateCount = BoltNeo4jUtils.calculateUpdateCount(stats);
		this.currentResultSet = null;
		return this.currentUpdateCount;
	}

	@Override public boolean execute(String sql) throws SQLException {
		StatementResult result = executeInternal(sql);

		boolean hasResultSet = false;
		if (result != null) {
			hasResultSet = hasResultSet(sql);
			if (hasResultSet) {
				this.currentResultSet = BoltNeo4jResultSet.newInstance(this.hasDebug(), this, result, this.resultSetParams);
				this.currentUpdateCount = -1;
			} else {
				this.currentResultSet = null;
				try {
					SummaryCounters stats = result.consume().counters();
					this.currentUpdateCount = BoltNeo4jUtils.calculateUpdateCount(stats);
				} catch (Exception e) {
					throw new SQLException(e);
				}
			}
		}
		return hasResultSet;
	}

	private StatementResult executeInternal(String statement) throws SQLException {
		this.checkClosed();
        try {
        	Transaction transaction = ((BoltNeo4jConnection) this.getConnection()).getTransaction();
			StatementResult result = transaction.run(statement);
			if (this.getConnection().getAutoCommit()) {
                ((BoltNeo4jConnection) this.getConnection()).doCommit();
			}
			return result;
		} catch (Exception e) {
        	throw new SQLException(e);
		}
    }

	private boolean hasResultSet(String sql) {
		return sql != null && sql.toLowerCase().contains("return ");
	}

	/*-------------------*/
	/*       Batch       */
	/*-------------------*/

	@Override public int[] executeBatch() throws SQLException {
		this.checkClosed();

		int[] result = new int[0];

		try {
			for (String query : this.batchStatements) {
				StatementResult res = ((BoltNeo4jConnection) this.connection).getTransaction().run(query);
                SummaryCounters count = res.consume().counters();
				result = Arrays.copyOf(result, result.length + 1);
				result[result.length - 1] = count.nodesCreated() + count.nodesDeleted();
				if (this.connection.getAutoCommit()) {
					((BoltNeo4jConnection) this.connection).doCommit();
				}
			}
		} catch (Exception e) {
			throw new BatchUpdateException(result, e);
		}

		return result;
	}


}
