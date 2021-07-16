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

import org.neo4j.driver.Result;
import org.neo4j.driver.summary.SummaryCounters;
import org.neo4j.jdbc.Loggable;
import org.neo4j.jdbc.Neo4jParameterMetaData;
import org.neo4j.jdbc.Neo4jPreparedStatement;
import org.neo4j.jdbc.Neo4jResultSetMetaData;
import org.neo4j.jdbc.bolt.impl.BoltNeo4jConnectionImpl;
import org.neo4j.jdbc.utils.BoltNeo4jUtils;
import org.neo4j.jdbc.utils.Neo4jInvocationHandler;

import java.lang.reflect.Proxy;
import java.sql.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.Calendar;
import java.util.Map;
import java.util.function.Function;

import static java.util.Arrays.copyOf;
import static org.neo4j.jdbc.utils.BoltNeo4jUtils.executeInTx;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltNeo4jPreparedStatement extends Neo4jPreparedStatement implements Loggable {

	private BoltNeo4jPreparedStatement(BoltNeo4jConnectionImpl connection, String rawStatement, int... rsParams) {
		super(connection, rawStatement);
		this.resultSetParams = rsParams;
	}

	public static PreparedStatement newInstance(boolean debug, BoltNeo4jConnectionImpl connection, String rawStatement, int... rsParams) {
		PreparedStatement ps = new BoltNeo4jPreparedStatement(connection, rawStatement, rsParams);
		((Neo4jPreparedStatement) ps).setDebug(debug);
		return (PreparedStatement) Proxy.newProxyInstance(BoltNeo4jPreparedStatement.class.getClassLoader(), new Class[] { PreparedStatement.class },
				new Neo4jInvocationHandler(ps, debug));
	}

	@Override public ResultSet executeQuery() throws SQLException {
		return executeInternal((result) -> {
			this.currentResultSet = BoltNeo4jResultSet.newInstance(this.hasDebug(), this, result, this.resultSetParams);
			this.currentUpdateCount = -1;
			return currentResultSet;
		});
	}

	@Override public int executeUpdate() throws SQLException {
		return executeInternal((result) -> {
			SummaryCounters stats = result.consume().counters();
			this.currentUpdateCount = BoltNeo4jUtils.calculateUpdateCount(stats);
			this.currentResultSet = null;
			return this.currentUpdateCount;
		});
	}

	@Override public boolean execute() throws SQLException {
		return executeInternal((result) -> {
			boolean hasResultSet = hasResultSet();
			if (hasResultSet) {
				this.currentResultSet = BoltNeo4jResultSet.newInstance(this.hasDebug(), this, result, this.resultSetParams);
				this.currentUpdateCount = -1;
			} else {
				this.currentResultSet = null;
				SummaryCounters stats = result.consume().counters();
				this.currentUpdateCount = BoltNeo4jUtils.calculateUpdateCount(stats);
			}
			return hasResultSet;
		});
	}

	private <T> T executeInternal(Function<Result, T> body) throws SQLException {
		this.checkClosed();
		return executeInTx((BoltNeo4jConnection) this.connection, this.statement, this.parameters, body);
	}

	private boolean hasResultSet() {
		return this.statement != null && this.statement.toLowerCase().contains("return");
	}

	@Override public Neo4jParameterMetaData getParameterMetaData() throws SQLException {
		this.checkClosed();
		return new BoltNeo4jParameterMetaData(this);
	}

	@Override public Neo4jResultSetMetaData getMetaData() throws SQLException {
		return this.currentResultSet == null ? null : (Neo4jResultSetMetaData) this.currentResultSet.getMetaData();
	}

	/*-------------------*/
	/*       Batch       */
	/*-------------------*/

	@Override public int[] executeBatch() throws SQLException {
		this.checkClosed();
		int[] result = new int[0];
		try {
			BoltNeo4jConnection connection = (BoltNeo4jConnection) this.connection;
			for (Map<String, Object> parameter : this.batchParameters) {
				int count = executeInTx(connection, this.statement, parameter, (statementResult) -> {
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

	/*-------------------*/
	/*   setParameter    */
	/*-------------------*/

	protected void setTemporal(int parameterIndex, long epoch, ZoneId zone, Function<ZonedDateTime, Temporal> extractTemporal) throws SQLException {
		checkClosed();
		checkParamsNumber(parameterIndex);

		ZonedDateTime zdt = Instant.ofEpochMilli(epoch).atZone(zone);

		insertParameter(parameterIndex, extractTemporal.apply(zdt));
	}

	@Override
	public void setDate(int parameterIndex, Date x) throws SQLException {
		setTemporal(parameterIndex, x.getTime(),ZoneId.systemDefault(), (zdt)-> zdt.toLocalDate());
	}

	@Override
	public void setTime(int parameterIndex, Time x) throws SQLException {
		setTemporal(parameterIndex, x.getTime(),ZoneId.systemDefault(), (zdt)-> zdt.toLocalTime());
	}

	@Override
	public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
		setTemporal(parameterIndex, x.getTime(),ZoneId.systemDefault(), (zdt)-> zdt.toLocalDateTime());
	}

	@Override
	public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
		setTemporal(parameterIndex, x.getTime(),cal.getTimeZone().toZoneId(), (zdt)-> zdt);
	}

	@Override
	public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
		setTemporal(parameterIndex, x.getTime(),cal.getTimeZone().toZoneId(), (zdt)-> zdt);
	}

	@Override
	public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
		setTemporal(parameterIndex, x.getTime(),cal.getTimeZone().toZoneId(), (zdt)-> zdt.toOffsetDateTime().toOffsetTime());
	}

	@Override
	public void setArray(int parameterIndex, Array x) throws SQLException {
		checkClosed();
		insertParameter(parameterIndex, x.getArray());
	}
}
