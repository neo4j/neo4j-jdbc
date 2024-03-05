/*
 * Copyright (c) 2023-2024 "Neo4j,"
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

import java.util.Comparator;

import org.neo4j.jdbc.translator.spi.PrioritizedTranslator;
import org.neo4j.jdbc.translator.spi.Translator;

/**
 * {@link Comparator} implementation for {@link Translator} instances, sorting by priority
 * descending.
 *
 * @author Michael J. Simons
 * @since 6.0.0
 */
enum TranslatorComparator implements Comparator<Translator> {

	/**
	 * The only instance of this comparator.
	 */
	INSTANCE;

	@Override
	public int compare(Translator o1, Translator o2) {

		boolean p1 = (o1 instanceof PrioritizedTranslator);
		boolean p2 = (o2 instanceof PrioritizedTranslator);
		if (p1 && !p2) {
			return -1;
		}
		else if (p2 && !p1) {
			return 1;
		}

		int i1 = getOrder(o1);
		int i2 = getOrder(o2);
		return Integer.compare(i1, i2);
	}

	private int getOrder(Translator translator) {
		if (translator != null) {
			return translator.getOrder();
		}
		return Translator.LOWEST_PRECEDENCE;
	}

}
