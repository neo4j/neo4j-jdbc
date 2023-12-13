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
package org.neo4j.driver.it.cp;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import com.sun.security.auth.module.UnixSystem;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Essentially hammering a cluster and getting back the results.
 */
@Testcontainers(disabledWithoutDocker = true)
@DisabledOnOs(OS.WINDOWS)
@DisabledInNativeImage // This does only check for something from the maven plugin for
						// GraalVM, which ofc is not there when the tests run to be
						// discovered
@DisabledIfSystemProperty(named = "native", matches = ".+", disabledReason = "Because $tools.")
@DisabledIfSystemProperty(named = "skipClusterIT", matches = ".+", disabledReason = "Because $tools.")
class ClusterIT {

	static final String USERNAME = "neo4j";
	static final String PASSWORD = "verysecret";

	protected static final UnixSystem unixSystem = new UnixSystem();

	@SuppressWarnings("resource")
	@Container
	protected static final ComposeContainer neo4jCluster = new ComposeContainer(
			new File("src/test/resources/cc/docker-compose.yml"))
		.withEnv(Map.of("USER_ID", Long.toString(unixSystem.getUid()), "GROUP_ID", Long.toString(unixSystem.getGid())))
		.withExposedService("server1", 7687, Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(5)))
		.withExposedService("server2", 7687, Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(5)))
		.withExposedService("server3", 7687, Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(5)))
		.withExposedService("server4", 7687, Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(5)))
		.withLocalCompose(true);

	@BeforeAll
	static void clearData() throws SQLException {

		var wait = Duration.ofSeconds(5);
		var currentWait = wait.toMillis();
		var n = 0;
		do {
			++n;
			try (var stmt = getClusterConnection(true).createStatement()) {
				stmt.execute("""
						MATCH (n)
						CALL {
							WITH n DETACH DELETE n
						} IN TRANSACTIONS OF 1000 ROWs""");
				break;
			}
			catch (SQLException caught) {
				if (n > 10) {
					throw caught;
				}
				if (caught.getCause().getMessage().startsWith("org.neo4j.kernel.availability.UnavailableException")) {
					try {
						Thread.sleep(currentWait);
						currentWait = (long) (wait.toMillis() * Math.pow(1.1, n));
					}
					catch (InterruptedException ex) {
						throw new RuntimeException(ex);
					}
				}
			}
		}
		while (true);
	}

	@Test
	void writeAndReadsShouldWorkAgainstCluster() throws SQLException {

		int numNodes = 50;
		var executor = Executors.newWorkStealingPool();
		var futures = IntStream.rangeClosed(1, numNodes).mapToObj(i -> CompletableFuture.supplyAsync(() -> {
			try (var connection = getClusterConnection(true);
					var stmt = connection.prepareStatement("CREATE (a:Node {cnt: $1}) RETURN elementId(a) AS id")) {
				stmt.setInt(1, i);
				try (var result = stmt.executeQuery()) {
					result.next();
					return result.getString("id");
				}
			}
			catch (SQLException caught) {
				throw new RuntimeException(caught);
			}
		}, executor)).toArray(CompletableFuture[]::new);
		CompletableFuture.allOf(futures).join();
		try (var connection = getClusterConnection(false);
				var stmt = connection.createStatement();
				var result = stmt.executeQuery("MATCH (a:Node) RETURN a.cnt AS cnt")) {

			var properties = new HashSet<Integer>();
			while (result.next()) {
				properties.add(result.getInt("cnt"));
			}
			Assertions.assertThat(properties).hasSize(numNodes);
		}

	}

	static Connection getClusterConnection(boolean rw) throws SQLException {

		var server = "server" + ThreadLocalRandom.current().nextInt(1, rw ? 4 : 5);
		var url = "jdbc:neo4j://%s:%d".formatted("localhost", neo4jCluster.getServicePort("server1", 7687));
		var driver = DriverManager.getDriver(url);
		var properties = new Properties();
		properties.put("user", USERNAME);
		properties.put("password", PASSWORD);
		properties.put("timeout", "5000");
		return driver.connect(url, properties);
	}

}
