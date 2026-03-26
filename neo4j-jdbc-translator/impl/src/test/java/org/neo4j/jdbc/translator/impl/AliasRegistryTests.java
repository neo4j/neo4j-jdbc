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
package org.neo4j.jdbc.translator.impl;

import java.util.Objects;

import org.jooq.Field;
import org.jooq.impl.QOM;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the alias registry that maps jOOQ expressions to WITH clause aliases. All
 * inputs are parsed through jOOQ's SQL parser.
 *
 * @author Ryan Knight
 * @author Michael J. Simons
 */
class AliasRegistryTests {

	@Nested
	@DisplayName("Structural lookup")
	class StructuralLookupTests {

		private AliasRegistry registry;

		@BeforeEach
		void setUp() {
			this.registry = new AliasRegistry();
		}

		@Test
		@DisplayName("Register count(*) → cnt, lookup count(*) → cnt")
		void lookupCountStar() {
			var select = TestUtils.parseSelect("SELECT count(*) AS cnt FROM People GROUP BY name");
			var selectFields = select.$select();

			// Register count(*) with alias "cnt"
			var countField = selectFields.get(0);
			this.registry.register(TestUtils.unwrapAlias(countField), TestUtils.getAliasName(countField));

			// Now parse a separate query with count(*) in HAVING to get a fresh count(*)
			var havingSelect = TestUtils
				.parseSelect("SELECT name, count(*) AS cnt FROM People GROUP BY name HAVING count(*) > 5");
			var gt = (QOM.Gt<?>) havingSelect.$having();
			assertThat(gt).isNotNull();
			var havingCount = (Field<?>) gt.$arg1();

			assertThat(this.registry.resolve(havingCount)).isEqualToIgnoringCase("cnt");
		}

		@Test
		@DisplayName("Register sum(age) → total, lookup sum(salary) → null (different column)")
		void lookupSumDifferentColumn() {
			var select = TestUtils.parseSelect("SELECT sum(age) AS total FROM People GROUP BY name");
			var sumField = select.$select().get(0);
			this.registry.register(TestUtils.unwrapAlias(sumField), TestUtils.getAliasName(sumField));

			// Parse a query with sum(salary) — different column
			var otherSelect = TestUtils
				.parseSelect("SELECT name, sum(salary) AS sal FROM People GROUP BY name HAVING sum(salary) > 100");
			var gt = (QOM.Gt<?>) otherSelect.$having();
			var havingSum = (Field<?>) Objects.requireNonNull(gt).$arg1();

			assertThat(this.registry.resolve(havingSum)).isNull();
		}

		@Test
		@DisplayName("Register sum(age) → total, lookup sum(age) → total")
		void lookupSumSameColumn() {
			var select = TestUtils.parseSelect("SELECT sum(age) AS total FROM People GROUP BY name");
			var sumField = select.$select().get(0);
			this.registry.register(TestUtils.unwrapAlias(sumField), TestUtils.getAliasName(sumField));

			// Parse a query with sum(age) in HAVING
			var havingSelect = TestUtils
				.parseSelect("SELECT name, sum(age) AS total FROM People GROUP BY name HAVING sum(age) > 100");
			var gt = (QOM.Gt<?>) havingSelect.$having();
			var havingSum = (Field<?>) Objects.requireNonNull(gt).$arg1();

			assertThat(this.registry.resolve(havingSum)).isEqualToIgnoringCase("total");
		}

		@Test
		@DisplayName("Register count(*), sum(age), max(age) with distinct aliases, lookup each → correct alias")
		void lookupMultipleAggregates() {
			var select = TestUtils
				.parseSelect("SELECT count(*) AS cnt, sum(age) AS total, max(age) AS oldest FROM People GROUP BY name");
			var selectFields = select.$select();

			for (var sf : selectFields) {
				this.registry.register(TestUtils.unwrapAlias(sf), TestUtils.getAliasName(sf));
			}

			// Parse queries to get fresh aggregates for lookup
			var countSelect = TestUtils.parseSelect("SELECT name FROM People GROUP BY name HAVING count(*) > 5");
			var countGt = (QOM.Gt<?>) countSelect.$having();
			assertThat(this.registry.resolve(Objects.requireNonNull(countGt).$arg1())).isEqualToIgnoringCase("cnt");

			var sumSelect = TestUtils.parseSelect("SELECT name FROM People GROUP BY name HAVING sum(age) > 100");
			var sumGt = (QOM.Gt<?>) sumSelect.$having();
			assertThat(this.registry.resolve(Objects.requireNonNull(sumGt).$arg1())).isEqualToIgnoringCase("total");

			var maxSelect = TestUtils.parseSelect("SELECT name FROM People GROUP BY name HAVING max(age) > 80");
			var maxGt = (QOM.Gt<?>) maxSelect.$having();
			assertThat(this.registry.resolve(Objects.requireNonNull(maxGt).$arg1())).isEqualToIgnoringCase("oldest");
		}

