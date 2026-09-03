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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.logging.Logger;

import com.github.stefanbirkner.systemlambda.Statement;
import com.github.stefanbirkner.systemlambda.SystemLambda;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.neo4j.jdbc.Neo4jDriver;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.neo4j.Neo4jContainer;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@TestInstance(Lifecycle.PER_CLASS)
@DisabledInNativeImage
@Disabled("SystemLambda not working reliable on standard Maven/JUnit setup, even when JVM is forked")
class TranslatorFactoryConfigIT {

	@SuppressWarnings("resource") // On purpose to reuse this
	protected final Neo4jContainer neo4j = TestUtils.getNeo4jContainer();

	@BeforeAll
	void startNeo4j() {
		this.neo4j.start();
	}

	@ParameterizedTest
	@CsvSource(
			textBlock = """
						org.neo4j.jdbc.it.cp.FunkyRandomClass                         | false | n/a                                           | false | false
						org.neo4j.jdbc.it.cp.FunkyFactory                             | false | n/a                                           | false | false
						org.neo4j.jdbc.it.cp.FunkyRandomClass                         | false | *                                             | false | false
						org.neo4j.jdbc.it.cp.FunkyFactory                             | false | *                                             | true  | true
						org.neo4j.jdbc.it.cp.FunkyFactory2                            | false | n/a                                           | false | false
						org.neo4j.jdbc.it.cp.FunkyFactory2                            | false | *                                             | true  | false
						org.neo4j.jdbc.it.cp.FunkyFactory2                            | false | ,org.neo4j.jdbc.it.cp.FunkyFactory2, whatever | true  | false
						org.neo4j.jdbc.it.cp.FunkyFactory2                            | false | elefant,*,a possum                            | true  | false
						org.neo4j.jdbc.translator.impl.SqlToCypherTranslatorFactory   | false | n/a                                           | true  | false
						org.neo4j.jdbc.it.cp.FunkyRandomClass                         | true  | n/a                                           | false | false
						org.neo4j.jdbc.it.cp.FunkyFactory                             | true  | n/a                                           | false | false
						org.neo4j.jdbc.it.cp.FunkyRandomClass                         | true  | *                                             | false | false
						org.neo4j.jdbc.it.cp.FunkyFactory3                            | true  | *                                             | true  | true
						org.neo4j.jdbc.it.cp.FunkyFactory2                            | true  | n/a                                           | false | false
						org.neo4j.jdbc.it.cp.FunkyFactory2                            | true  | *                                             | true  | false
						org.neo4j.jdbc.it.cp.FunkyFactory2                            | true  | ,org.neo4j.jdbc.it.cp.FunkyFactory2, whatever | true  | false
						org.neo4j.jdbc.it.cp.FunkyFactory2                            | true  | elefant,*,a possum                            | true  | false
						org.neo4j.jdbc.translator.impl.SqlToCypherTranslatorFactory   | true  | n/a                                           | true  | false
					""",
			nullValues = "n/a", delimiterString = "|")
	void mustNotPrematureInitialiseSQLTranslatorFactories(String fqn, boolean useSystemProperty, String allowList,
			boolean wasLoaded, boolean producedBogusMessage) throws Exception {

		Statement actualTest = () -> {
			// Capturing funky classes
			var output = new ByteArrayOutputStream();
			var original = System.err;
			var err = new PrintStream(output, true, StandardCharsets.UTF_8);
			System.setErr(err);

			// Capturing logger
			var handler = new CapturingHandler();
			Logger.getLogger("org.neo4j.jdbc").addHandler(handler);

			try {
				if (allowList != null && useSystemProperty) {
					System.setProperty("NEO4J_JDBC_ALLOWED_TRANSLATOR_FACTORIES", allowList);
				}

				var driver = new Neo4jDriver();

				var properties = new Properties();
				properties.put("user", "neo4j");
				properties.put("password", this.neo4j.getAdminPassword());
				properties.put("translatorFactory", fqn);

				var url = "jdbc:neo4j://%s:%s".formatted(this.neo4j.getHost(), this.neo4j.getMappedPort(7687));

				var connection = driver.connect(url, properties);
				assertThat(connection).isNotNull();
				try (var resultSet = connection.createStatement().executeQuery("RETURN 1")) {
					assertThat(resultSet.next()).isTrue();
					assertThat(resultSet.next()).isFalse();
				}
				if (wasLoaded) {
					String expected;
					if (fqn.endsWith("FunkyFactory2")) {
						expected = "I was there";
					}
					else if (fqn.endsWith("SqlToCypherTranslatorFactory")) {
						expected = "RETURN 1";
					}
					else {
						expected = "SELECT 1";
					}
					assertThat(connection.nativeSQL("SELECT 1")).isEqualTo(expected);
				}
			}
			finally {
				err.flush();
				System.setErr(original);

				Logger.getLogger("org.neo4j.jdbc.connection").removeHandler(handler);
			}

			var errorOutput = output.toString(StandardCharsets.UTF_8);

			if (producedBogusMessage) {
				assertThat(errorOutput).contains("This could have been a Runtime.getRuntime().exec() call.");
			}
			else {
				assertThat(errorOutput).doesNotContain("This could have been a Runtime.getRuntime().exec() call.");
			}
			if (wasLoaded) {
				assertThat(handler.messages).doesNotContain("Class {0} cannot be used as translator factory");
			}
			else {
				assertThat(handler.messages).contains("Class {0} cannot be used as translator factory");
			}
		};

		if (useSystemProperty) {
			SystemLambda.restoreSystemProperties(actualTest);
		}
		else {
			SystemLambda.withEnvironmentVariable("NEO4J_JDBC_ALLOWED_TRANSLATOR_FACTORIES", allowList)
				.execute(actualTest);
		}

	}

}
