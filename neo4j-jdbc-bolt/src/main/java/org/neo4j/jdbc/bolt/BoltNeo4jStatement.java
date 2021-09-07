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

import org.neo4j.driver.Result;
import org.neo4j.driver.summary.SummaryCounters;
import org.neo4j.jdbc.Neo4jStatement;
import org.neo4j.jdbc.bolt.impl.BoltNeo4jConnectionImpl;
import org.neo4j.jdbc.utils.BoltNeo4jUtils;
import org.neo4j.jdbc.utils.Neo4jInvocationHandler;

import java.lang.reflect.Proxy;
import java.sql.BatchUpdateException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.util.Arrays.copyOf;
import static org.neo4j.jdbc.utils.BoltNeo4jUtils.executeInTx;
import static org.neo4j.jdbc.utils.BoltNeo4jUtils.hasResultSet;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltNeo4jStatement extends Neo4jStatement {

	private final ResultSetFactory resultSetFactory;

	/**
	 * Default Constructor
	 *
	 * @param connection The connection used for sharing the transaction between statements
	 * @param rsParams   The params (type, concurrency and holdability) used to create a new ResultSet
	 */
	// visible for testing
	BoltNeo4jStatement(BoltNeo4jConnectionImpl connection, ResultSetFactory resultSetFactory, int... rsParams) {
		super(connection);
		this.resultSetFactory = resultSetFactory;
		this.resultSetParams = rsParams;
		this.batchStatements = new ArrayList<>();
	}

	public static Statement newInstance(boolean debug, BoltNeo4jConnectionImpl connection, int... rsParams) {
		Neo4jStatement statement = new BoltNeo4jStatement(connection, BoltNeo4jResultSet::newInstance, rsParams);
		statement.setDebug(debug);
		return (Statement) Proxy.newProxyInstance(BoltNeo4jStatement.class.getClassLoader(), new Class[] { Statement.class },
				new Neo4jInvocationHandler(statement, debug));
	}

	@Override public ResultSet executeQuery(String sql) throws SQLException {
		return executeInternal(sql, (result) -> {
			this.currentResultSet = this.resultSetFactory.create(this.hasDebug(), this, result, this.resultSetParams);
			this.currentUpdateCount = -1;
			return this.currentResultSet;
		});
	}

	@Override public int executeUpdate(String sql) throws SQLException {
		return executeInternal(sql, (result) -> {
			SummaryCounters stats = result.consume().counters();
			this.currentUpdateCount = BoltNeo4jUtils.calculateUpdateCount(stats);
			this.currentResultSet = null;
			return this.currentUpdateCount;
		});
	}

	@Override public boolean execute(String sql) throws SQLException {
		boolean hasResultSet = hasResultSet((BoltNeo4jConnection) connection, sql);
		return executeInternal(sql, (result) -> {
			if (result != null) {
				if (hasResultSet) {
					this.currentResultSet = BoltNeo4jResultSet.newInstance(this.hasDebug(), this, result, this.resultSetParams);
					this.currentUpdateCount = -1;
				} else {
					this.currentResultSet = null;
					SummaryCounters stats = result.consume().counters();
					this.currentUpdateCount = BoltNeo4jUtils.calculateUpdateCount(stats);
				}
			}
			return hasResultSet;
		});
	}

	private <T> T executeInternal(String statement,
								  Function<Result, T> body) throws SQLException {
		this.checkClosed();
		return executeInTx((BoltNeo4jConnection) this.connection, statement, body);
	}

	/*-------------------*/
	/*       Batch       */
	/*-------------------*/


	@Override public int[] executeBatch() throws SQLException {
		this.checkClosed();
		int[] result = new int[0];
		try {
			BoltNeo4jConnection connection = (BoltNeo4jConnection) this.connection;
			for (String query : this.batchStatements) {
				int count = executeInTx(connection, query, (statementResult) -> {
					SummaryCounters counters = statementResult.consume().counters();
					return counters.nodesCreated() + counters.nodesDeleted();
				});
				result = copyOf(result, result.length + 1);
				result[result.length - 1] = count;
			}
		} catch (Exception e) {
			throw new BatchUpdateException(result, e);
		}
		return result;
	}

	// visible for testing
	List<String> getBatchStatements() {
		return batchStatements;
	}
}
