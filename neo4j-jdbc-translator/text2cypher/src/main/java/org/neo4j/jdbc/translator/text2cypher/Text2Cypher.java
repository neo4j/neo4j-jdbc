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
package org.neo4j.jdbc.translator.text2cypher;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import org.neo4j.jdbc.translator.spi.Translator;

/**
 * OpenAI based implementation of a {@link Translator}. Using Langchain4j to interact with
 * models and apis.
 *
 * @author Michael J. Simons
 * @author Gerrit Meier
 */
final class Text2Cypher implements Translator {

	private static final Logger LOGGER = Logger.getLogger(Text2Cypher.class.getCanonicalName());

	static final String PREFIX = "🤖, ";
	static final int PREFIX_LENGTH = PREFIX.length();

	private final CypherExpert cypherExpert;

	Text2Cypher(Map<String, ?> config) {

		String openAIApiKey;
		if (config.containsKey("openAIApiKey")) {
			openAIApiKey = (String) config.get("openAIApiKey");
		}
		else {
			openAIApiKey = System.getenv("OPEN_AI_API_KEY");
		}
		openAIApiKey = Objects.requireNonNull(openAIApiKey,
				"Please configure an OpenAI API Key (Via system env OPEN_AI_API_KEY or explicit configuration)");
		var model = OpenAiChatModel.builder().modelName("gpt-4-turbo").temperature(0.0).apiKey(openAIApiKey).build();

		this.cypherExpert = AiServices.builder(CypherExpert.class).chatLanguageModel(model).build();
	}

	@Override
	public String translate(String question, DatabaseMetaData optionalDatabaseMetaData) {
		if (question == null || question.isBlank() || PREFIX.equals(question)) {
			throw new IllegalArgumentException("Cant translate a null or blank question");
		}

		if (optionalDatabaseMetaData == null) {
			throw new IllegalStateException("Database connection must be open and meta data be available");
		}

		if (!question.startsWith(PREFIX)) {
			return question;
		}

		question = (question.substring(PREFIX_LENGTH, PREFIX_LENGTH + 1).toUpperCase(Locale.ROOT)
				+ question.substring(PREFIX_LENGTH + 1))
			.trim();
		if (question.charAt(question.length() - 1) == ';') {
			question = question.substring(0, question.length() - 1);
		}

		LOGGER.log(Level.FINE, "Translating question '{0}'", new Object[] { question });

		try {
			var schema = Schema.from(optionalDatabaseMetaData.getConnection());
			var cypher = this.cypherExpert.translate(schema, question);

			LOGGER.log(Level.FINE, "Intermediate query '{0}'", new Object[] { cypher });

			cypher = schema.enforceRelationships(cypher);
			LOGGER.log(Level.FINE, "Final query '{0}'", new Object[] { cypher });
			return cypher;
		}
		catch (SQLException ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	interface CypherExpert {

		@SystemMessage("Given an input question, translate it to a Cypher query. Respond without formatting and pre-amble.")
		@UserMessage("""
				Based on the Neo4j graph schema below, write a Cypher query that would answer the user's question:
				{{schema}}

				Question: {{question}}
				""")
		String translate(@V("schema") Schema schema, @V("question") String question);

	}

}
