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

import org.assertj.core.api.Assertions;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.neo4j.jdbc.Neo4jDriver;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.neo4j.Neo4jContainer;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HibernateIT {

	@SuppressWarnings("resource") // On purpose to reuse this
	protected final Neo4jContainer neo4j = new Neo4jContainer(System.getProperty("neo4j-jdbc.default-neo4j-image"))
		.withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
		.waitingFor(Neo4jContainer.WAIT_FOR_BOLT)
		.withReuse(true);

	protected SessionFactory sessionFactory;

	@BeforeAll
	void startNeo4j() {
		this.neo4j.start();
		this.sessionFactory = new Configuration().addAnnotatedClass(Movie.class)
			.addAnnotatedClass(Person.class)
			.setProperty(AvailableSettings.JAKARTA_JDBC_DRIVER, Neo4jDriver.class)
			.setProperty(AvailableSettings.JAKARTA_JDBC_URL,
					"jdbc:neo4j://" + this.neo4j.getHost() + ":" + this.neo4j.getMappedPort(7687)
							+ "?enableSQLTranslation=true")
			.setProperty(AvailableSettings.JAKARTA_JDBC_USER, "neo4j")
			.setProperty(AvailableSettings.JAKARTA_JDBC_PASSWORD, this.neo4j.getAdminPassword())
			.setProperty(AvailableSettings.SHOW_SQL, true)
			.setProperty(AvailableSettings.FORMAT_SQL, true)
			.setProperty(AvailableSettings.HIGHLIGHT_SQL, true)
			.setProperty(AvailableSettings.DIALECT, Neo4jDialect.class)
			.setProperty(AvailableSettings.IMPLICIT_NAMING_STRATEGY, Neo4jNamingStrategy.class)
			.buildSessionFactory();
	}

	@BeforeEach
	void clearDatabase() {
		try (var session = this.sessionFactory.openSession();) {
			session.doWork(connection -> {
				connection.setAutoCommit(true);
				try (var stmt = connection.createStatement()) {
					stmt.execute("""
							/*+ NEO4J FORCE_CYPHER */
							MATCH (n)
							CALL {
								WITH n DETACH DELETE n
							}
							IN TRANSACTIONS OF 1000 ROWs""");

					stmt.execute("""
							/*+ NEO4J FORCE_CYPHER */
							CREATE (p:Person {id: randomUUID(), name: 'Helge Schneider', born: 1995})
							       -[:ACTED_IN {roles: ['Himself']}]
							       ->(:Movie {id: randomUUID(), title: 'The Klimperclown'})
							""");
				}
			});
		}
	}

	@AfterAll
	void closeSf() {
		this.sessionFactory.close();
	}

	@Test
	void shouldBeAbleToCRUDMovies() {

		var id = this.sessionFactory.fromSession(session -> {
			var movie = new Movie();
			var tx = session.beginTransaction();
			movie.setTitle("Winter is coming.");
			session.persist(movie);
			tx.commit();
			return movie.getId();
		});

		this.sessionFactory.inSession(session -> {
			var tx = session.beginTransaction();
			var movies = session.createSelectionQuery("from Movie where title like 'Winter%'", Movie.class)
				.getResultList();
			assertThat(movies).hasSize(1).first().satisfies(m -> {
				assertThat(m.getId()).isNotNull();
				assertThat(m.getTitle()).isEqualTo("Winter is coming.");
				m.setTitle("We hibernated.");
			});
			tx.commit();
		});

		this.sessionFactory.inSession(session -> {
			var tx = session.beginTransaction();
			var movie = session.find(Movie.class, id);
			assertThat(movie.getTitle()).isEqualTo("We hibernated.");
			session.remove(movie);
			tx.commit();
		});

		this.sessionFactory.inSession(session -> {
			var movies = session.createSelectionQuery("from Movie", Movie.class).getResultList();
			assertThat(movies).hasSize(1).first().satisfies(m -> {
				assertThat(m.getId()).isNotNull();
				assertThat(m.getTitle()).isEqualTo("The Klimperclown");
			});
		});
	}

	@Test
	void shouldBeAbleToWorkWithRelationships() {
		this.sessionFactory.inSession(session -> {
			var people = session.createSelectionQuery("from Person", Person.class).getResultList();
			assertThat(people).hasSize(1).first().satisfies(p -> {
				assertThat(p.getName()).isEqualTo("Helge Schneider");
				assertThat(p.getMovies()).hasSize(1);
			});
		});

		this.sessionFactory.inSession(session -> {
			var person = new Person();
			person.setName("Jack Nicholson");
			var movie = new Movie();
			movie.setTitle("The Shining");
			person.getMovies().add(movie);

			var tx = session.beginTransaction();
			session.persist(person);
			tx.commit();
		});

		this.sessionFactory.inSession(session -> session.doWork(connection -> {
			try (var stmt = connection.createStatement()) {
				var rs = stmt.executeQuery("/*+ NEO4J FORCE_CYPHER */ MATCH (n:Person) RETURN count(n)");
				rs.next();
				Assertions.assertThat(rs.getInt(1)).isEqualTo(2L);
				rs.close();

				rs = stmt.executeQuery("/*+ NEO4J FORCE_CYPHER */ MATCH (n:Movie) RETURN count(n)");
				rs.next();
				Assertions.assertThat(rs.getInt(1)).isEqualTo(2L);
				rs.close();

				rs = stmt.executeQuery("/*+ NEO4J FORCE_CYPHER */ MATCH ()-[r:ACTED_IN]->() RETURN count(r)");
				rs.next();
				Assertions.assertThat(rs.getInt(1)).isEqualTo(2L);
				rs.close();
			}
		}));
	}

}
