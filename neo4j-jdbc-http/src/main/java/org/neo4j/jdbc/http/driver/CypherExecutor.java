/*
 * Copyright (c) 2016 LARUS Business Automation [http://www.larus-ba.it]
 * <p>
 * This file is part of the "LARUS Integration Framework for Neo4j".
 * <p>
 * The "LARUS Integration Framework for Neo4j" is licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * Created on 15/4/2016
 */
package org.neo4j.jdbc.http.driver;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.neo4j.jdbc.Neo4jDatabaseMetaData.GET_DBMS_FUNCTIONS;
import static org.neo4j.jdbc.Neo4jDatabaseMetaData.GET_DBMS_FUNCTIONS_V3;

/**
 * Execute cypher queries.
 */
public class CypherExecutor {

	/**
	 * URL of the transaction endpoint.
	 */
	final String transactionUrl;

	/**
	 * If we are in https or not.
	 */
	private Boolean secure;

	/**
	 * Autocommit transaction.
	 * Must be null at creation time.
	 * Initiation of this property is made by its setter, that is called from the constructor.
	 */
	private Boolean autoCommit;

	/**
	 * The http client.
	 */
	private CloseableHttpClient http;

	/**
	 * URL of the current transaction.
	 */
	private String currentTransactionUrl;

	/**
	 * Name of the database (default: neo4j)
	 */
	private final String databaseName;

	/**
	 * Jackson mapper object.
	 */
	private static final ObjectMapper mapper = new ObjectMapper();

	private static final String DB_DATA_TRANSACTION_TEMPLATE = "/db/%s/tx";

	private static final Logger LOGGER = Logger.getLogger(CypherExecutor.class.getCanonicalName());

	static {
		mapper.configure(DeserializationFeature.USE_LONG_FOR_INTS, true);
	}

	private class PreemptiveAuthInterceptor implements HttpRequestInterceptor {
		public void process(final HttpRequest request, final HttpContext context) throws HttpException {
			AuthState authState = (AuthState) context.getAttribute(HttpClientContext.TARGET_AUTH_STATE);

			// If no auth scheme available yet, try to initialize it preemptively
			if (authState.getAuthScheme() == null) {
				CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(HttpClientContext.CREDS_PROVIDER);
				HttpHost targetHost = (HttpHost) context.getAttribute(HttpClientContext.HTTP_TARGET_HOST);
				Credentials creds = credsProvider.getCredentials(new AuthScope(targetHost.getHostName(), targetHost.getPort()));
				if (creds == null) {
					throw new HttpException("No credentials for preemptive authentication");
				}
				authState.update(new BasicScheme(), creds);
			}
		}
	}

	/**
	 * Default constructor.
	 *
	 * @param host       Hostname of the Neo4j instance.
	 * @param port       HTTP port of the Neo4j instance.
	 * @param secure	 If the connection used SSL.
	 * @param properties Properties of the url connection.
	 * @throws SQLException sqlexception
	 */
	public CypherExecutor(String host, Integer port, Boolean secure, Properties properties) throws SQLException {
		this.secure = secure;

		// Create the http client builder
		HttpClientBuilder builder = HttpClients.custom();

		// Setting user-agent
		String userAgent = properties.getProperty("useragent");
		builder.setUserAgent(getUserAgent(userAgent));

		// Adding authentication to the http client if needed
		try {
			if (isAuthenticationRequired(host, port, secure, properties)) {
				CredentialsProvider credentialsProvider = getCredentialsProvider(host, port, properties);
				if (credentialsProvider == null) {
					throw new SQLException("Authentication required");
				}
				builder.setDefaultCredentialsProvider(credentialsProvider);
				builder.addInterceptorFirst(new PreemptiveAuthInterceptor());
			}
		} catch (SQLException e) {
			throw e;
		} catch (Exception e) {
			throw new SQLException(e.getMessage());
		}
		// Create the http client
		this.http = builder.build();

		// Create the url endpoint
		this.databaseName = String.valueOf(properties.getOrDefault("database", "neo4j"));
		this.transactionUrl = createTransactionUrl(this.databaseName, host, port, this.secure);

		// Setting autocommit
		this.setAutoCommit(Boolean.valueOf(properties.getProperty("autoCommit", "true")));
	}

