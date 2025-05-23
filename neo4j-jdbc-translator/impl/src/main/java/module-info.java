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
import org.neo4j.jdbc.translator.impl.SqlToCypherTranslatorFactory;
import org.neo4j.jdbc.translator.spi.TranslatorFactory;

/**
 * The default Sql translator shipped with the JDK 17 version of the driver.
 */
module org.neo4j.jdbc.translator.impl {
	provides TranslatorFactory with SqlToCypherTranslatorFactory;

	requires com.fasterxml.jackson.jr.ob;
	requires org.neo4j.jdbc.translator.spi;
	requires org.jooq;
	requires org.neo4j.cypherdsl.core;
}
