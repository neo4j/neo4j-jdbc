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

import java.sql.Statement;

/**
 * A Neo4j specific extension of a {@link java.sql.Statement}. It may be referred to for
 * use with {@link #unwrap(Class)} to access specific Neo4j functionality.
 *
 * @author Neo4j Drivers Team
 * @since 6.0.0
 */
public sealed interface Neo4jStatement extends Statement, Neo4jMetadataWriter permits StatementImpl {

	/**
	 * The default (Bolt) fetch size that is used.
	 */
	int DEFAULT_FETCH_SIZE = 1000;

}
