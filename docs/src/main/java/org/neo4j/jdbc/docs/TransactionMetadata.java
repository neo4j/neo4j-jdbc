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
package org.neo4j.jdbc.docs;

// tag::example[]
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.neo4j.jdbc.Neo4jDriver;
import org.neo4j.jdbc.Neo4jMetadataWriter;
// end::example[]
// @formatter:off

// tag::example[]
public final class TransactionMetadata {

	private static final Logger LOGGER = Logger.getLogger(TransactionMetadata.class.getPackageName());

	// end::example[]
	private TransactionMetadata() {
	}

	// tag::example[]
	public static void main(String... args) throws SQLException {
		var url = "jdbc:neo4j://localhost:7687";

		var driver = (Neo4jDriver) DriverManager.getDriver(url);
		driver.withMetadata(Map.of("md_from_driver", "v1", "will_be_overwritten", "irrelevant"));

		var properties = new Properties();
		properties.put("user", "neo4j");
		properties.put("password", "verysecret");

		try (
			var con = driver.connect(url, properties)
				.unwrap(Neo4jMetadataWriter.class)
				.withMetadata(Map.of("md_from_connection", "v2", "will_be_overwritten", "xxx"))
				.unwrap(Connection.class);
			var statement = con.createStatement()
				.unwrap(Neo4jMetadataWriter.class)
				.withMetadata(Map.of("md_from_stmt", "v3", "will_be_overwritten", "v4"))
				.unwrap(Statement.class)
		) {
			try (var result = statement.executeQuery("SHOW TRANSACTIONS YIELD metaData")) {
				while (result.next()) {
					var metaData = result.getObject("metaData", Map.class);
					LOGGER.log(Level.INFO, "{0}", metaData);
				}
			}
		}
	}
}
// end::example[]
// @formatter:on
