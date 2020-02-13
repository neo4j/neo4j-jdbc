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
 * Created on 09/03/16
 */
package org.neo4j.jdbc;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.sql.SQLException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class Neo4jDataSourceTest {

	@Rule public ExpectedException expectedEx = ExpectedException.none();

	private Neo4jDataSource dataSource;

	@Before public void setUp() {
		this.dataSource = mock(Neo4jDataSource.class, Mockito.CALLS_REAL_METHODS);
		this.dataSource.setServerName("localhost"); //a protected non static non final variable is not initialized in a mock.
	}

	/*------------------------------*/
	/*         isWrapperFor         */
	/*------------------------------*/

	@Test public void isWrapperForShouldReturnTrue() throws SQLException {
		assertTrue(this.dataSource.isWrapperFor(Neo4jDataSource.class));
		assertTrue(this.dataSource.isWrapperFor(javax.sql.DataSource.class));
		assertTrue(this.dataSource.isWrapperFor(javax.sql.CommonDataSource.class));
		assertTrue(this.dataSource.isWrapperFor(java.sql.Wrapper.class));
	}

	@Test public void isWrapperForShouldReturnFalse() throws SQLException {
		assertFalse(this.dataSource.isWrapperFor(Neo4jResultSet.class));
		assertFalse(this.dataSource.isWrapperFor(java.sql.Driver.class));
		assertFalse(this.dataSource.isWrapperFor(Neo4jResultSet.class));
		assertFalse(this.dataSource.isWrapperFor(java.sql.ResultSet.class));
	}

	/*------------------------------*/
	/*            unwrap            */
	/*------------------------------*/

	@Test public void unwrapShouldReturnCorrectClass() throws SQLException {
		assertNotNull(this.dataSource.unwrap(Neo4jDataSource.class));
		assertNotNull(this.dataSource.unwrap(javax.sql.DataSource.class));
		assertNotNull(this.dataSource.unwrap(javax.sql.CommonDataSource.class));
		assertNotNull(this.dataSource.unwrap(java.sql.Wrapper.class));
	}

	@Test public void unwrapShouldThrowException() throws SQLException {
		expectedEx.expect(SQLException.class);

		this.dataSource.unwrap(Neo4jResultSet.class);
	}

	/*------------------------------*/
	/*            getURL            */
	/*------------------------------*/
	@Test public void getUrlTest() throws SQLException {
		this.dataSource.setIsSsl(false);
		this.dataSource.setUser("test");

		assertEquals("jdbc:neo4j:bolt://localhost?nossl,user=test", this.dataSource.getUrl("bolt"));

		this.dataSource.setIsSsl(true);
		assertEquals("jdbc:neo4j:bolt://localhost?user=test", this.dataSource.getUrl("bolt"));

		this.dataSource.setPassword("password");
		this.dataSource.setIsSsl(false);
		assertEquals("jdbc:neo4j:bolt://localhost?nossl,user=test,password=password", this.dataSource.getUrl("bolt"));
		assertEquals("jdbc:neo4j:http://localhost?nossl,user=test,password=password", this.dataSource.getUrl("http"));

		this.dataSource.setPortNumber(7687);
		assertEquals("jdbc:neo4j:bolt://localhost:7687?nossl,user=test,password=password", this.dataSource.getUrl("bolt"));

		this.dataSource.setPassword("pa&word");
		this.dataSource.setIsSsl(false);
		this.dataSource.setPortNumber(0);
		assertEquals("jdbc:neo4j:bolt://localhost?nossl,user=test,password=pa%26word", this.dataSource.getUrl("bolt"));
		assertEquals("jdbc:neo4j:http://localhost?nossl,user=test,password=pa%26word", this.dataSource.getUrl("http"));
	}

}
