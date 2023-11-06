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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
	protected final Neo4jContainer<?> neo4j = TestUtils.getNeo4jContainer("neo4j:5.13.0-enterprise")
		.withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
		.withReuse(true);

	@BeforeAll
	void startNeo4j() {
		this.neo4j.start();
	}

	@BeforeEach
	void createConnectionAndDriver() throws SQLException {
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
		var results = this.connection.getMetaData().getProcedures(null, null, null);

		var resultCount = 0;
		while (results.next()) {
			resultCount++;
			Assertions.assertThat(results.getString(1)).isNotNull();
		}
		assertThat(resultCount).isGreaterThan(0);
	}

	@Test
	public void getMetaDataProcedure() throws SQLException {
		var results = this.connection.getMetaData().getProcedures(null, null, "tx.getMetaData");

		var resultCount = 0;
		while (results.next()) {
			resultCount++;
			assertThat(results.getString(3)).isEqualTo("tx.getMetaData");
		}

		assertThat(resultCount).isEqualTo(1);
		results.close();

		results = this.connection.getMetaData().getProcedures(null, null, "tx.setMetaData");

		resultCount = 0;
		while (results.next()) {
			resultCount++;
			assertThat(results.getString(3)).isEqualTo("tx.setMetaData");
		}

		assertThat(resultCount).isEqualTo(1);
	}

	@Test
	public void passingAnUnknownCatalogMustError() throws SQLException {
		assertThatExceptionOfType(SQLException.class)
			.isThrownBy(() -> this.connection.getMetaData().getProcedures("somethingRandom", null, null));
	}

	@Test
	public void proceduresShouldBeExecutable() throws SQLException {
		var executable = this.connection.getMetaData().allProceduresAreCallable();

		assertThat(executable).isTrue();
	}

	@Test
	public void afterDenyingExecutionAllProceduresAreCallableShouldFail() throws SQLException {
		this.connection.createStatement().executeQuery("DENY EXECUTE PROCEDURE tx.getMetaData ON DBMS TO admin");
		var executable = this.connection.getMetaData().allProceduresAreCallable();

		assertThat(executable).isFalse();

		this.connection.createStatement()
			.executeQuery("REVOKE DENY EXECUTE PROCEDURE tx.getMetaData ON DBMS FROM admin");
		executable = this.connection.getMetaData().allProceduresAreCallable();

		assertThat(executable).isTrue();

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
	public void testGetDatabaseProductName() throws SQLException {
		var productName = this.connection.getMetaData().getDatabaseProductName();

		assertThat(productName).isEqualTo("Neo4j Kernel-enterprise-5.13.0");
	}

	@Test
	public void testGetDatabaseProductVersion() throws SQLException {
		var productName = this.connection.getMetaData().getDatabaseProductVersion();

		assertThat(productName).isEqualTo("5.13.0");
	}

	// @Test
	// public void testColumnNamesGetAllCatalogs() throws SQLException {
	// var catalogRs = this.connection.getMetaData().getCatalogs();
	// assertThat(catalogRs.getMetaData().getCatalogName(1)).isEqualTo("TABLE_CAT");
	// }
	//
	// @Test
	// public void testColumnNamesGetAllProcedures() throws SQLException {
	// var proceduresRs = this.connection.getMetaData().getProcedures(null, null, null);
	//
	// assertThat(proceduresRs.getMetaData().getCatalogName(1)).isEqualTo("PROCEDURE_CAT");
	// assertThat(proceduresRs.getMetaData().getCatalogName(2)).isEqualTo("PROCEDURE_SCHEM");
	// assertThat(proceduresRs.getMetaData().getCatalogName(3)).isEqualTo("PROCEDURE_NAME");
	// //these should not change as are reserved for the spec
	// assertThat(proceduresRs.getMetaData().getCatalogName(4)).isEqualTo("RESERVED_1");
	// assertThat(proceduresRs.getMetaData().getCatalogName(5)).isEqualTo("RESERVED_2");
	// assertThat(proceduresRs.getMetaData().getCatalogName(6)).isEqualTo("RESERVED_3");
	//
	// assertThat(proceduresRs.getMetaData().getCatalogName(7)).isEqualTo("REMARKS");
	// assertThat(proceduresRs.getMetaData().getCatalogName(8)).isEqualTo("PROCEDURE_TYPE");
	// }

}
