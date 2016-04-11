package it.larusba.neo4j.jdbc.http.driver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * A POJO to store a cypher query that match the cypher request endpoint.
 */
public class Neo4jStatement {

    /**
     * Cypher query.
     */
    public final String statement;

    /**
     * Params of the cypher query.
     */
    public final Map<String, Object> parameters;

    /**
     * Do we need to include stats with the query ?
     */
    public final Boolean includeStats;

    /**
     * Escape method for cypher queries.
     *
     * @param query Cypher query
     * @return
     */
    public static String escapeQuery(String query) {
        return query.replace('\"', '\'').replace('\n', ' ');
    }

    /**
     * Convert the list of query to a JSON compatible with Neo4j endpoint.
     *
     * @param queries List of cypher queries.
     * @return The JSON string that correspond to the body of the API call
     */
    public static String toJson(List<Neo4jStatement> queries, ObjectMapper mapper) throws SQLException {
        StringBuffer sb = new StringBuffer();
        try {
            sb.append("{\"statements\":");
            sb.append(mapper.writeValueAsString(queries));
            sb.append("}");

        } catch (JsonProcessingException e) {
            throw new SQLException("Can't convert Cypher statement(s) into JSON");
        }
        return sb.toString();
    }

    /**
     * Default constructor.
     *
     * @param statement Cypher query
     * @param parameters List of named params for the cypher query
     * @param includeStats Do we need to include stats
     */
    public Neo4jStatement(String statement, Map<String, Object> parameters, Boolean includeStats) {
        this.statement = statement;
        this.parameters = parameters;
        this.includeStats = includeStats;
    }

    /**
     * Constructor withtout stats
     *
     * @param statement Cypher query
     * @param parameters List of named params for the cypher query
     */
    public Neo4jStatement(String statement, Map<String, Object> parameters) {
        this.statement = statement;
        this.parameters = parameters;
        this.includeStats = Boolean.FALSE;
    }

    /**
     * Getter for Statements.
     * We escape the string for the API.
     */
    public String getStatement() {
        return escapeQuery(statement);
    }

}
