package org.neo4j.jdbc.bolt.utils;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

public class Neo4jContainerUtils {

    public static Neo4jContainer<?> createNeo4jContainer() {
        return new Neo4jContainer<>(neo4jImageCoordinates()).withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
                .waitingFor(boltStart())
                .withAdminPassword(null);
    }

    // Neo4jContainer overwrites the default password so in order to replicate a use-case
    // where we use the default password that needs to be changed we use a GenericContainer

    public static GenericContainer<?> createNeo4jContainerWithDefaultPassword() {
        return new GenericContainer<>(neo4jImageCoordinates())
                .withExposedPorts(7687)
                .withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
                .waitingFor(boltStart());
    }
    public static boolean isV3(Neo4jContainer<?> neo4j) {
        return versionStartsWith(neo4j, "3");
    }

    public static boolean isV4(Neo4jContainer<?> neo4j) {
        return versionStartsWith(neo4j, "4");
    }

    public static boolean isEnterpriseEdition(Neo4jContainer<?> neo4j) {
        return getVersion(neo4j).endsWith("-enterprise");
    }

    public static String getVersion(Neo4jContainer<?> neo4j) {
        return DockerImageName.parse(neo4j.getDockerImageName()).getVersionPart();
    }

    private static String neo4jImageCoordinates() {
        String neo4jVersion = System.getenv("NEO4J_VERSION");
        if (neo4jVersion == null) neo4jVersion = "4.4";
        String enterpriseEdition = System.getenv("NEO4J_ENTERPRISE_EDITION");
        if (enterpriseEdition == null) enterpriseEdition = "false";
        return String.format("neo4j:%s%s", neo4jVersion, Boolean.parseBoolean(enterpriseEdition) ? "-enterprise": "");
    }

    private static boolean versionStartsWith(Neo4jContainer<?> neo4j, String prefix) {
        return getVersion(neo4j).startsWith(prefix);
    }

    // more lenient than Neo4jContainer default strategy
    // no need for this once https://github.com/testcontainers/testcontainers-java/issues/4454 is fixed
    private static LogMessageWaitStrategy boltStart() {
        return new LogMessageWaitStrategy().withRegEx(".*Bolt enabled on .*:7687\\.\n");
    }
}
