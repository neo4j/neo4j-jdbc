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
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.*;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import sun.misc.BASE64Encoder;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;

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
	 * Jackson mapper object.
	 */
	private static final ObjectMapper mapper = new ObjectMapper();

	private static final String DB_DATA_TRANSACTION = "/db/data/transaction";

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
		this.transactionUrl = createTransactionUrl(host, port, this.secure);

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

	private String createTransactionUrl(String host, Integer port, Boolean secure) throws SQLException {
		try {
			if(secure)
				return new URL("https", host, port, DB_DATA_TRANSACTION).toString();
			else
				return new URL("http", host, port, DB_DATA_TRANSACTION).toString();
		} catch (MalformedURLException e) {
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
		String result = null;

		// Prepare the headers query
		HttpGet request = new HttpGet(this.transactionUrl.replace(DB_DATA_TRANSACTION, "/db/manage/server/version"));

		// Adding default headers to the request
		for (Header header : this.getDefaultHeaders()) {
			request.addHeader(header.getName(), header.getValue());
		}

		// Make the request
		try (CloseableHttpResponse response = http.execute(request)) {
			try (InputStream is = response.getEntity().getContent()) {
				Map body = mapper.readValue(is, Map.class);
				if (body.get("version") != null) {
					result = (String) body.get("version");
				}
			}
		} catch (Exception e) {
			result = "Unknown";
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
