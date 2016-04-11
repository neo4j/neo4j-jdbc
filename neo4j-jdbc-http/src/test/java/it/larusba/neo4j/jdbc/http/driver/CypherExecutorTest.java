package it.larusba.neo4j.jdbc.http.driver;

import it.larusba.neo4j.jdbc.http.Neo4jHttpUnitTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class CypherExecutorTest extends Neo4jHttpUnitTest {

    private CypherExecutor executor;

    @Before
    public void before() throws IOException, SQLException {
        String host = this.neo4j.httpsURI().getHost();
        Integer port = this.neo4j.httpsURI().getPort();
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
    public void makeAQueryShoudlSucceed() throws Exception {
        List<Neo4jStatement> queries = getRandomNeo4jStatementFromCSV("data/queries.csv", -1).get("object");
        Neo4jResponse response = executor.executeQueries(queries);

        Assert.assertEquals(queries.size(), response.results.size());
        Assert.assertFalse(response.hasErrors());
    }

}
