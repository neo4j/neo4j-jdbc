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

import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.jdbc.Neo4jDatabaseMetaData;
import org.neo4j.jdbc.bolt.impl.BoltNeo4jConnectionImpl;
import org.neo4j.jdbc.metadata.Column;
import org.neo4j.jdbc.metadata.Table;
import org.neo4j.jdbc.utils.Neo4jInvocationHandler;

import java.lang.reflect.Proxy;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides metadata
 *
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltNeo4jDatabaseMetaData extends Neo4jDatabaseMetaData {

	/**
	 * Used for some extra logging (for example in the constructor)
	 */
	private static final Logger LOGGER = Logger.getLogger(BoltNeo4jDatabaseMetaData.class.getCanonicalName());

	private List<String> functions;

	public BoltNeo4jDatabaseMetaData(BoltNeo4jConnectionImpl connection) {
		super(connection);

		// compute database metadata: version, tables == labels, columns = properties (by label)
		if (connection != null) {
			try {
				BoltNeo4jConnection conn = (BoltNeo4jConnection) DriverManager.getConnection(connection.getUrl(), connection.getProperties());
				Session session = conn.getSession();
				getDatabaseVersion(session);
				getDatabaseLabels(session);
				getDatabaseProperties(session);
				functions = callDbmsFunctions(session);
				conn.close();
			} catch (SQLException e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}

	public static DatabaseMetaData newInstance(boolean debug, BoltNeo4jConnectionImpl connection) {
		DatabaseMetaData dbmd = new BoltNeo4jDatabaseMetaData(connection);
		return (DatabaseMetaData) Proxy.newProxyInstance(BoltNeo4jDatabaseMetaData.class.getClassLoader(), new Class[] { DatabaseMetaData.class },
				new Neo4jInvocationHandler(dbmd, debug));
	}

	private void getDatabaseVersion(Session session) {
		StatementResult rs = session.run("CALL dbms.components() yield name,versions WITH * WHERE name=\"Neo4j Kernel\" RETURN versions[0] AS version");
		if (rs != null && rs.hasNext()) {
			Record record = rs.next();
			if (record.containsKey("version")) {
				databaseVersion = record.get("version").asString();
			}
		}
	}

	private void getDatabaseLabels(Session session) {
		StatementResult rs = session.run("CALL db.labels() yield label return label");
		if (rs != null) {
			while (rs.hasNext()) {
				Record record = rs.next();
				this.databaseLabels.add(new Table(record.get("label").asString()));
			}
		}
	}

	private void getDatabaseProperties(Session session) {
		if (this.databaseLabels != null) {
			for (Table databaseLabel : this.databaseLabels) {
				StatementResult rs = session.run("MATCH (n:" + databaseLabel.getTableName() + ") WITH n LIMIT " + Neo4jDatabaseMetaData.PROPERTY_SAMPLE_SIZE
						+ " UNWIND keys(n) as key RETURN collect(distinct key) as keys");
				if (rs != null) {
					cycleResultSetToSetDatabaseProperties(rs, databaseLabel);
				}
			}
		}
	}

	private void cycleResultSetToSetDatabaseProperties(StatementResult rs, Table databaseLabel) {
		while (rs.hasNext()) {
			Record record = rs.next();
			List<Object> keys = record.get("keys").asList();
			if (keys != null) {
				for (int i = 1; i <= keys.size(); i++) {
					String key = (String) keys.get(i - 1);
					this.databaseProperties.add(new Column(databaseLabel.getTableName(), key, i));
				}
			}
		}
	}

	/**
	 * Retrieve the functions of the database through CALL
	 * If not supported: empty string
	 * @param session
	 * @return
	 */
	private List<String> callDbmsFunctions(Session session){
		List<String> functions = new ArrayList<>();
		try{
			StatementResult rs = session.run("CALL dbms.functions() " +
					"YIELD name, signature\n" +
					"RETURN name \n" +
					"ORDER BY name ASC");
			while (rs != null && rs.hasNext()) {
				Record record = rs.next();
				functions.add(record.get("name").asString());
			}

			return functions;
		}catch(Exception e){
			return Collections.EMPTY_LIST;
		}
	}

	@Override
	public String getSystemFunctions() throws SQLException {
		return String.join(",",functions);
	}
}

