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
package it.larusba.neo4j.jdbc.http;

import it.larusba.neo4j.jdbc.Driver;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * JDBC Driver class for the HTTP connector.
 */
public class HttpDriver extends Driver {

    // Register the driver class
    static {
        try {
            DriverManager.registerDriver(new HttpDriver());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Default constructor.
     */
    public HttpDriver() {
        super("http");
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        Connection connection = null;
        try {
            if (acceptsURL(url)) {
                URL neo4jUrl = new URL(url.replace("jdbc:", ""));
                info = (info == null ? info : new Properties());
                parseUrlProperties(url, info);
                String host = neo4jUrl.getHost();
                int port = 7474;
                if (neo4jUrl.getPort() > 0) {
                    port = neo4jUrl.getPort();
                }
                connection = InstanceFactory.debug(HttpConnection.class, new HttpConnection(host, port, info), HttpConnection.hasDebug(info));
            }
        } catch (MalformedURLException e) {
            throw new SQLException(e);
        }
        return connection;
    }

}
