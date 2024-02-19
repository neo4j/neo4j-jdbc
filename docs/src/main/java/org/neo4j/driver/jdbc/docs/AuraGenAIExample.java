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
package org.neo4j.driver.jdbc.docs;

import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import com.opencsv.CSVReaderBuilder;
import org.neo4j.driver.jdbc.Neo4jDriver;
import org.neo4j.driver.jdbc.Neo4jPreparedStatement;

public final class AuraGenAIExample {

	private AuraGenAIExample() {
	}

	public static void main(String... args) throws Exception {

		var openAIToken = System.getenv("OPEN_AI_TOKEN");
		if (openAIToken == null || openAIToken.isBlank()) {
			throw new IllegalArgumentException(
					"Please set a system environment variable named `OPEN_AI_TOKEN` containing your OpenAI token");
		}

		// Getting a connection
		try (var con = Neo4jDriver.withSQLTranslation().fromEnv().orElseThrow()) {

			var indexes = new String[] { """
					/*+ NEO4J FORCE_CYPHER */
					CREATE CONSTRAINT `movie-link` IF NOT EXISTS
					FOR (n:Movie) REQUIRE n.link IS UNIQUE
					""", """
					/*+ NEO4J FORCE_CYPHER */
					CREATE CONSTRAINT `genre-name` IF NOT EXISTS
					FOR (n:Genre) REQUIRE n.name IS UNIQUE
					""", """
					/*+ NEO4J FORCE_CYPHER */
					CREATE VECTOR INDEX `movie-embeddings` IF NOT EXISTS
					FOR (n:Movie) ON (n.embedding)
					OPTIONS {indexConfig: {
						`vector.dimensions`: 1536,
						`vector.similarity_function`: 'cosine'
					}}
					""" };

			System.out.printf("# Using a simple statement several times for creating indexes:%n%n");
			// The most simple statement class in JDBC that exists: java.sql.Statement.
			// Can be used to execute arbitrary queries with results, or ddl such as index
			// creation. It also can be reused.
			try (var stmt = con.createStatement()) {
				for (var idx : indexes) {
					stmt.execute(idx);
				}
			}

			var movies = readMovies();
			var genres = movies.stream().flatMap(m -> m.genres.stream()).distinct().toList();

			System.out.printf("%n# Using a prepared statement with batching:%n%n");
			// Here we are using a java.sql.PreparedStatement that allows batching
			// statements. Take note of the log, our sql will be rewritten into a proper
			// unwind batched statement.
			try (var stmt = con.prepareStatement("INSERT INTO Genre(name) VALUES (?) ON CONFLICT DO NOTHING")) {
				for (var genre : genres) {
					stmt.setString(1, genre);
					stmt.addBatch();
				}
				var cnts = stmt.executeBatch();
				System.out.printf("Executed %d batches%n", cnts.length);
				for (int i = 0; i < cnts.length; ++i) {
					System.out.printf("Batch %d did %d updates%n", i, cnts[i]);
				}
			}

			System.out.printf("%n# Using a Neo4j prepared statement with Cypher and named parameters:%n%n");
			// Our own implementation does support named parameters too
			var insertMovieStatement = """
					/*+ NEO4J FORCE_CYPHER */
					UNWIND $parameters AS row
					MERGE (movie:Movie {link: row.movie.link})
					ON CREATE SET movie = row.movie
					WITH movie, row.genres AS genres
					UNWIND genres AS __genre
					MATCH (g:Genre {name: __genre})
					MERGE (movie) -[:HAS]->(g)
					""";
			try (var stmt = con.prepareStatement(insertMovieStatement).unwrap(Neo4jPreparedStatement.class)) {
				// Complex parameters such as a list of nested maps are allowed too.
				var parameters = movies.stream().map(Movie::asMap).toList();
				stmt.setObject("parameters", parameters);
				var updates = stmt.executeUpdate();
				System.out.printf("%d updates%n", updates);
			}

			System.out.printf("%n# Utilizing automatic SQL translation and projecting all attributes:%n%n");
			var selectMovies = """
					SELECT m.*, collect(g.name) AS genres
					FROM Movie m
					NATURAL JOIN HAS r
					NATURAL JOIN Genre g
					WHERE m.title LIKE 'A%'
					ORDER BY m.title LIMIT 20
					""";
			try (var stmt = con.createStatement(); var result = stmt.executeQuery(selectMovies)) {
				while (result.next()) {
					System.out.printf("%s %s%n", result.getString("title"), result.getObject("genres"));
				}
			}
			System.out.printf(
					"%nIf you want to see the Cypher into which SQL has been translated, use Connection#nativeSQL:%n%s%n",
					con.nativeSQL(selectMovies));

			System.out.printf("%n# Callable statement example:%n%n");
			// We can use Neo4j's new capabilities to call service providers for us. The
			// "correct" order of parameters would actually be
			// `genai.vector.encode(:resource, :provider, :configuration)`, but the
			// callable statement allows proper named parameters, that is: The names
			// specify the position, not only an arbitrary placeholder.
			try (var stmt = con.prepareCall("{CALL genai.vector.encode(:provider, :resource, :configuration)}")) {
				stmt.setString("resource", "Hello, Neo4j JDBC Driver");
				stmt.setString("provider", "OpenAI");
				stmt.setObject("configuration", Map.of("token", openAIToken));
				stmt.execute();
				System.out.printf("Embedding is %s%n", stmt.getObject(1));
			}

			System.out.printf("%n# Prepared statement executing an update (No output)%n");
			var createEmbeddingsStatement = """
					/*+ NEO4J FORCE_CYPHER */
					WITH 100 AS batch_size
					MATCH (m:Movie)
					WHERE m.description IS NOT NULL AND m.description <> '' AND m.embedding IS NULL
					WITH floor(id(m) / batch_size) as group, m
					WITH group, collect(m) AS nodes, collect(m.description) AS resources
					CALL {
						WITH nodes, resources
						CALL genai.vector.encodeBatch(resources, $provider, $configuration) YIELD index, vector
						CALL db.create.setNodeVectorProperty(nodes[index], "embedding", vector)
					} IN TRANSACTIONS OF 10 ROWS
					""";
			try (var stmt = con.prepareStatement(createEmbeddingsStatement).unwrap(Neo4jPreparedStatement.class)) {
				stmt.setString("provider", "OpenAI");
				stmt.setObject("configuration", Map.of("token", openAIToken));
				stmt.executeUpdate();
			}

			System.out.printf("%n# Prepared statement executing a query:%n%n");
			var query = """
					/*+ NEO4J FORCE_CYPHER */
					WITH genai.vector.encode($term, $provider, $configuration) AS searchTerm
					CALL db.index.vector.queryNodes('movie-embeddings', 10, searchTerm)
					YIELD node AS movie, score
					RETURN movie.title AS title, movie.released AS year, movie.description AS description, score
					ORDER BY score DESC
					""";
			try (var stmt = con.prepareStatement(query).unwrap(Neo4jPreparedStatement.class)) {
				stmt.setString("term", "A movie about love and positive emotions");
				stmt.setString("provider", "OpenAI");
				stmt.setObject("configuration", Map.of("token", openAIToken));
				try (var results = stmt.executeQuery()) {
					while (results.next()) {
						System.out.printf("%s (%d, %s), Score: %f%n", results.getString("title"),
								results.getInt("year"), results.getString("description"), results.getDouble("score"));
					}
				}
			}
		}
	}

	static List<Movie> readMovies() throws Exception {

		var result = new ArrayList<Movie>();

		try (var csvReader = new CSVReaderBuilder(new InputStreamReader(
				Objects.requireNonNull(AuraGenAIExample.class.getResourceAsStream("/movies.csv"))))
			.withSkipLines(1)
			.build()) {
			String[] nextRecord;
			while ((nextRecord = csvReader.readNext()) != null) {
				result.add(new Movie(nextRecord));
			}
		}
		return result;
	}

	record Movie(String title, Integer released, List<String> genres, String description, URI link) {

		Movie(String[] row) {
			this(row[0].trim(), Integer.parseInt(row[1]),
					Arrays.stream(row[2].split("/")).filter(Predicate.not(String::isBlank)).map(String::trim).toList(),
					row[3].trim(), URI.create(row[4].trim()));
		}

		Map<String, Object> asMap() {
			return Map.of("movie", Map.of("title", this.title, "released", this.released, "description",
					this.description, "link", this.link.toString()), "genres", this.genres);
		}
	}

}
