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
package org.neo4j.driver.it.cp;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CallableStatementIT extends IntegrationTestBase {

	@Test
	void shouldExecuteQueryWithOrdinalParameters() throws SQLException {
		try (var connection = getConnection();
				var statement = connection.prepareCall("CALL dbms.cluster.routing.getRoutingTable($1, $2)")) {
			statement.setObject(1, Collections.emptyMap());
			statement.setString(2, "neo4j");

			var resultSet = statement.executeQuery();

			assertThat(resultSet.next()).isTrue();
			assertThat(resultSet.getInt(1)).isGreaterThan(0);
			assertThat((List<?>) resultSet.getObject(2)).hasSize(3);
			assertThat(resultSet.next()).isFalse();
		}
	}

	@Test
	void shouldExecuteQueryWithNamedParameters() throws SQLException {
		try (var connection = getConnection();
				var statement = connection
					.prepareCall("CALL dbms.cluster.routing.getRoutingTable($context, $database)")) {
			statement.setObject("context", Collections.emptyMap());
			statement.setString("database", "neo4j");

			var resultSet = statement.executeQuery();

			assertThat(resultSet.next()).isTrue();
			assertThat(resultSet.getInt(1)).isGreaterThan(0);
			assertThat((List<?>) resultSet.getObject(2)).hasSize(3);
			assertThat(resultSet.next()).isFalse();
		}
	}

}
