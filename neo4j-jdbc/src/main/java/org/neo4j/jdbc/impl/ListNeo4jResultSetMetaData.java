package org.neo4j.jdbc.impl;

import org.neo4j.jdbc.Neo4jResultSetMetaData;

import java.util.List;

public class ListNeo4jResultSetMetaData extends Neo4jResultSetMetaData {
    /**
     * Default constructor with the list of column.
     *
     * @param keys List of column of the ResultSet
     */
    public ListNeo4jResultSetMetaData(List<String> keys) {
        super(keys);
    }

}
