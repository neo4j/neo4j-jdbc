/**
 * Copyright (c) 2004-2015 LARUS Business Automation Srl
 * <p>
 * All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
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
import org.neo4j.driver.v1.*;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * @author AgileLARUS
 *
 * @since 3.0.0
 */
public class BoltResultSetTest {

	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	@Ignore
	@Test
	public void nextShouldReturnFalseEmpty() throws SQLException {
		ResultCursor resultCursor = new InternalResultCursor(null, new LinkedList<Record>(), null);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		Assert.assertFalse(resultSet.next());
	}

	@Ignore
	@Test
	public void nextShouldReturnFalseAfterLast() throws SQLException {
		List<Record> records = new LinkedList<Record>();
		records.add(new InternalRecord(new LinkedList<>(), new HashMap<>(), new Value[0]));
		ResultCursor resultCursor = new InternalResultCursor(null, records, null);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		Assert.assertTrue(resultSet.next());

		Assert.assertFalse(resultSet.next());
	}

	@Ignore
	@Test
	public void nextShouldReturnTrue() throws SQLException {
		List<Record> records = new LinkedList<Record>();
		records.add(new InternalRecord(new LinkedList<>(), new HashMap<>(), new Value[0]));
		ResultCursor resultCursor = new InternalResultCursor(null, records, null);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		Assert.assertTrue(resultSet.next());
	}

	@Ignore
	@Test
	public void nextShouldThrowExceptionEmpty() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("ResultCursor not initialized");

		ResultSet resultSet = new BoltResultSet(null);

		resultSet.next();
	}
}
