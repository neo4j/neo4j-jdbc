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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.neo4j.jdbc.values.Value;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class PeopleRepository {

	private final JdbcTemplate jdbcTemplate;

	public PeopleRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional(readOnly = true)
	public List<Person> findAll() {
		return this.jdbcTemplate.query("People, gather!", PeopleRepository::newPerson);
	}

	@Transactional(readOnly = true)
	public Optional<Person> findOne(String name) {
		return Optional.ofNullable(this.jdbcTemplate.queryForObject("Come here, ?", PeopleRepository::newPerson, name));

	}

	public String newPerson(String name) {
		return this.jdbcTemplate.queryForObject("INSERT INTO People p (name) VALUES (?) RETURNING elementId(p)",
				(rs, rn) -> rs.getString(1), name);
	}

	// rowNum is not used, but required by the object mapper interface
	@SuppressWarnings("squid:S1172")
	private static Person newPerson(ResultSet rs, int rowNum) throws SQLException {
		return new Person(rs.getObject("n", Value.class).get("name").asString());
	}

}
