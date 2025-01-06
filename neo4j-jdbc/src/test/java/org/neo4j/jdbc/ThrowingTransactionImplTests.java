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

import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.stream.Stream;

import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

class ThrowingTransactionImplTests {

	@Test
	void mustAlwaysBeRolledBack() {
		assertThat(new ThrowingTransactionImpl().getState()).isEqualTo(Neo4jTransaction.State.ROLLEDBACK);
	}

	@Test
	void mustNotBeRunnable() {
		assertThat(new ThrowingTransactionImpl().isRunnable()).isFalse();
	}

	@Test
	void mustNotBeOpen() {
		assertThat(new ThrowingTransactionImpl().isOpen()).isFalse();
	}

	@Test
	void mustNotBeAutoCommit() {
		assertThat(new ThrowingTransactionImpl().isAutoCommit()).isFalse();
	}

	static Stream<Arguments> mustThrowOnAllOtherMethods() {
		var tx = new ThrowingTransactionImpl();
		return Stream.of(Arguments.of((ThrowableAssert.ThrowingCallable) () -> tx.runAndPull(null, null, 0, 0)),
				Arguments.of((ThrowableAssert.ThrowingCallable) () -> tx.runAndDiscard(null, null, 0, false)),
				Arguments.of((ThrowableAssert.ThrowingCallable) () -> tx.pull(null, 0)),
				Arguments.of((ThrowableAssert.ThrowingCallable) tx::commit),
				Arguments.of((ThrowableAssert.ThrowingCallable) tx::rollback));
	}

	@ParameterizedTest
	@MethodSource
	void mustThrowOnAllOtherMethods(ThrowableAssert.ThrowingCallable throwingCallable) {

		assertThatExceptionOfType(SQLFeatureNotSupportedException.class).isThrownBy(throwingCallable);
	}

	@Test
	void canOnlyFail() {
		var tx = new ThrowingTransactionImpl();
		var ex = new SQLException();
		assertThatNoException().isThrownBy(() -> tx.fail(ex));
	}

}
