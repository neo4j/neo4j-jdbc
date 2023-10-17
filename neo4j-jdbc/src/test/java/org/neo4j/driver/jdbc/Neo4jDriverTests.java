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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class Neo4jDriverTests {

	@Test
	void driverMustNotMarkItselfAsJDBCCompliant() {
		Assertions.assertThat(new Neo4jDriver().jdbcCompliant()).isFalse();
	}

	@Test
	void driverMustConnect() {

		var driver = new Neo4jDriver();
		Assertions.assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> driver.connect(null, null));
	}

	@ParameterizedTest
	@ValueSource(strings = "jdbc:neo4j:notyet")
	void driverMustAcceptValidUrl(String url) {

		var driver = new Neo4jDriver();
		Assertions.assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> driver.acceptsURL(url));
	}

	@Test
	void driverMustReturnPropertyInfo() {

		var driver = new Neo4jDriver();
		Assertions.assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> driver.getPropertyInfo(null, null));
	}

	@Test
	void getParentLoggerShouldWork() {

		var driver = new Neo4jDriver();
		Assertions.assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(driver::getParentLogger);
	}

}