	public String getUserAgent(String userAgent) {
		return "Neo4j JDBC Driver" + (userAgent != null ? " via "+userAgent : "");
	}

	public boolean isAuthenticationRequired(String host, Integer port, Boolean secure, Properties properties) throws Exception {
		HttpUriRequest request = RequestBuilder.head()
				.setUri(new URL(secure ? "https" : "http", host, port, "/db/data/").toURI())
				.build();
		try (CloseableHttpClient minimalHttp = HttpClients
				.custom()
				.disableAutomaticRetries()
				.setUserAgent(getUserAgent(properties.getProperty("useragent")))
				.build()) {
			HttpResponse response = minimalHttp.execute(request);
			return response.getStatusLine().getStatusCode() == 401;
		}
	}

	private String createTransactionUrl(String databaseName, String host, Integer port, Boolean secure) throws SQLException {
		String transactionPath = transactionPath(databaseName);
		try {
			if (secure) {
				return new URL("https", host, port, transactionPath).toString();
			}
			return new URL("http", host, port, transactionPath).toString();
		}
		catch (MalformedURLException e) {
			throw new SQLException("Invalid server URL", e);
		}
	}

	private CredentialsProvider getCredentialsProvider(String host, Integer port, Properties properties) {
		if (properties.containsKey("password")) {
			String user = properties.getProperty("user", properties.getProperty("username", "neo4j"));
			CredentialsProvider credsProvider = new BasicCredentialsProvider();
			UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(user, properties.getProperty("password"));
			credsProvider.setCredentials(new AuthScope(host, port), credentials);
			return credsProvider;
		}
		return null;
	}

	/**
	 * Execute a list of cypher queries.
	 *
	 * @param queries List of cypher query object
	 * @return the response for these queries
	 * @throws SQLException sqlexception
	 */
	public Neo4jResponse executeQueries(List<Neo4jStatement> queries) throws SQLException {
		// Prepare the headers query
		HttpPost request = new HttpPost(currentTransactionUrl);

		// Prepare body request
		StringEntity requestEntity = new StringEntity(Neo4jStatement.toJson(queries, mapper), ContentType.APPLICATION_JSON);
		request.setEntity(requestEntity);

		// Make the request
		return this.executeHttpRequest(request);
	}

	public List<String> callDbmsFunctions() {
		try {
			String serverVersion = getServerVersion();
			Neo4jResponse response = this.executeQuery(new Neo4jStatement(serverVersion.startsWith("3") ?
					GET_DBMS_FUNCTIONS_V3 : GET_DBMS_FUNCTIONS, Collections.emptyMap(), false));
			if (response.hasErrors()) {
				return Collections.emptyList();
			}
			return response.getResults()
					.stream()
					.flatMap(result ->
							result.getRows()
									.stream()
									.map(rows -> {
										Iterable<?> rowData = (Iterable<?>) rows.get("row");
										return (String) rowData.iterator().next();
									}))
					.distinct()
					.collect(Collectors.toList());
		} catch (SQLException e) {
			LOGGER.warning(String.format("Could not retrieve DBMS functions:%n%s", e));
			return Collections.emptyList();
		}
	}

	/**
	 * Execute a cypher query.
	 *
	 * @param query Cypher query object.
	 * @return the response for this query
	 * @throws SQLException sqlexception
	 */
	public Neo4jResponse executeQuery(Neo4jStatement query) throws SQLException {
		List<Neo4jStatement> queries = new ArrayList<>();
		queries.add(query);
		return this.executeQueries(queries);
	}

