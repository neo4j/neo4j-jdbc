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

import org.jooq.impl.QOM;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael J. Simons
 */
final class AggregatesTests {

	@Test
	@DisplayName("Simple: HAVING count(*) > 5 returns 1 aggregate")
	void simpleCountStar() {
		var select = TestUtils.parseSelect("SELECT name, count(*) FROM People GROUP BY name HAVING count(*) > 5");
		var aggregates = Aggregates.of(select.$having());

		assertThat(aggregates).hasSize(1);
		assertThat(aggregates).first().isInstanceOf(QOM.Count.class);
	}

	@Test
	@DisplayName("Compound AND: HAVING count(*) > 5 AND max(age) > 50 returns 2 aggregates")
	void compoundAnd() {
		var select = TestUtils.parseSelect(
				"SELECT name, count(*) AS cnt, max(age) AS mx FROM People GROUP BY name HAVING count(*) > 5 AND max(age) > 50");
		var aggregates = Aggregates.of(select.$having());

		assertThat(aggregates).hasSize(2);
	}

	@Test
	@DisplayName("Compound OR: HAVING count(*) > 2 OR min(age) < 18 returns 2 aggregates")
	void compoundOr() {
		var select = TestUtils.parseSelect(
				"SELECT name, count(*) AS cnt, min(age) AS mn FROM People GROUP BY name HAVING count(*) > 2 OR min(age) < 18");
		var aggregates = Aggregates.of(select.$having());

		assertThat(aggregates).hasSize(2);
	}

	@Test
	@DisplayName("Arithmetic: HAVING max(salary) > 2 * avg(salary) returns 2 aggregates")
	void arithmetic() {
		var select = TestUtils.parseSelect(
				"SELECT department, max(salary) AS mx, avg(salary) AS av FROM Employees GROUP BY department HAVING max(salary) > 2 * avg(salary)");
		var aggregates = Aggregates.of(select.$having());

		assertThat(aggregates).hasSize(2);
		assertThat(aggregates).anyMatch(f -> f instanceof QOM.Max);
		assertThat(aggregates).anyMatch(f -> f instanceof QOM.Avg);
	}

	@Test
	@DisplayName("Nested arithmetic: HAVING sum(age) + sum(salary) > 100 returns 2 aggregates")
	void nestedArithmetic() {
		var select = TestUtils.parseSelect(
				"SELECT department, sum(age) AS sa, sum(salary) AS ss FROM Employees GROUP BY department HAVING sum(age) + sum(salary) > 100");
		var aggregates = Aggregates.of(select.$having());

		assertThat(aggregates).hasSize(2);
		assertThat(aggregates).allMatch(f -> f instanceof QOM.Sum);
	}

	@Test
	@DisplayName("No aggregates: HAVING name = 'Alice' returns empty list")
	void noAggregates() {
		var select = TestUtils.parseSelect("SELECT name, count(*) FROM People GROUP BY name HAVING name = 'Alice'");
		var aggregates = Aggregates.of(select.$having());

		assertThat(aggregates).isEmpty();
	}

	@Test
	@DisplayName("Mixed: HAVING count(*) > 5 AND name = 'Alice' returns 1 aggregate")
	void mixed() {
		var select = TestUtils.parseSelect(
				"SELECT name, count(*) AS cnt FROM People GROUP BY name HAVING count(*) > 5 AND name = 'Alice'");
		var aggregates = Aggregates.of(select.$having());

		assertThat(aggregates).hasSize(1);
		assertThat(aggregates).first().isInstanceOf(QOM.Count.class);
	}

	@Test
	@DisplayName("Duplicate aggregates: HAVING count(*) > 5 OR count(*) < 100 returns 2 entries")
	void duplicateAggregates() {
		var select = TestUtils.parseSelect(
				"SELECT name, count(*) AS cnt FROM People GROUP BY name HAVING count(*) > 5 OR count(*) < 100");
		var aggregates = Aggregates.of(select.$having());

		assertThat(aggregates).hasSize(2);
		assertThat(aggregates).allMatch(f -> f instanceof QOM.Count);
	}

	@Test
	@DisplayName("BETWEEN: HAVING count(*) BETWEEN 5 AND 10 returns 1 aggregate")
	void between() {
		var select = TestUtils
			.parseSelect("SELECT name, count(*) AS cnt FROM People GROUP BY name HAVING count(*) BETWEEN 5 AND 10");
		var aggregates = Aggregates.of(select.$having());

		assertThat(aggregates).hasSize(1);
		assertThat(aggregates).first().isInstanceOf(QOM.Count.class);
	}

