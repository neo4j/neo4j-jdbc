/**
 * Copyright (c) 2004-2016 LARUS Business Automation [http://www.larus-ba.it]
 * <p>
 * This file is part of the "LARUS Integration Framework for Neo4j".
 * <p>
 * The "LARUS Integration Framework for Neo4j" is free software: you can
 * redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * Created on 17/02/16
 */
package it.neo4j.jdbc.bolt;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltConnectionTest {

	@Rule public ExpectedException expectedEx = ExpectedException.none();

	/*------------------------------*/
	/*           isClosed           */
	/*------------------------------*/
	@Ignore @Test public void isClosedShouldReturnFalse() throws SQLException {
		Connection connection = new BoltConnection();
		Assert.assertFalse(connection.isClosed());
	}

	@Ignore @Test public void isClosedShouldReturnTrue() throws SQLException {
		Connection connection = new BoltConnection();
		connection.close();
		Assert.assertTrue(connection.isClosed());
	}

	/*------------------------------*/
	/*             close            */
	/*------------------------------*/
	@Ignore @Test public void closeShouldCloseConnection() throws SQLException {
		Connection connection = new BoltConnection();
		Assert.assertFalse(connection.isClosed());
		connection.close();
		Assert.assertTrue(connection.isClosed());
	}

	/*------------------------------*/
	/*          isReadOnly          */
	/*------------------------------*/
	@Ignore @Test public void isReadOnlyShouldReturnFalse() throws SQLException {
		Connection connection = new BoltConnection();
		Assert.assertFalse(connection.isReadOnly());
	}

	@Ignore @Test public void isReadOnlyShouldReturnTrue() throws SQLException {
		Connection connection = new BoltConnection();
		connection.setReadOnly(true);
		Assert.assertTrue(connection.isReadOnly());
	}

	@Ignore @Test public void isReadOnlyShouldThrowExceptionWhenCalledOnAClosedConnection() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("Connection already closed");

		Connection connection = new BoltConnection();
		connection.close();
		connection.isReadOnly();
	}

	/*------------------------------*/
	/*         setReadOnly          */
	/*------------------------------*/
	@Ignore @Test public void setReadOnlyShouldSetReadOnlyTrue() throws SQLException {
		Connection connection = new BoltConnection();
		Assert.assertFalse(connection.isReadOnly());
		connection.setReadOnly(true);
		Assert.assertTrue(connection.isReadOnly());
	}

	@Ignore @Test public void setReadOnlyShouldSetReadOnlyFalse() throws SQLException {
		Connection connection = new BoltConnection();
		Assert.assertFalse(connection.isReadOnly());
		connection.setReadOnly(false);
		Assert.assertFalse(connection.isReadOnly());
	}

	@Ignore @Test public void setReadOnlyShouldSetReadOnlyFalseAfterSetItTrue() throws SQLException {
		Connection connection = new BoltConnection();
		Assert.assertFalse(connection.isReadOnly());
		connection.setReadOnly(true);
		Assert.assertTrue(connection.isReadOnly());
		connection.setReadOnly(false);
		Assert.assertFalse(connection.isReadOnly());
	}

	@Ignore @Test public void setReadOnlyShouldThrowExceptionIfCalledOnAClosedConnection() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("Connection already closed");

		Connection connection = new BoltConnection();
		connection.close();
		connection.setReadOnly(true);
	}
}
