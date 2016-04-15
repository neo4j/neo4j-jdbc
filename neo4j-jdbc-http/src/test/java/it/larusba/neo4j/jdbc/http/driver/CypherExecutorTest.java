package it.larusba.neo4j.jdbc.http.driver;

import it.larusba.neo4j.jdbc.http.test.Neo4jHttpUnitTest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Random;

public class CypherExecutorTest extends Neo4jHttpUnitTest {

    private CypherExecutor executor;

    @Before
    public void before() throws IOException, SQLException {
        String host = "azertyuiop";
        Integer port = 1234;
        Properties properties = new Properties();
        this.executor = new CypherExecutor(host, port, properties);
    }

    @Test
    public void getTransactionIdShouldReturnCorrectId() {
        Random idGenerator = new Random();
        for (Integer i = 1; i < 100; i++) {
            Integer id = idGenerator.nextInt();
            Integer returnId = executor.getTransactionId(executor.transactionUrl + "/" + id);
            Assert.assertEquals(id, returnId);
        }
    }

    @Test
    public void getTransactionIdShouldReturnNegativeId() {
        List<String> urls = Arrays.asList(
                "",
                "http://localhost1234:1234/db/data",
                executor.transactionUrl,
                executor.transactionUrl + "/commit"
        );
        for (String url : urls) {
            Integer returnId = executor.getTransactionId(url);
            Assert.assertTrue(returnId < 0);
        }
    }

    @Test
    public void executeQueryOnDownServerShouldFail() throws Exception {
        expectedEx.expect(SQLException.class);

        List<Neo4jStatement> queries = getRandomNeo4jStatementFromCSV("data/queries.csv", 1).get("object");
        executor.executeQueries(queries);
    }

    @Test
    public void executeEmptyQueryShouldFail() throws Exception {
        expectedEx.expect(SQLException.class);
        executor.executeQuery(new Neo4jStatement("", null, null));
    }

    @After
    public void after() throws SQLException {
        executor.close();
    }

}
