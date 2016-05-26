/**
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
 * Created on 23/02/16
 */
package org.neo4j.jdbc.bolt;

import org.neo4j.jdbc.BaseDriver;
import org.neo4j.jdbc.InstanceFactory;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.GraphDatabase;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import static org.neo4j.driver.v1.Config.build;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltDriver extends BaseDriver {

	public final static String JDBC_BOLT_PREFIX = "bolt";

    /**
     * Default constructor.
     */
    public BoltDriver() throws SQLException {
        super(JDBC_BOLT_PREFIX);
    }

	@Override public Connection connect(String url, Properties props) throws SQLException {
		if (url == null) {
			throw new SQLException("null is not a valid url");
		}
		Connection connection = null;
		if (acceptsURL(url)) {
			url = url.replace("jdbc:", "");
			try {
				Properties info = parseUrlProperties(url, props);
				if (!info.containsKey("noSsl")) {
					connection = InstanceFactory.debug(BoltConnection.class, new BoltConnection(GraphDatabase.driver(url,
							(info.containsKey("user") && info.containsKey("password") ?
									AuthTokens.basic(info.getProperty("user"), info.getProperty("password")) :
									AuthTokens.none())).session(), info), BoltConnection.hasDebug(info));
				} else {
					Config.ConfigBuilder builder = build();
					builder.withEncryptionLevel(Config.EncryptionLevel.NONE);
					Config config = builder.toConfig();
					connection = InstanceFactory.debug(BoltConnection.class, new BoltConnection(GraphDatabase.driver(url,
							(info.containsKey("user") && info.containsKey("password") ?
									AuthTokens.basic(info.getProperty("user"), info.getProperty("password")) :
									AuthTokens.none()), config).session(), info), BoltConnection.hasDebug(info));
				}
			} catch (Exception e) {
				throw new SQLException(e);
			}
		}
		return connection;
	}


}
