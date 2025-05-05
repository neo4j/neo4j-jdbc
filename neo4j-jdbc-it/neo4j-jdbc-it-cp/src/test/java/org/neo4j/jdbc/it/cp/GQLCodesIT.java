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
package org.neo4j.jdbc.it.cp;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.neo4j.jdbc.GqlStatusObject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

class GQLCodesIT extends IntegrationTestBase {

	@Test
	void gqlStatusFromDatabaseShouldBeAvailable() throws SQLException {
		try (var connection = super.getConnection(); var stmt = connection.createStatement();) {

			assertThatExceptionOfType(SQLException.class)
				.isThrownBy(() -> stmt.executeQuery("RETURN date('123456789')"))
				.matches(ex -> {
					if (ex.getErrorCode() == 0 && "22007".equals(ex.getSQLState())
							&& ex instanceof GqlStatusObject gqlStatus) {
						return !gqlStatus.diagnosticRecord().isEmpty()
								&& gqlStatus.cause().filter(cause -> cause.gqlStatus().equals("22N36")).isPresent();
					}
					return false;
				})
				.withMessage("error: data exception - invalid date, time, or datetime format");
		}
	}

	@Test
	void smokeTestFor22N37() throws SQLException {
		try (var connection = super.getConnection();
				var stmt = connection.createStatement();
				var rs = stmt.executeQuery("RETURN null AS v1, 'abc' AS v2")) {
			rs.next();

			assertThatNoException().isThrownBy(() -> rs.getBigDecimal(1));
			assertThatNoException().isThrownBy(() -> rs.getDouble(1));
			assertThat(rs.wasNull()).isTrue();
			assertThatNoException().isThrownBy(() -> rs.getDate(1));

			assertThatExceptionOfType(SQLException.class).isThrownBy(() -> rs.getBigDecimal(2))
				.withMessage("data exception - Cannot coerce \"abc\" (STRING) to java.math.BigDecimal")
				.matches(ex -> "22N37".equals(ex.getSQLState()));
			assertThatExceptionOfType(SQLException.class).isThrownBy(() -> rs.getDouble(2))
				.withMessage("data exception - Cannot coerce \"abc\" (STRING) to double")
				.matches(ex -> "22N37".equals(ex.getSQLState()));
			assertThatExceptionOfType(SQLException.class).isThrownBy(() -> rs.getDate(2))
				.withMessage("data exception - Cannot coerce \"abc\" (STRING) to java.sql.Date")
				.matches(ex -> "22N37".equals(ex.getSQLState()));
		}
	}

	@Test
	void smokeTestFor22003() throws SQLException {
		try (var connection = super.getConnection();
				var stmt = connection.createStatement();
				var rs = stmt.executeQuery("RETURN 9223372036854775807 AS v1")) {
			rs.next();

			var msg = "data exception - The numeric value %s is outside the required range".formatted(rs.getString(1));

			assertThatExceptionOfType(SQLException.class).isThrownBy(() -> rs.getByte(1))
				.withMessage(msg)
				.matches(ex -> "22003".equals(ex.getSQLState()));
			assertThatExceptionOfType(SQLException.class).isThrownBy(() -> rs.getShort(1))
				.withMessage(msg)
				.matches(ex -> "22003".equals(ex.getSQLState()));
			assertThatExceptionOfType(SQLException.class).isThrownBy(() -> rs.getInt(1))
				.withMessage(msg)
				.matches(ex -> "22003".equals(ex.getSQLState()));
		}
	}

}
