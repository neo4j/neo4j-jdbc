package org.neo4j.jdbc.http.driver;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;
import static org.neo4j.driver.SessionConfig.forDatabase;

public class CypherExecutorContainerIT {

	private static final String EXTRA_DATABASE_NAME = "extra";

	private static final String USERNAME = "neo4j";

	private static final String PASSWORD = "password";

	private static Neo4jContainer<?> neo4jContainer;

	@BeforeClass
	public static void startDockerImage() {
		neo4jContainer = startEnterpriseDockerContainer(imageCoordinates(), USERNAME, PASSWORD);
		Assume.assumeTrue("neo4j container should be not null", neo4jContainer != null);
		Assume.assumeTrue("neo4j container should be up and running", neo4jContainer.isRunning());
	}

	@BeforeClass
	public static void createExtraDatabase() throws Exception {
		createDatabase(EXTRA_DATABASE_NAME);
	}

	@AfterClass
	public static void dropExtraDatabase() throws Exception {
		dropDatabase(EXTRA_DATABASE_NAME);
	}

	@Test
	public void testDefaultDatabaseExecution() throws Exception {
		String host = neo4jContainer.getContainerIpAddress();
		Integer port = neo4jContainer.getMappedPort(7474);
		CypherExecutor executor = new CypherExecutor(host, port, false, baseProperties());

		Neo4jResponse neo4jResponse = executor
				.executeQuery(new Neo4jStatement("CREATE (:InDefault)", new HashMap<>(0), false));

		assertThat(neo4jResponse.getErrors(), empty());
		assertThat(neo4jResponse.getCode(), equalTo(200));
		doInSession(forDatabase("neo4j"), (session) -> {
			long count = session
					.readTransaction(tx -> tx.run("MATCH (n:InDefault) RETURN COUNT(n) AS count").single().get("count")
							.asLong());
			assertThat(count, equalTo(1L));
		});
		doInSession(forDatabase(EXTRA_DATABASE_NAME), (session) -> {
			long count = session
					.readTransaction(tx -> tx.run("MATCH (n:InDefault) RETURN COUNT(n) AS count").single().get("count")
							.asLong());
			assertThat(count, equalTo(0L));
		});
	}

	@Test
	public void testNonDefaultDatabaseExecution() throws Exception {
		String host = neo4jContainer.getContainerIpAddress();
		Integer port = neo4jContainer.getMappedPort(7474);
		Properties properties = new Properties();
		properties.putAll(baseProperties());
		properties.setProperty("database", EXTRA_DATABASE_NAME);
		CypherExecutor executor = new CypherExecutor(host, port, false, properties);

		Neo4jResponse neo4jResponse = executor
				.executeQuery(new Neo4jStatement("CREATE (:InNonDefault)", new HashMap<>(0), false));

		assertThat(neo4jResponse.getErrors(), empty());
		assertThat(neo4jResponse.getCode(), equalTo(200));
		doInSession(forDatabase("neo4j"), (session) -> {
			long count = session
					.readTransaction(tx -> tx.run("MATCH (n:InNonDefault) RETURN COUNT(n) AS count").single().get("count")
							.asLong());
			assertThat(count, equalTo(0L));
		});
		doInSession(forDatabase(EXTRA_DATABASE_NAME), (session) -> {
			long count = session
					.readTransaction(tx -> tx.run("MATCH (n:InNonDefault) RETURN COUNT(n) AS count").single().get("count")
							.asLong());
			assertThat(count, equalTo(1L));
		});
	}

	private static Neo4jContainer<?> startEnterpriseDockerContainer(String version, String username, String password) {
		try {
			Neo4jContainer<?> container = new Neo4jContainer<>(version)
					.waitingFor(new WaitAllStrategy() // no need to override this once https://github.com/testcontainers/testcontainers-java/issues/4454 is fixed
							.withStrategy(new LogMessageWaitStrategy().withRegEx(".*Bolt enabled on .*:7687\\.\n"))
							.withStrategy(new HttpWaitStrategy().forPort(7474).forStatusCodeMatching(response -> response == HTTP_OK)))
					.withEnv("NEO4J_AUTH", String.format("%s/%s", username, password))
					.withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes");
			container.start();
			return container;
		}
		catch (Exception exception) {
			exception.printStackTrace();
		}
		return null;
	}

	private static String imageCoordinates() {
		return String.format("neo4j:%s-enterprise", projectNeo4jVersion());
	}

	private static String projectNeo4jVersion() {
		String filteredClasspathResource = "/neo4j.version";
		List<String> lines = readLines(filteredClasspathResource);
		int lineCount = lines.size();
		if (lineCount != 1) {
			throw new RuntimeException(String
					.format("%s should have only 1 (filtered) line, found: %d", filteredClasspathResource, lineCount));
		}
		return lines.iterator().next();
	}

	private static List<String> readLines(String classpathResource) {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(CypherExecutorContainerIT.class
				.getResourceAsStream(classpathResource)))) {

			return reader.lines().collect(Collectors.toList());
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void createDatabase(String databaseName) throws Exception {
		doInSession(forDatabase("system"), (Session session) -> session
				.writeTransaction((tx) -> tx.run(String.format("CREATE DATABASE %s", databaseName))));
	}

	private static void dropDatabase(String databaseName) throws Exception {
		doInSession(forDatabase("system"), (Session session) -> session
				.writeTransaction((tx) -> tx.run(String.format("DROP DATABASE %s", databaseName))));
	}

	private static void doInSession(SessionConfig sessionConfig, Consumer<Session> sessionConsumer) throws Exception {
		try (Driver driver = GraphDatabase
				.driver(new URI(neo4jContainer.getBoltUrl()), AuthTokens.basic(USERNAME, PASSWORD));
			 Session session = driver.session(sessionConfig)) {

			sessionConsumer.accept(session);
		}
	}

	private static Properties baseProperties() {
		Properties properties = new Properties();
		properties.setProperty("userAgent", "Integration test");
		properties.setProperty("user", USERNAME);
		properties.setProperty("password", PASSWORD);
		return properties;
	}

}