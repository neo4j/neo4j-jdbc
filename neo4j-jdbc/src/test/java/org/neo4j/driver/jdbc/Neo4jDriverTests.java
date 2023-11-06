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
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.driver.jdbc.translator.spi.SqlTranslatorFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.verify;

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

		var sqlTranslators = new Iterator<SqlTranslatorFactory>() {
			@Override
			public boolean hasNext() {
				return false;
			}

			@Override
			public SqlTranslatorFactory next() {
				return null;
			}
		};
		assertThatExceptionOfType(NoSuchElementException.class)
			.isThrownBy(() -> Neo4jDriver.uniqueOrThrow(sqlTranslators))
			.withMessage("No SQL translators available");
	}

	@Test
	void oneSqlTranslatorShouldWork() {

		var sqlTranslators = List.of((SqlTranslatorFactory) sql -> null);
		assertThat(Neo4jDriver.uniqueOrThrow(sqlTranslators.iterator())).isEqualTo(sqlTranslators.get(0));
	}

	@ParameterizedTest
	@ValueSource(ints = { 2, 3 })
	void severalSqlTranslatorsMustFail(int numTranslators) {

		var sqlTranslators = new ArrayList<SqlTranslatorFactory>();
		for (int i = 0; i < numTranslators; ++i) {
			var mock = mock(SqlTranslatorFactory.class);

			given(mock.getName()).willReturn("Translator " + i);
			sqlTranslators.add(mock);
		}

		var it = sqlTranslators.iterator();
		assertThatIllegalArgumentException().isThrownBy(() -> Neo4jDriver.uniqueOrThrow(it))
			.withMessageMatching("More than one SQL translator found: \\[(?: ?Translator \\d,?){1,3}]");

		sqlTranslators.forEach(t -> verify(t).getName());
	}

	static Stream<Arguments> determinationToAutomaticallyTranslateSqlToCypherShouldWork() {
		return Stream.of(Arguments.of(new String[0], properties(Map.of()), false),
				Arguments.of(new String[0], properties(Map.of("sql2cypher", "true")), true),
				Arguments.of(new String[] { "sql2cypher=false" }, properties(Map.of("sql2cypher", "true")), false),
				Arguments.of(new String[] { "sql2cypher=true" }, properties(Map.of("sql2cypher", "false")), true),
				Arguments.of(new String[] { "sql2cypher=true" }, properties(Map.of()), true));
	}

	static Properties properties(Map<String, String> src) {
		var properties = new Properties();
		src.forEach(properties::setProperty);
		return properties;
	}

	@ParameterizedTest
	@MethodSource
	void determinationToAutomaticallyTranslateSqlToCypherShouldWork(String[] urlParams, Properties properties,
			boolean expected) {

		assertThat(Neo4jDriver.isAutomaticSqlTranslation(urlParams, properties)).isEqualTo(expected);
	}

	@Test
	void sqlTranslatorShouldBeLazilyLoadedWithoutAutomaticTranslation() {

		var sqlTranslatorSupplier = Neo4jDriver.getSqlTranslatorSupplier(new String[0], properties(Map.of()), () -> {
			throw new UnsupportedOperationException("later");
		});
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(sqlTranslatorSupplier::get)
			.withMessage("later");
	}

	@Test
	void sqlTranslatorShouldBeLoadedImmediateWithAutomaticTranslation() {

		var urlParams = new String[0];
		var properties = properties(Map.of("sql2cypher", "true"));
		Supplier<SqlTranslatorFactory> throwingSupplier = () -> {
			throw new UnsupportedOperationException("now");
		};
		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> Neo4jDriver.getSqlTranslatorSupplier(urlParams, properties, throwingSupplier))
			.withMessage("now");
	}

}
