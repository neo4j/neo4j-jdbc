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

import java.sql.DatabaseMetaData;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import org.jooq.DSLContext;
import org.jooq.conf.ParseWithMetaLookups;
import org.jooq.impl.DSL;
import org.jooq.impl.ParserException;
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

	private final SqlToCypherConfig config;

	private volatile DSLContext dslContext;

	SparkSubqueryCleaningTranslator(SqlToCypherConfig config) {
		this.config = config;
	}

	@Override
	public int getOrder() {
		return Optional.ofNullable(this.config.getPrecedence()).orElse(Translator.LOWEST_PRECEDENCE) - 1;
	}

	@Override
	public String translate(String statement, DatabaseMetaData optionalDatabaseMetaData) {
		if (!mightBeASparkQuery(statement)) {
			return statement;
		}

		var extractedSubquery = extractSubquery(statement);
		if (extractedSubquery.isEmpty()) {
			return statement;
		}

		if (canParseAsSelect(extractedSubquery.get())) {
			return statement;
		}

		// Thing is by a good chance Cypher…
		return """
				/*+ NEO4J FORCE_CYPHER */
				CALL {%s} RETURN * LIMIT 1
				""".formatted(extractedSubquery.get()).strip();
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

	private DSLContext getDslContext() {
		DSLContext result = this.dslContext;
		if (result == null) {
			synchronized (this) {
				result = this.dslContext;
				if (result == null) {
					this.dslContext = DSL.using(this.config.getSqlDialect(),
							this.config.asSettings(ParseWithMetaLookups.OFF));
					result = this.dslContext;
				}
			}
		}
		return result;
	}

	boolean canParseAsSelect(String statement) {
		try {
			getDslContext().parser().parseSelect(statement);
		}
		catch (ParserException ex) {
			return false;
		}
		return true;
	}

}
