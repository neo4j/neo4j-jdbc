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
package org.neo4j.jdbc.it.cp;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;

/**
 * Mapping tests are disabled in native image so that we don't have to register the
 * records. Only the fact that JSON mappers can be loaded must be asserted in native
 * image.
 */
class JacksonIT extends IntegrationTestBase {

	private final ObjectMapper objectMapper;

	JacksonIT() {
		super.doClean = false;

		this.objectMapper = new ObjectMapper();
		this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		this.objectMapper.enable(JsonGenerator.Feature.IGNORE_UNKNOWN);
	}

	@BeforeAll
	void prepareData() throws SQLException, IOException {
		try (var connection = getConnection()) {
			connection.setAutoCommit(false);
			var stmt = connection.createStatement();
			stmt.executeQuery("MATCH (n) DETACH DELETE n");
			TestUtils.createMovieGraph(connection);
			connection.setAutoCommit(true);
		}
	}

	@Test
	void jsonMappingShouldWork() throws SQLException {
		try (var connection = getConnection(); var stmt = connection.createStatement()) {
			var result = stmt.executeQuery("""
					RETURN true AS bv
					""");

			assertThat(result.next()).isTrue();
			var json = result.getObject("bv", JsonNode.class);
			assertThat(json.isValueNode()).isTrue();
		}
	}

	@Test
	@DisabledInNativeImage
	void nodeMappingShouldWork() throws SQLException, IOException {

		try (var connection = getConnection(); var stmt = connection.createStatement()) {
			var result = stmt.executeQuery("""
					MATCH (n:Movie {title: 'The Matrix'})
					RETURN n AS movieNode,
					       n{.*} AS movieMap,
					       [n] AS movieList,
					       [n{.*}] AS movieMapList,
					       elementId(n) AS elementId
					""");

			assertThat(result.next()).isTrue();

			var elementId = result.getString("elementId");
			var expectedJson = """
					{
					  "elementId" : "%s",
					  "labels" : [ "Movie" ],
					  "properties" : {
					    "title" : "The Matrix",
					    "tagline" : "Welcome to the Real World",
					    "released" : 1999
					  }
					}""".formatted(elementId);

			// Given the JDBC driver returns a graph node as JsonNode
			var json = result.getObject("movieNode", JsonNode.class);

			// then the json node is an object node
			assertThat(json.isObject()).isTrue();

			// then the object node adheres to the same format as the Query api
			assertThat(prettyPrint(json)).isEqualTo(expectedJson);

			record Movie(String title, String tagline, int released) {
			}

			Consumer<Movie> assertMovie = movie -> {
				assertThat(movie.title).isEqualTo("The Matrix");
				assertThat(movie.tagline).isEqualTo("Welcome to the Real World");
				assertThat(movie.released).isEqualTo(1999);
			};

			// then it can easily be further processed by $someoneElsesTooling
			assertMovie.accept(this.objectMapper.treeToValue(json.get("properties"), Movie.class));

			// then the same should apply to a map
			var map = result.getObject("movieMap", JsonNode.class);
			assertMovie.accept(this.objectMapper.treeToValue(map, Movie.class));

			// then it should be able to deal with lists of nodes
			for (var element : result.getObject("movieList", JsonNode.class)) {
				assertMovie.accept(this.objectMapper.treeToValue(element.get("properties"), Movie.class));
			}

			// then it also should be able to deal with a list of maps
			for (var element : result.getObject("movieMapList", JsonNode.class)) {
				assertMovie.accept(this.objectMapper.treeToValue(element, Movie.class));
			}
		}
	}

