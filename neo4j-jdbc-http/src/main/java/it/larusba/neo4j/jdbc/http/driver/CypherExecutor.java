package it.larusba.neo4j.jdbc.http.driver;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.Header;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Execute cypher queries.
 */
public class CypherExecutor {

    /**
     * URL of the transaction endpoint.
     */
    protected final String transactionUrl;

    /**
     * Autocommit transaction.
     * Must be null at creation time.
     * Initiation of this property is made by its setter, that is called from the constructor.
     */
    protected Boolean autoCommit;

    /**
     * The http client.
     */
    private CloseableHttpClient http;

    /**
     * URL of the current transaction.
     */
    private String currentTransactionUrl;

    /**
     * Jackson mapper object.
     */
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Default constructor.
     *
     * @param host       Hostname of the Neo4j instance.
     * @param port       HTTP port of the Neo4j instance.
     * @param properties Properties of the url connection.
     * @throws SQLException
     */
    public CypherExecutor(String host, Integer port, Properties properties) throws SQLException {
        // Create the http client builder
        HttpClientBuilder builder = HttpClients.custom();
        // Adding authentication to the http client if needed
        if (properties.containsKey("user") && properties.containsKey("password")) {
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(
                    new AuthScope(host, port),
                    new UsernamePasswordCredentials(properties.get("user").toString(), properties.get("password").toString())
            );
            builder.setDefaultCredentialsProvider(credsProvider);
        }
        // Setting user-agent
        StringBuilder sb = new StringBuilder();
        sb.append("Neo4j JDBC Driver");
        if (properties.containsKey("userAgent")) {
            sb.append(" via ");
            sb.append(properties.getProperty("userAgent"));
        }
        builder.setUserAgent(sb.toString());
        // Create the http client
        this.http = builder.build();


        // Create the url endpoint
        StringBuffer sbEndpoint = new StringBuffer();
        sbEndpoint.append("http://")
                .append(host)
                .append(":")
                .append(port)
                .append("/db/data/transaction");
        this.transactionUrl = sbEndpoint.toString();


        // Setting autocommit
        this.setAutoCommit(Boolean.valueOf(properties.getProperty("autoCommit", "true")));
    }

    /**
     * Execute a list of cypher queries.
     *
     * @param queries List of cypher query object
     * @return A list of Neo4j response
     * @throws SQLException
     */
    public Neo4jResponse executeQueries(List<Neo4jStatement> queries) throws SQLException {
        // Prepare the headers query
        HttpPost request = new HttpPost(currentTransactionUrl);

        // Prepare body request
        StringEntity requestEntity = new StringEntity(Neo4jStatement.toJson(queries, this.mapper), ContentType.APPLICATION_JSON);
        request.setEntity(requestEntity);

        // Make the request
        return this.executeHttpRequest(request);
    }

    /**
     * Execute a cypher query.
     *
     * @param query Cypher query object.
     * @throws SQLException
     */
    public Neo4jResponse executeQuery(Neo4jStatement query) throws SQLException {
        List<Neo4jStatement> queries = new ArrayList<>();
        queries.add(query);
        return this.executeQueries(queries);
    }

    /**
     * Commit the current transaction.
     *
     * @throws SQLException
     */
    public void commit() throws SQLException {
        if (this.getOpenTransactionId() > 0) {
            HttpPost request = new HttpPost(currentTransactionUrl + "/commit");
            Neo4jResponse response = this.executeHttpRequest(request);
            if (response.hasErrors()) {
                throw new SQLException(response.displayErrors());
            }
            this.currentTransactionUrl = this.transactionUrl;
        } else {
            throw new SQLException("There is no transaction to commit");
        }
    }

    /**
     * Rollback the current transaction.
     *
     * @throws SQLException if there is no transaction to rollback
     */
    public void rollback() throws SQLException {
        if (this.getOpenTransactionId() > 0) {
            // Prepare the request
            HttpDelete request = new HttpDelete(currentTransactionUrl);
            Neo4jResponse response = this.executeHttpRequest(request);
            if (response.code != 200 & response.hasErrors()) {
                throw new SQLException(response.displayErrors());
            }
            this.currentTransactionUrl = this.transactionUrl;
        } else {
            throw new SQLException("There is no transaction to rollback");
        }
    }

    /**
     * Getter for AutoCommit.
     */
    public Boolean getAutoCommit() {
        return autoCommit;
    }

    /**
     * Setter for autocommit.
     *
     * @param autoCommit
     * @throws SQLException
     */
    public void setAutoCommit(Boolean autoCommit) throws SQLException {
        // we only do something if there is a change
        if (this.autoCommit != autoCommit) {

            if (autoCommit) {
                // Check if a transaction is currently opened before
                // If so, we commit it
                if (getOpenTransactionId() > 0) {
                    this.commit();
                }
                this.autoCommit = Boolean.TRUE;
                this.currentTransactionUrl = new StringBuffer().append(this.transactionUrl).append("/commit").toString();
            } else {
                this.autoCommit = Boolean.FALSE;
                this.currentTransactionUrl = this.transactionUrl;
            }
        }
    }

    /**
     * Close all thing in this object.
     */
    public void close() throws SQLException {
        try {
            http.close();
        } catch (IOException e) {
            throw new SQLException(e);
        }
    }

    /**
     * Retrieve the transaction id from an url.
     * @param url An url
     * @return The transaction id if there is an opened transaction, <code>-1</code> otherwise
     */
    protected Integer getTransactionId(String url) {
        Integer transactId = -1;
        if (url != null && url.contains(transactionUrl)) {
            String[] tab = url.split("/");
            String last = tab[tab.length - 1];
            try {
                transactId = Integer.valueOf(last);
            } catch (NumberFormatException e) {
                transactId = -1;
            }
        }
        return transactId;
    }

    /**
     * Retrieve the current transaction id.
     *
     * @return The transaction id
     */
    protected Integer getOpenTransactionId() {
        return getTransactionId(this.currentTransactionUrl);
    }

    /**
     * Give the default http client default header for Neo4j API.
     *
     * @return List of default headers.
     */
    protected Header[] getDefaultHeaders() {
        Header[] headers = new Header[2];
        headers[0] = new BasicHeader("Accept", ContentType.APPLICATION_JSON.toString());
        headers[1] = new BasicHeader("X-Stream", "true");

        return headers;
    }

    /**
     * Execute the http client request.
     *
     * @param request The request to make
     * @throws SQLException
     */
    protected Neo4jResponse executeHttpRequest(HttpRequestBase request) throws SQLException {
        Neo4jResponse result = null;

        // Adding default headers to the request
        for (Header header : this.getDefaultHeaders()) {
            request.addHeader(header.getName(), header.getValue());
        }

        // Make the request
        try (CloseableHttpResponse response = http.execute(request)) {
            result = new Neo4jResponse(response, this.mapper);
            if (result.location != null) {
                // Here we reconstruct the location in case of a proxy, but in this case you should redirect write queries to the master.
                Integer transactionId = this.getTransactionId(result.location);
                this.currentTransactionUrl = new StringBuffer().append(this.transactionUrl).append("/").append(transactionId).toString();
            }
        } catch (Exception e) {
            throw new SQLException(e);
        }

        return result;
    }

}
