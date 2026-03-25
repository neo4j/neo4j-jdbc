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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the structural field equivalence matcher. All inputs are parsed through
 * jOOQ's SQL parser to ensure we test against the exact types the production code will
 * encounter.
 *
 * @author Ryan Knight
 * @author Michael J. Simons
 */
class FieldEquivalencePredicateTests {

	private static final FieldEquivalencePredicate PREDICATE_UNDER_TEST = new FieldEquivalencePredicate();

	@Nested
	@DisplayName("Column reference matching")
	class ColumnReferenceTests {

		@Test
		@DisplayName("Same table, same column -> true")
		void sameTableSameColumn() {
			var select = TestUtils
				.parseSelect("SELECT c.name FROM Customers c JOIN Orders o ON c.id = o.customer_id GROUP BY c.name");

			// Extract c.name from SELECT and GROUP BY
			var selectField = TestUtils.unwrapAlias(select.$select().get(0));
			var groupField = (Field<?>) select.$groupBy().get(0);

			assertThat(PREDICATE_UNDER_TEST.test(selectField, groupField)).isTrue();
		}

		@Test
		@DisplayName("Same table, different column -> false")
		void sameTableDifferentColumn() {
			var select = TestUtils
				.parseSelect("SELECT c.name, c.age FROM Customers c JOIN Orders o ON c.id = o.customer_id");

			var field1 = TestUtils.unwrapAlias(select.$select().get(0));
			var field2 = TestUtils.unwrapAlias(select.$select().get(1));

			assertThat(PREDICATE_UNDER_TEST.test(field1, field2)).isFalse();
		}

		@Test
		@DisplayName("Different table, same column name -> false")
		void differentTableSameColumn() {
			var select = TestUtils
				.parseSelect("SELECT c.id, o.id FROM Customers c JOIN Orders o ON c.id = o.customer_id");

			var field1 = TestUtils.unwrapAlias(select.$select().get(0));
			var field2 = TestUtils.unwrapAlias(select.$select().get(1));

			assertThat(PREDICATE_UNDER_TEST.test(field1, field2)).isFalse();
		}

		@Test
		@DisplayName("Unqualified field matches qualified field -> true")
		void unqualifiedMatchesQualified() {
			// Use two separate queries: one with table qualifier, one without
			var qualifiedSelect = TestUtils
				.parseSelect("SELECT c.name FROM Customers c JOIN Orders o ON c.id = o.customer_id");
			var unqualifiedSelect = TestUtils.parseSelect("SELECT name FROM Customers");

			var qualified = TestUtils.unwrapAlias(qualifiedSelect.$select().get(0));
			var unqualified = TestUtils.unwrapAlias(unqualifiedSelect.$select().get(0));

			assertThat(PREDICATE_UNDER_TEST.test(qualified, unqualified)).isTrue();
		}

		@Test
		@DisplayName("Case-insensitive matching -> true")
		void caseInsensitiveMatching() {
			var select1 = TestUtils.parseSelect("SELECT name FROM People");
			var select2 = TestUtils.parseSelect("SELECT NAME FROM People");

			var field1 = TestUtils.unwrapAlias(select1.$select().get(0));
			var field2 = TestUtils.unwrapAlias(select2.$select().get(0));

			assertThat(PREDICATE_UNDER_TEST.test(field1, field2)).isTrue();
		}

	}

	@Nested
	@DisplayName("Aggregate function matching")
	class AggregateFunctionTests {

		@Test
		@DisplayName("count(*) vs count(*) -> true")
		void countStarVsCountStar() {
			var select = TestUtils.parseSelect("SELECT count(*) AS cnt FROM People GROUP BY name HAVING count(*) > 5");

			var selectCount = TestUtils.unwrapAlias(select.$select().get(0));
			var havingCount = ((QOM.Gt<?>) Objects.requireNonNull(select.$having())).$arg1();

			assertThat(PREDICATE_UNDER_TEST.test(selectCount, havingCount)).isTrue();
		}

		@Test
		@DisplayName("count(*) vs sum(age) -> false")
		void countStarVsSumAge() {
			var select1 = TestUtils.parseSelect("SELECT count(*) FROM People");
			var select2 = TestUtils.parseSelect("SELECT sum(age) FROM People");

			var count = TestUtils.unwrapAlias(select1.$select().get(0));
			var sum = TestUtils.unwrapAlias(select2.$select().get(0));

			assertThat(PREDICATE_UNDER_TEST.test(count, sum)).isFalse();
		}

		@Test
		@DisplayName("count(name) vs count(age) -> false")
		void countNameVsCountAge() {
			var select = TestUtils.parseSelect("SELECT count(name) AS cn, count(age) AS ca FROM People");

			var countName = TestUtils.unwrapAlias(select.$select().get(0));
			var countAge = TestUtils.unwrapAlias(select.$select().get(1));

			assertThat(PREDICATE_UNDER_TEST.test(countName, countAge)).isFalse();
		}

