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
/**
 * The Neo4j JDBC Driver exposes both the actual JDBC API and the Neo4j value system being used. The value system can be
 * used with methods like {@link java.sql.ResultSet#getObject(int, Class)}, for retrieving Neo4j nodes, relationships,
 * paths and especially, the original value objects.
 */
@SuppressWarnings({"requires-automatic"}) // Netty is an automatic module :(
module org.neo4j.jdbc {
	requires transitive java.sql;
	requires static micrometer.core;
	requires static com.fasterxml.jackson.databind;

	// start::shaded-dependencies
	requires io.github.cdimascio.dotenv.java;
	requires org.neo4j.bolt.connection;
	requires org.neo4j.bolt.connection.netty;
	requires org.neo4j.bolt.connection.query_api;
	requires transitive org.neo4j.jdbc.authn.spi;
	requires org.neo4j.jdbc.translator.spi;
	requires org.neo4j.cypherdsl.support.schema_name;
	// end::shaded-dependencies

	// requires jdk.unsupported;

	exports org.neo4j.jdbc;
	exports org.neo4j.jdbc.events;
	exports org.neo4j.jdbc.tracing;
	exports org.neo4j.jdbc.values;
	// exports org.neo4j.jdbc.authn.spi;
	// exports org.neo4j.jdbc.translator.spi;

	provides java.sql.Driver with org.neo4j.jdbc.Neo4jDriver;
	// provides org.neo4j.jdbc.translator.spi.TranslatorFactory with org.neo4j.jdbc.translator.impl.SqlToCypherTranslatorFactory;

	uses org.neo4j.jdbc.authn.spi.AuthenticationSupplierFactory;
	uses org.neo4j.jdbc.translator.spi.TranslatorFactory;

	// start::shaded-dependencies
	uses org.neo4j.bolt.connection.BoltConnectionProviderFactory;
	// end::shaded-dependencies

	// uses org.neo4j.jdbc.internal.shaded.bolt.BoltConnectionProviderFactory;
	// provides org.neo4j.jdbc.internal.shaded.bolt.BoltConnectionProviderFactory with org.neo4j.jdbc.internal.shaded.bolt.netty.NettyBoltConnectionProviderFactory, org.neo4j.jdbc.internal.shaded.bolt.query_api.QueryApiBoltConnectionProviderFactory;
}
