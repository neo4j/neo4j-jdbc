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
 * Factories that produce {@link SqlTranslator SQL translators}. Any implementation is
 * free to use cached instances for translators produced for configurations considered
 * equal as long as the translators themselves are threadsafe.
 *
 * @author Michael J. Simons
 * @since 1.0.0
 */
public interface SqlTranslatorFactory {

	/**
	 * Creates a new {@link SqlTranslator} or in case of an identical configuration,
	 * returns a possible cached or shared instance. In that case, the translator must be
	 * thread safe.
	 * @param properties properties that will be used to configure the translator
	 * @return a new or a cached translator instance
	 */
	SqlTranslator create(Map<String, Object> properties);

	/**
	 * Return a human-readable name of the translators that are produced by this factory.
	 * Defaults to the simple name of the factory class.
	 * @return the name of this translator
	 */
	default String getName() {
		return this.getClass().getSimpleName();
	}

}
