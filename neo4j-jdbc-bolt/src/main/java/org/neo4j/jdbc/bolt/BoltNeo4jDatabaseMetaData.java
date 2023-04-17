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
 * Created on 01/03/2016
 */
package org.neo4j.jdbc.bolt;

import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.TransactionWork;
import org.neo4j.jdbc.Neo4jDatabaseMetaData;
import org.neo4j.jdbc.bolt.impl.BoltNeo4jConnectionImpl;
import org.neo4j.jdbc.metadata.Column;
import org.neo4j.jdbc.metadata.Table;
import org.neo4j.jdbc.utils.BoltNeo4jUtils;
import org.neo4j.jdbc.utils.Neo4jInvocationHandler;

import java.lang.reflect.Proxy;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Provides metadata
 *
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltNeo4jDatabaseMetaData extends Neo4jDatabaseMetaData {

	private static final String DB_PROPERTIES_QUERY = "MATCH (n:`%s`) WITH n LIMIT %d UNWIND keys(n) as key RETURN collect(distinct key) as keys";

	/**
	 * Used for some extra logging (for example in the constructor)
	 */
	private static final Logger LOGGER = Logger.getLogger(BoltNeo4jDatabaseMetaData.class.getCanonicalName());

	private List<String> functions;

	public BoltNeo4jDatabaseMetaData(BoltNeo4jConnectionImpl connection) {
		super(connection);

		// compute database metadata: version, tables == labels, columns = properties (by label)
		if (connection != null) {
			Session session = null;
			try {
				session = connection.newNeo4jSession();
				getDatabaseVersion(session);
				getDatabaseLabels(session);
				getDatabaseProperties(session);
				functions = callDbmsFunctions(session);
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}finally{
				BoltNeo4jUtils.closeSafely(session, LOGGER);
			}
		}
	}

	public static DatabaseMetaData newInstance(boolean debug, BoltNeo4jConnectionImpl connection) {
		DatabaseMetaData dbmd = new BoltNeo4jDatabaseMetaData(connection);
		return (DatabaseMetaData) Proxy.newProxyInstance(BoltNeo4jDatabaseMetaData.class.getClassLoader(), new Class[] { DatabaseMetaData.class },
				new Neo4jInvocationHandler(dbmd, debug));
	}

	private void getDatabaseVersion(Session session) {
		this.databaseVersion = session.readTransaction(tx -> {
			Result records = tx.run("CALL dbms.components() yield name,versions WITH * WHERE name=\"Neo4j Kernel\" RETURN versions[0] AS version");
			if (!records.hasNext()) {
				return null;
			}
			return records.next().get("version").asString();
		});
	}

	private void getDatabaseLabels(Session session) {
		this.databaseLabels = session.readTransaction(tx ->
				tx.run("CALL db.labels() YIELD label RETURN label")
						.list(record -> new Table(record.get("label").asString())));
	}

	private void getDatabaseProperties(Session session) {
		if (this.databaseLabels == null) {
			return;
		}
		List<Column> properties = new ArrayList<>(this.databaseLabels.size() * 3);
		for (Table label : this.databaseLabels) {
			properties.addAll(session.readTransaction(getColumnSample(label.getTableName())));
		}
		this.databaseProperties = properties;
	}

	private TransactionWork<List<Column>> getColumnSample(String label) {
		return tx -> {
			String query = String.format(DB_PROPERTIES_QUERY, label, Neo4jDatabaseMetaData.PROPERTY_SAMPLE_SIZE);
			AtomicInteger keyIndex = new AtomicInteger(0);
			return tx.run(query)
					.stream()
					.flatMap(record -> record.get("keys").asList().stream())
					.map(key -> new Column(label, (String) key, keyIndex.getAndIncrement()))
					.collect(Collectors.toList());
		};
	}

	/**
	 * Retrieve the functions of the database through CALL
	 * If not supported: empty string
	 * @param session
	 * @return
	 */
	private List<String> callDbmsFunctions(Session session){
		try {
			return session.readTransaction(tx -> {
				List<String> functions = new ArrayList<>();
				Result rs = tx.run(this.databaseVersion.startsWith("3") ? GET_DBMS_FUNCTIONS_V3 : GET_DBMS_FUNCTIONS);
				while (rs != null && rs.hasNext()) {
					Record record = rs.next();
					functions.add(record.get("name").asString());
				}
				return functions;
			});
		} catch (Exception e) {
			LOGGER.warning(String.format("Could not retrieve DBMS functions:%n%s", e));
			return Collections.emptyList();
		}
	}

	@Override
	public String getSystemFunctions() throws SQLException {
		return String.join(",",functions);
	}
}

