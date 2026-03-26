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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.QOM;

final class Aggregates implements Iterable<Field<?>> {

	/**
	 * Walks a jOOQ {@link Condition} tree and collects all aggregate sub-expressions
	 * found within it. Handles logical connectives ({@code AND}, {@code OR}, {@code XOR},
	 * {@code NOT}), comparison operators ({@code >}, {@code >=}, {@code <}, {@code <=},
	 * {@code =}, {@code <>}, {@code IS DISTINCT FROM}, {@code IS NOT DISTINCT FROM}),
	 * {@code BETWEEN}, {@code IS NULL}, {@code IS NOT NULL}, {@code IN}, {@code NOT IN},
	 * {@code LIKE}, {@code NOT LIKE}, {@code LIKE} (case-insensitive), {@code NOT LIKE}
	 * (case-insensitive), and arithmetic expressions ({@code +}, {@code -}, {@code *},
	 * {@code /}) that may contain nested aggregates.
	 * @param condition the condition tree to walk
	 * @return a list of all aggregate {@link Field} instances found (may contain
	 * duplicates)
	 */
	static Aggregates of(Condition condition) {
		var result = new ArrayList<Field<?>>();
		collectAggregatesFromCondition(condition, result);
		return new Aggregates(result);
	}

	static boolean isAggregate(Field<?> f) {
		return f instanceof QOM.Count || f instanceof QOM.Sum || f instanceof QOM.Min || f instanceof QOM.Max
				|| f instanceof QOM.Avg || f instanceof QOM.StddevSamp || f instanceof QOM.StddevPop;
	}

	@SuppressWarnings("squid:S3776") // Yep, this is complex.
	private static void collectAggregatesFromCondition(Condition condition, List<Field<?>> result) {
		if (condition instanceof QOM.And and) {
			collectAggregatesFromCondition(and.$arg1(), result);
			collectAggregatesFromCondition(and.$arg2(), result);
		}
		else if (condition instanceof QOM.Or or) {
			collectAggregatesFromCondition(or.$arg1(), result);
			collectAggregatesFromCondition(or.$arg2(), result);
		}
		else if (condition instanceof QOM.Xor xor) {
			collectAggregatesFromCondition(xor.$arg1(), result);
			collectAggregatesFromCondition(xor.$arg2(), result);
		}
		else if (condition instanceof QOM.Not not) {
			collectAggregatesFromCondition(not.$arg1(), result);
		}
		else if (condition instanceof QOM.Gt<?> gt) {
			collectAggregatesFromField(gt.$arg1(), result);
			collectAggregatesFromField(gt.$arg2(), result);
		}
		else if (condition instanceof QOM.Ge<?> ge) {
			collectAggregatesFromField(ge.$arg1(), result);
			collectAggregatesFromField(ge.$arg2(), result);
		}
		else if (condition instanceof QOM.Lt<?> lt) {
			collectAggregatesFromField(lt.$arg1(), result);
			collectAggregatesFromField(lt.$arg2(), result);
		}
		else if (condition instanceof QOM.Le<?> le) {
			collectAggregatesFromField(le.$arg1(), result);
			collectAggregatesFromField(le.$arg2(), result);
		}
		else if (condition instanceof QOM.Eq<?> eq) {
			collectAggregatesFromField(eq.$arg1(), result);
			collectAggregatesFromField(eq.$arg2(), result);
		}
		else if (condition instanceof QOM.Ne<?> ne) {
			collectAggregatesFromField(ne.$arg1(), result);
			collectAggregatesFromField(ne.$arg2(), result);
		}
		else if (condition instanceof QOM.Between<?> between) {
			collectAggregatesFromField(between.$arg1(), result);
			collectAggregatesFromField(between.$arg2(), result);
			collectAggregatesFromField(between.$arg3(), result);
		}
		else if (condition instanceof QOM.IsNull isNull) {
			collectAggregatesFromField(isNull.$arg1(), result);
		}
		else if (condition instanceof QOM.IsNotNull isNotNull) {
			collectAggregatesFromField(isNotNull.$arg1(), result);
		}
		else if (condition instanceof QOM.InList<?> inList) {
			collectAggregatesFromField(inList.$field(), result);
			for (var element : inList.$list()) {
				collectAggregatesFromField(element, result);
			}
		}
		else if (condition instanceof QOM.Like like) {
			collectAggregatesFromField(like.$arg1(), result);
			collectAggregatesFromField(like.$arg2(), result);
		}
		else if (condition instanceof QOM.NotLike notLike) {
			collectAggregatesFromField(notLike.$arg1(), result);
			collectAggregatesFromField(notLike.$arg2(), result);
		}
		else if (condition instanceof QOM.LikeIgnoreCase likeIc) {
			collectAggregatesFromField(likeIc.$arg1(), result);
			collectAggregatesFromField(likeIc.$arg2(), result);
		}
		else if (condition instanceof QOM.NotLikeIgnoreCase notLikeIc) {
			collectAggregatesFromField(notLikeIc.$arg1(), result);
			collectAggregatesFromField(notLikeIc.$arg2(), result);
		}
		else if (condition instanceof QOM.NotInList<?> notInList) {
			collectAggregatesFromField(notInList.$field(), result);
			for (var element : notInList.$list()) {
				collectAggregatesFromField(element, result);
			}
		}
		else if (condition instanceof QOM.IsDistinctFrom<?> isDistinctFrom) {
			collectAggregatesFromField(isDistinctFrom.$arg1(), result);
			collectAggregatesFromField(isDistinctFrom.$arg2(), result);
		}
		else if (condition instanceof QOM.IsNotDistinctFrom<?> isNotDistinctFrom) {
			collectAggregatesFromField(isNotDistinctFrom.$arg1(), result);
			collectAggregatesFromField(isNotDistinctFrom.$arg2(), result);
		}
	}

	private static void collectAggregatesFromField(Field<?> field, List<Field<?>> result) {
		if (field == null) {
			return;
		}
		if (isAggregate(field)) {
			result.add(field);
		}
		else if (field instanceof QOM.Add<?> add) {
			collectAggregatesFromField(add.$arg1(), result);
			collectAggregatesFromField(add.$arg2(), result);
		}
		else if (field instanceof QOM.Sub<?> sub) {
			collectAggregatesFromField(sub.$arg1(), result);
			collectAggregatesFromField(sub.$arg2(), result);
		}
		else if (field instanceof QOM.Mul<?> mul) {
			collectAggregatesFromField(mul.$arg1(), result);
			collectAggregatesFromField(mul.$arg2(), result);
		}
		else if (field instanceof QOM.Div<?> div) {
			collectAggregatesFromField(div.$arg1(), result);
			collectAggregatesFromField(div.$arg2(), result);
		}
	}

	private final List<Field<?>> value;

	private Aggregates(List<Field<?>> value) {
		this.value = value;
	}

	@Override
	public Iterator<Field<?>> iterator() {
		return this.value.iterator();
	}

}
