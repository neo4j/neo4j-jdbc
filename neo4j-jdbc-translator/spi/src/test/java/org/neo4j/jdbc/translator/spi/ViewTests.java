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
package org.neo4j.jdbc.translator.spi;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class ViewTests {

	@Test
	void columnNameShouldBeRequired() {
		assertThatNullPointerException().isThrownBy(() -> View.column(null, "x", "y"))
			.withMessage("Column name is required");
	}

	@Test
	void columnNameMustNotBeBlank() {
		assertThatIllegalArgumentException().isThrownBy(() -> View.column("", "x", "y"))
			.withMessage("Column name is required");
	}

	@Test
	void viewShouldBeImmutable() {
		List<View.Column> columns = new ArrayList<>();
		columns.add(View.column("a", "b", "c"));
		var view = new View("v", "MATCH (n) RETURN n", columns);
		columns.add(View.column("d", "e", "f"));
		var theViewColumns = view.columns();
		assertThat(theViewColumns).hasSize(1);
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(theViewColumns::clear);
	}

}
