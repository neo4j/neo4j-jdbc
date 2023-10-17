/*
 * Copyright (c) 2023 "Neo4j,"
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
package org.neo4j.driver.jdbc;

import java.sql.Connection;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DatabaseMetadataImplTests {

	@Test
	void getDriverName() {
	}

	@Test
	void getDriverVersion() {
	}

	@Test
	void getDriverMajorVersion() {

		var databaseMetadata = newDatabaseMetadata();
		Assertions.assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(databaseMetadata::getDriverMajorVersion)
			.withMessage("Unsupported or unknown version 'unknown'");
	}

	@Test
	void getDriverMinorVersion() {
		var databaseMetadata = newDatabaseMetadata();
		Assertions.assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(databaseMetadata::getDriverMinorVersion)
			.withMessage("Unsupported or unknown version 'unknown'");
	}

	@Test
	void getJDBCMajorVersion() {

		var databaseMetadata = newDatabaseMetadata();
		Assertions.assertThat(databaseMetadata.getJDBCMajorVersion()).isEqualTo(4);
	}

	@Test
	void getJDBCMinorVersion() {

		var databaseMetadata = newDatabaseMetadata();
		Assertions.assertThat(databaseMetadata.getJDBCMinorVersion()).isEqualTo(3);
	}

	static DatabaseMetadataImpl newDatabaseMetadata() {
		var connection = Mockito.mock(Connection.class);
		return new DatabaseMetadataImpl(connection);
	}

}
