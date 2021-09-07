/*
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
 * Created on 24/03/16
 */
package org.neo4j.jdbc.bolt;

import org.neo4j.jdbc.Neo4jParameterMetaData;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltNeo4jParameterMetaData extends Neo4jParameterMetaData {

	private final BoltNeo4jPreparedStatement preparedStatement;

	public BoltNeo4jParameterMetaData(BoltNeo4jPreparedStatement preparedStatement) {
		this.preparedStatement = preparedStatement;
	}

	// visible for testing
	public BoltNeo4jPreparedStatement getPreparedStatement() {
		return preparedStatement;
	}
}
