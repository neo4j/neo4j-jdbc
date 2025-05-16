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
package org.neo4j.jdbc.translator.impl;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.jdbc.translator.spi.View;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * @author Michael J. Simons
 */
class ViewDefinitionReaderTests {

	@ParameterizedTest
	@ValueSource(strings = { "ftp://foo:bar@bazbar:4711/foo.json", "git://github.com:neo4j/neo4j-jdbc.git", "foobar" })
	void shouldThrowOnInvalidScheme(String input) {
		assertThatIllegalArgumentException().isThrownBy(() -> ViewDefinitionReader.of(input))
			.withMessageMatching("Unsupported scheme: .+, supported schemes are \\[file, http, https, resource]");
	}

	@ParameterizedTest
	@ValueSource(strings = { "git@github.com:neo4j/neo4j-jdbc.git" })
	void shouldThrowOnInvalidURI(String input) {
		assertThatIllegalArgumentException().isThrownBy(() -> ViewDefinitionReader.of(input))
			.withMessageMatching("Unsupported url: %s".formatted(input));
	}

	@ParameterizedTest
	@ValueSource(strings = { "file:///foobar.json", "file:/foobar.json", "http://host/foobar.json",
			"https://host/foobar.json" })
	void shouldDealWithValidSchemes(String input) {
		assertThatNoException().isThrownBy(() -> ViewDefinitionReader.of(input));
	}

	@Test
	void shouldNotExceptInvalidFileUrls1() {
		assertThatIllegalArgumentException().isThrownBy(() -> ViewDefinitionReader.of("file://path.json"))
			.withMessage(
					"No path specified in this url: file://path.json (path.json is the host in in that format, not the path; you probably meant file:///path.json)");
	}

	@Test
	void shouldNotExceptInvalidFileUrls2() {
		assertThatIllegalArgumentException().isThrownBy(() -> ViewDefinitionReader.of("file:path.json"))
			.withMessage(
					"No path specified in this url: file:path.json (path.json is a schema specific part in that format, not the path; you probably meant file:///path.json)");
	}

	@Test
	void shouldReadDefaultFormat() throws IOException {
		var resource = Objects.requireNonNull(this.getClass().getResource("/cbv/default.json")).toString();
		var views = ViewDefinitionReader.of(resource).read();

		assertWineViews(views);
	}

	@Test
	void shouldReadAlternativeFormat() throws IOException {
		var resource = Objects.requireNonNull(this.getClass().getResource("/cbv/alt-version.json")).toString();
		var views = ViewDefinitionReader.of(resource).read();

		assertWineViews(views);
	}

	@ParameterizedTest
	@ValueSource(strings = { "/cbv/empty-array.json", "/cbv/empty-object.json", "/cbv/totally-empty.json" })
	void shouldNotFailForTheEmptiness(String machine) throws IOException {

		var resource = Objects.requireNonNull(this.getClass().getResource(machine)).toString();
		var views = ViewDefinitionReader.of(resource).read();
		assertThat(views).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = { "/cbv/broken-column.json" })
	void columnNameOrPropertyIsRequired(String input) {

		var resource = Objects.requireNonNull(this.getClass().getResource(input)).toString();
		var viewDefinitionReader = ViewDefinitionReader.of(resource);
		assertThatIllegalArgumentException().isThrownBy(viewDefinitionReader::read)
			.withRootCauseInstanceOf(NullPointerException.class)
			.withStackTraceContaining("Column name is required");
	}

	@ParameterizedTest
	@ValueSource(strings = { "/cbv/broken-array.json", "/cbv/broken-object.json" })
	void shouldFailGracefully(String machine) {

		var resource = Objects.requireNonNull(this.getClass().getResource(machine)).toString();
		var viewDefinitionReader = ViewDefinitionReader.of(resource);
		assertThatIllegalArgumentException().isThrownBy(viewDefinitionReader::read)
			.withMessageMatching("Invalid JSON content, cannot read .*\\.json(:.*)?");
	}

	private static void assertWineViews(List<View> views) {
		assertThat(views).hasSize(1);
		assertThat(views).first().satisfies(view -> {
			assertThat(view.name()).isEqualTo("CountryCountsView");
			assertThat(view.query()).isEqualTo(
					"MATCH (n:Country)-[]-(:Province)-[]-(:Region)-[]-(:Winery)-[]-(w:Wine) RETURN DISTINCT n.country AS countries, COUNT(DISTINCT w) AS numWines, elementId(n)");
			assertThat(view.columns()).map(View.Column::name)
				.containsExactlyInAnyOrder("_NodeId_", "Country", "numWines");
		});
	}

}
