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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.neo4j.driver.jdbc.Neo4jDriver;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DatabaseMetadataIT {

	private Connection connection;

	@SuppressWarnings("resource") // On purpose to reuse this
	protected final Neo4jContainer<?> neo4j = TestUtils.getNeo4jContainer("neo4j:5.13.0-enterprise");

	@BeforeAll
	void startNeo4j() throws SQLException {
		this.neo4j.start();

	}

	@BeforeAll
	void startDriver() throws SQLException {
		var driver = new Neo4jDriver();

		var properties = new Properties();
		properties.put("user", "neo4j");
		properties.put("password", this.neo4j.getAdminPassword());

		var url = "jdbc:neo4j://%s:%s".formatted(this.neo4j.getHost(), this.neo4j.getMappedPort(7687));
		this.connection = driver.connect(url, properties);
		assertThat(this.connection).isNotNull();
	}

	@Test
	void getAllProcedures() throws SQLException {
		try (var results = this.connection.getMetaData().getProcedures(null, null, null)) {

			var resultCount = 0;
			while (results.next()) {
				resultCount++;
				assertThat(results.getString(1)).isNotNull();
			}
			assertThat(resultCount).isGreaterThan(0);
		}
	}

	@Test
	public void getMetaDataProcedure() throws SQLException {
		var resultCount = 0;
		try (var results = this.connection.getMetaData().getProcedures(null, null, "tx.getMetaData")) {

			while (results.next()) {
				resultCount++;
				assertThat(results.getString(3)).isEqualTo("tx.getMetaData");
			}

			assertThat(resultCount).isEqualTo(1);
		}

		resultCount = 0;
		try (var results = this.connection.getMetaData().getProcedures(null, null, "tx.setMetaData")) {

			while (results.next()) {
				resultCount++;
				assertThat(results.getString(3)).isEqualTo("tx.setMetaData");
			}

			assertThat(resultCount).isEqualTo(1);
		}
	}

	@Test
	public void passingAnUnknownCatalogMustError() throws SQLException {
		var metaData = this.connection.getMetaData();
		assertThatExceptionOfType(SQLException.class)
			.isThrownBy(() -> metaData.getProcedures("somethingRandom", null, null));
	}

	@Test
	public void proceduresShouldBeExecutable() throws SQLException {
		var executable = this.connection.getMetaData().allProceduresAreCallable();

		assertThat(executable).isTrue();
	}

	@Test
	public void afterDenyingExecutionAllProceduresAreCallableShouldFail() throws SQLException {
		executeQueryWithoutResult("DENY EXECUTE PROCEDURE tx.getMetaData ON DBMS TO admin");
		var executable = this.connection.getMetaData().allProceduresAreCallable();

		assertThat(executable).isFalse();

		executeQueryWithoutResult("REVOKE DENY EXECUTE PROCEDURE tx.getMetaData ON DBMS FROM admin");
		executable = this.connection.getMetaData().allProceduresAreCallable();

		assertThat(executable).isTrue();

	}

	void executeQueryWithoutResult(String query) throws SQLException {
		try (var stmt = this.connection.createStatement(); var result = stmt.executeQuery(query)) {
			result.next();
		}
	}

	@Test
	public void getAllCatalogsShouldReturnAllDbs() throws SQLException {
		var catalogRs = this.connection.getMetaData().getCatalogs();

		List<String> names = new ArrayList<>();
		while (catalogRs.next()) {
			var name = catalogRs.getString(1);
			names.add(name);
		}

		assertThat(names).contains("system", "neo4j");
	}

	@Test
	public void testGetUser() throws SQLException {
		var username = this.connection.getMetaData().getUserName();

		assertThat(username).isEqualTo("neo4j");
	}

	@Test
	public void testGetDatabaseProductNameShouldReturnNeo4j() throws SQLException {
		var productName = this.connection.getMetaData().getDatabaseProductName();

		assertThat(productName).isEqualTo("Neo4j Kernel-enterprise-5.13.0");
	}

	@Test
	public void getDatabaseProductVersionShouldReturnTestContainerVersion() throws SQLException {
		var productName = this.connection.getMetaData().getDatabaseProductVersion();

		assertThat(productName).isEqualTo("5.13.0");
	}

	@Test
	public void getAllTablesShouldReturnAllLabelsOnATable() throws SQLException {
		List<String> expectedLabels = new ArrayList<>();
		expectedLabels.add("TestLabel1");
		expectedLabels.add("TestLabel2");

		for (String label : expectedLabels) {
			this.connection.createStatement().executeQuery("Create (:%s)".formatted(label));
		}

		try (var labelsRs = this.connection.getMetaData().getTables(null, null, "", null)) {
			while (labelsRs.next()) {
				var labelName = labelsRs.getString(3);
				assertThat(expectedLabels).contains(labelName);
			}
		}
	}

	@Test
	public void getAllTablesShouldReturnCurrentCatalog() throws SQLException {
		List<String> expectedLabels = new ArrayList<>();
		expectedLabels.add("TestLabel1");
		expectedLabels.add("TestLabel2");

		for (String label : expectedLabels) {
			executeQueryWithoutResult("Create (:%s)".formatted(label));
		}

		try (var labelsRs = this.connection.getMetaData().getTables(null, null, "", null)) {
			while (labelsRs.next()) {
				var catalog = labelsRs.getString(1);
				assertThat(catalog).isEqualTo("neo4j");
			}
		}
	}

	@Test
	public void getAllTablesShouldOnlyReturnSpecifiedCatalog() throws SQLException {
		List<String> expectedLabels = new ArrayList<>();
		expectedLabels.add("TestLabel1");
		expectedLabels.add("TestLabel2");

		for (String label : expectedLabels) {
			this.connection.createStatement().executeQuery("Create (:%s)".formatted(label));
		}

		try (var labelsRs = this.connection.getMetaData().getTables(null, null, expectedLabels.get(0), null)) {
			assertThat(labelsRs.next()).isTrue();
			var catalog = labelsRs.getString(3);
			assertThat(catalog).isEqualTo(expectedLabels.get(0));
			assertThat(labelsRs.next()).isFalse();
		}
	}

	@Test
	public void getAllTablesShouldErrorIfYouPassGarbageToCatalog() throws SQLException {
		var getMetadata = this.connection.getMetaData();
		assertThatExceptionOfType(SQLException.class)
			.isThrownBy(() -> getMetadata.getTables("someRandomGarbage", null, "", new String[0]));
	}

	@Test
	public void getAllTablesShouldErrorIfYouPassAValidCatalogButNotTheCurrentOne() throws SQLException {
		var getMetadata = this.connection.getMetaData();
		assertThatExceptionOfType(SQLException.class)
			.isThrownBy(() -> getMetadata.getTables("system", null, "", new String[0]));
	}

}
