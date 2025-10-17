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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.ast.Block;
import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Treeprocessor;
import org.jooq.impl.ParserException;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.cypherdsl.core.renderer.Configuration;
import org.neo4j.cypherdsl.core.renderer.Dialect;
import org.neo4j.cypherdsl.core.renderer.Renderer;
import org.neo4j.cypherdsl.parser.CypherParser;
import org.neo4j.jdbc.translator.spi.Translator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author Michael J. Simons
 * @author Michael Hunger
 */
class SqlToCypherTests {

	private static final Translator NON_PRETTY_PRINTING_TRANSLATOR = SqlToCypher
		.with(SqlToCypherConfig.builder().withPrettyPrint(false).withAlwaysEscapeNames(false).build());

	private static ResultSet makeColumns(String firstName, String... names) throws SQLException {
		var personColumns = mock(ResultSet.class);
		if (firstName == null) {
			given(personColumns.next()).willReturn(false);
			return personColumns;
		}
		Boolean[] results = new Boolean[names.length + 1];
		for (int i = 0; i < results.length; i++) {
			results[i] = i != results.length - 1;
		}
		given(personColumns.next()).willReturn(true, results);
		given(personColumns.getString("COLUMN_NAME")).willReturn(firstName, names);
		return personColumns;
	}

	static Stream<Arguments> concatShouldWork() {
		return Stream.of(Arguments.of("SELECT 'a' || 'b'", "RETURN ('a' + 'b')"),
				Arguments.of("SELECT 'a' || 'b' || 'c'", "RETURN (('a' + 'b') + 'c')"),
				Arguments.of("SELECT CONCAT('a', 'b')", "RETURN ('a' + 'b')"),
				Arguments.of("SELECT CONCAT('a')", "RETURN 'a'"),
				Arguments.of("SELECT CONCAT('a', 'b', 'c')", "RETURN ('a' + ('b' + 'c'))"));
	}

	@ParameterizedTest
	@MethodSource
	void concatShouldWork(String sql, String expectedCypher) {

		var cypher = NON_PRETTY_PRINTING_TRANSLATOR.translate(sql);
		assertThat(cypher).isEqualTo(expectedCypher);
	}

	@Test
	void singleTable() {
		var cypher = NON_PRETTY_PRINTING_TRANSLATOR.translate(
				"""
						SELECT "installed_rank","version","description","type","script","checksum","installed_on","installed_by","execution_time","success"
						FROM "public"."flyway_schema_history"
						WHERE "installed_rank" > ?
						ORDER BY "installed_rank"
						""");
		assertThat(cypher).isEqualTo(
				"MATCH (flyway_schema_history:flyway_schema_history) WHERE flyway_schema_history.installed_rank > $1 RETURN flyway_schema_history.installed_rank AS installed_rank, flyway_schema_history.version AS version, flyway_schema_history.description AS description, flyway_schema_history.type AS type, flyway_schema_history.script AS script, flyway_schema_history.checksum AS checksum, flyway_schema_history.installed_on AS installed_on, flyway_schema_history.installed_by AS installed_by, flyway_schema_history.execution_time AS execution_time, flyway_schema_history.success AS success ORDER BY flyway_schema_history.installed_rank");
	}

	@Test
	void selectNShouldWork() {
		assertThat(NON_PRETTY_PRINTING_TRANSLATOR.translate("SELECT 1")).isEqualTo("RETURN 1");
	}

	@Test
	void parsingExceptionMustBeWrapped() {
		assertThatIllegalArgumentException().isThrownBy(() -> NON_PRETTY_PRINTING_TRANSLATOR.translate("whatever"))
			.withCauseInstanceOf(ParserException.class);
	}

