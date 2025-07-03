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
package org.neo4j.jdbc;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Access to our JSON Mappers, very indirect to avoid touch any classes in a premature
 * fashion.
 *
 * @author Michael J. Simons
 * @since 6.7.0
 */
enum JSONMappers {

	/** Single instance of this utility class. */
	INSTANCE;

	private static final Logger LOGGER = Logger.getLogger(JSONMappers.class.getName());

	private static final Map<String, String> KNOWN_MAPPERS = Map.of("com.fasterxml.jackson.databind.JsonNode",
			"JacksonJSONMapperImpl");

	@SuppressWarnings("squid:S1452") // Generic items, this is exactly what we want here
	private final Map<String, Optional<JSONMapper<?>>> loadedMappers = new ConcurrentHashMap<>();

	public Optional<JSONMapper<?>> getMapper(String typeName) {
		return this.loadedMappers.computeIfAbsent(typeName, JSONMappers::loadMapper);
	}

	private static Optional<JSONMapper<?>> loadMapper(String typeName) {

		var mapperClass = KNOWN_MAPPERS.get(typeName);
		if (mapperClass == null) {
			// If this isn't preconfigured, we compare types and assignability
			try {
				var type = Class.forName(typeName);

				for (@SuppressWarnings("squid:S2864") var mappedTypedName : KNOWN_MAPPERS.keySet()) {
					var mappedType = Class.forName(mappedTypedName, false, JSONMappers.class.getClassLoader());
					if (mappedType.isAssignableFrom(type)) {
						mapperClass = KNOWN_MAPPERS.get(mappedTypedName);
					}
				}
			}
			catch (Exception ignored) {
				// This is fine
			}
		}

		if (mapperClass == null) {
			return Optional.empty();
		}

		try {
			var name = JSONMappers.class.getPackageName() + "." + mapperClass;
			LOGGER.log(Level.FINE, "Trying to load mapper {0}", name);
			@SuppressWarnings("unchecked")
			var mapperType = (Class<JSONMapper<?>>) Class.forName(name, true, JSONMappers.class.getClassLoader());
			return Optional.of(mapperType.getDeclaredConstructor().newInstance());
		}
		catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException
				| InvocationTargetException | NoClassDefFoundError ex) {
			LOGGER.log(Level.WARNING, "Could not load a mapper for %s".formatted(typeName));
			return Optional.empty();
		}
	}

}
