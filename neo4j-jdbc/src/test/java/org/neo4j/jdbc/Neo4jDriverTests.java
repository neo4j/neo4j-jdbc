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
package org.neo4j.jdbc;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.bolt.connection.AuthToken;
import org.neo4j.bolt.connection.AuthTokens;
import org.neo4j.jdbc.authn.spi.Authentication;
import org.neo4j.jdbc.authn.spi.CustomAuthentication;
import org.neo4j.jdbc.authn.spi.TokenAuthentication;
import org.neo4j.jdbc.authn.spi.UsernamePasswordAuthentication;
import org.neo4j.jdbc.internal.bolt.BoltAdapters;
import org.neo4j.jdbc.translator.spi.Translator;
import org.neo4j.jdbc.translator.spi.TranslatorFactory;
import org.neo4j.jdbc.values.Values;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

class Neo4jDriverTests {

	@Test
	void driverMustNotMarkItselfAsJDBCCompliant() {
		assertThat(new Neo4jDriver().jdbcCompliant()).isFalse();
	}

	@Test
	void getParentLoggerShouldWork() {

		var driver = new Neo4jDriver();
		assertThat(driver.getParentLogger().getName()).isEqualTo("org.neo4j.jdbc");
	}

	@Test
	void noSqlTranslatorsShouldWork() {

		assertThatExceptionOfType(SQLException.class)
			.isThrownBy(() -> Neo4jDriver.getSqlTranslatorSupplier(true, Map.of(), List::of))
			.withMessage("general processing exception - No translators available");
	}

	@Test
	void oneSqlTranslatorShouldWork() throws SQLException {

		var translator = new Translator() {

			@Override
			public String translate(String statement, DatabaseMetaData optionalDatabaseMetaData) {
				return null;
			}
		};
		var sqlTranslators = List.<TranslatorFactory>of(properties -> translator);
		assertThat(Neo4jDriver.getSqlTranslatorSupplier(true, Map.of(), () -> sqlTranslators).get())
			.containsExactly(translator);
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldSortSqlTranslators(boolean eager) throws SQLException {
		var t1 = mock(Translator.class);
		given(t1.getOrder()).willReturn(Translator.HIGHEST_PRECEDENCE);
		var t2 = mock(Translator.class);
		given(t2.getOrder()).willReturn(Translator.HIGHEST_PRECEDENCE + 10);
		var t3 = mock(Translator.class);
		given(t3.getOrder()).willReturn(Translator.HIGHEST_PRECEDENCE + 20);

		var translators = Neo4jDriver
			.getSqlTranslatorSupplier(eager, Map.of(),
					() -> List.of(properties -> t3, properties -> t1, properties -> t2))
			.get();

		assertThat(translators).containsExactly(t1, t2, t3);
	}

	static Stream<Arguments> mergeOfUrlParamsAndPropertiesShouldWork() {
		return Stream.of(Arguments.of(new String[0], properties(Map.of()), false),
				Arguments.of(new String[0], properties(Map.of("enableSQLTranslation", "true")), true),
				Arguments.of(new String[] { "enableSQLTranslation=false" },
						properties(Map.of("enableSQLTranslation", "true")), false),
				Arguments.of(new String[] { "enableSQLTranslation=true" },
						properties(Map.of("enableSQLTranslation", "false")), true),
				Arguments.of(new String[] { "enableSQLTranslation=true" }, properties(Map.of()), true));
	}

	static Properties properties(Map<String, String> src) {
		var properties = new Properties();
		src.forEach(properties::setProperty);
		return properties;
	}

	@ParameterizedTest
	@MethodSource
	void mergeOfUrlParamsAndPropertiesShouldWork(String[] urlParams, Properties properties, boolean expected) {

		var config = Neo4jDriver.mergeConfig(urlParams, properties);
		if (urlParams.length == 0 && properties.isEmpty()) {
			assertThat(config).isEmpty();
		}
		else {
			assertThat(config).containsEntry("enableSQLTranslation", Boolean.toString(expected));
		}
	}

	@Test
	void sqlTranslatorShouldBeLazilyLoadedWithoutAutomaticTranslation() throws SQLException {

		var sqlTranslatorSupplier = Neo4jDriver.getSqlTranslatorSupplier(false, Map.of(), () -> {
			throw new UnsupportedOperationException("later");
		});
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(sqlTranslatorSupplier::get)
			.withMessage("later");
	}

	@Test
	void sqlTranslatorShouldBeLoadedImmediateWithAutomaticTranslation() {

		Supplier<List<TranslatorFactory>> throwingSupplier = () -> {
			throw new UnsupportedOperationException("now");
		};
		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> Neo4jDriver.getSqlTranslatorSupplier(true, Map.of(), throwingSupplier))
			.withMessage("now");
	}

