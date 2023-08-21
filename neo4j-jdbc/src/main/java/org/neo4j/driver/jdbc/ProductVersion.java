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
package org.neo4j.driver.jdbc;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Utility class to retrieve the version of the core module aka the driver- or
 * product-version.
 *
 * @author Michael J. Simons
 * @since 1.0.0
 */
final class ProductVersion {

	private static volatile String value;

	static String getValue() {

		String computedVersion = value;
		if (computedVersion == null) {
			synchronized (ProductVersion.class) {
				computedVersion = value;
				if (computedVersion == null) {
					value = getVersionImpl();
					computedVersion = value;
				}
			}
		}
		return computedVersion;
	}

	static int getMajorVersion() {
		return getVersion(0);
	}

	static int getMinorVersion() {
		return getVersion(1);
	}

	private static int getVersion(int idx) {

		var value = getValue();
		if ("unknown".equalsIgnoreCase(value)) {
			throw new UnsupportedOperationException("Unsupported or unknown version '%s'".formatted(value));
		}
		var part = value.split("\\.")[idx];
		var indexOfDash = part.indexOf("-");
		return Integer.parseInt(part.substring(0, (indexOfDash < 0) ? part.length() : indexOfDash));
	}

	private static String getVersionImpl() {
		try {
			Enumeration<URL> resources = Neo4jDriver.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
			while (resources.hasMoreElements()) {
				URL url = resources.nextElement();
				Manifest manifest = new Manifest(url.openStream());
				if (isApplicableManifest(manifest)) {
					Attributes attr = manifest.getMainAttributes();
					return get(attr, "Implementation-Version").toString();
				}
			}
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Unable to read from neo4j-jdbc manifest.", ex);
		}

		return "unknown";
	}

	private static boolean isApplicableManifest(Manifest manifest) {
		Attributes attributes = manifest.getMainAttributes();
		return "neo4j-jdbc".equals(get(attributes, "Artifact-Id"));
	}

	private static Object get(Attributes attributes, String key) {
		return attributes.get(new Attributes.Name(key));
	}

	private ProductVersion() {
	}

}
