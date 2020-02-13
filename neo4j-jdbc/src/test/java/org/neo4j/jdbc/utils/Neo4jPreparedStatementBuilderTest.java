/*
 * Copyright (c) 2016 LARUS Business Automation [http://www.larus-ba.it]
 * <p>
 * This file is part of the "LARUS Integration Framework for Neo4j".
 * <p>
 * The "LARUS Integration Framework for Neo4j" is licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * Created on 25/03/16
 */
package org.neo4j.jdbc.utils;

import org.junit.Test;

import static org.neo4j.jdbc.utils.PreparedStatementBuilder.replacePlaceholders;
import static org.junit.Assert.assertEquals;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class Neo4jPreparedStatementBuilderTest {

	@Test public void replacePlaceholderShouldReturnSameStatementIfNoPlaceholders() {
		String raw = "MATCH statement RETURN same WHERE thereAreNoPlaceholders";
		assertEquals(raw, replacePlaceholders(raw));
	}

	@Test public void replacePlaceholderShouldReplaceQuestionMarksByDefaultWithOrderedCurlyBraces() {
		String raw = "MATCH statement RETURN same WHERE thereIsAPlaceholder = ?";
		assertEquals("MATCH statement RETURN same WHERE thereIsAPlaceholder = $1", replacePlaceholders(raw));
	}

	@Test public void replacePlaceholderShouldReplaceQuestionMarksByDefaultWithOrderedCurlyBracesMultiple() {
		String raw = "MATCH statement RETURN same WHERE thereIsAPlaceholder = ? AND another = ?";
		assertEquals("MATCH statement RETURN same WHERE thereIsAPlaceholder = $1 AND another = $2", replacePlaceholders(raw));
	}

	@Test public void replacePlaceholderShouldReplaceQuestionMarksByDefaultWithOrderedCurlyBracesMultipleNotInStrings() {
		String raw = "MATCH statement RETURN same WHERE thisIs = \"a string ?\" AND thereIsAPlaceholder = ? AND another = ? AND notInString = \"shall I replace this?\"";
		assertEquals(
				"MATCH statement RETURN same WHERE thisIs = \"a string ?\" AND thereIsAPlaceholder = $1 AND another = $2 AND notInString = \"shall I replace this?\"",
				replacePlaceholders(raw));
	}

	@Test public void replacePlaceholderShouldReplaceQuestionMarksMultiline() {
		String raw = "MATCH statement RETURN same WHERE thereIsAPlaceholder = ? \nAND newLinePar = ?";
		assertEquals("MATCH statement RETURN same WHERE thereIsAPlaceholder = $1 \nAND newLinePar = $2", replacePlaceholders(raw));
	}

	@Test public void replacePlaceholderShouldReplaceQuestionMarksWithOrderedCurlyBracesNotInQuotesAndNotCommentedMultilineMulti() {
		String raw = "MATCH statement RETURN same WHERE thisIs = \"a string ?\"\n" +
				"AND thereIsAPlaceholder = ?\n" +
				"AND another = ?";
		assertEquals("MATCH statement RETURN same WHERE thisIs = \"a string ?\"\nAND thereIsAPlaceholder = $1\nAND another = $2", replacePlaceholders(raw));
	}

	@Test public void placeholdersCountShouldCountCorrectlyIfNoPlaceholdersArePresent() {
		String raw = "MATCH n RETURN n";
		assertEquals(0, PreparedStatementBuilder.namedParameterCount(raw));
	}

	@Test public void placeholdersCountShouldCountCorrectlyIfOneLine() {
		String raw = "MATCH n RETURN n WHERE param = $1";
		assertEquals(1, PreparedStatementBuilder.namedParameterCount(raw));
	}

	@Test public void placeholdersCountShouldCountCorrectlyIfOneLineString() {
		String raw = "MATCH n RETURN n WHERE param = $1 AND paramString = \"string$2\"";
		assertEquals(1, PreparedStatementBuilder.namedParameterCount(raw));
	}

	@Test public void placeholdersCountShouldCountCorrectlyIfOneLineStringMultiline() {
		String raw = "MATCH n RETURN n WHERE param = $2 AND paramString = \"string$3\"\n" + "AND param2 = $1";
		assertEquals(2, PreparedStatementBuilder.namedParameterCount(raw));
	}
}
