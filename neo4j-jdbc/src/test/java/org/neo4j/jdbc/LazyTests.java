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
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;

class LazyTests {

	@Test
	void shouldMemorize() {
		var cnt = new AtomicInteger(0);
		var lazy = Lazy.of(cnt::incrementAndGet);
		assertThat(lazy.resolve()).isOne();
		assertThat(lazy.resolve()).isOne();
		assertThat(cnt.get()).isOne();
	}

	@Test
	void shouldNoteWhenResolved() {
		var cnt = new AtomicInteger(0);
		var lazy = Lazy.of(cnt::incrementAndGet);
		assertThat(lazy.isResolved()).isFalse();
		assertThat(lazy.resolve()).isOne();
		assertThat(lazy.isResolved()).isTrue();
	}

	@Test
	void shouldForget() {
		var cnt = new AtomicInteger(0);
		var lazy = Lazy.of(cnt::incrementAndGet);
		assertThat(lazy.isResolved()).isFalse();
		assertThat(lazy.resolve()).isOne();
		assertThatNoException().isThrownBy(lazy::forget);
		assertThat(lazy.isResolved()).isFalse();
		assertThat(lazy.resolve()).isEqualTo(2);
		assertThat(lazy.isResolved()).isTrue();
		assertThat(cnt.get()).isEqualTo(2);
	}

	@Test
	void shouldNotWrapRuntimeException() {
		var lazy = Lazy.of(() -> {
			throw new RuntimeException("inner");
		});
		assertThatRuntimeException().isThrownBy(lazy::resolve).withNoCause().withMessage("inner");
	}

	@Test
	void shouldWrapOtherExceptions() {
		var lazy = Lazy.of(() -> {
			throw new SQLException("inner");
		});
		assertThatRuntimeException().isThrownBy(lazy::resolve)
			.withCauseInstanceOf(SQLException.class)
			.withMessage("java.sql.SQLException: inner");
	}

	@Test
	void shouldUnwrapSQLException() {
		var lazy = Lazy.of(() -> {
			throw new SQLException("inner");
		});
		assertThatExceptionOfType(SQLException.class).isThrownBy(() -> lazy.resolveThrowing(SQLException.class))
			.withNoCause()
			.withMessage("inner");

		assertThatRuntimeException().isThrownBy(() -> lazy.resolveThrowing(ArithmeticException.class))
			.withCauseInstanceOf(SQLException.class)
			.withMessage("java.sql.SQLException: inner");
	}

	@Test
	void shouldWrapOtherExceptionsIfNotSQL() {
		var lazy = Lazy.of(() -> {
			throw new Exception("inner");
		});
		assertThatRuntimeException().isThrownBy(() -> lazy.resolveThrowing(ArithmeticException.class))
			.withCauseInstanceOf(Exception.class)
			.withMessage("java.lang.Exception: inner");
	}

}
