/*
 * Copyright (c) 2023-2026 "Neo4j,"
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
package org.neo4j.jdbc.translator.impl;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.ob.JSONObjectException;
import org.neo4j.jdbc.translator.spi.View;

/**
 * Reads a set of views from a JSON resource. Currently, uses Jackson-JR, without any
 * additional annotation processing. Can be extracted to a separate module if necessary,
 * but as the default translator will be the only translator supporting Cypher-backed
 * views for the time being, it doesn't matter that much.
 *
 * @author Michael J. Simons
 * @since 6.5.0
 */
final class ViewDefinitionReader {

	private static final Set<String> SUPPORTED_SCHEMES = Collections
		.unmodifiableSet(new TreeSet<>(Set.of("file", "http", "https", "resource")));

	static ViewDefinitionReader of(String url) {
		URI uri;
		try {
			uri = URI.create(url);
		}
		catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("Unsupported url: %s".formatted(url), ex);
		}
		var scheme = Optional.ofNullable(uri.getScheme()).map(s -> s.toLowerCase(Locale.ROOT)).orElse("n/a");
		if (!SUPPORTED_SCHEMES.contains(scheme)) {
			throw new IllegalArgumentException(
					"Unsupported scheme: %s, supported schemes are %s".formatted(scheme, SUPPORTED_SCHEMES));
		}
		if ("file".equals(scheme) && uri.getPath() == null || uri.getPath().isBlank()) {
			throw new IllegalArgumentException(
					"No path specified in this url: %s (%s is %s in that format, not the path; you probably meant file:///%2$s)"
						.formatted(url, Optional.ofNullable(uri.getHost()).orElseGet(uri::getSchemeSpecificPart),
								(uri.getHost() == null && uri.getSchemeSpecificPart() != null)
										? "a schema specific part" : "the host in"));
		}
		return new ViewDefinitionReader(uri);
	}

	private final URI source;

	private ViewDefinitionReader(URI source) {
		this.source = source;
	}

	@SuppressWarnings("unchecked")
	List<View> read() throws IOException {

		var parser = JSON.std.createParser(this.source.toURL()).enable(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION);
		parser.nextToken();
		// This is by a good chance Simbas format
		if (parser.isExpectedStartObjectToken()) {
			var raw = JSON.std.mapFrom(parser);
			if (raw.containsKey("Schemas")) {
				return readSchemas((List<Map<String, Object>>) raw.get("Schemas"));
			}
			if (raw.isEmpty()) {
				return List.of();
			}
			throw new IllegalArgumentException(
					"Invalid JSON content, cannot read %s: %s".formatted(this.source, parser.currentLocation()));
		}

		if (parser.isExpectedStartArrayToken()) {
			try {
				return List.of(JSON.std.arrayOfFrom(View.class, parser));
			}
			catch (JSONObjectException ex) {
				throw new IllegalArgumentException("Invalid JSON content, cannot read %s".formatted(this.source), ex);
			}
		}

		if (!parser.hasCurrentToken()) {
			return List.of();
		}

		throw new IOException("Invalid JSON content in %s".formatted(this.source));
	}

	@SuppressWarnings("unchecked")
	List<View> readSchemas(List<Map<String, Object>> schemas) {
		var result = new ArrayList<View>();
		for (var schema : schemas) {
			List<Map<String, Object>> views = (List<Map<String, Object>>) Objects
				.requireNonNullElseGet(schema.get("Views"), List::of);
			for (var view : views) {
				var name = (String) view.get("Name");
				var query = (String) view.get("CypherQuery");
				var columns = ((List<Map<String, Object>>) Objects.requireNonNullElseGet(view.get("Columns"), List::of))
					.stream()
					.map(column -> {
						var columnName = (String) column.get("Name");
						var sourceName = (String) column.get("SourceName");
						var type = "String";
						if (column.get("Neo4jType") instanceof List<?> types && types.size() == 1) {
							type = (String) types.get(0);
						}
						return View.column(columnName, sourceName, type);
					})
					.toList();
				result.add(new View(name, query, columns));
			}
		}
		return result;
	}

}