	@Test
	void defaultUAShouldWork() {
		assertThat(Neo4jDriver.getDefaultUserAgent()).matches("neo4j-jdbc/dev");
	}

	@Nested
	class AuthenticationSupplierDeterminationTest {

		@Test
		void explicitProviderShouldHaveHighestPrecedence() {

			var driver = new Neo4jDriver();
			driver.setAuthenticationSupplier(() -> Authentication.usernameAndPassword("global", "pw"));
			var provider = driver.determineAuthenticationSupplier(
					() -> Authentication.usernameAndPassword("local", "pw"), newDriverConfig());
			assertThat(provider.get())
				.asInstanceOf(InstanceOfAssertFactories.type(UsernamePasswordAuthentication.class))
				.extracting(UsernamePasswordAuthentication::username)
				.isEqualTo("local");
		}

		@Test
		void globalHasPrecedenceOverExplicit() {

			var driver = new Neo4jDriver();
			driver.setAuthenticationSupplier(() -> Authentication.usernameAndPassword("global", "pw"));
			var provider = driver.determineAuthenticationSupplier(null, newDriverConfig());
			assertThat(provider.get())
				.asInstanceOf(InstanceOfAssertFactories.type(UsernamePasswordAuthentication.class))
				.extracting(UsernamePasswordAuthentication::username)
				.isEqualTo("global");
		}

		@Test
		void explicitLast() {

			var driver = new Neo4jDriver();
			driver.setAuthenticationSupplier(null);
			var provider = driver.determineAuthenticationSupplier(null, newDriverConfig());
			assertThat(provider.get())
				.asInstanceOf(InstanceOfAssertFactories.type(UsernamePasswordAuthentication.class))
				.extracting(UsernamePasswordAuthentication::username)
				.isEqualTo("explicit");
		}

		private static Neo4jDriver.DriverConfig newDriverConfig(Map<String, String> raw) {
			return new Neo4jDriver.DriverConfig("na", 7687, "db", Neo4jDriver.AuthScheme.BASIC, "explicit", "pw", null,
					null, 0, false, false, false, false, false, 0, null, raw);
		}

		private static Neo4jDriver.DriverConfig newDriverConfig() {
			return newDriverConfig(Map.of());
		}

	}

	@Nested
	class AuthenticationTransformerTests {

		static Stream<Arguments> straightConversionsShouldWork() {
			var valueFactory = BoltAdapters.getValueFactory();
			return Stream.of(Arguments.of(Authentication.none(), AuthTokens.none(valueFactory)),
					Arguments.of(Authentication.usernameAndPassword("u", "p", "r"),
							AuthTokens.basic("u", "p", "r", valueFactory)),
					Arguments.of(Authentication.usernameAndPassword("u", "p"),
							AuthTokens.basic("u", "p", null, valueFactory)),
					Arguments.of(Authentication.bearer("t"), AuthTokens.bearer("t", valueFactory)),
					Arguments.of(Authentication.kerberos("t"), AuthTokens.kerberos("t", valueFactory)),
					Arguments.of(new CustomAuthentication() {
						@Override
						public Map<String, Object> toMap() {
							return Map.of("s", Values.value("a string"), "n", 123);
						}
					}, AuthTokens.custom(Map.of("s", valueFactory.value("a string"), "n", valueFactory.value(123)))));
		}

		@ParameterizedTest
		@MethodSource
		void straightConversionsShouldWork(Authentication in, AuthToken expected) {
			assertThat(Neo4jDriver.toAuthToken(in)).isEqualTo(expected);
		}

		@Test
		void unsupportedScheme() {
			assertThatIllegalArgumentException()
				.isThrownBy(() -> Neo4jDriver
					.toAuthToken(new TokenAuthentication(Neo4jDriver.AuthScheme.BASIC.getName(), "foo", null)))
				.withMessage("Invalid scheme `basic` for token based authentication");
		}

	}

}
