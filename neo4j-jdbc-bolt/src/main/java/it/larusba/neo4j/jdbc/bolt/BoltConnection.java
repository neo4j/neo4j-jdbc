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

import it.larusba.neo4j.jdbc.Connection;
import it.larusba.neo4j.jdbc.DatabaseMetaData;
import it.larusba.neo4j.jdbc.ResultSet;
import it.larusba.neo4j.jdbc.Statement;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.Transaction;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltConnection extends Connection implements Loggable {

	private boolean readOnly   = false;
	private boolean autoCommit = true;
	private Session     session;
	private Transaction transaction;
	private Properties  properties;
	private boolean loggable = false;
	private int holdability;

	public BoltConnection(Session session, Properties properties) {
		this.session = session;
		this.properties = properties;
	}

	public BoltConnection(Session session) {
		this(session, new Properties());
	}

	public Transaction getTransaction() {
		return this.transaction;
	}

	public Session getSession() {
		return this.session;
	}

	private void checkClosed() throws SQLException {
		if (this.isClosed()) {
			throw new SQLException("Connection already closed");
		}
	}

	private void checkConcurrencyParams(int resultSetConcurrency) throws SQLException {
		// @formatter:off
		if( resultSetConcurrency != ResultSet.CONCUR_UPDATABLE &&
			resultSetConcurrency != ResultSet.CONCUR_READ_ONLY
		){
			throw new SQLFeatureNotSupportedException();
		}
		// @formatter:on
	}

	private void checkTypeParams(int resultSetType) throws SQLException {
		// @formatter:off
		if( resultSetType != ResultSet.TYPE_FORWARD_ONLY &&
			resultSetType != ResultSet.TYPE_SCROLL_INSENSITIVE &&
			resultSetType != ResultSet.TYPE_SCROLL_SENSITIVE
		){
			throw new SQLFeatureNotSupportedException();
		}
		// @formatter:on
	}

	private void checkHoldabilityParams(int resultSetHoldability) throws SQLException {
		// @formatter:off
		if( resultSetHoldability != ResultSet.HOLD_CURSORS_OVER_COMMIT &&
			resultSetHoldability != ResultSet.CLOSE_CURSORS_AT_COMMIT
		){
			throw new SQLFeatureNotSupportedException();
		}
		// @formatter:on
	}

	private void checkAutoCommit() throws SQLException {
		if (this.autoCommit) {
			throw new SQLException("Cannot commit when in autocommit");
		}
	}

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

	@Override public DatabaseMetaData getMetaData() throws SQLException {
		return new BoltDatabaseMetaData(this);
	}

	@Override public void setReadOnly(boolean readOnly) throws SQLException {
		this.checkClosed();
		this.readOnly = readOnly;
	}

	@Override public boolean isReadOnly() throws SQLException {
		this.checkClosed();
		return this.readOnly;
	}

	@Override public void setCatalog(String catalog) throws SQLException {
		this.checkClosed();
		return;
	}

	@Override public String getCatalog() throws SQLException {
		this.checkClosed();
		return null;
	}

	@Override public int getTransactionIsolation() throws SQLException {
		this.checkClosed();
		return TRANSACTION_READ_COMMITTED;
	}

	@Override public Statement createStatement() throws SQLException {
		this.checkClosed();
		if (this.transaction == null && !this.autoCommit) {
			this.transaction = this.session.beginTransaction();
		}
		return InstanceFactory
				.debug(BoltStatement.class, new BoltStatement(this, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT),
						this.isLoggable());
	}

	@Override public PreparedStatement prepareStatement(String sql) throws SQLException {
		this.checkClosed();
		return InstanceFactory.debug(BoltPreparedStatement.class,
				new BoltPreparedStatement(this, sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT),
				this.isLoggable());
	}

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

	@Override public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
		this.checkClosed();
		this.checkTypeParams(resultSetType);
		this.checkConcurrencyParams(resultSetConcurrency);
		return InstanceFactory
				.debug(BoltStatement.class, new BoltStatement(this, resultSetType, resultSetConcurrency, ResultSet.CLOSE_CURSORS_AT_COMMIT), this.isLoggable());
	}

	@Override public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
		this.checkClosed();
		this.checkTypeParams(resultSetType);
		this.checkConcurrencyParams(resultSetConcurrency);
		return InstanceFactory.debug(BoltPreparedStatement.class,
				new BoltPreparedStatement(this, sql, resultSetType, resultSetConcurrency, ResultSet.CLOSE_CURSORS_AT_COMMIT), this.isLoggable());
	}

	@Override public void setHoldability(int holdability) throws SQLException {
		this.checkClosed();
		if (holdability != ResultSet.HOLD_CURSORS_OVER_COMMIT && holdability != ResultSet.CLOSE_CURSORS_AT_COMMIT) {
			throw new SQLFeatureNotSupportedException("Holdability " + holdability + " not supported.");
		}
		this.holdability = holdability;
	}

	@Override public int getHoldability() throws SQLException {
		this.checkClosed();
		return (this.holdability > 0) ? this.holdability : BoltResultSet.DEFAULT_HOLDABILITY;
	}

	@Override public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
		this.checkClosed();
		this.checkTypeParams(resultSetType);
		this.checkConcurrencyParams(resultSetConcurrency);
		this.checkHoldabilityParams(resultSetHoldability);
		return InstanceFactory
				.debug(BoltStatement.class, new BoltStatement(this, resultSetType, resultSetConcurrency, resultSetHoldability), this.isLoggable());
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

	public static boolean hasDebug(Properties properties) {
		return "true".equalsIgnoreCase(properties.getProperty("debug", "false"));
	}

	@Override public boolean isLoggable() {
		return this.loggable;
	}

	@Override public void setLoggable(boolean loggable) {
		this.loggable = loggable;
	}
}