		@Test
		@DisplayName("sum(age) vs sum(age) -> true")
		void sumVsSum() {
			var select = TestUtils
				.parseSelect("SELECT sum(age) AS total FROM People GROUP BY name HAVING sum(age) > 100");

			var selectSum = TestUtils.unwrapAlias(select.$select().get(0));
			var havingSum = ((QOM.Gt<?>) Objects.requireNonNull(select.$having())).$arg1();

			assertThat(PREDICATE_UNDER_TEST.test(selectSum, havingSum)).isTrue();
		}

		@Test
		@DisplayName("min(salary) vs min(salary) -> true")
		void minVsMin() {
			var select = TestUtils
				.parseSelect("SELECT min(salary) AS mn FROM Employees GROUP BY department HAVING min(salary) > 50000");

			var selectMin = TestUtils.unwrapAlias(select.$select().get(0));
			var havingMin = ((QOM.Gt<?>) Objects.requireNonNull(select.$having())).$arg1();

			assertThat(PREDICATE_UNDER_TEST.test(selectMin, havingMin)).isTrue();
		}

		@Test
		@DisplayName("max(age) vs max(age) -> true")
		void maxVsMax() {
			var select = TestUtils.parseSelect("SELECT max(age) AS mx FROM People GROUP BY name HAVING max(age) > 60");

			var selectMax = TestUtils.unwrapAlias(select.$select().get(0));
			var havingMax = ((QOM.Gt<?>) Objects.requireNonNull(select.$having())).$arg1();

			assertThat(PREDICATE_UNDER_TEST.test(selectMax, havingMax)).isTrue();
		}

		@Test
		@DisplayName("avg(score) vs avg(score) -> true")
		void avgVsAvg() {
			var select = TestUtils
				.parseSelect("SELECT avg(score) AS av FROM Results GROUP BY name HAVING avg(score) > 75");

			var selectAvg = TestUtils.unwrapAlias(select.$select().get(0));
			var havingAvg = ((QOM.Gt<?>) Objects.requireNonNull(select.$having())).$arg1();

			assertThat(PREDICATE_UNDER_TEST.test(selectAvg, havingAvg)).isTrue();
		}

		@Test
		@DisplayName("count(name) vs count(DISTINCT name) -> false")
		void countVsCountDistinct() {
			var select = TestUtils.parseSelect("SELECT count(name) AS cn, count(DISTINCT name) AS cdn FROM People");

			var countAll = TestUtils.unwrapAlias(select.$select().get(0));
			var countDistinct = TestUtils.unwrapAlias(select.$select().get(1));

			assertThat(PREDICATE_UNDER_TEST.test(countAll, countDistinct)).isFalse();
		}

		@Test
		@DisplayName("count(DISTINCT name) vs count(DISTINCT name) -> true")
		void countDistinctVsCountDistinct() {
			var select = TestUtils.parseSelect(
					"SELECT count(DISTINCT name) AS cdn FROM People GROUP BY age HAVING count(DISTINCT name) > 3");

			var selectCountDistinct = TestUtils.unwrapAlias(select.$select().get(0));
			var havingCountDistinct = ((QOM.Gt<?>) select.$having()).$arg1();

			assertThat(PREDICATE_UNDER_TEST.test(selectCountDistinct, havingCountDistinct)).isTrue();
		}

	}

	@Nested
	@DisplayName("Alias transparency")
	class AliasTransparencyTests {

		@Test
		@DisplayName("count(*) AS cnt vs count(*) -> true")
		void aliasedCountVsUnaliasedCount() {
			var select1 = TestUtils.parseSelect("SELECT count(*) AS cnt FROM People");
			var select2 = TestUtils.parseSelect("SELECT count(*) FROM People");

			// The first is aliased, the second is not
			var aliasedCount = TestUtils.asField(select1.$select().get(0));
			assertThat(aliasedCount).isInstanceOf(QOM.FieldAlias.class);

			var unaliasedCount = TestUtils.asField(select2.$select().get(0));

			assertThat(PREDICATE_UNDER_TEST.test(aliasedCount, unaliasedCount)).isTrue();
		}

		@Test
		@DisplayName("sum(age) AS total vs sum(age) -> true")
		void aliasedSumVsUnaliasedSum() {
			var select1 = TestUtils.parseSelect("SELECT sum(age) AS total FROM People");
			var select2 = TestUtils.parseSelect("SELECT sum(age) FROM People");

			var aliasedSum = TestUtils.asField(select1.$select().get(0));
			assertThat(aliasedSum).isInstanceOf(QOM.FieldAlias.class);

			var unaliasedSum = TestUtils.asField(select2.$select().get(0));

			assertThat(PREDICATE_UNDER_TEST.test(aliasedSum, unaliasedSum)).isTrue();
		}

