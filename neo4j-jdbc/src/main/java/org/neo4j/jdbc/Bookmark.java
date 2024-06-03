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
package org.neo4j.jdbc;

/**
 * Represents a Neo4j causal bookmark. The value does not include any information that
 * should be parsed or stored in any other way than passing it to different instances of
 * the Neo4j JDBC driver.
 *
 * @param value The actual value of the bookmark
 * @author Michael J. Simons
 * @since 6.0.0
 */
public record Bookmark(String value) {
}