	@Test
	@DisplayName("IsNull: HAVING count(*) IS NULL returns 1 aggregate")
	void isNull() {
		var select = TestUtils.parseSelect("SELECT name, count(*) FROM People GROUP BY name HAVING count(*) IS NULL");
		var aggregates = Aggregates.of(select.$having());

		assertThat(aggregates).hasSize(1);
		assertThat(aggregates).first().isInstanceOf(QOM.Count.class);
	}

	@Test
	@DisplayName("IsNotNull: HAVING max(age) IS NOT NULL returns 1 aggregate")
	void isNotNull() {
		var select = TestUtils
			.parseSelect("SELECT name, max(age) FROM People GROUP BY name HAVING max(age) IS NOT NULL");
		var aggregates = Aggregates.of(select.$having());

		assertThat(aggregates).hasSize(1);
		assertThat(aggregates).first().isInstanceOf(QOM.Max.class);
	}

	@Test
	@DisplayName("InList: HAVING count(*) IN (1, 2, 3) returns 1 aggregate")
	void inList() {
		var select = TestUtils
			.parseSelect("SELECT name, count(*) FROM People GROUP BY name HAVING count(*) IN (1, 2, 3)");
		var aggregates = Aggregates.of(select.$having());

		assertThat(aggregates).hasSize(1);
		assertThat(aggregates).first().isInstanceOf(QOM.Count.class);
	}

	@Test
	@DisplayName("Like: HAVING max(name) LIKE 'A%' returns 1 aggregate")
	void like() {
		var select = TestUtils
			.parseSelect("SELECT department, max(name) FROM Employees GROUP BY department HAVING max(name) LIKE 'A%'");
		var aggregates = Aggregates.of(select.$having());

		assertThat(aggregates).hasSize(1);
		assertThat(aggregates).first().isInstanceOf(QOM.Max.class);
	}

	@Test
	@DisplayName("NotLike: HAVING max(name) NOT LIKE 'A%' returns 1 aggregate")
	void notLike() {
		var select = TestUtils.parseSelect(
				"SELECT department, max(name) FROM Employees GROUP BY department HAVING max(name) NOT LIKE 'A%'");
		var aggregates = Aggregates.of(select.$having());

		assertThat(aggregates).hasSize(1);
		assertThat(aggregates).first().isInstanceOf(QOM.Max.class);
	}

	@Test
	@DisplayName("NotInList: HAVING count(*) NOT IN (1, 2, 3) returns 1 aggregate")
	void notInList() {
		var select = TestUtils
			.parseSelect("SELECT name, count(*) FROM People GROUP BY name HAVING count(*) NOT IN (1, 2, 3)");
		var aggregates = Aggregates.of(select.$having());

		assertThat(aggregates).hasSize(1);
		assertThat(aggregates).first().isInstanceOf(QOM.Count.class);
	}

	@Test
	@DisplayName("IsDistinctFrom: HAVING count(*) IS DISTINCT FROM 0 returns 1 aggregate")
	void isDistinctFrom() {
		var select = TestUtils
			.parseSelect("SELECT name, count(*) FROM People GROUP BY name HAVING count(*) IS DISTINCT FROM 0");
		var aggregates = Aggregates.of(select.$having());

		assertThat(aggregates).hasSize(1);
		assertThat(aggregates).first().isInstanceOf(QOM.Count.class);
	}

	@Test
	@DisplayName("IsNotDistinctFrom: HAVING count(*) IS NOT DISTINCT FROM 5 returns 1 aggregate")
	void isNotDistinctFrom() {
		var select = TestUtils
			.parseSelect("SELECT name, count(*) FROM People GROUP BY name HAVING count(*) IS NOT DISTINCT FROM 5");
		var aggregates = Aggregates.of(select.$having());

		assertThat(aggregates).hasSize(1);
		assertThat(aggregates).first().isInstanceOf(QOM.Count.class);
	}

	@Test
	@DisplayName("LikeIgnoreCase: HAVING max(name) ILIKE 'a%' returns 1 aggregate")
	void likeIgnoreCase() {
		var select = TestUtils
			.parseSelect("SELECT department, max(name) FROM Employees GROUP BY department HAVING max(name) ILIKE 'a%'");
		var aggregates = Aggregates.of(select.$having());

		assertThat(aggregates).hasSize(1);
		assertThat(aggregates).first().isInstanceOf(QOM.Max.class);
	}

	@Test
	@DisplayName("NotLikeIgnoreCase: HAVING max(name) NOT ILIKE 'a%' returns 1 aggregate")
	void notLikeIgnoreCase() {
		var select = TestUtils.parseSelect(
				"SELECT department, max(name) FROM Employees GROUP BY department HAVING max(name) NOT ILIKE 'a%'");
		var aggregates = Aggregates.of(select.$having());

		assertThat(aggregates).hasSize(1);
		assertThat(aggregates).first().isInstanceOf(QOM.Max.class);
	}

}
