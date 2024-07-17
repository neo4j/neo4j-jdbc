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

import java.sql.Wrapper;
import java.util.Map;

/**
 * A Neo4j specific JDBC extension for adding metadata to ongoing Neo4j transactions.
 *
 * @author Michael J. Simons
 * @since 6.0.0
 */
public sealed interface Neo4jMetadataWriter extends Wrapper
		permits Neo4jDriverExtensions, Neo4jConnection, Neo4jStatement {

	/**
	 * Set the transaction metadata. Specified metadata will be attached to the ongoing
	 * transaction and visible in the output of {@code dbms.listQueries} and
	 * {@code dbms.listTransactions} procedures. It will also get logged to the
	 * {@code query.log}.
	 * <p>
	 * This functionality makes it easier to tag transactions and is equivalent to
	 * {@code dbms.setTXMetaData} procedure.
	 * @param metadata the metadata
	 * @return this object
	 */
	Neo4jMetadataWriter withMetadata(Map<String, Object> metadata);

}
