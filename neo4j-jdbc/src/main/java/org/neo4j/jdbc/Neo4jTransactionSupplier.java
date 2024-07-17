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

import java.sql.SQLException;
import java.util.Map;

@FunctionalInterface
interface Neo4jTransactionSupplier {

	/**
	 * Supplies the existing or a new transaction in {@link Neo4jTransaction.State#NEW}
	 * state.
	 * @param additionalMetadata any additional metadata for new transactions
	 * @return the existing or a new transaction.
	 * @throws SQLException if there is a connection issue or another issue prohibiting
	 * the transaction supply.
	 */
	Neo4jTransaction getTransaction(Map<String, Object> additionalMetadata) throws SQLException;

}
