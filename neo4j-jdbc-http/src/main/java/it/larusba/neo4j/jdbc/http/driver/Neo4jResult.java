package it.larusba.neo4j.jdbc.http.driver;

import java.util.List;
import java.util.Map;

/**
 * A POJO that store a Neo4j query result that match the cypher endpoint.
 */
public class Neo4jResult {

    /**
     * List of columns.
     */
    private List<String> columns;

    /**
     * List of data row.
     */
    private List<Map> rows;

    /**
     * List fof stats
     */
    private Map<String, String> stats;

    /**
     * Constructor.
     *
     * @param map JSON Map
     */
    public Neo4jResult(Map map) {
        this.columns = (List<String>) map.get("columns");
        this.rows = (List<Map>) map.get("data");

        if (map.containsKey("stats")) {
            this.stats = (Map<String, String>) map.get("stats");
        }
    }
}
