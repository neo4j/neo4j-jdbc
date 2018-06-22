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
 * Created on 01/12/17
 */
package org.neo4j.jdbc.bolt;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.neo4j.jdbc.bolt.utils.JdbcConnectionTestUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author AgileLARUS
 *
 * @since 3.0.0
 */
public class BoltNeo4jDataSourceIT {
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	@ClassRule
	public static Neo4jBoltRule neo4j = new Neo4jBoltRule();

	/*------------------------------*/
	/*        getConnection         */
	/*------------------------------*/

	@Test public void getConnectionShouldWork() throws SQLException {

		BoltNeo4jDataSource boltNeo4jDataSource = new BoltNeo4jDataSource();
		boltNeo4jDataSource.setServerName(neo4j.getHost());
		boltNeo4jDataSource.setPortNumber(neo4j.getPort());
		boltNeo4jDataSource.setIsSsl(JdbcConnectionTestUtils.SSL_ENABLED);
		boltNeo4jDataSource.setUser(JdbcConnectionTestUtils.USERNAME);
		boltNeo4jDataSource.setPassword(JdbcConnectionTestUtils.PASSWORD);

		Connection connection = boltNeo4jDataSource.getConnection();
		assertNotNull(connection);

		Statement statement = connection.createStatement();
		assertTrue(statement.execute("RETURN 1"));
	}
}