		@Test
		@DisplayName("Lookup unregistered field → null")
		void lookupUnregisteredField() {
			// Registry is empty
			var select = TestUtils.parseSelect("SELECT name FROM People GROUP BY name HAVING count(*) > 5");
			var gt = (QOM.Gt<?>) select.$having();

			assertThat(this.registry.resolve(Objects.requireNonNull(gt).$arg1())).isNull();
		}

		@Test
		@DisplayName("Duplicate registration — first registration wins")
		void duplicateRegistrationFirstWins() {
			var select1 = TestUtils.parseSelect("SELECT count(*) AS first_alias FROM People GROUP BY name");
			var field1 = select1.$select().get(0);
			this.registry.register(TestUtils.unwrapAlias(field1), "first_alias");

			var select2 = TestUtils.parseSelect("SELECT count(*) AS second_alias FROM People GROUP BY name");
			var field2 = select2.$select().get(0);
			this.registry.register(TestUtils.unwrapAlias(field2), "second_alias");

			// Structural lookup should return the first registered alias
			var lookupSelect = TestUtils.parseSelect("SELECT name FROM People GROUP BY name HAVING count(*) > 5");
			var gt = (QOM.Gt<?>) lookupSelect.$having();
			assertThat(this.registry.resolve(Objects.requireNonNull(gt).$arg1())).isEqualToIgnoringCase("first_alias");
		}

	}

	@Nested
	@DisplayName("Name-based lookup")
	class NameBasedLookupTests {

		private AliasRegistry registry;

		@BeforeEach
		void setUp() {
			this.registry = new AliasRegistry();
		}

		@Test
		@DisplayName("Register count(*) → cnt, lookup plain Field(cnt) → cnt")
		void lookupByAliasNameForCount() {
			var select = TestUtils.parseSelect("SELECT count(*) AS cnt FROM People GROUP BY name");
			var countField = select.$select().get(0);
			this.registry.register(TestUtils.unwrapAlias(countField), TestUtils.getAliasName(countField));

			// Parse ORDER BY cnt — jOOQ keeps "cnt" as unresolved field reference
			var orderSelect = TestUtils
				.parseSelect("SELECT name, count(*) AS cnt FROM People GROUP BY name ORDER BY cnt");
			var orderField = orderSelect.$orderBy().get(0).$field();

			// This should match by name, not structurally
			assertThat(orderField).isNotInstanceOf(QOM.Count.class);
			assertThat(this.registry.resolve(orderField)).isEqualToIgnoringCase("cnt");
		}

		@Test
		@DisplayName("Register sum(age) → total, lookup plain Field(total) → total")
		void lookupByAliasNameForSum() {
			var select = TestUtils.parseSelect("SELECT sum(age) AS total FROM People GROUP BY name");
			var sumField = select.$select().get(0);
			this.registry.register(TestUtils.unwrapAlias(sumField), TestUtils.getAliasName(sumField));

			// Parse ORDER BY total — jOOQ keeps "total" as unresolved field reference
			var orderSelect = TestUtils
				.parseSelect("SELECT name, sum(age) AS total FROM People GROUP BY name ORDER BY total");
			var orderField = orderSelect.$orderBy().get(0).$field();

			assertThat(orderField).isNotInstanceOf(QOM.Sum.class);
			assertThat(this.registry.resolve(orderField)).isEqualToIgnoringCase("total");
		}

		@Test
		@DisplayName("Lookup plain Field(unknown) → null")
		void lookupByUnknownName() {
			var select = TestUtils.parseSelect("SELECT count(*) AS cnt FROM People GROUP BY name");
			var countField = select.$select().get(0);
			this.registry.register(TestUtils.unwrapAlias(countField), TestUtils.getAliasName(countField));

			// Parse ORDER BY unknown — should not match anything
			var orderSelect = TestUtils.parseSelect("SELECT name FROM People ORDER BY unknown");
			var orderField = orderSelect.$orderBy().get(0).$field();

			assertThat(this.registry.resolve(orderField)).isNull();
		}

	}

	@Nested
	@DisplayName("Combined mode — structural + name-based")
	class CombinedModeTests {

