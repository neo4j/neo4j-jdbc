/*
 * Copyright (c) 2023-2024 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.jdbc.benchkit;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.neo4j.jdbc.Neo4jPreparedStatement;
import org.neo4j.jdbc.benchkit.api.WorkloadApiDelegate;
import org.neo4j.jdbc.benchkit.model.Workload;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.stereotype.Service;

@Service
public class JdbcBasedWorkloadApiDelegate implements WorkloadApiDelegate {

	private final JdbcTemplate jdbcTemplate;

	public JdbcBasedWorkloadApiDelegate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Autowired
	DataSourceProperties f;

	@Override
	public ResponseEntity<Void> executeSuppliedWorkload(Workload workload) {
		if (workload.getMode() != Workload.ModeEnum.SEQUENTIAL_SESSIONS
				&& workload.getMode() != Workload.ModeEnum.SEQUENTIAL_QUERIES
				&& workload.getMode() != Workload.ModeEnum.SEQUENTIAL_TRANSACTIONS) {
			return ResponseEntity.badRequest().build();
		}
		if (!"neo4j".equalsIgnoreCase(
				Optional.ofNullable(workload.getDatabase()).filter(Predicate.not(String::isBlank)).orElse("neo4j"))) {
			return ResponseEntity.badRequest().build();
		}

		try {
			workload.getQueries().forEach(query -> {
				this.jdbcTemplate.execute(query.getText(), (PreparedStatementCallback<Void>) ps -> {
					if (query.getParameters() != null && query.getParameters() instanceof Map<?, ?> parameters) {
						var nps = ps.unwrap(Neo4jPreparedStatement.class);
						for (Map.Entry<?, ?> entry : parameters.entrySet()) {
							Object k = entry.getKey();
							Object v = entry.getValue();
							nps.setObject((String) k, v);
						}
					}
					ps.execute();
					return null;
				});
			});
		}
		catch (Exception ex) {
			return ResponseEntity.internalServerError().build();
		}
		return ResponseEntity.noContent().build();
	}

}
