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

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.neo4j.jdbc.translator.spi.PrioritizedTranslator;
import org.neo4j.jdbc.translator.spi.Translator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class TranslatorComparatorTests {

	@Test
	void prioritizedOverNormal() {
		var t1 = new ComparableTranslator(Mockito.mock(PrioritizedTranslator.class));
		var t2 = new ComparableTranslator(Mockito.mock(Translator.class));

		assertThat(t2).isGreaterThan(t1);
		assertThat(t1).isLessThan(t2);
	}

	@Test
	void higherValueLowerPrecedence() {
		var td1 = mock(Translator.class);
		given(td1.getOrder()).willReturn(10);
		var t1 = new ComparableTranslator(td1);
		var td2 = mock(Translator.class);
		given(td2.getOrder()).willReturn(20);
		var t2 = new ComparableTranslator(td2);

		assertThat(t2).isGreaterThan(t1);
		assertThat(t1).isLessThan(t2);
	}

	@Test
	void defaultIsLow() {
		var td1 = mock(Translator.class);
		given(td1.getOrder()).willReturn(10);
		var t1 = new ComparableTranslator(td1);
		var td2 = mock(Translator.class);
		given(td2.getOrder()).willCallRealMethod();
		var t2 = new ComparableTranslator(td2);

		assertThat(t2).isGreaterThan(t1);
		assertThat(t1).isLessThan(t2);
	}

	// Needed due to restrictions in AssertJ and the fact that the SqlTranslator should
	// explicitly not be comparable and always obey our ordered principle.
	// See https://github.com/assertj/assertj/issues/2041
	record ComparableTranslator(Translator delegate) implements Comparable<ComparableTranslator> {
		@Override
		public int compareTo(ComparableTranslator o) {
			return TranslatorComparator.INSTANCE.compare(this.delegate, o.delegate);
		}
	}

}
