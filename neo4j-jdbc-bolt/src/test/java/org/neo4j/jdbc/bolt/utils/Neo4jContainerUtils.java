package org.neo4j.jdbc.bolt.utils;

import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.utility.DockerImageName;

public class Neo4jContainerUtils {

    public static String neo4jImageCoordinates() {
        String neo4jVersion = System.getenv("NEO4J_VERSION");
        if (neo4jVersion == null) neo4jVersion = "4.3";
        String enterpriseEdition = System.getenv("NEO4J_ENTERPRISE_EDITION");
        if (enterpriseEdition == null) enterpriseEdition = "false";
        return String.format("neo4j:%s%s", neo4jVersion, Boolean.parseBoolean(enterpriseEdition) ? "-enterprise": "");
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

    private static boolean versionStartsWith(Neo4jContainer<?> neo4j, String prefix) {
        return getVersion(neo4j).startsWith(prefix);
    }
}
