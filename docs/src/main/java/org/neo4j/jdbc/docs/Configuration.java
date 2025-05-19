/*
 * Copyright (c) 2023-2025 "Neo4j,"
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

// tag::cdm[]
// tag::ddm[]
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

// end::cdm[]
// end::ddm[]

@SuppressWarnings({ "resource", "squid:S1854", "squid:S2068" })
// tag::cdm[]
// tag::ddm[]
class Configuration {

	// end::cdm[]
	void obtainDriverAndConnection() throws SQLException {

		var url = "jdbc:neo4j://localhost:7687";
		var driver = DriverManager.getDriver(url);

		var properties = new Properties();
		properties.put("username", "neo4j");
		properties.put("password", "verysecret");
		var connection = driver.connect(url, properties);
	}
	// end::ddm[]

	// tag::cdm[]
	void obtainConnection() throws SQLException {

		var url = "jdbc:neo4j://localhost:7687";
		var username = "neo4j";
		var password = "verysecret";
		var connection = DriverManager.getConnection(url, username, password);
	}
	// tag::ddm[]

}
// end::cdm[]
// end::ddm[]
