/*
 * Copyright (c) 2023 "Neo4j,"
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
package org.neo4j.driver.it.quarkus;

import javax.sql.DataSource;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;

/**
 * Creates a global instance of {@link Jdbi} based on the one {@link DataSource}.
 *
 * @author Michael J. Simons
 */
public final class JdbiProducer {

	/**
	 * Creates a Jdbi instance configured with a bunch of row mappers.
	 * @param dataSource the datasource for which Jdbi should be produced
	 * @return a Jdbi instance
	 */
	@Produces
	@Singleton
	public Jdbi jdbi(DataSource dataSource) {

		return Jdbi.create(dataSource).registerRowMapper(Movie.class, ConstructorMapper.of(Movie.class));
	}

}
