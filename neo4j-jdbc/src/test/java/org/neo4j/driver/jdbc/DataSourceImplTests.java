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

import com.openpojo.reflection.impl.PojoClassFactory;
import com.openpojo.validation.ValidatorBuilder;
import com.openpojo.validation.test.impl.GetterTester;
import com.openpojo.validation.test.impl.SetterTester;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class DataSourceImplTests {

	@Test
	void getLogWriter() {

		var ds = new DataSourceImpl();
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(ds::getLogWriter);
	}

	@Test
	void setLogWriter() {

		var ds = new DataSourceImpl();
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> ds.setLogWriter(null));
	}

	@Test
	void setLoginTimeout() {

		var ds = new DataSourceImpl();
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> ds.setLoginTimeout(1));
	}

	@Test
	void getLoginTimeout() {

		var ds = new DataSourceImpl();
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(ds::getLoginTimeout);
	}

	@Test
	void getParentLogger() {

		var ds = new DataSourceImpl();
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(ds::getParentLogger);
	}

	@Test
	void unwrap() {

		var ds = new DataSourceImpl();
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> ds.unwrap(String.class));
	}

	@Test
	void isWrapperFor() {

		var ds = new DataSourceImpl();
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> ds.isWrapperFor(String.class));
	}

	@Test
	void getUrlShouldRequireProtocol() {

		var ds = new DataSourceImpl();
		ds.setServerName("whatever");
		assertThatNullPointerException().isThrownBy(ds::getUrl)
			.withMessage("The network protocol must be specified on the data source");
	}

	@Test
	void getUrlShouldRequireServerName() {

		var ds = new DataSourceImpl();
		ds.setNetworkProtocol("whatever");
		assertThatNullPointerException().isThrownBy(ds::getUrl)
			.withMessage("The server name must be specified on the data source");
	}

	@Test
	void getUrlShouldUseCorrectFormat() {

		var ds = new DataSourceImpl();
		ds.setNetworkProtocol("onlyfortesting");
		ds.setServerName("localhost");
		ds.setDatabaseName("movies");
		assertThat(ds.getUrl()).isEqualTo("jdbc:neo4j:onlyfortesting://localhost:7687/movies");
	}

	@Test
	void validateSettersAndGetters() {

		var pojoClass = PojoClassFactory.getPojoClass(DataSourceImpl.class);
		var validator = ValidatorBuilder.create().with(new SetterTester()).with(new GetterTester()).build();

		validator.validate(pojoClass);
	}

	@Test
	void passwordShouldWork() {

		var ds = new DataSourceImpl();
		ds.setPassword("foo");
		assertThat(ds.getPassword()).isEqualTo("foo");
	}

}
