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
package org.neo4j.jdbc.translator.spi;

import java.util.Map;

/**
 * Factories that produce {@link Translator translators}. Any implementation is free to
 * use cached instances for translators produced for configurations considered equal as
 * long as the translators themselves are thread safe.
 *
 * @author Michael J. Simons
 * @since 6.0.0
 */
public interface TranslatorFactory {

	/**
	 * Creates a new {@link Translator} or in case of an identical configuration, returns
	 * a possible cached or shared instance. In that case, the translator must be thread
	 * safe.
	 * @param properties properties that will be used to configure the translator
	 * @return a new or a cached translator instance
	 */
	Translator create(Map<String, ?> properties);

}
