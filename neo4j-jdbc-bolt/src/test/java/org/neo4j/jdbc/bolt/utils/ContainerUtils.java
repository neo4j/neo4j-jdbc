package org.neo4j.jdbc.bolt.utils;

public class ContainerUtils {

    public static String neo4jImageCoordinates() {
        String neo4jVersion = System.getenv("NEO4J_VERSION");
        if (neo4jVersion == null) neo4jVersion = "4.3";
        String enterpriseEdition = System.getenv("NEO4J_ENTERPRISE_EDITION");
        if (enterpriseEdition == null) enterpriseEdition = "false";
        return String.format("neo4j:%s%s", neo4jVersion, Boolean.parseBoolean(enterpriseEdition) ? "-enterprise": "");
    }
}
