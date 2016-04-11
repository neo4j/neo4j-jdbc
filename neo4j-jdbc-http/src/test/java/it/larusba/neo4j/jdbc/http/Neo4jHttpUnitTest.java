package it.larusba.neo4j.jdbc.http;

import au.com.bytecode.opencsv.CSVReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.larusba.neo4j.jdbc.http.driver.Neo4jStatement;
import org.junit.Assert;
import org.junit.ClassRule;
import org.neo4j.harness.junit.Neo4jRule;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Neo4jHttpUnitTest  {

    @ClassRule
    public static Neo4jRule neo4j = new Neo4jRule().withFixture(  new File(Neo4jHttpUnitTest.class.getClassLoader().getResource
            ("data/movie.cyp").getFile()));

    private final static int CSV_STATEMENT = 0;
    private final static int CSV_PARAMETERS = 1;
    private final static int CSV_INCLUDESTATS = 2;

    /**
     * Retrieve some random queries from a csv file.
     * We return a map with the CSV source into the kye <code>source</code> and its object Neo4jStatement into key <code>object</code>.
     *
     * @param filename The filename that contains queries
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

        Map<String, List> result= new HashMap<>();
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
    protected Neo4jStatement transformCsvLineToNeo4jStatement(String[] line) throws IOException {
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
    protected void assertCSVQueryEqual( List<String[]> expected, String result) {
        String pattern = "{\"statement\":\"@@statement@@\",\"parameters\":@@parameters@@,\"includeStats\":@@includeStats@@}";

        String jsonExpected = "{\"statements\":[";
        for(int i=0; i< expected.size(); i++) {
            if(i > 0 && i < expected.size()) {
                jsonExpected +=",";
            }
            String[] line = expected.get(i);
            String jsonQuery = pattern.replaceAll("@@includeStats@@", line[CSV_INCLUDESTATS]);
            jsonQuery = jsonQuery.replaceAll("@@parameters@@", line[CSV_PARAMETERS]);
            jsonQuery = jsonQuery.replaceAll("@@statement@@", Neo4jStatement.escapeQuery(line[CSV_STATEMENT]));

            jsonExpected += jsonQuery;
        }
        jsonExpected += "]}";

        Assert.assertEquals(jsonExpected.replaceAll("\\s+",""), result.replaceAll("\\s+",""));
    }
}
