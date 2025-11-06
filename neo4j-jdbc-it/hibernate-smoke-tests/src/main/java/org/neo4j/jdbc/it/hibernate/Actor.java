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
package org.neo4j.jdbc.it.hibernate;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "Person_ACTED_IN_Movie")
public class Actor {

	@EmbeddedId
	private ActorId id;

	@ManyToOne
	@MapsId("movieId")
	private Movie movie;

	@ManyToOne
	@MapsId("personId")
	private Person person;

	private List<String> roles = new ArrayList<>();

	public Actor() {
	}

	public ActorId getId() {
		return this.id;
	}

	public void setId(ActorId id) {
		this.id = id;
	}

	public Movie getMovie() {
		return this.movie;
	}

	public void setMovie(Movie movie) {
		this.movie = movie;
	}

	public Person getPerson() {
		return this.person;
	}

	public void setPerson(Person person) {
		this.person = person;
	}

	public List<String> getRoles() {
		return this.roles;
	}

	@Embeddable
	public record ActorId(String movieId, String personId) {

		public ActorId() {
			this(null, null);
		}
	}

}