	@Test
	void nonSupportedStatementsShouldBeCaught() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> NON_PRETTY_PRINTING_TRANSLATOR.translate("CREATE SEQUENCE IF NOT EXISTS bike_id"))
			.withMessageStartingWith("Unsupported SQL expression: create sequence if not exists bike_id");

	}

	@Test
	void countStar() {
		assertThat(NON_PRETTY_PRINTING_TRANSLATOR.translate("SELECT count(*) FROM test_write__c"))
			.isEqualTo("MATCH (test_write__c:test_write__c) RETURN count(*)");
	}

	@Test
	void countDistinctStar() {
		assertThat(NON_PRETTY_PRINTING_TRANSLATOR.translate("SELECT count(DISTINCT t.n) FROM test_write__c t"))
			.isEqualTo("MATCH (t:test_write__c) RETURN count(DISTINCT t.n)");
	}

	@Test
	void inClauseWithConstants() {

		assertThat(NON_PRETTY_PRINTING_TRANSLATOR.translate("""
				SELECT * FROM public.Genre
				WHERE name IN ('action comedy film', 'romcom')
				""")).isEqualTo("MATCH (genre:Genre) WHERE genre.name IN ['action comedy film', 'romcom'] RETURN *");
	}

	@Test
	void inClauseWithConstantsJoin() {

		assertThat(NON_PRETTY_PRINTING_TRANSLATOR.translate("""
				SELECT * FROM Movie NATURAL JOIN HAS NATURAL JOIN Genre
				WHERE genre.name IN ('action comedy film', 'romcom')
				""")).isEqualTo(
				"MATCH (movie:Movie)-[has:HAS]->(genre:Genre) WHERE genre.name IN ['action comedy film', 'romcom'] RETURN *");
	}

	@Test
	void inClauseWithConstantsJoinWithmeta() throws SQLException {

		DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
		given(databaseMetaData.getTables(any(), any(), any(), any())).willReturn(mock(ResultSet.class));
		var resultSet = makeColumns("title");
		given(databaseMetaData.getColumns(null, null, "Movie", null)).willReturn(resultSet);
		resultSet = makeColumns("foobar");
		given(databaseMetaData.getColumns(null, null, "HAS", null)).willReturn(resultSet);
		resultSet = makeColumns("name");
		given(databaseMetaData.getColumns(null, null, "Genre", null)).willReturn(resultSet);

		var renderer = Renderer.getRenderer(Configuration.newConfig()
			.withPrettyPrint(false)
			.alwaysEscapeNames(false)
			.withDialect(Dialect.NEO4J_5)
			.build());

		assertThat(NON_PRETTY_PRINTING_TRANSLATOR.translate("""
				SELECT * FROM Movie NATURAL JOIN HAS NATURAL JOIN Genre
				WHERE genre.name IN ('action comedy film', 'romcom')
				""", databaseMetaData)).isEqualTo(renderer.render(CypherParser.parse("""
				MATCH (movie:Movie)-[has:HAS]->(genre:Genre)
				WHERE genre.name IN ['action comedy film', 'romcom']
				RETURN elementId(movie) AS `v$id`, movie.title AS title, elementId(movie) AS `v$movie_id`,
					elementId(has) AS `v$id1`, has.foobar AS foobar,
					elementId(genre) AS `v$genre_id`, elementId(genre) AS `v$id2`, genre.name AS name
				""")));
	}

	@Test
	void plainColumnsEverywhere() throws SQLException {

		DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
		given(databaseMetaData.getTables(any(), any(), any(), any())).willReturn(mock(ResultSet.class));
		var resultSet1 = makeColumns("title", "released");
		var resultSet2 = makeColumns("title", "released");
		var resultSet3 = makeColumns("title", "released");
		var resultSet4 = makeColumns("title", "released");
		given(databaseMetaData.getColumns(null, null, "Movie", null)).willReturn(resultSet1)
			.willReturn(resultSet2)
			.willReturn(resultSet3)
			.willReturn(resultSet4);
		resultSet1 = makeColumns(null);
		resultSet2 = makeColumns(null);
		resultSet3 = makeColumns(null);
		given(databaseMetaData.getColumns(null, null, "HAS", null)).willReturn(resultSet1)
			.willReturn(resultSet2)
			.willReturn(resultSet3);
		resultSet1 = makeColumns("name");
		resultSet2 = makeColumns("name");
		resultSet3 = makeColumns("name");
		given(databaseMetaData.getColumns(null, null, "Genre", null)).willReturn(resultSet1)
			.willReturn(resultSet2)
			.willReturn(resultSet3);

		var sql = "SELECT title, released from Movie NATURAL JOIN HAS NATURAL JOIN Genre where name IN ('action film') order by title";
		var expected = "MATCH (movie:Movie)-[has:HAS]->(genre:Genre) WHERE genre.name IN ['action film'] RETURN movie.title, movie.released ORDER BY movie.title";

		assertThat(NON_PRETTY_PRINTING_TRANSLATOR.translate(sql, databaseMetaData)).isEqualTo(expected);
	}

	@Test
	void projectingRandomColumnsFromTable() {

		assertThat(NON_PRETTY_PRINTING_TRANSLATOR.translate("SELECT name, born FROM Person"))
			.isEqualTo("MATCH (person:Person) RETURN person.name AS name, person.born AS born");
	}

	@Test
	void projectingRandomColumnsFromTable2() {

		assertThat(NON_PRETTY_PRINTING_TRANSLATOR.translate("SELECT name, born FROM Person p"))
			.isEqualTo("MATCH (p:Person) RETURN p.name AS name, p.born AS born");
	}

	@Test
	void projectingRandomColumnsFromTable3() {

		assertThat(NON_PRETTY_PRINTING_TRANSLATOR.translate("SELECT p.name, p.born FROM Person p"))
			.isEqualTo("MATCH (p:Person) RETURN p.name, p.born");
	}

	@Test
	void joinsUsing() {

		var in = """
				SELECT p, m FROM Person p
				JOIN Movie m USING (ACTED_IN)
				""";
		assertThat(NON_PRETTY_PRINTING_TRANSLATOR.translate(in))
			.isEqualTo("MATCH (p:Person)-[acted_in:ACTED_IN]->(m:Movie) RETURN p, m");
	}

	@Test
	void naturalJoins() {

		var in = """
				SELECT p, c FROM Person p
				NATURAL JOIN ACTED_IN r
				NATURAL JOIN Movie m
				NATURAL JOIN PLAYED_IN
				NATURAL JOIN Cinema c
				""";
		assertThat(NON_PRETTY_PRINTING_TRANSLATOR.translate(in))
			.isEqualTo("MATCH (p:Person)-[r:ACTED_IN]->(m:Movie)-[played_in:PLAYED_IN]->(c:Cinema) RETURN p, c");
	}

	@Test
	void naturalAndUsingJoins() {

		var in = """
				SELECT p, r, m FROM Person p
				NATURAL JOIN ACTED_IN r
				JOIN Movie m USING (MOVIE_ID)
				""";
		assertThat(NON_PRETTY_PRINTING_TRANSLATOR.translate(in))
			.isEqualTo("MATCH (p:Person)-[r:ACTED_IN]->(m:Movie) RETURN p, r, m");
	}

	@Test
	void naturalAndOnJoins() {

		var in = """
				SELECT p, r, m FROM Person p
				NATURAL JOIN ACTED_IN r
				JOIN Movie m ON (m.id = r.movie_id)
				""";
		assertThat(NON_PRETTY_PRINTING_TRANSLATOR.translate(in))
			.isEqualTo("MATCH (p:Person)-[r:ACTED_IN]->(m:Movie) RETURN p, r, m");
	}

	@Test
	void namedParameterPrefixForParsingShouldBeConfigurable() {
		var translator = SqlToCypher.with(SqlToCypherConfig.builder()
			.withParseNamedParamPrefix("$")
			.withPrettyPrint(false)
			.withAlwaysEscapeNames(true)
			.build());
		assertThat(translator.translate("INSERT INTO Movie (Movie.title) VALUES($a)"))
			.isEqualTo("CREATE (movie:`Movie` {title: $a})");
	}

	@Test
	void standardInsertShouldWork() {
		var translator = SqlToCypher
			.with(SqlToCypherConfig.builder().withPrettyPrint(false).withAlwaysEscapeNames(true).build());
		assertThat(translator.translate("INSERT INTO Movie (Movie.title) VALUES('a')"))
			.isEqualTo("CREATE (movie:`Movie` {title: 'a'})");
	}

	@Test
	void unwindWithMergeWithoutPropertiesIsNotSupported() {
		var sql = """
					INSERT INTO People (first_name, last_name, born) VALUES
					('Helge', 'Schneider', 1955),
					('Bela', 'B', 1962) ON CONFLICT DO NOTHING
				""";
		var translator = SqlToCypher
			.with(SqlToCypherConfig.builder().withPrettyPrint(false).withAlwaysEscapeNames(true).build());
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> translator.translate(sql))
			.withMessage(
					"`ON DUPLICATE` and `ON CONFLICT` clauses are not supported when inserting multiple rows without using a property to merge on");
	}

	DatabaseMetaData mockRelationshipMeta() throws SQLException {
		var cbvs = mock(ResultSet.class);
		given(cbvs.next()).willReturn(false);

		var databaseMetadata = mock(DatabaseMetaData.class);
		given(databaseMetadata.getTables(null, null, null, new String[] { "CBV" })).willReturn(cbvs);

		var relationships = mock(ResultSet.class);
		given(relationships.next()).willReturn(true, false);
		given(relationships.getString("REMARKS")).willReturn("Person\nACTED_IN\nMovie");
		given(relationships.getString("TYPE")).willReturn("RELATIONSHIP");
		given(databaseMetadata.getTables(null, null, "Person_ACTED_IN_Movie", new String[] { "TABLE", "RELATIONSHIP" }))
			.willReturn(relationships);

		return databaseMetadata;
	}

	@Test
	void insertIntoRelationshipsWithMergeIsNotoSupported() throws SQLException {
		var sql = "	INSERT INTO Person_ACTED_IN_Movie (a, b, c) VALUES('a', 'b', 'c') ON CONFLICT DO NOTHING\n";

		var databaseMetadata = mockRelationshipMeta();

		var translator = SqlToCypher
			.with(SqlToCypherConfig.builder().withPrettyPrint(false).withAlwaysEscapeNames(true).build());
		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> translator.translate(sql, databaseMetadata))
			.withMessage("`ON DUPLICATE` and `ON CONFLICT` clauses are not supported for inserting relationships");
	}

	@ParameterizedTest
	@CsvSource(delimiterString = "|",
			textBlock = """
					true  | INSERT INTO Person_ACTED_IN_Movie (a, b, c, Person_ACTED_IN_Movie.d, Person_ACTED_IN_Movie.e) VALUES('a', 'b', 'c', 'd', 'e')                           | MERGE (_lhs:Person {a: 'a'}) MERGE (_rhs:Movie {c: 'c'}) CREATE (_lhs)-[:ACTED_IN {b: 'b', d: 'd', e: 'e'}]->(_rhs)
					true  | INSERT INTO Person_ACTED_IN_Movie (b, c, Person_ACTED_IN_Movie.d, Person_ACTED_IN_Movie.e) VALUES('b', 'c', 'd', 'e')                                   | CREATE (_lhs:Person) MERGE (_rhs:Movie {c: 'c'}) CREATE (_lhs)-[:ACTED_IN {b: 'b', d: 'd', e: 'e'}]->(_rhs)
					true  | INSERT INTO Person_ACTED_IN_Movie (a, b, Person_ACTED_IN_Movie.d, Person_ACTED_IN_Movie.e) VALUES('a', 'b', 'd', 'e')                                   | MERGE (_lhs:Person {a: 'a'}) CREATE (_rhs:Movie) CREATE (_lhs)-[:ACTED_IN {b: 'b', d: 'd', e: 'e'}]->(_rhs)
					true  | INSERT INTO Person_ACTED_IN_Movie (a, b, c, d) VALUES ('av1', 'bv1', 'cv1', 'dv1'), ('av2', 'bv2', 'cv2', 'dv2')                                        | UNWIND [{lhs: {a: 'av1'}, rel: {b: 'bv1', d: 'dv1'}, rhs: {c: 'cv1'}}, {lhs: {a: 'av2'}, rel: {b: 'bv2', d: 'dv2'}, rhs: {c: 'cv2'}}] AS properties MERGE (_lhs:Person {a: properties['lhs']['a']}) MERGE (_rhs:Movie {c: properties['rhs']['c']}) CREATE (_lhs)-[person_acted_in_movie:ACTED_IN]->(_rhs) SET person_acted_in_movie = properties['rel']
					true  | INSERT INTO Person_ACTED_IN_Movie (b, c, d) VALUES ('bv1', 'cv1', 'dv1'), ('bv2', 'cv2', 'dv2')                                                         | UNWIND [{lhs: {}, rel: {b: 'bv1', d: 'dv1'}, rhs: {c: 'cv1'}}, {lhs: {}, rel: {b: 'bv2', d: 'dv2'}, rhs: {c: 'cv2'}}] AS properties CREATE (_lhs:Person) MERGE (_rhs:Movie {c: properties['rhs']['c']}) CREATE (_lhs)-[person_acted_in_movie:ACTED_IN]->(_rhs) SET person_acted_in_movie = properties['rel']
					true  | INSERT INTO Person_ACTED_IN_Movie (a, b, d) VALUES ('av1', 'bv1', 'dv1'), ('av2', 'bv2', 'dv2')                                                         | UNWIND [{lhs: {a: 'av1'}, rel: {b: 'bv1', d: 'dv1'}, rhs: {}}, {lhs: {a: 'av2'}, rel: {b: 'bv2', d: 'dv2'}, rhs: {}}] AS properties MERGE (_lhs:Person {a: properties['lhs']['a']}) CREATE (_rhs:Movie) CREATE (_lhs)-[person_acted_in_movie:ACTED_IN]->(_rhs) SET person_acted_in_movie = properties['rel']
					false | INSERT INTO Person_ACTED_IN_Movie (a, b, c, ACTED_IN.d, ACTED_IN.e) VALUES('a', 'b', 'c', 'd', 'e')                                                     | CREATE (_lhs:Person) CREATE (_rhs:Movie) CREATE (_lhs)-[:ACTED_IN {d: 'd', e: 'e', a: 'a', b: 'b', c: 'c'}]->(_rhs)
					false | INSERT INTO Person_ACTED_IN_Movie (a, b, c, d, ACTED_IN.e) VALUES ('av1', 'bv1', 'cv1', 'dv1', 'ev1'), ('av2', 'bv2', 'cv2', 'dv2', 'ev2')              | UNWIND [{lhs: {}, rel: {e: 'ev1', a: 'av1', b: 'bv1', c: 'cv1', d: 'dv1'}, rhs: {}}, {lhs: {}, rel: {e: 'ev2', a: 'av2', b: 'bv2', c: 'cv2', d: 'dv2'}, rhs: {}}] AS properties CREATE (_lhs:Person)-[person_acted_in_movie:ACTED_IN]->(_rhs:Movie) SET _lhs = properties['lhs'] SET person_acted_in_movie = properties['rel'] SET _rhs = properties['rhs']
					false | INSERT INTO Person_ACTED_IN_Movie (Person.a, Movie.b, c, d, ACTED_IN.e) VALUES ('av1', 'bv1', 'cv1', 'dv1', 'ev1'), ('av2', 'bv2', 'cv2', 'dv2', 'ev2') | UNWIND [{lhs: {a: 'av1'}, rel: {e: 'ev1', c: 'cv1', d: 'dv1'}, rhs: {b: 'bv1'}}, {lhs: {a: 'av2'}, rel: {e: 'ev2', c: 'cv2', d: 'dv2'}, rhs: {b: 'bv2'}}] AS properties MERGE (_lhs:Person {a: properties['lhs']['a']}) MERGE (_rhs:Movie {b: properties['rhs']['b']}) CREATE (_lhs)-[person_acted_in_movie:ACTED_IN]->(_rhs) SET person_acted_in_movie = properties['rel']
					""")
	void insertIntoRelationshipTableShouldWork(boolean withMeta, String sql, String cypher) throws SQLException {
		var translator = SqlToCypher
			.with(SqlToCypherConfig.builder().withPrettyPrint(false).withAlwaysEscapeNames(false).build());

		DatabaseMetaData databaseMetadata = null;
		if (withMeta) {
			databaseMetadata = mockRelationshipMeta();

			mockColumnResults(databaseMetadata, "Person", "a");
			mockColumnResults(databaseMetadata, "Person_ACTED_IN_Movie", "b", "d");
			mockColumnResults(databaseMetadata, "Movie", "c");
		}

		assertThat(translator.translate(sql, databaseMetadata)).isEqualTo(cypher);

		if (withMeta) {
			verify(databaseMetadata).getTables(null, null, null, new String[] { "CBV" });
			verify(databaseMetadata).getTables(null, null, "Person_ACTED_IN_Movie",
					new String[] { "TABLE", "RELATIONSHIP" });
			verify(databaseMetadata, times(3)).getColumns(any(), any(), anyString(), any());
			verifyNoMoreInteractions(databaseMetadata);
		}
	}

	@ParameterizedTest
	@CsvSource(delimiterString = "|",
			textBlock = """
					true  | UPDATE Person_ACTED_IN_Movie SET b = 'x' WHERE a = 'y' AND c = 'z'                       | MATCH (_lhs:Person)-[person_acted_in_movie:ACTED_IN]->(_rhs:Movie) WHERE (_lhs.a = 'y' AND _rhs.c = 'z') SET person_acted_in_movie.b = 'x'
					true  | UPDATE Person_ACTED_IN_Movie SET b = 'x' WHERE v$id = 'y'                                | MATCH (_lhs:Person)-[person_acted_in_movie:ACTED_IN]->(_rhs:Movie) WHERE elementId(person_acted_in_movie) = 'y' SET person_acted_in_movie.b = 'x'
					true  | UPDATE Person_ACTED_IN_Movie SET b = 'x'                                                 | MATCH (_lhs:Person)-[person_acted_in_movie:ACTED_IN]->(_rhs:Movie) SET person_acted_in_movie.b = 'x'
					true  | UPDATE Person_ACTED_IN_Movie SET a = 'x'                                                 | MATCH (_lhs:Person)-[person_acted_in_movie:ACTED_IN]->(_rhs:Movie) SET _lhs.a = 'x'
					true  | UPDATE Person_ACTED_IN_Movie SET c = 'x'                                                 | MATCH (_lhs:Person)-[person_acted_in_movie:ACTED_IN]->(_rhs:Movie) SET _rhs.c = 'x'
					true  | UPDATE Person_ACTED_IN_Movie SET a = 'x' WHERE v$movie_id = 'wurstsalat'                 | MATCH (_lhs:Person)-[person_acted_in_movie:ACTED_IN]->(_rhs:Movie) WHERE elementId(_rhs) = 'wurstsalat' SET _lhs.a = 'x'
					false | UPDATE Person_ACTED_IN_Movie SET b = 'x' WHERE a = 'y' AND c = 'z'                       | MATCH (_lhs:Person)-[person_acted_in_movie:ACTED_IN]->(_rhs:Movie) WHERE (person_acted_in_movie.a = 'y' AND person_acted_in_movie.c = 'z') SET person_acted_in_movie.b = 'x'
					false | UPDATE Person_ACTED_IN_Movie SET b = 'x' WHERE v$id = 'y'                                | MATCH (_lhs:Person)-[person_acted_in_movie:ACTED_IN]->(_rhs:Movie) WHERE elementId(person_acted_in_movie) = 'y' SET person_acted_in_movie.b = 'x'
					false | UPDATE Person_ACTED_IN_Movie SET b = 'x'                                                 | MATCH (_lhs:Person)-[person_acted_in_movie:ACTED_IN]->(_rhs:Movie) SET person_acted_in_movie.b = 'x'
					false | UPDATE Person_ACTED_IN_Movie SET a = 'x'                                                 | MATCH (_lhs:Person)-[person_acted_in_movie:ACTED_IN]->(_rhs:Movie) SET person_acted_in_movie.a = 'x'
					false | UPDATE Person_ACTED_IN_Movie SET c = 'x'                                                 | MATCH (_lhs:Person)-[person_acted_in_movie:ACTED_IN]->(_rhs:Movie) SET person_acted_in_movie.c = 'x'
					false | UPDATE Person_ACTED_IN_Movie SET a = 'x' WHERE v$movie_id = 'wurstsalat'                 | MATCH (_lhs:Person)-[person_acted_in_movie:ACTED_IN]->(_rhs:Movie) WHERE elementId(_rhs) = 'wurstsalat' SET person_acted_in_movie.a = 'x'
					false | UPDATE Person_ACTED_IN_Movie SET ACTED_IN.b = 'x' WHERE Person.a = 'y' AND Movie.c = 'z' | MATCH (_lhs:Person)-[person_acted_in_movie:ACTED_IN]->(_rhs:Movie) WHERE (_lhs.a = 'y' AND _rhs.c = 'z') SET person_acted_in_movie.b = 'x'
					false | UPDATE Person_ACTED_IN_Movie SET ACTED_IN.b = 'x' WHERE v$id = 'y'                       | MATCH (_lhs:Person)-[person_acted_in_movie:ACTED_IN]->(_rhs:Movie) WHERE elementId(person_acted_in_movie) = 'y' SET person_acted_in_movie.b = 'x'
					false | UPDATE Person_ACTED_IN_Movie SET ACTED_IN.b = 'x'                                        | MATCH (_lhs:Person)-[person_acted_in_movie:ACTED_IN]->(_rhs:Movie) SET person_acted_in_movie.b = 'x'
					false | UPDATE Person_ACTED_IN_Movie SET Person.a = 'x'                                          | MATCH (_lhs:Person)-[person_acted_in_movie:ACTED_IN]->(_rhs:Movie) SET _lhs.a = 'x'
					false | UPDATE Person_ACTED_IN_Movie SET Movie.c = 'x'                                           | MATCH (_lhs:Person)-[person_acted_in_movie:ACTED_IN]->(_rhs:Movie) SET _rhs.c = 'x'
					false | UPDATE Person_ACTED_IN_Movie SET Person.a = 'x' WHERE v$movie_id = 'wurstsalat'          | MATCH (_lhs:Person)-[person_acted_in_movie:ACTED_IN]->(_rhs:Movie) WHERE elementId(_rhs) = 'wurstsalat' SET _lhs.a = 'x'
					""")
	void updateRelationshipTableShouldWork(boolean withMeta, String sql, String cypher) throws SQLException {
		var translator = SqlToCypher
			.with(SqlToCypherConfig.builder().withPrettyPrint(false).withAlwaysEscapeNames(false).build());

		DatabaseMetaData databaseMetadata = null;
		if (withMeta) {
			databaseMetadata = mockRelationshipMeta();

			mockColumnResults(databaseMetadata, "Person", "a");
			mockColumnResults(databaseMetadata, "Person_ACTED_IN_Movie", "b", "d");
			mockColumnResults(databaseMetadata, "Movie", "c");
		}

		assertThat(translator.translate(sql, databaseMetadata)).isEqualTo(cypher);
	}

	@ParameterizedTest
	@CsvSource(delimiterString = "|",
			textBlock = """
					true  | DELETE FROM Person_ACTED_IN_Movie WHERE v$id = 'asd'                     | MATCH (_lhs:Person)-[person_acted_in_movie:ACTED_IN]->(_rhs:Movie) WHERE elementId(person_acted_in_movie) = 'asd' DELETE person_acted_in_movie
					true  | DELETE FROM Person_ACTED_IN_Movie WHERE v$person_id = 'asd'              | MATCH (_lhs:Person)-[person_acted_in_movie:ACTED_IN]->(_rhs:Movie) WHERE elementId(_lhs) = 'asd' DELETE person_acted_in_movie
					true  | DELETE FROM Person_ACTED_IN_Movie WHERE v$movie_id = 'asd'               | MATCH (_lhs:Person)-[person_acted_in_movie:ACTED_IN]->(_rhs:Movie) WHERE elementId(_rhs) = 'asd' DELETE person_acted_in_movie
					true  | DELETE FROM Person_ACTED_IN_Movie                                        | MATCH (_lhs:Person)-[person_acted_in_movie:ACTED_IN]->(_rhs:Movie) DELETE person_acted_in_movie
					true  | DELETE FROM Person_ACTED_IN_Movie WHERE a = 'x'                          | MATCH (_lhs:Person)-[person_acted_in_movie:ACTED_IN]->(_rhs:Movie) WHERE _lhs.a = 'x' DELETE person_acted_in_movie
					true  | DELETE FROM Person_ACTED_IN_Movie WHERE Person.a = 'x'                   | MATCH (_lhs:Person)-[person_acted_in_movie:ACTED_IN]->(_rhs:Movie) WHERE _lhs.a = 'x' DELETE person_acted_in_movie
					true  | DELETE FROM Person_ACTED_IN_Movie WHERE b = 'x'                          | MATCH (_lhs:Person)-[person_acted_in_movie:ACTED_IN]->(_rhs:Movie) WHERE person_acted_in_movie.b = 'x' DELETE person_acted_in_movie
					true  | DELETE FROM Person_ACTED_IN_Movie WHERE ACTED_IN.b = 'x'                 | MATCH (_lhs:Person)-[person_acted_in_movie:ACTED_IN]->(_rhs:Movie) WHERE person_acted_in_movie.b = 'x' DELETE person_acted_in_movie
					true  | DELETE FROM Person_ACTED_IN_Movie WHERE c = 'x'                          | MATCH (_lhs:Person)-[person_acted_in_movie:ACTED_IN]->(_rhs:Movie) WHERE _rhs.c = 'x' DELETE person_acted_in_movie
					true  | DELETE FROM Person_ACTED_IN_Movie WHERE Movie.c = 'x'                    | MATCH (_lhs:Person)-[person_acted_in_movie:ACTED_IN]->(_rhs:Movie) WHERE _rhs.c = 'x' DELETE person_acted_in_movie
					true  | TRUNCATE TABLE Person_ACTED_IN_Movie                                     | MATCH (_lhs:Person)-[person_acted_in_movie:ACTED_IN]->(_rhs:Movie) DELETE person_acted_in_movie
					true  | DELETE FROM Person_ACTED_IN_Movie WHERE a = 'x' AND b = 'x' AND c = 'x'  | MATCH (_lhs:Person)-[person_acted_in_movie:ACTED_IN]->(_rhs:Movie) WHERE (_lhs.a = 'x' AND person_acted_in_movie.b = 'x' AND _rhs.c = 'x') DELETE person_acted_in_movie
					false | DELETE FROM Person_ACTED_IN_Movie                                        | MATCH (_lhs:Person)-[person_acted_in_movie:ACTED_IN]->(_rhs:Movie) DELETE person_acted_in_movie
					false | DELETE FROM Person_ACTED_IN_Movie WHERE a = 'x'                          | MATCH (_lhs:Person)-[person_acted_in_movie:ACTED_IN]->(_rhs:Movie) WHERE person_acted_in_movie.a = 'x' DELETE person_acted_in_movie
					false | DELETE FROM Person_ACTED_IN_Movie WHERE Person.a = 'x'                   | MATCH (_lhs:Person)-[person_acted_in_movie:ACTED_IN]->(_rhs:Movie) WHERE _lhs.a = 'x' DELETE person_acted_in_movie
					false | DELETE FROM Person_ACTED_IN_Movie WHERE b = 'x'                          | MATCH (_lhs:Person)-[person_acted_in_movie:ACTED_IN]->(_rhs:Movie) WHERE person_acted_in_movie.b = 'x' DELETE person_acted_in_movie
					false | DELETE FROM Person_ACTED_IN_Movie WHERE ACTED_IN.b = 'x'                 | MATCH (_lhs:Person)-[person_acted_in_movie:ACTED_IN]->(_rhs:Movie) WHERE person_acted_in_movie.b = 'x' DELETE person_acted_in_movie
					false | DELETE FROM Person_ACTED_IN_Movie WHERE c = 'x'                          | MATCH (_lhs:Person)-[person_acted_in_movie:ACTED_IN]->(_rhs:Movie) WHERE person_acted_in_movie.c = 'x' DELETE person_acted_in_movie
					false | DELETE FROM Person_ACTED_IN_Movie WHERE Movie.c = 'x'                    | MATCH (_lhs:Person)-[person_acted_in_movie:ACTED_IN]->(_rhs:Movie) WHERE _rhs.c = 'x' DELETE person_acted_in_movie
					false | TRUNCATE TABLE Person_ACTED_IN_Movie                                     | MATCH (_lhs:Person)-[person_acted_in_movie:ACTED_IN]->(_rhs:Movie) DELETE person_acted_in_movie
					""")
	void deleteFromRelationshipTableShouldWork(boolean withMeta, String sql, String cypher) throws SQLException {
		var translator = SqlToCypher
			.with(SqlToCypherConfig.builder().withPrettyPrint(false).withAlwaysEscapeNames(false).build());

		DatabaseMetaData databaseMetadata = null;
		if (withMeta) {
			databaseMetadata = mockRelationshipMeta();

			mockColumnResults(databaseMetadata, "Person", "a");
			mockColumnResults(databaseMetadata, "Person_ACTED_IN_Movie", "b", "d");
			mockColumnResults(databaseMetadata, "Movie", "c");
		}

		assertThat(translator.translate(sql, databaseMetadata)).isEqualTo(cypher);
	}

	private void mockColumnResults(DatabaseMetaData databaseMetadata, String targetTable, String columnName,
			String... more) throws SQLException {

		var lhsColumns = mock(ResultSet.class);
		Boolean[] next = new Boolean[more.length + 1];
		for (int i = 0; i < more.length; ++i) {
			next[i] = true;
		}
		next[more.length] = false;
		given(lhsColumns.getString("COLUMN_NAME")).willReturn(columnName, more);
		given(lhsColumns.next()).willReturn(true, next);
		given(databaseMetadata.getColumns(null, null, targetTable, null)).willReturn(lhsColumns);
	}

	@Test
	void simpleUpdateShouldWork() {

		var translator = SqlToCypher.defaultTranslator();
		assertThat(translator.translate("UPDATE Actor a SET name = 'Foo' WHERE id(a) = 4711"))
			.isEqualTo("MATCH (a:Actor) WHERE id(a) = 4711 SET a.name = 'Foo'");
	}

	@Test
	void emptyStatementShouldNotFail() {

		var cypher = SqlToCypher.defaultTranslator().translate("   // test");
		assertThat(cypher).isEqualTo("FINISH");
	}

	@ParameterizedTest
	@CsvSource(delimiterString = "|", textBlock = """
			SELECT name, count(*) FROM People p GROUP BY name|MATCH (p:People) RETURN p.name AS name, count(*)
			SELECT name, max(age) FROM People p GROUP BY name|MATCH (p:People) RETURN p.name AS name, max(p.age)
			SELECT name, min(age) FROM People p GROUP BY name|MATCH (p:People) RETURN p.name AS name, min(p.age)
			SELECT sum(age) FROM People p GROUP BY name|MATCH (p:People) RETURN sum(p.age)
			SELECT avg(age) FROM People p GROUP BY name|MATCH (p:People) RETURN avg(p.age)
			SELECT percentileCont(age) FROM People p GROUP BY name|MATCH (p:People) RETURN percentileCont(p.age)
			SELECT percentileDisc(age) FROM People p GROUP BY name|MATCH (p:People) RETURN percentileDisc(p.age)
			SELECT stDev(age) FROM People p GROUP BY name|MATCH (p:People) RETURN stDev(p.age)
			SELECT stDevP(age) FROM People p GROUP BY name|MATCH (p:People) RETURN stDevP(p.age)
			""")
	void aggregates(String sql, String cypher) {

		var translator = SqlToCypher.defaultTranslator();
		assertThat(translator.translate(sql)).isEqualTo(cypher);

	}

	@Test
	void outerSelectStarShouldBeRemoved() {

		var translator = SqlToCypher.defaultTranslator();
		assertThat(translator
			.translate("SELECT * FROM (SELECT * FROM \"Movie\") AS \"tempTable_5301953691072342668\" WHERE 1 = 0"))
			.isEqualTo("MATCH (movie:Movie) RETURN * LIMIT 1");
	}

	@Test
	void upsert() {

		assertThat(
				NON_PRETTY_PRINTING_TRANSLATOR.translate("INSERT INTO Movie(title) VALUES(?) ON DUPLICATE KEY IGNORE"))
			.isEqualTo("MERGE (movie:Movie {title: $1})");
	}

	@Test // GH-675
	void simpleDistinct() {

		assertThat(NON_PRETTY_PRINTING_TRANSLATOR
			.translate("select distinct \"NAME\" from \"Pgm\" where \"snapshotId\" = ?"))
			.isEqualTo("MATCH (pgm:Pgm) WHERE pgm.snapshotId = $1 RETURN DISTINCT pgm.NAME AS NAME");
	}

	@ParameterizedTest
	@CsvSource(delimiterString = "|", textBlock = """
			SELECT id(n) FROM Movies n|MATCH (n:`Movies`) RETURN id(n)
			SELECT elementId(n) FROM Movies n|MATCH (n:`Movies`) RETURN elementId(n)
			SELECT foobar('const', bazbar(:a))|RETURN foobar('const', bazbar($a))
			""")
	void parserShallNotFailOnUnknownFunctions(String in, String expected) {

		var translator = SqlToCypher
			.with(SqlToCypherConfig.builder().withPrettyPrint(false).withAlwaysEscapeNames(true).build());
		assertThat(translator.translate(in)).isEqualTo(expected);
	}

	static List<TestData> getTestData(Path path) {
		try (var asciidoctor = Asciidoctor.Factory.create()) {
			var collector = new TestDataExtractor();
			asciidoctor.javaExtensionRegistry().treeprocessor(collector);

			var content = Files.readString(path);
			asciidoctor.load(content, Options.builder().build());
			return collector.testData;
		}
		catch (IOException ioe) {
			throw new RuntimeException("Error reading TCK file " + path, ioe);
		}
	}

	@TestFactory
	Stream<DynamicContainer> tck() {

		var path = ClassLoader.getSystemResource("simple.adoc").getPath();
		var parentFolder = new File(path).getParentFile();
		if (!parentFolder.isDirectory()) {
			return Stream.empty();
		}

		var files = parentFolder.listFiles(f -> f.getName().toLowerCase().endsWith(".adoc"));
		if (files == null) {
			return Stream.empty();
		}

		return Arrays.stream(files).map(file -> {
			var tests = getTestData(file.toPath()).stream()
				.map(t -> DynamicTest.dynamicTest(
						Optional.ofNullable(t.name).filter(Predicate.not(String::isBlank)).orElse(t.id),
						() -> assertThatSqlIsTranslatedAsExpected(t.sql, t.cypher, t.tableMappings,
								t.joinColumnsMappings, t.prettyPrint, t.databaseMetaData)))
				.toList();
			return DynamicContainer.dynamicContainer(file.getName(), tests);
		});
	}

	void assertThatSqlIsTranslatedAsExpected(String sql, String expected, Map<String, String> tableMappings,
			Map<String, String> join_columns_mappings, boolean prettyPrint, DatabaseMetaData databaseMetaData) {
		assertThat(SqlToCypher
			.with(SqlToCypherConfig.builder()
				.withPrettyPrint(prettyPrint)
				.withAlwaysEscapeNames(!prettyPrint)
				.withTableToLabelMappings(tableMappings)
				.withJoinColumnsToTypeMappings(join_columns_mappings)
				.build())
			.translate(sql, databaseMetaData)).isEqualTo(expected);
	}

	@ParameterizedTest
	@CsvSource(textBlock = """
			,,MATCH (movies:movies)$RETURN *
			true,,MATCH (movies:movies)$RETURN *
			false,,MATCH (movies:movies) RETURN *
			,true,MATCH (movies:`movies`)$RETURN *
			,false,MATCH (movies:movies)$RETURN *
			true,false,MATCH (movies:movies)$RETURN *
			true,true,MATCH (movies:`movies`)$RETURN *
			false,false,MATCH (movies:movies)$RETURN *
			false,true,MATCH (movies:`movies`)$RETURN *
			""")
	void escapingShouldWork(Boolean prettyPrint, Boolean alwaysEscapeNames, String expected) {
		var properties = new HashMap<String, Object>();
		if (prettyPrint != null) {
			properties.put("s2c.prettyPrint", prettyPrint.toString());
		}
		if (alwaysEscapeNames != null) {
			properties.put("s2c.alwaysEscapeNames", alwaysEscapeNames.toString());
		}
		var cfg = SqlToCypherConfig.of(properties);
		var defaultCfg = SqlToCypherConfig.defaultConfig();
		assertThat(cfg.isPrettyPrint()).isEqualTo((prettyPrint != null) ? prettyPrint : defaultCfg.isPrettyPrint());
		assertThat(cfg.isAlwaysEscapeNames())
			.isEqualTo((alwaysEscapeNames != null) ? alwaysEscapeNames : defaultCfg.isAlwaysEscapeNames());
		var sql = "Select * from movies";
		var cypher = SqlToCypher.with(cfg).translate(sql);
		assertThat(cypher).isEqualTo(expected.replace("$", cfg.isPrettyPrint() ? System.lineSeparator() : " "));
	}

	@Test
	void extractionShouldFailOnSomeProperties() {
		var translator = SqlToCypher.defaultTranslator();
		assertThatIllegalStateException().isThrownBy(() -> translator.translate("""
				SELECT COUNT(*)
							FROM Orders o
							WHERE CENTURY(OrderDate) = 1997
				""")).withMessage("Unsupported value for date/time extraction: CENTURY");
	}

	@TestFactory
	Stream<DynamicContainer> directCompare() {

		var like = List.of(
				SqlAndCypher.of("SELECT * FROM blub b WHERE name like '%Test%'",
						"MATCH (b:blub) WHERE b.name CONTAINS 'Test' RETURN *"),
				SqlAndCypher.of("SELECT * FROM blub b WHERE name like '%Test'",
						"MATCH (b:blub) WHERE b.name ENDS WITH 'Test' RETURN *"),
				SqlAndCypher.of("SELECT * FROM blub b WHERE name like 'Test%'",
						"MATCH (b:blub) WHERE b.name STARTS WITH 'Test' RETURN *"),
				SqlAndCypher.of("SELECT * FROM blub b WHERE name like 'This is _ %Test%'",
						"MATCH (b:blub) WHERE b.name =~ 'This is . .*Test.*' RETURN *"),
				SqlAndCypher.of("SELECT * FROM blub b WHERE name like '%'",
						"MATCH (b:blub) WHERE b.name =~ '.*' RETURN *"),
				SqlAndCypher.of("SELECT * FROM blub b WHERE name like '%%'",
						"MATCH (b:blub) WHERE b.name =~ '.*' RETURN *"),
				SqlAndCypher.of("SELECT * FROM blub b WHERE name like '%%%'",
						"MATCH (b:blub) WHERE b.name =~ '.*' RETURN *"),
				SqlAndCypher.of("SELECT * FROM blub b WHERE name like '_'",
						"MATCH (b:blub) WHERE b.name =~ '.' RETURN *"),
				SqlAndCypher.of("SELECT * FROM blub b WHERE name like '__'",
						"MATCH (b:blub) WHERE b.name =~ '..' RETURN *"),
				SqlAndCypher.of("SELECT * FROM blub b WHERE name like '___'",
						"MATCH (b:blub) WHERE b.name =~ '...' RETURN *"),
				SqlAndCypher.of("SELECT * FROM blub b WHERE name like '%_%'",
						"MATCH (b:blub) WHERE b.name =~ '.*..*' RETURN *"),
				SqlAndCypher.of("SELECT * FROM blub b WHERE name like '%ein%schöner%Name%'",
						"MATCH (b:blub) WHERE b.name =~ '.*ein.*schöner.*Name.*' RETURN *"));
		var blog = List.of(
				SqlAndCypher.of("Get all columns from the Customers table.", "SELECT * FROM Customers",
						"MATCH (customers:Customer) RETURN *"),
				SqlAndCypher.of("Get all columns from the Customers table.", "SELECT * FROM Customers c",
						"MATCH (c:Customer) RETURN *"),
				SqlAndCypher.of("Get all columns from the Customers table.", "SELECT c FROM Customers c",
						"MATCH (c:Customer) RETURN c"),
				SqlAndCypher.of("Get the count of all Orders made during 1997.", """
						SELECT COUNT(*)
						FROM Orders o
						WHERE YEAR(OrderDate) = 1997
						""", "MATCH (o:Order) WHERE o.OrderDate.year = 1997 RETURN count(*)"),
				SqlAndCypher.of("Get the count of all Orders made during 1997.", """
						SELECT COUNT(o)
						FROM Orders o
						WHERE YEAR(OrderDate) = 1997
						""", "MATCH (o:Order) WHERE o.OrderDate.year = 1997 RETURN count(o)"),
				SqlAndCypher.of(
						"Get all orders placed on the 19th of May, 1997. (Not a chance without schema figuring out that this is a date)",
						"""
								SELECT *
								FROM Orders
								WHERE OrderDate = '19970319'
								""", "MATCH (orders:Order) WHERE orders.OrderDate = '19970319' RETURN *"),
				SqlAndCypher.of("Create a report for all the orders of 1996 and their Customers.", """
						SELECT *
						FROM Orders o
						INNER JOIN Customers c ON o.CustomerID = c.CustomerID
						WHERE YEAR(o.OrderDate) = 1996
						""",
						"MATCH (o:Order)<-[purchased:PURCHASED]-(c:Customer) WHERE o.OrderDate.year = 1996 RETURN *"),
				SqlAndCypher.of(
						"Create a report for all 1996 orders and their Customers. Return only the Order ID, Order Date, Customer ID, Name, and Country.",
						"""
								SELECT o.OrderID, o.OrderDate, c.CustomerID, c.ContactName, c.Country
								FROM Orders o
								INNER JOIN Customers c ON o.CustomerID = c.CustomerID
								WHERE YEAR(o.OrderDate) = 1996
								""",
						"MATCH (o:Order)<-[purchased:PURCHASED]-(c:Customer) WHERE o.OrderDate.year = 1996 RETURN o.OrderID, o.OrderDate, c.CustomerID, c.ContactName, c.Country"),
				SqlAndCypher.of("Create a report that shows the number of customers from each city.", """
						SELECT c.City, COUNT(*)
						FROM Orders o
						INNER JOIN Customers c ON o.CustomerID = c.CustomerID
						GROUP BY c.City
						""", "MATCH (o:Order)<-[purchased:PURCHASED]-(c:Customer) RETURN c.City, count(*)"),
				SqlAndCypher.of("Insert yourself into the Customers table",
						"""
								INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
								VALUES ('ILYA1', 'Acme Corp', 'Ilya Verbitskiy', 'Manager', '123 Main St', 'New York', 'NY', '10001', 'USA', '555-1234', '555-5678')
								""",
						"CREATE (customers:Customer {CustomerID: 'ILYA1', CompanyName: 'Acme Corp', ContactName: 'Ilya Verbitskiy', ContactTitle: 'Manager', Address: '123 Main St', City: 'New York', Region: 'NY', PostalCode: '10001', Country: 'USA', Phone: '555-1234', Fax: '555-5678'})"),
				SqlAndCypher.of("Update the phone number.",
						"UPDATE Customers SET Phone = '000-4321' WHERE CustomerID = 'ILYA'",
						"MATCH (customers:Customer) WHERE customers.CustomerID = 'ILYA' SET customers.Phone = '000-4321'"),
				SqlAndCypher.of("Get the top 25 Customers alphabetically by Country and name.", """
						SELECT TOP 25 *
						FROM Customers
						ORDER BY Country, ContactName
						""",
						"MATCH (customers:Customer) RETURN * ORDER BY customers.Country, customers.ContactName LIMIT 25"),
				SqlAndCypher.of("Get the top 25 Countries alphabetically by Country and name.", """
						SELECT TOP 25 Country
						FROM Customers
						ORDER BY Country, ContactName
						""",
						"MATCH (customers:Customer) RETURN customers.Country AS Country ORDER BY customers.Country, customers.ContactName LIMIT 25"),
				SqlAndCypher.of("Get the top 25 Customers alphabetically by Country and name.", """
						SELECT TOP 25 *
						FROM Customers c
						ORDER BY Country, ContactName
						""", "MATCH (c:Customer) RETURN * ORDER BY c.Country, c.ContactName LIMIT 25"),
				SqlAndCypher.of("Get the top 25 Customers alphabetically by Country and name.", """
						SELECT TOP 25 c
						FROM Customers c
						ORDER BY Country, ContactName
						""", "MATCH (c:Customer) RETURN c ORDER BY c.Country, c.ContactName LIMIT 25"));

		return Stream.of(
				DynamicContainer.dynamicContainer("https://verbitskiy.co/blog/neo4j-cypher-cheat-sheet/",
						DynamicTest.stream(blog.stream(), SqlAndCypher::displayName, this::compare)),
				DynamicContainer.dynamicContainer("likeShouldBeHandledNicely",
						DynamicTest.stream(like.stream(), SqlAndCypher::displayName, this::compare)));
	}

	void compare(SqlAndCypher sqlAndCypher) {
		var translator = SqlToCypher.with(SqlToCypherConfig.builder()
			.withTableToLabelMappings(Map.of("Customers", "Customer", "Orders", "Order"))
			.withJoinColumnsToTypeMappings(Map.of("Orders.CustomerID", "PURCHASED"))
			.build());
		assertThat(translator.translate(sqlAndCypher.sql())).isEqualTo(sqlAndCypher.cypher());
	}

	private record SqlAndCypher(String name, String sql, String cypher) {
		static SqlAndCypher of(String name, String sql, String cypher) {
			return new SqlAndCypher(name, sql, cypher);

		}

		static SqlAndCypher of(String sql, String cypher) {
			return new SqlAndCypher(null, sql, cypher);
		}

		String displayName() {
			return Optional.ofNullable(this.name).orElse(this.sql);
		}
	}

	private static class TestDataExtractor extends Treeprocessor {

		private final List<TestData> testData = new ArrayList<>();

		TestDataExtractor() {
			super(new HashMap<>()); // Must be mutable
		}

		@Override
		public Document process(Document document) {

			var blocks = document.findBy(Map.of("context", ":listing", "style", "source"))
				.stream()
				.map(Block.class::cast)
				.filter(b -> b.hasAttribute("id"))
				.collect(Collectors.toMap(ContentNode::getId, Function.identity()));

			blocks.values().stream().filter(b -> "sql".equals(b.getAttribute("language"))).map(sqlBlock -> {
				var name = (String) sqlBlock.getAttribute("name");
				var sql = String.join("\n", sqlBlock.getLines());
				var cypherBlock = blocks.get(sqlBlock.getId() + "_expected");
				var cypher = String.join("\n", cypherBlock.getLines());
				DatabaseMetaData databaseMetaData = null;
				Map<String, String> tableMappings = new HashMap<>();
				Map<String, String> join_columns_mappings = new HashMap<>();
				if (sqlBlock.getAttribute("table_mappings") != null) {
					tableMappings = SqlToCypherConfig.buildMap((String) sqlBlock.getAttribute("table_mappings"));
				}
				if (sqlBlock.getAttribute("join_column_mappings") != null) {
					join_columns_mappings = SqlToCypherConfig
						.buildMap((String) sqlBlock.getAttribute("join_column_mappings"));
				}
				if (sqlBlock.getAttribute("metaData") != null) {
					String metaData = (String) sqlBlock.getAttribute("metaData");

					databaseMetaData = mock(DatabaseMetaData.class);
					try {
						given(databaseMetaData.getTables(any(), any(), any(), any())).willReturn(mock(ResultSet.class));
					}
					catch (SQLException ex) {
						throw new RuntimeException(ex);
					}
					for (String m : metaData.split(";")) {
						var endIndex = m.indexOf(":");
						String label = m.substring(0, endIndex);
						String[] properties = m.substring(endIndex + 1).split("\\|");
						try {
							var columns = (properties.length > 1)
									? makeColumns(properties[0], Arrays.copyOfRange(properties, 1, properties.length))
									: makeColumns(properties[0]);
							given(databaseMetaData.getColumns(null, null, label, null)).willReturn(columns);
						}
						catch (SQLException ex) {
							throw new RuntimeException(ex);
						}
					}
				}
				boolean parseCypher = Boolean.parseBoolean(((String) cypherBlock.getAttribute("parseCypher", "true")));
				boolean prettyPrint = true;
				if (parseCypher) {
					var renderer = Renderer.getRenderer(Configuration.newConfig().withDialect(Dialect.NEO4J_5).build());
					cypher = renderer.render(CypherParser.parse(cypher));
					prettyPrint = false;
				}
				return new TestData(sqlBlock.getId(), name, sql, cypher, tableMappings, join_columns_mappings,
						prettyPrint, databaseMetaData);
			}).forEach(this.testData::add);
			return document;
		}

	}

	private record TestData(String id, String name, String sql, String cypher, Map<String, String> tableMappings,
			Map<String, String> joinColumnsMappings, boolean prettyPrint, DatabaseMetaData databaseMetaData) {
	}

}
