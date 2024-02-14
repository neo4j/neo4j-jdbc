/*
 * Copyright (c) 2023-2024 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.driver.jdbc.docs;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.neo4j.driver.jdbc.Neo4jDriver;

public final class Neo4jExtensions {

	private static final Logger LOGGER = Logger.getAnonymousLogger();

	private Neo4jExtensions() {
	}

	public static void main(String... a) throws SQLException {

		try (var con = Neo4jDriver.fromEnv().orElseThrow();
				var stmt = con.prepareCall("{? = CALL dbms.components()}")) {
			stmt.execute();
			LOGGER.log(Level.INFO, "{0} = {1}", new Object[] { stmt.getString("name"), stmt.getObject("versions") });
		}

		try (var con = Neo4jDriver.fromEnv("Neo4j-cb3d8b2d-Created-2024-02-14.txt").orElseThrow();
				var stmt = con.prepareCall("{? = CALL dbms.components()}")) {
			stmt.execute();
			LOGGER.log(Level.INFO, "{0} = {1}", new Object[] { stmt.getString("name"), stmt.getObject("versions") });
		}

	}

}
