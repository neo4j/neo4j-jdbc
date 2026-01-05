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

import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.stefanbirkner.systemlambda.SystemLambda;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.commons.util.ReflectionUtils;
import org.neo4j.jdbc.Neo4jDriver;
import org.neo4j.jdbc.authn.spi.Authentication;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.neo4j.Neo4jContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisabledInNativeImage
class Neo4jDriverExtensionsIT {

	@SuppressWarnings("resource") // On purpose to reuse this
	protected final Neo4jContainer neo4j = TestUtils.getNeo4jContainer();

	@BeforeAll
	void startNeo4j() {
		this.neo4j.start();
	}

	@Test
	void fromEnvShouldWork() throws Exception {

		SystemLambda
			.withEnvironmentVariable("NEO4J_URI",
					"jdbc:neo4j://%s:%s".formatted(this.neo4j.getHost(), this.neo4j.getMappedPort(7687)))
			.and("NEO4J_USERNAME", "neo4j")
			.and("NEO4J_PASSWORD", this.neo4j.getAdminPassword())
			.and("NEO4J_SQL_TRANSLATION_ENABLED", "true")
			.execute(() -> {
				try (@SuppressWarnings("deprecation")
				var connection = Neo4jDriver.withSQLTranslation()
					.withProperties(Map.of("rewritePlaceholders", "true"))
					.fromEnv()
					.orElseThrow()) {

					assertConnection(connection);
				}
			});

	}

	@Test
	void shouldLoadFromExplicitFile() throws Exception {

		var envFile = Files.createTempFile("neo4j-jdbc", ".txt");
		Files.write(envFile, List.of("NEO4J_USERNAME=neo4j",
				"NEO4J_URI=jdbc:neo4j://%s:%s".formatted(this.neo4j.getHost(), this.neo4j.getMappedPort(7687)),
				"NEO4J_PASSWORD=%s".formatted(this.neo4j.getAdminPassword()), "NEO4J_SQL_TRANSLATION_ENABLED=true"));

		try (@SuppressWarnings("deprecation")
		var connection = Neo4jDriver.withSQLTranslation()
			.withProperties(Map.of("rewritePlaceholders", "true"))
			.fromEnv(envFile.getParent(), envFile.getFileName().toString())
			.orElseThrow()) {

			assertConnection(connection);
		}
	}

	Stream<Arguments> allBuilderPermutationsShouldWork() throws Exception {

		var neo4jUri = "jdbc:neo4j://%s:%d?user=neo4j&password=%s".formatted(this.neo4j.getHost(),
				this.neo4j.getMappedPort(7687), this.neo4j.getAdminPassword());

		var builder = Stream.<Arguments>builder();

		var methods = List.of(
				new NameAndArguments("withAuthenticationSupplier", new Class<?>[] { Supplier.class },
						new Object[] { (Supplier<Authentication>) () -> Authentication.usernameAndPassword("neo4j",
								this.neo4j.getAdminPassword()) }),
				new NameAndArguments("withProperties", new Class<?>[] { Map.class },
						new Object[] { Map.of("rewritePlaceholders", "true") }),
				new NameAndArguments("withSQLTranslation", new Class<?>[0], new Object[0]));
		var permutations = new ArrayList<List<NameAndArguments>>();
		for (int i = 2; i <= methods.size(); ++i) {
			permutations.addAll(permutate(i, methods));
		}
		methods.forEach(v -> permutations.add(List.of(v)));

		for (var stack : permutations) {
			Object target = null;
			for (NameAndArguments nameAndArgument : stack) {
				var method = ReflectionUtils
					.findMethod((target != null) ? target.getClass() : Neo4jDriver.class, nameAndArgument.name(),
							nameAndArgument.parameterTypes())
					.orElseThrow();
				method.setAccessible(true);
				target = method.invoke(target, nameAndArgument.arguments());
			}
			var specifyEnvStep = (Neo4jDriver.SpecifyEnvStep) target;

			builder.add(Arguments.argumentSet(
					"fromEnv: " + stack.stream().map(NameAndArguments::name).collect(Collectors.joining(".")),
					(Supplier<Connection>) () -> {
						try {
							return SystemLambda.withEnvironmentVariable("NEO4J_URI", neo4jUri)
								.execute(() -> specifyEnvStep.fromEnv().orElseThrow());
						}
						catch (Exception ex) {
							throw new RuntimeException(ex);
						}
					}));

			var envFile = Files.createTempFile("neo4j-jdbc", ".txt");
			Files.write(envFile, List.of("NEO4J_URI=%s".formatted(neo4jUri)));

			var tmpDir = Files.createTempDirectory("neo4j-jdbc");
			var dotEnvFile = Files.createFile(tmpDir.resolve(".env"));
			Files.write(dotEnvFile, List.of("NEO4J_URI=%s".formatted(neo4jUri)));

			builder.add(Arguments.argumentSet(
					"fromEnv(File): " + stack.stream().map(NameAndArguments::name).collect(Collectors.joining(".")),
					(Supplier<Connection>) () -> {
						try {
							return specifyEnvStep.fromEnv(envFile.getParent(), envFile.getFileName().toString())
								.orElseThrow();
						}
						catch (SQLException ex) {
							throw new RuntimeException(ex);
						}
					}));

			builder.add(Arguments.argumentSet(
					"fromEnv(Dir): " + stack.stream().map(NameAndArguments::name).collect(Collectors.joining(".")),
					(Supplier<Connection>) () -> {
						try {
							return specifyEnvStep.fromEnv(tmpDir).orElseThrow();
						}
						catch (SQLException ex) {
							throw new RuntimeException(ex);
						}
					}));
		}

		return builder.build();
	}

	@ParameterizedTest
	@MethodSource
	void allBuilderPermutationsShouldWork(Supplier<Connection> connectionSupplier) throws SQLException {

		try (var connection = connectionSupplier.get(); var statement = connection.createStatement()) {
			assertThatNoException().isThrownBy(() -> statement.executeQuery("/*+ NEO4J FORCE_CYPHER */ RETURN 1"));
			assertThatNoException().isThrownBy(statement::close);
		}
	}

	private static void assertConnection(Connection connection) throws SQLException {
		var statement = connection.createStatement();
		assertThatNoException().isThrownBy(() -> statement.executeQuery("SELECT 1"));
		assertThatNoException().isThrownBy(statement::close);

		var ps = connection.prepareStatement("/*+ NEO4J FORCE_CYPHER */ RETURN ?");
		ps.setInt(1, 23);
		var rs = ps.executeQuery();
		assertThat(rs.next()).isTrue();
		assertThat(rs.getInt(1)).isEqualTo(23);
	}

	private static List<List<NameAndArguments>> permutate(int n, List<NameAndArguments> input) {

		var workingList = new ArrayList<>(input);
		var result = new ArrayList<List<NameAndArguments>>();
		result.add(List.copyOf(input.subList(0, n)));

		int[] c = new int[workingList.size()];
		int i = 1;
		while (i < input.size()) {
			if (c[i] < i) {
				int idx;
				if (i % 2 == 0) {
					idx = 0;
				}
				else {
					idx = c[i];
				}
				Collections.swap(workingList, idx, i);
				result.add(List.copyOf(workingList.subList(0, n)));
				c[i] += 1;
				i = 1;
			}
			else {
				c[i++] = 0;
			}
		}
		return result;
	}

	private record NameAndArguments(String name, Class<?>[] parameterTypes, Object[] arguments) {

	}

}
