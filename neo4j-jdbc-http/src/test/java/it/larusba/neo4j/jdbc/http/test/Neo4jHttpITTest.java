package it.larusba.neo4j.jdbc.http.test;

import org.junit.ClassRule;
import org.neo4j.harness.junit.Neo4jRule;

import java.io.File;

public class Neo4jHttpITTest extends Neo4jHttpUnitTest {

    @ClassRule
    public static Neo4jRule neo4j = new Neo4jRule().withFixture( new File(Neo4jHttpUnitTest.class.getClassLoader().getResource("data/movie.cyp").getFile()));

}
