package it.larusba.neo4j.jdbc.http.driver;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.larusba.neo4j.jdbc.http.test.Neo4jHttpUnitTest;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class Neo4jStatementTest extends Neo4jHttpUnitTest {

    @Test
    public void toJsonShouldSucceed() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // Testing with all queries into a statement
        Map<String, List> queries = getRandomNeo4jStatementFromCSV("data/queries.csv", -1);
        assertCSVQueryEqual(queries.get("source"), Neo4jStatement.toJson(queries.get("object"), mapper));

        // Testing with only one query
        queries = getRandomNeo4jStatementFromCSV("data/queries.csv", 1);
        assertCSVQueryEqual(queries.get("source"), Neo4jStatement.toJson(queries.get("object"), mapper));
    }
}