		@Test
		@DisplayName("name AS n vs name -> true")
		void aliasedFieldVsUnaliasedField() {
			var select1 = TestUtils.parseSelect("SELECT name AS n FROM People");
			var select2 = TestUtils.parseSelect("SELECT name FROM People");

			var aliasedField = TestUtils.asField(select1.$select().get(0));
			assertThat(aliasedField).isInstanceOf(QOM.FieldAlias.class);

			var unaliasedField = TestUtils.asField(select2.$select().get(0));

			assertThat(PREDICATE_UNDER_TEST.test(aliasedField, unaliasedField)).isTrue();
		}

	}

	@Nested
	@DisplayName("Cross-parse matching")
	class CrossParseTests {

		@Test
		@DisplayName("count(*) from SELECT vs count(*) from HAVING of the same query -> true")
		void countFromSelectVsHaving() {
			var select = TestUtils
				.parseSelect("SELECT name, count(*) AS cnt FROM People GROUP BY name HAVING count(*) > 5");

			// Extract count(*) from SELECT (aliased)
			var selectCount = TestUtils.asField(select.$select().get(1));
			assertThat(selectCount).isInstanceOf(QOM.FieldAlias.class);

			// Extract count(*) from HAVING
			var havingCount = ((QOM.Gt<?>) Objects.requireNonNull(select.$having())).$arg1();
			assertThat(havingCount).isInstanceOf(QOM.Count.class);

			assertThat(PREDICATE_UNDER_TEST.test(selectCount, havingCount)).isTrue();
		}

		@Test
		@DisplayName("sum(age) from SELECT vs sum(age) from ORDER BY of the same query -> true")
		void sumFromSelectVsOrderBy() {
			var select = TestUtils
				.parseSelect("SELECT name, sum(age) AS total FROM People GROUP BY name ORDER BY sum(age)");

			// Extract sum(age) from SELECT (aliased)
			var selectSum = TestUtils.asField(select.$select().get(1));
			assertThat(selectSum).isInstanceOf(QOM.FieldAlias.class);

			// Extract sum(age) from ORDER BY
			var orderByField = select.$orderBy().get(0).$field();
			assertThat(orderByField).isInstanceOf(QOM.Sum.class);

			assertThat(PREDICATE_UNDER_TEST.test(selectSum, orderByField)).isTrue();
		}

	}

	@Nested
	@DisplayName("Negative and false-positive cases")
	class NonMatchingAndFalsePositiveCasesTests {

		@Test
		@DisplayName("count(*) vs count(name) -> false")
		void countStarVsCountName() {
			var select = TestUtils.parseSelect("SELECT count(*) AS c1, count(name) AS c2 FROM People");

			var countStar = TestUtils.unwrapAlias(select.$select().get(0));
			var countName = TestUtils.unwrapAlias(select.$select().get(1));

			assertThat(PREDICATE_UNDER_TEST.test(countStar, countName)).isFalse();
		}

		@Test
		@DisplayName("sum(age) vs avg(age) -> false")
		void sumVsAvg() {
			var select = TestUtils.parseSelect("SELECT sum(age) AS s, avg(age) AS a FROM People");

			var sum = TestUtils.unwrapAlias(select.$select().get(0));
			var avg = TestUtils.unwrapAlias(select.$select().get(1));

			assertThat(PREDICATE_UNDER_TEST.test(sum, avg)).isFalse();
		}

		@Test
		@DisplayName("min(x) vs max(x) -> false")
		void minVsMax() {
			var select = TestUtils.parseSelect("SELECT min(score) AS mn, max(score) AS mx FROM Results");

			var min = TestUtils.unwrapAlias(select.$select().get(0));
			var max = TestUtils.unwrapAlias(select.$select().get(1));

			assertThat(PREDICATE_UNDER_TEST.test(min, max)).isFalse();
		}

		@Test
		@DisplayName("count(x) vs count(DISTINCT x) -> false")
		void countVsCountDistinctNegative() {
			var select = TestUtils.parseSelect("SELECT count(name) AS cn, count(DISTINCT name) AS cdn FROM People");

			var countAll = TestUtils.unwrapAlias(select.$select().get(0));
			var countDistinct = TestUtils.unwrapAlias(select.$select().get(1));

			assertThat(PREDICATE_UNDER_TEST.test(countAll, countDistinct)).isFalse();
		}

		@Test
		@DisplayName("null vs field -> false")
		void nullVsField() {
			var select = TestUtils.parseSelect("SELECT name FROM People");
			var field = TestUtils.unwrapAlias(select.$select().get(0));

			assertThat(PREDICATE_UNDER_TEST.test(null, field)).isFalse();
			assertThat(PREDICATE_UNDER_TEST.test(field, null)).isFalse();
			assertThat(PREDICATE_UNDER_TEST.test(null, null)).isFalse();
		}

	}

}
