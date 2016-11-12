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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A POJO that store a Neo4j response from the cypher endpoint.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class Neo4jResponse {

	/**
	 * HTTP code
	 */
	private Integer code;

	/**
	 * Transaction url receive in case of a ne one.
	 */
	private String location;

	/**
	 * List of query result.
	 */
	private List<Neo4jResult> results;

	/**
	 * List of Neo4j error.
	 */
	private List<SQLException> errors;

	/**
	 * Construct the object directly from the HttpResponse.
	 *
	 * @param response Http response
	 * @param mapper   Jackson object mapper
	 * @throws SQLException sqlexception
	 */
	public Neo4jResponse(HttpResponse response, ObjectMapper mapper) throws SQLException {
		// Parse response headers
		if (response.getStatusLine() != null) {

			// Save the http code
			this.code = response.getStatusLine().getStatusCode();

			// If status code is 201, then we retrieve the Location header to keep the transaction url.
			if (this.code == HttpStatus.SC_CREATED) {
				this.location = response.getFirstHeader("Location").getValue();
			}

			// Parsing the body
			HttpEntity json = response.getEntity();
			if (json != null) {
				try (InputStream is = json.getContent()) {
					Map body = mapper.readValue(is, Map.class);

					// Error parsing
					this.errors = new ArrayList<>();
					for (Map<String, String> error : (List<Map<String, String>>) body.get("errors")) {
						String message = "";
						String code = "";
						if(error.get("message") != null) {
							message = error.get("message");
						}
						if(error.get("code") != null) {
							code = error.get("code");
						}
						errors.add(new SQLException(message, code));
					}

					// Data parsing
					this.results = new ArrayList<>();
					if (body.containsKey("results")) {
						for (Map map : (List<Map>) body.get("results")) {
							results.add(new Neo4jResult(map));
						}
					}

				} catch (Exception e) {
					throw new SQLException(e);
				}
			}

		} else {
			throw new SQLException("Receive request without status code ...");
		}
	}
	
	/**
	 * @return the code
	 */
	public Integer getCode() {
		return this.code;
	}
	
	/**
	 * @return the location
	 */
	public String getLocation() {
		return this.location;
	}
	
	/**
	 * @return the results
	 */
	public List<Neo4jResult> getResults() {
		return this.results;
	}
	
	/**
	 * @return the first result
	 */
	public Neo4jResult getFirstResult() {
		if (this.results != null && this.results.size() > 0)
			return this.results.get(0);
		else
			return null;
	}
	
	/**
	 * @return the errors
	 */
	public List<SQLException> getErrors() {
		return this.errors;
	}

	/**
	 * Is this response has errors ?
	 * @return true is there're errors
	 */
	public boolean hasErrors() {
		Boolean errors = Boolean.FALSE;
		if (this.errors != null && this.errors.size() > 0) {
			errors = Boolean.TRUE;
		}
		return errors;
	}

	/**
	 * Has this response at least one result set ?
	 * @return true is there're result sets
	 */
	public boolean hasResultSets() {
		Boolean hasResultSets = Boolean.FALSE;
		if (this.results != null && this.results.size() > 0) {
			for (int i = 0; i < this.results.size() && !hasResultSets; i++) {
				Neo4jResult result = this.results.get(i);
				if ((result.getColumns() != null && result.getColumns().size() > 0) || (result.getRows() != null && result.getRows().size() > 0)) {
				  hasResultSets = Boolean.TRUE;
				}
			}
		}
		return hasResultSets;
	}

	/**
	 * Transform the error list to a string for display purpose.
	 * @return A String with all errors
	 */
	public String displayErrors() {
		StringBuilder sb = new StringBuilder();
		if (hasErrors()) {
			sb.append("Some errors occurred : \n");
			for (SQLException error : errors) {
				sb.append("[").append(error.getSQLState()).append("]").append(":").append(error.getMessage()).append("\n");
			}
		}
		return sb.toString();
	}

}
