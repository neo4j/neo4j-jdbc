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
 * Created on 15/4/2016
 */
package it.larusba.neo4j.jdbc.http.test;

import au.com.bytecode.opencsv.CSVReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.larusba.neo4j.jdbc.http.driver.Neo4jStatement;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class Neo4jHttpUnitTest {

	@Rule public ExpectedException expectedEx = ExpectedException.none();

	private final static int CSV_STATEMENT    = 0;
	private final static int CSV_PARAMETERS   = 1;
	private final static int CSV_INCLUDESTATS = 2;

	/**
	 * Retrieve some random queries from a csv file.
	 * We return a map with the CSV source into the kye <code>source</code> and its object Neo4jStatement into key <code>object</code>.
	 *
	 * @param filename  The filename that contains queries
	 * @param nbElement The number element to return
	 * @return
	 * @throws Exception
	 */
	protected Map<String, List> getRandomNeo4jStatementFromCSV(String filename, int nbElement) throws Exception {
		List<Neo4jStatement> queriesObj = new ArrayList<>();
		List<String[]> queriesCsv = new ArrayList<>();

		File csv = new File(getClass().getClassLoader().getResource(filename).getFile());
		CSVReader reader = new CSVReader(new FileReader(csv), ';', '"');
		List<String[]> entries = reader.readAll();
		entries.remove(0); // remove headers

		if (nbElement > 0) {
			Random random = new Random();
			for (int i = 0; i < nbElement; i++) {
				Integer id = random.nextInt(entries.size());
				String[] line = entries.get(id);
				queriesObj.add(transformCsvLineToNeo4jStatement(line));
				queriesCsv.add(line);
			}
		} else {
			for (String[] line : entries) {
				queriesObj.add(transformCsvLineToNeo4jStatement(line));
				queriesCsv.add(line);
			}
		}

		Map<String, List> result = new HashMap<>();
		result.put("source", queriesCsv);
		result.put("object", queriesObj);

		return result;
	}

	/**
	 * Transform a CSV line into an Object query.
	 *
	 * @param line The CSV line
	 * @return The corresponding QUery object
	 * @throws IOException
	 */
	protected Neo4jStatement transformCsvLineToNeo4jStatement(String[] line) throws SQLException, IOException {
		String statement = line[CSV_STATEMENT];
		Map parameters = (Map) new ObjectMapper().readValue(line[CSV_PARAMETERS], HashMap.class);
		Boolean withStat = Boolean.valueOf(line[CSV_INCLUDESTATS]);
		return new Neo4jStatement(statement, parameters, withStat);
	}

	/**
	 * Just an assert method to validate the transformation of CSV to JSON query.
	 *
	 * @param expected
	 * @param result
	 */
	protected void assertCSVQueryEqual(List<String[]> expected, String result) {
		String pattern = "{\"statement\":\"@@statement@@\",\"parameters\":@@parameters@@,\"includeStats\":@@includeStats@@}";

		String jsonExpected = "{\"statements\":[";
		for (int i = 0; i < expected.size(); i++) {
			if (i > 0 && i < expected.size()) {
				jsonExpected += ",";
			}
			String[] line = expected.get(i);
			String jsonQuery = pattern.replaceAll("@@includeStats@@", line[CSV_INCLUDESTATS]);
			jsonQuery = jsonQuery.replaceAll("@@parameters@@", line[CSV_PARAMETERS]);
			jsonQuery = jsonQuery.replaceAll("@@statement@@", Neo4jStatement.escapeQuery(line[CSV_STATEMENT]));

			jsonExpected += jsonQuery;
		}
		jsonExpected += "]}";

		Assert.assertEquals(jsonExpected.replaceAll("\\s+", ""), result.replaceAll("\\s+", ""));
	}
}
