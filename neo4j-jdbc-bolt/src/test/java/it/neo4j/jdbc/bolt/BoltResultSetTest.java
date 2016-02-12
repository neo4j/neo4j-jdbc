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
 * Created on 11/02/16
 */
package it.neo4j.jdbc.bolt;

import it.neo4j.jdbc.ResultSet;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.neo4j.driver.internal.InternalRecord;
import org.neo4j.driver.internal.InternalResultCursor;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.ResultCursor;
import org.neo4j.driver.v1.Value;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltResultSetTest {

	@Rule public ExpectedException expectedEx = ExpectedException.none();

	@Ignore @Test public void nextShouldReturnFalseEmpty() throws SQLException {
		ResultCursor resultCursor = new InternalResultCursor(null, new LinkedList<Record>(), null);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		Assert.assertFalse(resultSet.next());
	}

	@Ignore @Test public void nextShouldReturnFalseAfterLast() throws SQLException {
		List<Record> records = new LinkedList<Record>();
		records.add(new InternalRecord(new LinkedList<>(), new HashMap<>(), new Value[0]));
		ResultCursor resultCursor = new InternalResultCursor(null, records, null);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		Assert.assertTrue(resultSet.next());

		Assert.assertFalse(resultSet.next());
	}

	@Ignore @Test public void nextShouldReturnTrue() throws SQLException {
		List<Record> records = new LinkedList<Record>();
		records.add(new InternalRecord(new LinkedList<>(), new HashMap<>(), new Value[0]));
		ResultCursor resultCursor = new InternalResultCursor(null, records, null);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		Assert.assertTrue(resultSet.next());
	}

	@Ignore @Test public void nextShouldThrowExceptionEmpty() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("ResultCursor not initialized");

		ResultSet resultSet = new BoltResultSet(null);

		resultSet.next();
	}
}
