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
 * Created on 17/02/16
 */
package it.larusba.neo4j.jdbc.bolt;

import it.larusba.neo4j.jdbc.*;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.Transaction;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltConnection extends Connection implements Loggable {

	private Session     session;
	private Transaction transaction;
	private boolean autoCommit = true;
	private boolean loggable   = false;

	/**
	 * Constructor with Session and Properties.
	 *
	 * @param session    Bolt Session
	 * @param properties Driver properties
	 */
	public BoltConnection(Session session, Properties properties) {
		super(properties,  BoltResultSet.DEFAULT_HOLDABILITY);
		this.session = session;
	}

	/**
	 * Constructor with Session.
	 *
	 * @param session Bolt Session
	 */
	public BoltConnection(Session session) {
		this(session, new Properties());
	}

	/**
	 * Getter for transaction.
	 *
	 * @return
	 */
	public Transaction getTransaction() {
		return this.transaction;
	}

	/**
	 * Getter for session.
	 *
	 * @return
	 */
	public Session getSession() {
		return this.session;
	}

	@Override public DatabaseMetaData getMetaData() throws SQLException {
		return new BoltDatabaseMetaData(this);
	}

	/*------------------------------*/
	/*       Commit, rollback       */
	/*------------------------------*/

	@Override public void setAutoCommit(boolean autoCommit) throws SQLException {
		if (this.autoCommit != autoCommit) {
			if (this.transaction != null && !this.autoCommit) {
				this.commit();
			}

			if (this.autoCommit) {
				//Simply restart the transaction
				this.transaction = this.session.beginTransaction();
			} else {
				this.transaction.close();
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
		this.checkClosed();
		if (this.transaction == null && !this.autoCommit) {
			this.transaction = this.session.beginTransaction();
		}
		return InstanceFactory
				.debug(BoltStatement.class, new BoltStatement(this, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT),
						this.isLoggable());
	}

	@Override public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
		this.checkClosed();
		this.checkTypeParams(resultSetType);
		this.checkConcurrencyParams(resultSetConcurrency);
		return InstanceFactory
				.debug(BoltStatement.class, new BoltStatement(this, resultSetType, resultSetConcurrency, ResultSet.CLOSE_CURSORS_AT_COMMIT), this.isLoggable());
	}

	@Override public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
		this.checkClosed();
		this.checkTypeParams(resultSetType);
		this.checkConcurrencyParams(resultSetConcurrency);
		this.checkHoldabilityParams(resultSetHoldability);
		return InstanceFactory
				.debug(BoltStatement.class, new BoltStatement(this, resultSetType, resultSetConcurrency, resultSetHoldability), this.isLoggable());
	}

	/*-------------------------------*/
	/*       Prepare Statement       */
	/*-------------------------------*/

	@Override public PreparedStatement prepareStatement(String sql) throws SQLException {
		this.checkClosed();
		return InstanceFactory.debug(BoltPreparedStatement.class,
				new BoltPreparedStatement(this, nativeSQL(sql), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT),
				this.isLoggable());
	}

	@Override public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
		this.checkClosed();
		this.checkTypeParams(resultSetType);
		this.checkConcurrencyParams(resultSetConcurrency);
		return InstanceFactory.debug(BoltPreparedStatement.class,
				new BoltPreparedStatement(this, sql, resultSetType, resultSetConcurrency, ResultSet.CLOSE_CURSORS_AT_COMMIT), this.isLoggable());
	}

	@Override public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
		this.checkClosed();
		this.checkTypeParams(resultSetType);
		this.checkConcurrencyParams(resultSetConcurrency);
		this.checkHoldabilityParams(resultSetHoldability);
		return InstanceFactory
				.debug(BoltPreparedStatement.class, new BoltPreparedStatement(this, sql, resultSetType, resultSetConcurrency, resultSetHoldability),
						this.isLoggable());
	}

	/*-------------------*/
	/*       Close       */
	/*-------------------*/

	@Override public void close() throws SQLException {
		try {
			if (!this.isClosed()) {
				session.close();
			}
		} catch (Exception e) {
			throw new SQLException("A database access error has occurred");
		}
	}

	@Override public boolean isClosed() throws SQLException {
		return !this.session.isOpen();
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
