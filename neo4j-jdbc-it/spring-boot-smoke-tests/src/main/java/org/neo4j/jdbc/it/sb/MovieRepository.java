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
package org.neo4j.jdbc.it.sb;

import java.util.List;
import java.util.Map;

import org.neo4j.jdbc.values.Node;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class MovieRepository {

	private final JdbcTemplate jdbcTemplate;

	public MovieRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional(readOnly = true)
	public List<Movie> findAll(boolean fail) {
		// This is using a small fetch size specifically to test some annotations on spans
		return this.jdbcTemplate.query(con -> {
			var stmt = con.prepareStatement(fail ? "asd" : "SELECT title FROM Movie m");
			stmt.setFetchSize(4);
			return stmt;
		}, (rs, rowNum) -> new Movie(rs.getString("title")));
	}

	public int createOrUpdate(Movie movie) {
		return this.jdbcTemplate.update("INSERT INTO Movie(title) VALUES(?) ON DUPLICATE KEY IGNORE", movie.title());
	}

	public Map<String, Object> findAsMap(String title) {
		// Magic comment just added because I configured the driver here to always assume
		// SQL by default
		// SQL parameters start at 1, though
		return this.jdbcTemplate
			.queryForObject("/*+ NEO4J FORCE_CYPHER */ MATCH (p:Movie) WHERE p.title = $1 RETURN p", Node.class, title)
			.asMap();
	}

	public void deleteAll() {
		this.jdbcTemplate.execute("DELETE FROM Movie");
	}

}
