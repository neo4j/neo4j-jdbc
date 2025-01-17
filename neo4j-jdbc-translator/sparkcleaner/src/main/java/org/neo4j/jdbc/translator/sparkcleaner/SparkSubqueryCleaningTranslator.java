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
package org.neo4j.jdbc.translator.sparkcleaner;

import java.sql.DatabaseMetaData;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.neo4j.cypher.internal.parser.v5.Cypher5Lexer;
import org.neo4j.cypher.internal.parser.v5.Cypher5Parser;
import org.neo4j.jdbc.translator.spi.Translator;

/**
 * This translator will search for the occurrence of {@literal SPARK_GEN_SUBQ_} in a query
 * before passing it on towards to the next translator. If such a literal is found, a
 * check if it might be a spark subquery containing cypher instead of SQL is performed.
 * The check is performed by parsing it with jOOQ as well.
 *
 * @author Michael J. Simons
 * @since 6.2.0
 */
final class SparkSubqueryCleaningTranslator implements Translator {

	private static final Pattern SUBQUERY_PATTERN = Pattern
		.compile("(?ims)SELECT\\s+\\*\\s+FROM\\s+\\((.*?)\\)\\s+SPARK_GEN_SUBQ_0.*");

	private final int precedence;

	SparkSubqueryCleaningTranslator(int precedence) {
		this.precedence = precedence;
	}

	@Override
	public int getOrder() {
		return this.precedence;
	}

	@Override
	public String translate(String statement, DatabaseMetaData optionalDatabaseMetaData) {
		if (!mightBeASparkQuery(statement)) {
			return null;
		}

		var extractedSubquery = extractSubquery(statement);
		return extractedSubquery.filter(this::canParseAsCypher).map(v -> """
				/*+ NEO4J FORCE_CYPHER */
				CALL {%s} RETURN * LIMIT 1
				""".formatted(v).strip()).orElse(null);
	}

	static boolean mightBeASparkQuery(String statement) {
		return statement != null && statement.toUpperCase(Locale.ROOT).contains("SPARK_GEN_SUBQ");
	}

	static Optional<String> extractSubquery(String statement) {
		var matcher = SUBQUERY_PATTERN.matcher(statement);
		if (!matcher.matches()) {
			return Optional.empty();
		}
		return Optional.of(matcher.group(1).trim());
	}

	boolean canParseAsCypher(String statement) {

		// We might want to replace Unicode escape characters in the future
		// https://github.com/neo-technology/neo4j/blob/dev/public/community/cypher/front-end/parser/common/antlr-ast-common/src/main/java/org/neo4j/cypher/internal/parser/lexer/UnicodeEscapeReplacementReader.java
		var tokens = new CommonTokenStream(new Cypher5Lexer(CharStreams.fromString(statement)));
		var parser = new Cypher5Parser(tokens);
		parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
		parser.setErrorHandler(new BailErrorStrategy());
		Cypher5Parser.StatementsContext statements;
		try {
			parser.statements();
		}
		catch (Exception ex) {
			tokens.seek(0);
			parser.reset();
			parser.getInterpreter().setPredictionMode(PredictionMode.LL);
			try {
				parser.statements();
			}
			catch (ParseCancellationException ex2) {
				return false;
			}
		}

		return true;
	}

}
