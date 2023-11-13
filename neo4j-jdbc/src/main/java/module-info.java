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

@SuppressWarnings({"requires-automatic"}) // Netty is an automatic module :(
module neo4j.jdbc {
	requires transitive java.sql;

	// start::shaded-dependencies
	requires io.netty.buffer;
	requires io.netty.codec;
	requires io.netty.common;
	requires io.netty.handler;
	requires io.netty.resolver;
	requires io.netty.transport;
	requires neo4j.jdbc.translator.spi;
	requires org.neo4j.cypherdsl.support.schema_name;
	// end::shaded-dependencies

	// automatic::jdk.unsupported

	exports org.neo4j.driver.jdbc;
	exports org.neo4j.driver.jdbc.values;

	provides java.sql.Driver with
		org.neo4j.driver.jdbc.Neo4jDriver;

	uses org.neo4j.driver.jdbc.translator.spi.SqlTranslatorFactory;
}