		private AliasRegistry registry;

		@BeforeEach
		void setUp() {
			this.registry = new AliasRegistry();

			// Register count(*) → "cnt" and name → "name"
			var select = TestUtils.parseSelect("SELECT name, count(*) AS cnt FROM People GROUP BY name");
			var selectFields = select.$select();
			for (var sf : selectFields) {
				this.registry.register(TestUtils.unwrapAlias(sf), TestUtils.getAliasName(sf));
			}
		}

		@Test
		@DisplayName("Lookup count(*) structurally → cnt")
		void lookupCountStructurally() {
			var select = TestUtils.parseSelect("SELECT name FROM People GROUP BY name HAVING count(*) > 5");
			var gt = (QOM.Gt<?>) select.$having();

			assertThat(this.registry.resolve(Objects.requireNonNull(gt).$arg1())).isEqualToIgnoringCase("cnt");
		}

		@Test
		@DisplayName("Lookup Field(cnt) by name → cnt")
		void lookupCntByName() {
			var select = TestUtils.parseSelect("SELECT name, count(*) AS cnt FROM People GROUP BY name ORDER BY cnt");
			var orderField = select.$orderBy().get(0).$field();

			assertThat(this.registry.resolve(orderField)).isEqualToIgnoringCase("cnt");
		}

		@Test
		@DisplayName("Lookup Field(name) by name → name")
		void lookupNameByName() {
			var select = TestUtils.parseSelect("SELECT name, count(*) AS cnt FROM People GROUP BY name ORDER BY name");
			var orderField = select.$orderBy().get(0).$field();

			assertThat(this.registry.resolve(orderField)).isEqualToIgnoringCase("name");
		}

		@Test
		@DisplayName("Lookup sum(age) → null (not registered)")
		void lookupUnregisteredAggregate() {
			var select = TestUtils.parseSelect("SELECT name FROM People GROUP BY name HAVING sum(age) > 100");
			var gt = (QOM.Gt<?>) select.$having();

			assertThat(this.registry.resolve(Objects.requireNonNull(gt).$arg1())).isNull();
		}

		@Test
		@DisplayName("Lookup Field(bogus) → null")
		void lookupBogusName() {
			var select = TestUtils.parseSelect("SELECT name FROM People ORDER BY bogus");
			var orderField = select.$orderBy().get(0).$field();

			assertThat(this.registry.resolve(orderField)).isNull();
		}

	}

	@Nested
	@DisplayName("Round-trip from parsed SQL")
	class RoundTripTests {

		@Test
		@DisplayName("count(*) round-trip: register from SELECT, lookup from HAVING (structural) and ORDER BY (name)")
		void countStarRoundTrip() {
			var select = TestUtils
				.parseSelect("SELECT name, count(*) AS cnt FROM People GROUP BY name HAVING count(*) > 5 ORDER BY cnt");

			// Build registry from SELECT fields
			var registry = new AliasRegistry();
			for (var sf : select.$select()) {
				registry.register(TestUtils.unwrapAlias(sf), TestUtils.getAliasName(sf));
			}

			// Lookup from HAVING: count(*) → "cnt" (structural match)
			var gt = (QOM.Gt<?>) select.$having();
			var havingField = (Field<?>) Objects.requireNonNull(gt).$arg1();
			assertThat(havingField).isInstanceOf(QOM.Count.class);
			assertThat(registry.resolve(havingField)).isEqualToIgnoringCase("cnt");

			// Lookup from ORDER BY: Field("cnt") → "cnt" (name-based match)
			var orderField = select.$orderBy().get(0).$field();
			assertThat(orderField).isNotInstanceOf(QOM.Count.class);
			assertThat(registry.resolve(orderField)).isEqualToIgnoringCase("cnt");
		}

		@Test
		@DisplayName("sum(age) round-trip: register from SELECT, lookup from ORDER BY (structural)")
		void sumAgeRoundTrip() {
			var select = TestUtils
				.parseSelect("SELECT name, sum(age) AS total FROM People GROUP BY name ORDER BY sum(age)");

			// Build registry from SELECT fields
			var registry = new AliasRegistry();
			for (var sf : select.$select()) {
				registry.register(TestUtils.unwrapAlias(sf), TestUtils.getAliasName(sf));
			}

			// Lookup from ORDER BY: sum(age) → "total" (structural match)
			var orderField = select.$orderBy().get(0).$field();
			assertThat(orderField).isInstanceOf(QOM.Sum.class);
			assertThat(registry.resolve(orderField)).isEqualToIgnoringCase("total");
		}

	}

}
