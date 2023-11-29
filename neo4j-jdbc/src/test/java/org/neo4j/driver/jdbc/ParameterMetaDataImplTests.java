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

import java.sql.ParameterMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Wrapper;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParameterMetaDataImplTests {

	private ParameterMetaDataImpl metaData;

	@Test
	void shouldHaveNoParameterCount() {
		this.metaData = new ParameterMetaDataImpl();

		assertThat(this.metaData.getParameterCount()).isEqualTo(0);
	}

	@ParameterizedTest
	@MethodSource("getUnwrapArgs")
	void shouldUnwrap(Class<?> cls, boolean shouldUnwrap) throws SQLException {
		// given
		this.metaData = new ParameterMetaDataImpl();

		// when & then
		if (shouldUnwrap) {
			var unwrapped = this.metaData.unwrap(cls);
			assertThat(unwrapped).isInstanceOf(cls);
		}
		else {
			assertThatThrownBy(() -> this.metaData.unwrap(cls)).isInstanceOf(SQLException.class);
		}
	}

	@ParameterizedTest
	@MethodSource("getUnwrapArgs")
	void shouldHandleIsWrapperFor(Class<?> cls, boolean shouldUnwrap) throws SQLException {
		// given
		this.metaData = new ParameterMetaDataImpl();

		// when
		var wrapperFor = this.metaData.isWrapperFor(cls);

		// then
		assertThat(wrapperFor).isEqualTo(shouldUnwrap);
	}

	private static Stream<Arguments> getUnwrapArgs() {
		return Stream.of(Arguments.of(ParameterMetaDataImpl.class, true), Arguments.of(ParameterMetaData.class, true),
				Arguments.of(Wrapper.class, true), Arguments.of(Object.class, true),
				Arguments.of(ResultSet.class, false));
	}

}
