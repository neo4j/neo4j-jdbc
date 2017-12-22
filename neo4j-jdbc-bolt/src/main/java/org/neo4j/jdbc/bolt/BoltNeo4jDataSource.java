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

import org.neo4j.jdbc.Neo4jDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * @author Gianmarco Laggia @ Larus B.A.
 *
 * @since 3.2.0
 */
public class BoltNeo4jDataSource extends Neo4jDataSource {

	private static final String PROTOCOL = "bolt";

	@Override public Connection getConnection() throws SQLException {
		return getConnection(user, password);
	}

	@Override public Connection getConnection(String username, String pass) throws SQLException {
		setUser(username);
		setPassword(pass);
		return DriverManager.getConnection(getUrl(PROTOCOL));
	}
}
