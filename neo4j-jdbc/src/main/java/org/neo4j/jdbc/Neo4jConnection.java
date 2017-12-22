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
 * Created on 03/02/16
 */
package org.neo4j.jdbc;

import java.util.Properties;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public interface Neo4jConnection extends java.sql.Connection {

	/**
	 * Get the connection url.
	 *
	 * @return String the connection url
	 */
	String getUrl();

	/**
	 * Get the properties for this connection.
	 *
	 * @return Properties the properties for this connection
	 */
	Properties getProperties();

	/**
	 * Get the user of this connection.
	 *
	 * @return String
	 */
	String getUserName();

	/**
	 * Get the flattening sample rows (-1 if no flattening).
	 *
	 * @return int
	 */
	int getFlattening();

}
