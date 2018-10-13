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
 */
package org.neo4j.jdbc.bolt.impl;

import org.neo4j.driver.v1.AccessMode;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.jdbc.Neo4jDatabaseMetaData;
import org.neo4j.jdbc.Neo4jResultSet;
import org.neo4j.jdbc.bolt.*;
import org.neo4j.jdbc.boltrouting.BoltRoutingNeo4jDriver;
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

	private Driver driver;
	private Session session;
	private Transaction transaction;
	private boolean autoCommit = true;
	private BoltNeo4jDatabaseMetaData metadata;

	private static final Logger LOGGER = Logger.getLogger(BoltNeo4jConnectionImpl.class.getName());

    /**
	 * Constructor with Session and Properties.
	 *
	 * @param driver     Bolt driver
	 * @param properties Driver properties
	 * @param url        Url used for this connection
	 */
	public BoltNeo4jConnectionImpl(Driver driver, Properties properties, String url) {
		super(properties, url, BoltNeo4jResultSet.DEFAULT_HOLDABILITY);
		this.driver = driver;
		this.initSession();
	}

	public static BoltNeo4jConnection newInstance(Driver driver, Properties info, String url) {
		BoltNeo4jConnection boltConnection = new BoltNeo4jConnectionImpl(driver, info, url);
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
	    initTransaction();
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

	/**
	 * Build an internal neo4j session, without saving reference (stateless)
	 * @return
	 */
	public Session newNeo4jSession(){
		try {
			String bookmark = this.getClientInfo(BoltRoutingNeo4jDriver.BOOKMARK);
			return this.driver.session(getReadOnly() ? AccessMode.READ : AccessMode.WRITE, bookmark);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override public Neo4jDatabaseMetaData getMetaData() throws SQLException {
		if(metadata == null){
			metadata = new BoltNeo4jDatabaseMetaData(this);
		}
		return metadata;
	}

    @Override public void setReadOnly(boolean readOnly) throws SQLException {
        this.checkClosed();
        if (this.transaction != null && this.transaction.isOpen()) {
            throw new SQLException("Method can't be called during a transaction");
        }
        super.doSetReadOnly(readOnly);
        if (this.session != null && this.session.isOpen()){
            this.session.close();
            initSession();
        }
    }

	/*------------------------------*/
	/*       Commit, rollback       */
	/*------------------------------*/

    @Override public void setAutoCommit(boolean autoCommit) throws SQLException {
        if (this.autoCommit != autoCommit) {
            this.checkClosed();
            this.doCommit();
			this.autoCommit = autoCommit;
		}
	}

	@Override public boolean getAutoCommit() throws SQLException {
		this.checkClosed();
		return this.autoCommit;
	}

	@Override public void commit() throws SQLException {
		this.checkClosed();
		this.checkAutoCommit();
        doCommit();
	}

    @Override public void doCommit() throws SQLException {
        if (this.transaction != null && this.transaction.isOpen()) {
            this.transaction.success();
            this.transaction.close();
            this.transaction = null;
            this.setClientInfo(BoltRoutingNeo4jDriver.BOOKMARK, this.session.lastBookmark());
        }
    }

    @Override public void rollback() throws SQLException {
		this.checkClosed();
		this.checkAutoCommit();
        doRollback();
	}

    @Override public void doRollback() {
        if (this.transaction != null && this.transaction.isOpen()) {
            this.transaction.failure();
            this.transaction.close();
            this.transaction = null;
        }
    }

	/*------------------------------*/
	/*       Create Statement       */
	/*------------------------------*/

	@Override public Statement createStatement() throws SQLException {
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
		this.initTransaction();
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
		this.initTransaction();
		return BoltNeo4jPreparedStatement.newInstance(false, this, nativeSQL(sql), resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	/*-------------------*/
	/*       Close       */
	/*-------------------*/

	@Override public boolean isClosed() throws SQLException {
        return this.driver == null || (this.session != null && !this.session.isOpen());
	}

	@Override public void close() throws SQLException {
		try {
			if (!this.isClosed()) {
				if (this.transaction != null) {
					this.transaction.close();
					this.transaction = null;
				}
                if (this.session != null) {
                    this.session.close();
                    this.session = null;
                    this.driver.close();
                    this.driver = null;
                }
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

	/*-------------------------------------------------------*/
	/*       Some useful initializer and check method        */
	/*-------------------------------------------------------*/

	/**
	 * BOLT Session is initialized right before the very first statement is created
	 * in order to check whether the connection is in readonly mode or not.
	 * This way we point to the right cluster instance (core vs read replica).
	 */
	private void initSession() {
		this.session = newNeo4jSession();
    }

    private void initTransaction()  {
	    try {
            if (this.transaction == null) {
                this.transaction = this.session.beginTransaction();
			}
            else if (this.getAutoCommit()) {
                if (this.transaction.isOpen()) {
                    this.transaction.success();
                    this.transaction.close();
                    this.setClientInfo(BoltRoutingNeo4jDriver.BOOKMARK, this.session.lastBookmark());
                }
                this.transaction = this.session.beginTransaction();
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
	}
}
