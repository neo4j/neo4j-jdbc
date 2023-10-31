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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.neo4j.driver.jdbc.translator.spi.SqlTranslator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class Neo4jDriverTests {

	@Test
	void driverMustNotMarkItselfAsJDBCCompliant() {
		assertThat(new Neo4jDriver().jdbcCompliant()).isFalse();
	}

	@Test
	void driverMustReturnPropertyInfo() {

		var driver = new Neo4jDriver();
		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> driver.getPropertyInfo(null, null));
	}

	@Test
	void getParentLoggerShouldWork() {

		var driver = new Neo4jDriver();
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(driver::getParentLogger);
	}

	@Test
	void noSqlTranslatorsShouldWork() {

		assertThat(Neo4jDriver.uniqueOrThrow(new Iterator<>() {
			@Override
			public boolean hasNext() {
				return false;
			}

			@Override
			public SqlTranslator next() {
				return null;
			}
		})).isNull();
	}

	@Test
	void oneSqlTranslatorShouldWork() {

		var sqlTranslators = List.of((SqlTranslator) sql -> null);
		assertThat(Neo4jDriver.uniqueOrThrow(sqlTranslators.iterator())).isEqualTo(sqlTranslators.get(0));
	}

	@ParameterizedTest
	@ValueSource(ints = { 2, 3 })
	void severalSqlTranslatorsMustFail(int numTranslators) {

		var sqlTranslators = new ArrayList<SqlTranslator>();
		for (int i = 0; i < numTranslators; ++i) {
			sqlTranslators.add(Mockito.mock(SqlTranslator.class));
		}
		var it = sqlTranslators.iterator();
		assertThatIllegalArgumentException().isThrownBy(() -> Neo4jDriver.uniqueOrThrow(it))
			.withMessageMatching(
					"More than one implementation of a SQL translator was found: \\[(.*SqlTranslator.*,){1,2}(.*SqlTranslator.*)?]");
	}

}
