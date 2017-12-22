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
 * Created on 17/02/16
 */
package org.neo4j.jdbc.bolt.impl;

import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.jdbc.Neo4jDatabaseMetaData;
import org.neo4j.jdbc.Neo4jResultSet;
import org.neo4j.jdbc.bolt.*;
import org.neo4j.jdbc.impl.Neo4jConnectionImpl;
import org.neo4j.jdbc.utils.Neo4jInvocationHandler;
import org.neo4j.jdbc.utils.TimeLimitedCodeBlock;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltNeo4jConnectionImpl extends Neo4jConnectionImpl implements BoltNeo4jConnection {

	private Session     session;
	private Transaction transaction;
	private boolean autoCommit = true;

	private static final Logger LOGGER = Logger.getLogger(BoltNeo4jConnectionImpl.class.getName());

	/**
	 * Constructor with Session and Properties.
	 *
	 * @param session    Bolt Session
	 * @param properties Driver properties
	 * @param url        Url used for this connection
	 */
	public BoltNeo4jConnectionImpl(Session session, Properties properties, String url) {
		super(properties, url, BoltNeo4jResultSet.DEFAULT_HOLDABILITY);
		this.session = session;
	}

	/**
	 * Constructor with Session.
	 *
	 * @param session Bolt Session
	 */
	public BoltNeo4jConnectionImpl(Session session) {
		this(session, new Properties(), "");
	}

	public static BoltNeo4jConnection newInstance(Session session, Properties info, String url) {
		BoltNeo4jConnection boltConnection = new BoltNeo4jConnectionImpl(session, info, url);
		return (BoltNeo4jConnection) Proxy
				.newProxyInstance(BoltNeo4jConnectionImpl.class.getClassLoader(), new Class[] { Connection.class, BoltNeo4jConnection.class },
						new Neo4jInvocationHandler(boltConnection, BoltNeo4jConnectionImpl.hasDebug(info)));
	}

	/**
	 * Getter for transaction.
	 *
	 * @return the transaction
	 */
	@Override public Transaction getTransaction() {
		return this.transaction;
	}

	/**
	 * Getter for session.
	 *
	 * @return the internal session
	 */
	@Override public Session getSession() {
		return this.session;
	}

	@Override public Neo4jDatabaseMetaData getMetaData() throws SQLException {
		return new BoltNeo4jDatabaseMetaData(this);
	}

	/*------------------------------*/
	/*       Commit, rollback       */
	/*------------------------------*/

	@Override public void setAutoCommit(boolean autoCommit) throws SQLException {
		if (this.autoCommit != autoCommit) {
			if (this.transaction != null && !this.autoCommit) {
				this.commit();
				this.transaction.close();
			}

			if (this.autoCommit) {
				//Simply restart the transaction
				this.transaction = this.session.beginTransaction();
			}

			this.autoCommit = autoCommit;
		}
	}

	@Override public boolean getAutoCommit() throws SQLException {
		this.checkClosed();
		return autoCommit;
	}

	@Override public void commit() throws SQLException {
		this.checkClosed();
		this.checkAutoCommit();
		if (this.transaction == null) {
			throw new SQLException("The transaction is null");
		}
		this.transaction.success();
		this.transaction.close();
		this.transaction = this.session.beginTransaction();
	}

	@Override public void rollback() throws SQLException {
		this.checkClosed();
		this.checkAutoCommit();
		if (this.transaction == null) {
			throw new SQLException("The transaction is null");
		}
		this.transaction.failure();
	}

	/*------------------------------*/
	/*       Create Statement       */
	/*------------------------------*/

	@Override public Statement createStatement() throws SQLException {
		if (this.transaction == null && !this.autoCommit) {
			this.transaction = this.session.beginTransaction();
		}
		return createStatement(Neo4jResultSet.TYPE_FORWARD_ONLY, Neo4jResultSet.CONCUR_READ_ONLY, Neo4jResultSet.CLOSE_CURSORS_AT_COMMIT);
	}

	@Override public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
		return createStatement(resultSetType, resultSetConcurrency, Neo4jResultSet.CLOSE_CURSORS_AT_COMMIT);
	}

	@Override public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
		this.checkClosed();
		this.checkTypeParams(resultSetType);
		this.checkConcurrencyParams(resultSetConcurrency);
		this.checkHoldabilityParams(resultSetHoldability);
		return BoltNeo4jStatement.newInstance(false, this, resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	/*-------------------------------*/
	/*       Prepare Statement       */
	/*-------------------------------*/

	@Override public PreparedStatement prepareStatement(String sql) throws SQLException {
		return prepareStatement(nativeSQL(sql), Neo4jResultSet.TYPE_FORWARD_ONLY, Neo4jResultSet.CONCUR_READ_ONLY, Neo4jResultSet.CLOSE_CURSORS_AT_COMMIT);
	}

	@Override public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
		return prepareStatement(nativeSQL(sql), resultSetType, resultSetConcurrency, Neo4jResultSet.CLOSE_CURSORS_AT_COMMIT);
	}

	@Override public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
		this.checkClosed();
		this.checkTypeParams(resultSetType);
		this.checkConcurrencyParams(resultSetConcurrency);
		this.checkHoldabilityParams(resultSetHoldability);
		return BoltNeo4jPreparedStatement.newInstance(false, this, nativeSQL(sql), resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	/*-------------------*/
	/*       Close       */
	/*-------------------*/

	@Override public boolean isClosed() throws SQLException {
		return !this.session.isOpen();
	}

	@Override public void close() throws SQLException {
		try {
			if (!this.isClosed()) {
				session.close();
			}
		} catch (Exception e) {
			throw new SQLException("A database access error has occurred: " + e.getMessage());
		}
	}

	/*-------------------*/
	/*      isValid      */
	/*-------------------*/
	@Override public boolean isValid(int timeout) throws SQLException {
		if (timeout < 0) {
			throw new SQLException("Timeout can't be less than zero");
		}
		if (this.isClosed()) {
			return false;
		}

		Runnable r = new Runnable() {
			@Override public void run() {
				Session s = getSession();
				Transaction tr = getTransaction();
				if (tr != null && tr.isOpen()) {
					tr.run(FASTEST_STATEMENT);
				} else {
					s.run(FASTEST_STATEMENT);
				}
			}
		};

		try {
			TimeLimitedCodeBlock.runWithTimeout(r, timeout, TimeUnit.SECONDS);
		} catch (Exception e) { // also timeout
			LOGGER.log(Level.FINEST, "Catch exception totally fine", e);
			return false;
		}

		return true;
	}

}
