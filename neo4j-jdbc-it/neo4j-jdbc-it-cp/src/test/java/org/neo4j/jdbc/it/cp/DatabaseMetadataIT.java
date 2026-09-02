/*
 * Copyright (c) 2023-2026 "Neo4j,"
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

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class DatabaseMetadataIT extends AbstractDatabaseMetadata {

	DatabaseMetadataIT() {
		super(false);
	}

	@Override
	boolean apocShouldBeAvailable() {
		return false;
	}

	@Test
	@DisabledInNativeImage
	void connectionMustReset() throws SQLException, NoSuchFieldException, IllegalAccessException {
		try (var connection = this.getConnection()) {
			var metaData = connection.getMetaData();
			fakePresenceOfApoc(metaData);

			assertThatExceptionOfType(SQLException.class).isThrownBy(() -> metaData.getTables(null, null, "%", null));

			var procCount = 0;
			try (var rs = metaData.getProcedures(null, null, null)) {
				while (rs.next()) {
					++procCount;
				}
			}

			assertThat(procCount).isGreaterThan(0);
		}

	}

	private static void fakePresenceOfApoc(DatabaseMetaData metaData)
			throws NoSuchFieldException, IllegalAccessException {
		// We do this on purpose: It's the easiest way to trigger a metadata server side
		// error without relying on soon to be fixed other bugs.
		var apocAvailableField = metaData.getClass().getDeclaredField("apocAvailable");
		apocAvailableField.setAccessible(true);
		var apocAvailable = apocAvailableField.get(metaData);
		var valueField = apocAvailableField.getType().getDeclaredField("resolved");
		valueField.setAccessible(true);
		valueField.set(apocAvailable, true);
	}

}
