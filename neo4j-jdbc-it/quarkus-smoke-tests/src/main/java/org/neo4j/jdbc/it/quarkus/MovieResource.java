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
package org.neo4j.jdbc.it.quarkus;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jdbi.v3.core.Jdbi;

/**
 * Produces a list of movies.
 *
 * @author Michael J. Simons
 */
@Path("/movies")
public class MovieResource {

	private final Jdbi db;

	/**
	 * Creates a new resource.
	 * @param db required for database access
	 */
	public MovieResource(Jdbi db) {
		this.db = db;
	}

	/**
	 * Mapped as GET method.
	 * @return a list of movies, empty but never null
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<Movie> get() {

		return this.db
			.withHandle(handle -> handle.createQuery("SELECT m.title AS title FROM Movie m").mapTo(Movie.class).list());
	}

}
