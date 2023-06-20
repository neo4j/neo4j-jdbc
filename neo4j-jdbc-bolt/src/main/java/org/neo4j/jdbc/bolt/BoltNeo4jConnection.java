/**
 * Copyright (c) 2016 LARUS Business Automation [http://www.larus-ba.it]
 * <p>
 * This file is part of the "LARUS Integration Framework for Neo4j".
 * <p>
 * The "LARUS Integration Framework for Neo4j" is licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * Created on 22/12/17
 */
package org.neo4j.jdbc.bolt;

import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Gianmarco Laggia @ Larus B.A.
 * @since 3.2.0
 */
public interface BoltNeo4jConnection extends Connection {

	/**
	 * Getter for transaction.
	 *
	 * @return the transaction
	 */
	Transaction getTransaction();

	/**
	 * Getter for session.
	 *
	 * @return the internal session
	 */
	Session getOrCreateSession();

	void doCommit() throws SQLException;
	void doRollback() throws SQLException;

	/**
	 * Build an internal neo4j session, without saving reference (stateless)
	 * Close using {@link #close()} for driver management
	 * @return
	 */
	Session newNeo4jSession();

}
