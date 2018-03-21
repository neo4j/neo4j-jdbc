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
 * Created on 15/4/2016
 */
package org.neo4j.jdbc.http.driver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A POJO to store a cypher query that match the cypher request endpoint.
 */
public class Neo4jStatement {

	/**
	 * Cypher query.
	 */
	public final String statement;

	/**
	 * Params of the cypher query.
	 */
	public final Map<String, Object> parameters;

	/**
	 * Do we need to include stats with the query ?
	 */
	public final Boolean includeStats;

	/**
	 * Default constructor.
	 *
	 * @param statement    Cypher query
	 * @param parameters   List of named params for the cypher query
	 * @param includeStats Do we need to include stats
	 * @throws SQLException sqlexception
	 */
	public Neo4jStatement(String statement, Map<String, Object> parameters, Boolean includeStats) throws SQLException {
		if (statement != null && !"".equals(statement)) {
			this.statement = statement;
		} else {
			throw new SQLException("Creating a NULL query");
		}
		if (parameters != null) {
			this.parameters = parameters;
		} else {
			this.parameters = new HashMap<>();
		}
		if (includeStats != null) {
			this.includeStats = includeStats;
		} else {
			this.includeStats = Boolean.FALSE;
		}
	}

	/**
	 * Convert the list of query to a JSON compatible with Neo4j endpoint.
	 *
	 * @param queries List of cypher queries.
	 * @param mapper mapper
	 * @return The JSON string that correspond to the body of the API call
	 * @throws SQLException sqlexception
	 */
	public static String toJson(List<Neo4jStatement> queries, ObjectMapper mapper) throws SQLException {
		StringBuilder sb = new StringBuilder();
		try {
			sb.append("{\"statements\":");
			sb.append(mapper.writeValueAsString(queries));
			sb.append("}");

		} catch (JsonProcessingException e) {
			throw new SQLException("Can't convert Cypher statement(s) into JSON", e);
		}
		return sb.toString();
	}

	/**
	 * Getter for Statements.
	 * We escape the string for the API.
	 * 
	 * @return the statement
	 */
	public String getStatement() {
		return statement;
	}

}
