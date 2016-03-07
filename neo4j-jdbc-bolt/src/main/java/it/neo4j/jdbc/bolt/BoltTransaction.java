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
 * Created on 07/03/16
 */
package it.neo4j.jdbc.bolt;

import org.neo4j.driver.v1.Transaction;

/**
 * This class is a wrapper to share the bolt driver Transaction between BoltConnection and BoltStatement
 *
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltTransaction {
	private Transaction t = null;

	/**
	 * @param t The bolt java driver Transaction
	 */
	public void setTransaction(Transaction t) {
		this.t = t;
	}

	/**
	 * The actual transaction or null otherwise
	 *
	 * @return
	 */
	public Transaction getTransaction() {
		return this.t;
	}
}
