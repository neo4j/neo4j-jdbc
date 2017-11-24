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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.neo4j.jdbc.http.test.Neo4jHttpUnitTestUtil;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class Neo4jStatementTest extends Neo4jHttpUnitTestUtil {

	@Test public void toJsonShouldSucceed() throws Exception {
		ObjectMapper mapper = new ObjectMapper();

		// Testing with all queries into a statement
		Map<String, List> queries = getRandomNeo4jStatementFromCSV("data/queries.csv", -1);
		assertCSVQueryEqual(queries.get("source"), Neo4jStatement.toJson(queries.get("object"), mapper));

		// Testing with only one query
		queries = getRandomNeo4jStatementFromCSV("data/queries.csv", 1);
		assertCSVQueryEqual(queries.get("source"), Neo4jStatement.toJson(queries.get("object"), mapper));
	}
}
