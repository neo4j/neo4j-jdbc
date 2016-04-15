package it.larusba.neo4j.jdbc.http.driver;

import it.larusba.neo4j.jdbc.http.test.Neo4jHttpITTest;
import org.junit.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public class CypherExecutorITTest extends Neo4jHttpITTest {

    private CypherExecutor executor;

    @Before
    public void before() throws IOException, SQLException {
        String host = this.neo4j.httpsURI().getHost();
        Integer port = this.neo4j.httpsURI().getPort();
        Properties properties = new Properties();
        properties.put("userAgent", "Unit Test");
        this.executor = new CypherExecutor(host, port, properties);
    }

    @Test
    public void executeQueryShouldSucceed() throws Exception {
        List<Neo4jStatement> queries = getRandomNeo4jStatementFromCSV("data/queries.csv", -1).get("object");
        Neo4jResponse response = executor.executeQueries(queries);

        Assert.assertEquals(queries.size(), response.results.size());
        Assert.assertFalse(response.hasErrors());
    }

    @Test
    public void executeQueryThenRollbackShouldSucceed() throws SQLException {
        executor.setAutoCommit(Boolean.FALSE);
        String randomLabel = "Test" + UUID.randomUUID();
        executor.executeQuery(new Neo4jStatement("CREATE (:`" + randomLabel + "`)", null, Boolean.TRUE));
        executor.rollback();

        executor.setAutoCommit(Boolean.TRUE);
        Neo4jResponse response = executor.executeQuery(new Neo4jStatement("MATCH (n:`" + randomLabel + "`) RETURN n", null, Boolean.FALSE));
        Assert.assertEquals(0, response.results.get(0).rows.size());
    }

    @Test
    public void executeMultipleQueryThenCommitShouldSucceed() throws SQLException {
        executor.setAutoCommit(Boolean.FALSE);
        String randomLabel = "Test" + UUID.randomUUID();
        executor.executeQuery(new Neo4jStatement("CREATE (:`" + randomLabel + "` {value:1})", null, Boolean.FALSE));
        executor.executeQuery(new Neo4jStatement("CREATE (:`" + randomLabel + "` {value:2})", null, Boolean.FALSE));
        executor.commit();

        executor.setAutoCommit(Boolean.TRUE);
        Neo4jResponse response = executor.executeQuery(new Neo4jStatement("MATCH (n:`" + randomLabel + "`) RETURN n", null, Boolean.FALSE));
        Assert.assertEquals(2, response.results.get(0).rows.size());
    }

    @Test
    public void rollbackClosedTransactionShouldFail() throws SQLException {
        expectedEx.expect(SQLException.class);

        executor.setAutoCommit(Boolean.FALSE);
        String randomLabel = "Test" + UUID.randomUUID();
        executor.executeQuery(new Neo4jStatement("CREATE (:`" + randomLabel + "` {value:1})", null, Boolean.FALSE));
        executor.executeQuery(new Neo4jStatement("CREATE (:`" + randomLabel + "` {value:2})", null, Boolean.FALSE));
        executor.commit();

        executor.rollback();
    }

    @Test
    public void rollbackOnAutocommitShouldFail() throws SQLException {
        expectedEx.expect(SQLException.class);

        String randomLabel = "Test" + UUID.randomUUID();
        executor.executeQuery(new Neo4jStatement("CREATE (:`" + randomLabel + "` {value:1})", null, Boolean.FALSE));
        executor.rollback();
    }

    @Test
    public void commitOnAutocommitShouldFail() throws SQLException {
        expectedEx.expect(SQLException.class);

        String randomLabel = "Test" + UUID.randomUUID();
        executor.executeQuery(new Neo4jStatement("CREATE (:`" + randomLabel + "` {value:1})", null, Boolean.FALSE));
        executor.commit();
    }

    @Test
    public void transactionToAutoCommitModeShouldCommitCurrentTransaction() throws SQLException {
        executor.setAutoCommit(Boolean.FALSE);
        String randomLabel = "Test" + UUID.randomUUID();
        executor.executeQuery(new Neo4jStatement("CREATE (:`" + randomLabel + "` {value:1})", null, Boolean.FALSE));
        executor.executeQuery(new Neo4jStatement("CREATE (:`" + randomLabel + "` {value:2})", null, Boolean.FALSE));

        executor.setAutoCommit(Boolean.TRUE);
        Neo4jResponse response = executor.executeQuery(new Neo4jStatement("MATCH (n:`" + randomLabel + "`) RETURN n", null, Boolean.FALSE));
        Assert.assertEquals(2, response.results.get(0).rows.size());
    }

    @After
    public void after() throws SQLException {
        executor.close();
    }

}
