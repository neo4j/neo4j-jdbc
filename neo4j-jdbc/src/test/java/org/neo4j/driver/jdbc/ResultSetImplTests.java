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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.driver.jdbc.internal.bolt.BoltConnection;
import org.neo4j.driver.jdbc.internal.bolt.response.PullResponse;
import org.neo4j.driver.jdbc.internal.bolt.response.RunResponse;
import org.neo4j.driver.jdbc.internal.bolt.values.Record;
import org.neo4j.driver.jdbc.internal.bolt.values.Value;
import org.neo4j.driver.jdbc.internal.bolt.values.Values;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class ResultSetImplTests {

	private static final int INDEX = 1;

	private static final String LABEL = "label";

	private ResultSet resultSet;

	@ParameterizedTest
	@MethodSource("getStringArgs")
	void shouldProcessValueOnGetString(Value value, VerificationLogic<String> verificationLogic, boolean indexAccess)
			throws SQLException {
		// given
		this.resultSet = setupWithValue(value);

		// when & then
		verificationLogic.run(() -> indexAccess ? this.resultSet.getString(INDEX) : this.resultSet.getString(LABEL));
	}

	private static Stream<Arguments> getStringArgs() {
		return Stream.of(
				// string handling
				Arguments.of(Values.value("0"),
						Named.<VerificationLogic<String>>of("verify returns '0'",
								supplier -> assertThat(supplier.get()).isEqualTo("0"))),
				Arguments.of(Values.value(""),
						Named.<VerificationLogic<String>>of("verify returns ''",
								supplier -> assertThat(supplier.get()).isEqualTo(""))),
				Arguments.of(Values.value("testing"),
						Named.<VerificationLogic<String>>of("verify returns 'testing'",
								supplier -> assertThat(supplier.get()).isEqualTo("testing"))),
				// null handling
				Arguments.of(Values.NULL,
						Named.<VerificationLogic<Boolean>>of("verify returns null",
								supplier -> assertThat(supplier.get()).isNull())),
				// other types handling
				Arguments.of(Values.value(0),
						Named.<VerificationLogic<Boolean>>of("verify throws exception",
								supplier -> assertThatThrownBy(supplier::get).isInstanceOf(SQLException.class))),
				Arguments.of(Values.value(true),
						Named.<VerificationLogic<Boolean>>of("verify throws exception",
								supplier -> assertThatThrownBy(supplier::get).isInstanceOf(SQLException.class))))
			// map each set of arguments to both index and label access methods
			.flatMap(ResultSetImplTests::mapArgumentToBothIndexAndLabelAccess);
	}

	@ParameterizedTest
	@MethodSource("getBooleanArgs")
	void shouldProcessValueOnGetBoolean(Value value, VerificationLogic<Boolean> verificationLogic, boolean indexAccess)
			throws SQLException {
		// given
		this.resultSet = setupWithValue(value);

		// when & then
		verificationLogic.run(() -> indexAccess ? this.resultSet.getBoolean(INDEX) : this.resultSet.getBoolean(LABEL));
	}

	private static Stream<Arguments> getBooleanArgs() {
		return Stream.of(
				// string handling
				Arguments.of(Values.value("0"),
						Named.<VerificationLogic<Boolean>>of("verify returns false",
								supplier -> assertThat(supplier.get()).isFalse())),
				Arguments.of(Values.value("1"),
						Named.<VerificationLogic<Boolean>>of("verify returns true",
								supplier -> assertThat(supplier.get()).isTrue())),
				Arguments.of(Values.value("-1"),
						Named.<VerificationLogic<Boolean>>of("verify throws exception",
								supplier -> assertThatThrownBy(supplier::get).isInstanceOf(SQLException.class))),
				Arguments.of(Values.value("5"),
						Named.<VerificationLogic<Boolean>>of("verify throws exception",
								supplier -> assertThatThrownBy(supplier::get).isInstanceOf(SQLException.class))),
				Arguments.of(Values.value(""),
						Named.<VerificationLogic<Boolean>>of("verify throws exception",
								supplier -> assertThatThrownBy(supplier::get).isInstanceOf(SQLException.class))),
				Arguments.of(Values.value("testing"),
						Named.<VerificationLogic<Boolean>>of("verify throws exception",
								supplier -> assertThatThrownBy(supplier::get).isInstanceOf(SQLException.class))),
				// char handling
				Arguments.of(Values.value('0'),
						Named.<VerificationLogic<Boolean>>of("verify returns false",
								supplier -> assertThat(supplier.get()).isFalse())),
				Arguments.of(Values.value('1'),
						Named.<VerificationLogic<Boolean>>of("verify returns true",
								supplier -> assertThat(supplier.get()).isTrue())),
				Arguments.of(Values.value('5'),
						Named.<VerificationLogic<Boolean>>of("verify throws exception",
								supplier -> assertThatThrownBy(supplier::get).isInstanceOf(SQLException.class))),
				Arguments.of(Values.value(' '),
						Named.<VerificationLogic<Boolean>>of("verify throws exception",
								supplier -> assertThatThrownBy(supplier::get).isInstanceOf(SQLException.class))),
				Arguments.of(Values.value('t'),
						Named.<VerificationLogic<Boolean>>of("verify throws exception",
								supplier -> assertThatThrownBy(supplier::get).isInstanceOf(SQLException.class))),
				// number handling
				Arguments.of(Values.value(0),
						Named.<VerificationLogic<Boolean>>of("verify returns false",
								supplier -> assertThat(supplier.get()).isFalse())),
				Arguments.of(Values.value(1),
						Named.<VerificationLogic<Boolean>>of("verify returns true",
								supplier -> assertThat(supplier.get()).isTrue())),
				Arguments.of(Values.value(-1),
						Named.<VerificationLogic<Boolean>>of("verify throws exception",
								supplier -> assertThatThrownBy(supplier::get).isInstanceOf(SQLException.class))),
				Arguments.of(Values.value(5),
						Named.<VerificationLogic<Boolean>>of("verify throws exception",
								supplier -> assertThatThrownBy(supplier::get).isInstanceOf(SQLException.class))),
				// null handling
				Arguments.of(Values.NULL,
						Named.<VerificationLogic<Boolean>>of("verify returns false",
								supplier -> assertThat(supplier.get()).isFalse())),
				// boolean handling
				Arguments.of(Values.value(false),
						Named.<VerificationLogic<Boolean>>of("verify returns false",
								supplier -> assertThat(supplier.get()).isFalse())),
				Arguments.of(Values.value(true),
						Named.<VerificationLogic<Boolean>>of("verify returns true",
								supplier -> assertThat(supplier.get()).isTrue())),
				// other types handling
				Arguments.of(Values.value(new byte[] {}),
						Named.<VerificationLogic<Boolean>>of("verify throws exception",
								supplier -> assertThatThrownBy(supplier::get).isInstanceOf(SQLException.class))),
				Arguments.of(Values.value(List.of("value")),
						Named.<VerificationLogic<Boolean>>of("verify throws exception",
								supplier -> assertThatThrownBy(supplier::get).isInstanceOf(SQLException.class))))
			// map each set of arguments to both index and label access methods
			.flatMap(ResultSetImplTests::mapArgumentToBothIndexAndLabelAccess);
	}

	@ParameterizedTest
	@MethodSource("getByteArgs")
	void shouldProcessValueOnGetByte(Value value, VerificationLogic<Byte> verificationLogic, boolean indexAccess)
			throws SQLException {
		// given
		this.resultSet = setupWithValue(value);

		// when & then
		verificationLogic.run(() -> indexAccess ? this.resultSet.getByte(INDEX) : this.resultSet.getByte(LABEL));
	}

	private static Stream<Arguments> getByteArgs() {
		return Stream.of(
				// number handling
				Arguments.of(Values.value(0),
						Named.<VerificationLogic<Byte>>of("verify returns 0",
								supplier -> assertThat(supplier.get()).isEqualTo((byte) 0))),
				Arguments.of(Values.value(Byte.MIN_VALUE),
						Named.<VerificationLogic<Byte>>of("verify returns Byte.MIN_VALUE",
								supplier -> assertThat(supplier.get()).isEqualTo(Byte.MIN_VALUE))),
				Arguments.of(Values.value(Byte.MAX_VALUE),
						Named.<VerificationLogic<Byte>>of("verify returns Byte.MAX_VALUE",
								supplier -> assertThat(supplier.get()).isEqualTo(Byte.MAX_VALUE))),
				Arguments.of(Values.value(Byte.MAX_VALUE + 1),
						Named.<VerificationLogic<Byte>>of("verify throws exception",
								supplier -> assertThatThrownBy(supplier::get).isInstanceOf(SQLException.class))),
				Arguments.of(Values.value(Byte.MIN_VALUE - 1),
						Named.<VerificationLogic<Byte>>of("verify throws exception",
								supplier -> assertThatThrownBy(supplier::get).isInstanceOf(SQLException.class))),
				// null handling
				Arguments.of(Values.NULL,
						Named.<VerificationLogic<Byte>>of("verify returns 0",
								supplier -> assertThat(supplier.get()).isEqualTo((byte) 0))),
				// other types handling
				Arguments.of(Values.value(new byte[] {}),
						Named.<VerificationLogic<Boolean>>of("verify throws exception",
								supplier -> assertThatThrownBy(supplier::get).isInstanceOf(SQLException.class))),
				Arguments.of(Values.value(List.of("value")),
						Named.<VerificationLogic<Boolean>>of("verify throws exception",
								supplier -> assertThatThrownBy(supplier::get).isInstanceOf(SQLException.class))))
			// map each set of arguments to both index and label access methods
			.flatMap(ResultSetImplTests::mapArgumentToBothIndexAndLabelAccess);
	}

	@ParameterizedTest
	@MethodSource("getShortArgs")
	void shouldProcessValueOnGetShort(Value value, VerificationLogic<Short> verificationLogic, boolean indexAccess)
			throws SQLException {
		// given
		this.resultSet = setupWithValue(value);

		// when & then
		verificationLogic.run(() -> indexAccess ? this.resultSet.getShort(INDEX) : this.resultSet.getShort(LABEL));
	}

	private static Stream<Arguments> getShortArgs() {
		return Stream.of(
				// number handling
				Arguments.of(Values.value(0),
						Named.<VerificationLogic<Short>>of("verify returns 0",
								supplier -> assertThat(supplier.get()).isEqualTo((short) 0))),
				Arguments.of(Values.value(Short.MIN_VALUE),
						Named.<VerificationLogic<Short>>of("verify returns Short.MIN_VALUE",
								supplier -> assertThat(supplier.get()).isEqualTo(Short.MIN_VALUE))),
				Arguments.of(Values.value(Short.MAX_VALUE),
						Named.<VerificationLogic<Short>>of("verify returns Short.MAX_VALUE",
								supplier -> assertThat(supplier.get()).isEqualTo(Short.MAX_VALUE))),
				Arguments.of(Values.value(Short.MAX_VALUE + 1),
						Named.<VerificationLogic<Short>>of("verify throws exception",
								supplier -> assertThatThrownBy(supplier::get).isInstanceOf(SQLException.class))),
				Arguments.of(Values.value(Short.MIN_VALUE - 1),
						Named.<VerificationLogic<Short>>of("verify throws exception",
								supplier -> assertThatThrownBy(supplier::get).isInstanceOf(SQLException.class))),
				// null handling
				Arguments.of(Values.NULL,
						Named.<VerificationLogic<Short>>of("verify returns 0",
								supplier -> assertThat(supplier.get()).isEqualTo((short) 0))),
				// other types handling
				Arguments.of(Values.value(new byte[] {}),
						Named.<VerificationLogic<Boolean>>of("verify throws exception",
								supplier -> assertThatThrownBy(supplier::get).isInstanceOf(SQLException.class))),
				Arguments.of(Values.value(List.of("value")),
						Named.<VerificationLogic<Boolean>>of("verify throws exception",
								supplier -> assertThatThrownBy(supplier::get).isInstanceOf(SQLException.class))))
			// map each set of arguments to both index and label access methods
			.flatMap(ResultSetImplTests::mapArgumentToBothIndexAndLabelAccess);
	}

	@ParameterizedTest
	@MethodSource("getIntArgs")
	void shouldProcessValueOnGetInt(Value value, VerificationLogic<Integer> verificationLogic, boolean indexAccess)
			throws SQLException {
		// given
		this.resultSet = setupWithValue(value);

		// when & then
		verificationLogic.run(() -> indexAccess ? this.resultSet.getInt(INDEX) : this.resultSet.getInt(LABEL));
	}

	private static Stream<Arguments> getIntArgs() {
		return Stream.of(
				// number handling
				Arguments.of(Values.value(0),
						Named.<VerificationLogic<Integer>>of("verify returns 0",
								supplier -> assertThat(supplier.get()).isEqualTo(0))),
				Arguments.of(Values.value(Integer.MIN_VALUE),
						Named.<VerificationLogic<Integer>>of("verify returns Integer.MIN_VALUE",
								supplier -> assertThat(supplier.get()).isEqualTo(Integer.MIN_VALUE))),
				Arguments.of(Values.value(Integer.MAX_VALUE),
						Named.<VerificationLogic<Integer>>of("verify returns Integer.MAX_VALUE",
								supplier -> assertThat(supplier.get()).isEqualTo(Integer.MAX_VALUE))),
				Arguments.of(Values.value(Integer.MAX_VALUE + 1L),
						Named.<VerificationLogic<Integer>>of("verify throws exception",
								supplier -> assertThatThrownBy(supplier::get).isInstanceOf(SQLException.class))),
				Arguments.of(Values.value(Integer.MIN_VALUE - 1L),
						Named.<VerificationLogic<Integer>>of("verify throws exception",
								supplier -> assertThatThrownBy(supplier::get).isInstanceOf(SQLException.class))),
				// null handling
				Arguments.of(Values.NULL,
						Named.<VerificationLogic<Integer>>of("verify returns 0",
								supplier -> assertThat(supplier.get()).isEqualTo(0))),
				// other types handling
				Arguments.of(Values.value(new byte[] {}),
						Named.<VerificationLogic<Boolean>>of("verify throws exception",
								supplier -> assertThatThrownBy(supplier::get).isInstanceOf(SQLException.class))),
				Arguments.of(Values.value(List.of("value")),
						Named.<VerificationLogic<Boolean>>of("verify throws exception",
								supplier -> assertThatThrownBy(supplier::get).isInstanceOf(SQLException.class))))
			// map each set of arguments to both index and label access methods
			.flatMap(ResultSetImplTests::mapArgumentToBothIndexAndLabelAccess);
	}

	@ParameterizedTest
	@MethodSource("getLongArgs")
	void shouldProcessValueOnGetLong(Value value, VerificationLogic<Long> verificationLogic, boolean indexAccess)
			throws SQLException {
		// given
		this.resultSet = setupWithValue(value);

		// when & then
		verificationLogic.run(() -> indexAccess ? this.resultSet.getLong(INDEX) : this.resultSet.getLong(LABEL));
	}

	private static Stream<Arguments> getLongArgs() {
		return Stream.of(
				// number handling
				Arguments.of(Values.value(0),
						Named.<VerificationLogic<Long>>of("verify returns 0",
								supplier -> assertThat(supplier.get()).isEqualTo(0))),
				Arguments.of(Values.value(Long.MIN_VALUE),
						Named.<VerificationLogic<Long>>of("verify returns Long.MIN_VALUE",
								supplier -> assertThat(supplier.get()).isEqualTo(Long.MIN_VALUE))),
				Arguments.of(Values.value(Long.MAX_VALUE),
						Named.<VerificationLogic<Long>>of("verify returns Long.MAX_VALUE",
								supplier -> assertThat(supplier.get()).isEqualTo(Long.MAX_VALUE))),
				// null handling
				Arguments.of(Values.NULL,
						Named.<VerificationLogic<Long>>of("verify returns 0",
								supplier -> assertThat(supplier.get()).isEqualTo(0))),
				// other types handling
				Arguments.of(Values.value(new byte[] {}),
						Named.<VerificationLogic<Boolean>>of("verify throws exception",
								supplier -> assertThatThrownBy(supplier::get).isInstanceOf(SQLException.class))),
				Arguments.of(Values.value(List.of("value")),
						Named.<VerificationLogic<Boolean>>of("verify throws exception",
								supplier -> assertThatThrownBy(supplier::get).isInstanceOf(SQLException.class))))
			// map each set of arguments to both index and label access methods
			.flatMap(ResultSetImplTests::mapArgumentToBothIndexAndLabelAccess);
	}

	@ParameterizedTest
	@MethodSource("getFloatArgs")
	void shouldProcessValueOnGetFloat(Value value, VerificationLogic<Float> verificationLogic, boolean indexAccess)
			throws SQLException {
		// given
		this.resultSet = setupWithValue(value);

		// when & then
		verificationLogic.run(() -> indexAccess ? this.resultSet.getFloat(INDEX) : this.resultSet.getFloat(LABEL));
	}

	private static Stream<Arguments> getFloatArgs() {
		return Stream.of(
				// number handling
				Arguments.of(Values.value(0.0f),
						Named.<VerificationLogic<Float>>of("verify returns 0",
								supplier -> assertThat(supplier.get()).isEqualTo(0.0f))),
				Arguments.of(Values.value(Float.MIN_VALUE),
						Named.<VerificationLogic<Float>>of("verify returns Float.MIN_VALUE",
								supplier -> assertThat(supplier.get()).isEqualTo(Float.MIN_VALUE))),
				Arguments.of(Values.value(Float.MAX_VALUE),
						Named.<VerificationLogic<Float>>of("verify returns Float.MAX_VALUE",
								supplier -> assertThat(supplier.get()).isEqualTo(Float.MAX_VALUE))),
				Arguments.of(Values.value(Double.MAX_VALUE),
						Named.<VerificationLogic<Float>>of("verify throws exception",
								supplier -> assertThatThrownBy(supplier::get).isInstanceOf(SQLException.class))),
				Arguments.of(Values.value(Double.MIN_VALUE),
						Named.<VerificationLogic<Float>>of("verify throws exception",
								supplier -> assertThatThrownBy(supplier::get).isInstanceOf(SQLException.class))),
				// null handling
				Arguments.of(Values.NULL,
						Named.<VerificationLogic<Float>>of("verify returns 0.0",
								supplier -> assertThat(supplier.get()).isEqualTo(0.0f))),
				// other types handling
				Arguments.of(Values.value(new byte[] {}),
						Named.<VerificationLogic<Boolean>>of("verify throws exception",
								supplier -> assertThatThrownBy(supplier::get).isInstanceOf(SQLException.class))),
				Arguments.of(Values.value(List.of("value")),
						Named.<VerificationLogic<Boolean>>of("verify throws exception",
								supplier -> assertThatThrownBy(supplier::get).isInstanceOf(SQLException.class))))
			// map each set of arguments to both index and label access methods
			.flatMap(ResultSetImplTests::mapArgumentToBothIndexAndLabelAccess);
	}

	@ParameterizedTest
	@MethodSource("getDoubleArgs")
	void shouldProcessValueOnGetDouble(Value value, VerificationLogic<Double> verificationLogic, boolean indexAccess)
			throws SQLException {
		// given
		this.resultSet = setupWithValue(value);

		// when & then
		verificationLogic.run(() -> indexAccess ? this.resultSet.getDouble(INDEX) : this.resultSet.getDouble(LABEL));
	}

	private static Stream<Arguments> getDoubleArgs() {
		return Stream.of(
				// number handling
				Arguments.of(Values.value(0.0f),
						Named.<VerificationLogic<Double>>of("verify returns 0",
								supplier -> assertThat(supplier.get()).isEqualTo(0.0))),
				Arguments.of(Values.value(Double.MIN_VALUE),
						Named.<VerificationLogic<Double>>of("verify returns Double.MIN_VALUE",
								supplier -> assertThat(supplier.get()).isEqualTo(Double.MIN_VALUE))),
				Arguments.of(Values.value(Double.MAX_VALUE),
						Named.<VerificationLogic<Double>>of("verify returns Double.MAX_VALUE",
								supplier -> assertThat(supplier.get()).isEqualTo(Double.MAX_VALUE))),
				// null handling
				Arguments.of(Values.NULL,
						Named.<VerificationLogic<Double>>of("verify returns 0.0",
								supplier -> assertThat(supplier.get()).isEqualTo(0.0))),
				// other types handling
				Arguments.of(Values.value(new byte[] {}),
						Named.<VerificationLogic<Boolean>>of("verify throws exception",
								supplier -> assertThatThrownBy(supplier::get).isInstanceOf(SQLException.class))),
				Arguments.of(Values.value(List.of("value")),
						Named.<VerificationLogic<Boolean>>of("verify throws exception",
								supplier -> assertThatThrownBy(supplier::get).isInstanceOf(SQLException.class))))
			// map each set of arguments to both index and label access methods
			.flatMap(ResultSetImplTests::mapArgumentToBothIndexAndLabelAccess);
	}

	@ParameterizedTest
	@MethodSource("getBytesArgs")
	void shouldProcessValueOnGetBytes(Value value, VerificationLogic<byte[]> verificationLogic, boolean indexAccess)
			throws SQLException {
		// given
		this.resultSet = setupWithValue(value);

		// when & then
		verificationLogic.run(() -> indexAccess ? this.resultSet.getBytes(INDEX) : this.resultSet.getBytes(LABEL));
	}

	private static Stream<Arguments> getBytesArgs() {
		return Stream.of(
				// byte array handling
				Arguments.of(Values.value(new byte[] {}),
						Named.<VerificationLogic<byte[]>>of("verify returns empty byte array",
								supplier -> assertThat(supplier.get()).isEqualTo(new byte[] {}))),
				Arguments.of(Values.value(new byte[] { 1, 2, 3, 4, 5 }),
						Named.<VerificationLogic<byte[]>>of("verify returns non-empty byte array",
								supplier -> assertThat(supplier.get()).isEqualTo(new byte[] { 1, 2, 3, 4, 5 }))),
				// null handling
				Arguments.of(Values.NULL,
						Named.<VerificationLogic<Double>>of("verify returns null",
								supplier -> assertThat(supplier.get()).isNull())),
				// other types handling
				Arguments.of(Values.value(false),
						Named.<VerificationLogic<Boolean>>of("verify throws exception",
								supplier -> assertThatThrownBy(supplier::get).isInstanceOf(SQLException.class))),
				Arguments.of(Values.value(List.of("value")),
						Named.<VerificationLogic<Boolean>>of("verify throws exception",
								supplier -> assertThatThrownBy(supplier::get).isInstanceOf(SQLException.class))))
			// map each set of arguments to both index and label access methods
			.flatMap(ResultSetImplTests::mapArgumentToBothIndexAndLabelAccess);
	}

	private static Stream<Arguments> mapArgumentToBothIndexAndLabelAccess(Arguments arguments) {
		return Stream.of(Arguments.of(Stream.concat(Arrays.stream(arguments.get()), Stream.of(true)).toArray()),
				Arguments.of(Stream.concat(Arrays.stream(arguments.get()), Stream.of(false)).toArray()));
	}

	private ResultSet setupWithValue(Value expectedValue) throws SQLException {
		var boltConnection = mock(BoltConnection.class);
		var statement = mock(StatementImpl.class);
		given(statement.getBoltConnection()).willReturn(boltConnection);
		var runResponse = mock(RunResponse.class);

		var boltRecord = mock(Record.class);
		given(boltRecord.size()).willReturn(1);
		given(boltRecord.get(INDEX - 1)).willReturn(expectedValue);
		given(boltRecord.get(LABEL)).willReturn(expectedValue);

		var pullResponse = mock(PullResponse.class);
		given(pullResponse.records()).willReturn(List.of(boltRecord));

		var resultSet = new ResultSetImpl(statement, runResponse, pullResponse, 1000);
		resultSet.next();
		return resultSet;
	}

	@FunctionalInterface
	private interface VerificationLogic<T> {

		void run(ValueSupplier<T> valueSupplier) throws SQLException;

	}

	@FunctionalInterface
	private interface ValueSupplier<T> {

		T get() throws SQLException;

	}

}