	@Test
	@DisabledInNativeImage
	void relationshipMappingShouldWork() throws SQLException, IOException {

		try (var connection = getConnection(); var stmt = connection.createStatement()) {
			var result = stmt.executeQuery("""
					MATCH (:Movie {title: 'Cloud Atlas'})<-[r:ACTED_IN]-()
					RETURN r AS actedNode,
					       r{.*} AS actedMap,
					       [r] AS actedList,
					       [r{.*}] AS actedMapList,
					       r.roles AS roles,
					       elementId(r) AS elementId,
					       elementId(startNode(r)) AS startNodeElementId,
					       elementId(endNode(r)) AS endNodeElementId
					""");

			record Acted(List<String> roles) {
			}

			var results = new AtomicBoolean();
			while (result.next()) {
				results.compareAndSet(false, true);
				@SuppressWarnings("unchecked")
				var roles = (List<String>) result.getObject("roles", List.class);
				var elementId = result.getString("elementId");
				var expectedJson = """
						{
						  "elementId" : "%s",
						  "startNodeElementId" : "%s",
						  "endNodeElementId" : "%s",
						  "type" : "ACTED_IN",
						  "properties" : {
						    "roles" : %s
						  }
						}""".formatted(elementId, result.getString("startNodeElementId"),
						result.getString("endNodeElementId"),
						roles.stream().map("\"%s\""::formatted).collect(Collectors.joining(", ", "[ ", " ]")));

				// Given the JDBC driver returns a graph node as JsonNode
				var json = result.getObject("actedNode", JsonNode.class);

				// then the json node is an object node
				assertThat(json.isObject()).isTrue();

				// then the object node adheres to the same format as the Query api
				assertThat(prettyPrint(json)).isEqualTo(expectedJson);

				Consumer<Acted> assertActed = acted -> assertThat(acted.roles)
					.containsExactlyInAnyOrderElementsOf(roles);

				// then it can easily be further processed by $someoneElsesTooling
				assertActed.accept(this.objectMapper.treeToValue(json.get("properties"), Acted.class));

				// then the same should apply to a map
				var map = result.getObject("actedMap", JsonNode.class);
				assertActed.accept(this.objectMapper.treeToValue(map, Acted.class));

				// then it should be able to deal with lists of relationships
				for (var element : result.getObject("actedList", JsonNode.class)) {
					assertActed.accept(this.objectMapper.treeToValue(element.get("properties"), Acted.class));
				}

				// then it also should be able to deal with a list of maps
				for (var element : result.getObject("actedMapList", JsonNode.class)) {
					assertActed.accept(this.objectMapper.treeToValue(element, Acted.class));
				}
			}
			assertThat(results).isTrue();
		}
	}

	@Test
	@DisabledInNativeImage
	void pathMappingShouldWork() throws SQLException {
		try (var connection = getConnection(); var stmt = connection.createStatement()) {
			var result = stmt.executeQuery("""
					MATCH p=(actor:Person)-[:ACTED_IN]->(movie:Movie)<-[:DIRECTED]-(director:Person)
					RETURN p
					""");

			assertThat(result.next()).isTrue();
			assertThatNoException().isThrownBy(() -> result.getObject("p", JsonNode.class));
			assertThatNoException().isThrownBy(() -> result.getObject("p", ArrayNode.class));
		}
	}

	@Test
	void shouldNotMapToWrongType() throws SQLException {
		try (var connection = getConnection(); var stmt = connection.createStatement()) {
			var result = stmt.executeQuery("""
					MATCH p=(actor:Person)-[:ACTED_IN]->(movie:Movie)<-[:DIRECTED]-(director:Person)
					RETURN p
					""");

			assertThat(result.next()).isTrue();
			assertThatRuntimeException().isThrownBy(() -> result.getObject("p", ObjectNode.class))
				.withMessage(
						"Resulting type after mapping is incompatible, use com.fasterxml.jackson.databind.node.ArrayNode or com.fasterxml.jackson.databind.JsonNode for reification");
		}
	}

	@Test
	@DisabledInNativeImage
	void javaDriverMappingExamplesShouldWorkToo() throws SQLException, JsonProcessingException {
		record MovieInfo(String title, String director, List<String> actors) {
		}

		try (var connection = getConnection(); var stmt = connection.createStatement()) {
			var result = stmt.executeQuery("""
					MATCH (actor:Person)-[:ACTED_IN]->(movie:Movie)<-[:DIRECTED]-(director:Person)
					WITH movie.title AS title, director.name AS director, collect(actor.name) AS actors
					RETURN {title: title, director: director, actors: actors}
					""");

			var results = new AtomicBoolean();
			while (result.next()) {
				results.compareAndSet(false, true);
				var json = result.getObject(1, JsonNode.class);
				var movieInfo = this.objectMapper.treeToValue(json, MovieInfo.class);
				assertThat(movieInfo.title).isNotNull();
				assertThat(movieInfo.director).isNotNull();
				assertThat(movieInfo.actors).isNotEmpty();
			}

			assertThat(results).isTrue();

		}
	}

	@Test
	@DisabledInNativeImage
	void writingShouldWork() throws Exception {
		record Movie(String title, String tagline, long released) {
		}

		var movie = new Movie("title", "tagline", 2025);
		try (var connection = getConnection(); var stmt = connection.prepareStatement("CREATE (m:Movie $1) RETURN m")) {
			stmt.setObject(1, this.objectMapper.valueToTree(movie));
			var rs = stmt.executeQuery();
			assertThat(rs.next()).isTrue();
			var json = rs.getObject("m", JsonNode.class);
			assertThat(json.get("elementId")).isNotNull();
			var newMovie = this.objectMapper.treeToValue(json.get("properties"), Movie.class);
			assertThat(newMovie.title).isEqualTo("title");
		}
	}

	private String prettyPrint(JsonNode json) throws IOException {
		var sw = new StringWriter();
		var gen = this.objectMapper.createGenerator(sw);
		gen.writeTree(json);
		return sw.toString();
	}

}