	/**
	 * Commit the current transaction.
	 *
	 * @throws SQLException sqlexception
	 */
	public void commit() throws SQLException {
		if (this.getOpenTransactionId() > 0) {
			HttpPost request = new HttpPost(currentTransactionUrl + "/commit");
			Neo4jResponse response = this.executeHttpRequest(request);
			if (response.hasErrors()) {
				throw new SQLException(response.displayErrors());
			}
			this.currentTransactionUrl = this.transactionUrl;
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
			if (response.getCode() != 200 & response.hasErrors()) {
				throw new SQLException(response.displayErrors());
			}
			this.currentTransactionUrl = this.transactionUrl;
		}
	}

	/**
	 * Getter for AutoCommit.
	 *
	 * @return true if statement are automatically committed
	 */
	public Boolean getAutoCommit() {
		return autoCommit;
	}

	/**
	 * Setter for autocommit.
	 *
	 * @param autoCommit enable/disable autocommit
	 * @throws SQLException sqlexception
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
				this.currentTransactionUrl = this.transactionUrl + "/commit";
			} else {
				this.autoCommit = Boolean.FALSE;
				this.currentTransactionUrl = this.transactionUrl;
			}
		}
	}

	/**
	 * Retrieve the Neo4j version from the server.
	 *
	 * @return A string that represent the neo4j server version
	 */
	public String getServerVersion() {
		try {
			return getServerVersion("/db/data");
		} catch (Exception e) {
			return getServerVersion("");
		}
	}

	private String getServerVersion(String dbEndpointSuffix) {
		String result = null;

		// Prepare the headers query

		HttpGet request = new HttpGet(this.transactionUrl
				.replace(transactionPath(this.databaseName), dbEndpointSuffix));

		// Adding default headers to the request
		for (Header header : this.getDefaultHeaders()) {
			request.addHeader(header.getName(), header.getValue());
		}

		// Make the request
		try (CloseableHttpResponse response = http.execute(request)) {
			try (InputStream is = response.getEntity().getContent()) {
				Map body = mapper.readValue(is, Map.class);
				final String neo4j_version = (String) body.get("neo4j_version");
				if (neo4j_version != null) {
					result = neo4j_version;
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return result;
	}

	/**
	 * Close all thing in this object.
	 * @throws SQLException sqlexception
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
	 *
	 * @param url An url
	 * @return The transaction id if there is an opened transaction, <code>-1</code> otherwise
	 */
	Integer getTransactionId(String url) {
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
	 * @return The transaction id, or <code>-1</code> if there is no transaction.
	 */
	public Integer getOpenTransactionId() {
		return getTransactionId(this.currentTransactionUrl);
	}

	private String transactionPath(String databaseName) {
		return String.format(DB_DATA_TRANSACTION_TEMPLATE, databaseName);
	}

	/**
	 * Give the default http client default header for Neo4j API.
	 *
	 * @return List of default headers.
	 */
	private Header[] getDefaultHeaders() {
		Header[] headers = new Header[2];
		headers[0] = new BasicHeader("Accept", ContentType.APPLICATION_JSON.toString());
		headers[1] = new BasicHeader("X-Stream", "true");

		return headers;
	}

	/**
	 * Execute the http client request.
	 *
	 * @param request The request to make
	 */
	private Neo4jResponse executeHttpRequest(HttpRequestBase request) throws SQLException {
		Neo4jResponse result;

		// Adding default headers to the request
		for (Header header : this.getDefaultHeaders()) {
			request.addHeader(header.getName(), header.getValue());
		}

		// Make the request
		try (CloseableHttpResponse response = http.execute(request)) {
			result = new Neo4jResponse(response, mapper);
			if(!getAutoCommit()) {
				if (result.hasErrors()) {
					// The transaction *was* rolled back server-side. Whether a transaction existed or not before, it should
					// now be considered rolled back on this side as well.
					this.currentTransactionUrl = this.transactionUrl;
				} else if (result.getLocation() != null) {
					// Here we reconstruct the location in case of a proxy, but in this case you should redirect write queries to the master.
					Integer transactionId = this.getTransactionId(result.getLocation());
					this.currentTransactionUrl = this.transactionUrl + "/" + transactionId;
				}
			}
		} catch (Exception e) {
			throw new SQLException(e);
		}

		return result;
	}

}
