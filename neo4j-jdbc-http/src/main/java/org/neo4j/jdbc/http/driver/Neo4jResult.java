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

import java.util.List;
import java.util.Map;

/**
 * A POJO that store a Neo4j query result that match the cypher endpoint.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class Neo4jResult {

	/**
	 * List of columns.
	 */
	private List<String> columns;

	/**
	 * List of data row.
	 */
	private List<Map> rows;

	/**
	 * List fof stats
	 */
	private Map<String, Object> stats;

	/**
	 * Constructor.
	 *
	 * @param map JSON Map
	 */
	public Neo4jResult(Map map) {
		this.columns = (List<String>) map.get("columns");
		this.rows = (List<Map>) map.get("data");

		if (map.containsKey("stats")) {
			this.stats = (Map<String, Object>) map.get("stats");
		}
	}
	
	/**
	 * @return the column names in the result
	 */
	public List<String> getColumns() {
		return columns;
	}
	
	/**
	 * @return the rows in the result set
	 */
	public List<Map> getRows() {
		return rows;
	}

	/**
	 * @return the statistics for the statement
	 */
	public Map<String, Object> getStats() {
		return stats;
	}
	
	/**
	 * Compute updated elements number.
	 *
	 * @return the number of updated elements
	 */
	public int getUpdateCount() {
		int updated = 0;
		if (this.stats != null && (boolean) this.stats.get("contains_updates")) {
			updated += ((Long) stats.get("nodes_created")).intValue();
			updated += ((Long) stats.get("nodes_deleted")).intValue();
			updated += ((Long) stats.get("relationships_created")).intValue();
			updated += ((Long) stats.get("relationship_deleted")).intValue();
		}
		return updated;
	}
}
