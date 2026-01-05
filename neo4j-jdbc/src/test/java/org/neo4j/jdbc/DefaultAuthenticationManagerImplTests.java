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
package org.neo4j.jdbc;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.jdbc.authn.spi.Authentication;
import org.neo4j.jdbc.authn.spi.ExpiringAuthentication;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultAuthenticationManagerImplTests {

	private static final Clock CLOCK = Clock.fixed(LocalDateTime.of(2013, 5, 6, 20, 0).toInstant(ZoneOffset.UTC),
			ZoneId.of(ZoneOffset.UTC.getId()));

	static Stream<Arguments> isValid() {
		return Stream.of(Arguments.of(Instant.now(CLOCK).plus(Duration.ofSeconds(10)), Duration.ofSeconds(0), true),
				Arguments.of(Instant.now(CLOCK).plus(Duration.ofSeconds(10)), Duration.ofSeconds(1), true),
				Arguments.of(Instant.now(CLOCK).plus(Duration.ofSeconds(10)), Duration.ofSeconds(9), true),
				Arguments.of(Instant.now(CLOCK).plus(Duration.ofSeconds(10)), Duration.ofSeconds(10), false),

				Arguments.of(Instant.now(CLOCK).minus(Duration.ofSeconds(10)), Duration.ofSeconds(0), false),
				Arguments.of(Instant.now(CLOCK).minus(Duration.ofSeconds(10)), Duration.ofSeconds(1), false),
				Arguments.of(Instant.now(CLOCK).minus(Duration.ofSeconds(10)), Duration.ofSeconds(9), false),
				Arguments.of(Instant.now(CLOCK).minus(Duration.ofSeconds(10)), Duration.ofSeconds(10), false));
	}

	@ParameterizedTest
	@MethodSource
	void isValid(Instant expiration, Duration offset, boolean expected) {

		Supplier<Authentication> supplier = () -> Authentication.bearer("f", expiration);
		var manager = new DefaultAuthenticationManagerImpl(URI.create("localhost:7687"), supplier, CLOCK, offset);
		var authentication = supplier.get();
		assertThat(manager.isValid(authentication)).isEqualTo(expected);
	}

	@Test
	void getOrRefresh() {

		var counter = new AtomicInteger();

		class FunnyAuth implements ExpiringAuthentication {

			@Override
			public Instant expiresAt() {
				if (counter.getAndIncrement() < 1) {
					return Instant.now(CLOCK).plusSeconds(1);
				}
				return Instant.now(CLOCK).minusSeconds(1);
			}

		}

		var manager = new DefaultAuthenticationManagerImpl(URI.create("localhost:7687"), FunnyAuth::new, CLOCK,
				Duration.ZERO);

		var authentication1 = manager.getOrRefresh();
		assertThat(counter).hasValue(0);

		var authentication2 = manager.getOrRefresh();
		assertThat(counter).hasValue(1);
		assertThat(authentication1).isSameAs(authentication2);

		var authentication3 = manager.getOrRefresh();
		assertThat(counter).hasValue(2);
		assertThat(authentication2).isNotSameAs(authentication3);
	}

}
