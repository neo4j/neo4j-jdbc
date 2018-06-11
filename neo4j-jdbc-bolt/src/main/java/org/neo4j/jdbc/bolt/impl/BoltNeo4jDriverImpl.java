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
 */
package org.neo4j.jdbc.bolt.impl;

import org.neo4j.driver.v1.*;
import org.neo4j.jdbc.Neo4jDriver;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

import static org.neo4j.driver.v1.Config.build;

/**
 * @author AgileLARUS
 * @since 3.3.1
 */
public abstract class BoltNeo4jDriverImpl extends Neo4jDriver {

    public static final String TRUST_STRATEGY_KEY       = "trust.strategy";
    public static final String TRUSTED_CERTIFICATE_KEY  = "trusted.certificate.file";

    protected BoltNeo4jDriverImpl(String prefix) {
        super(prefix);
    }

    @Override
    public Connection connect(String url, Properties props) throws SQLException {
        if (url == null) {
            throw new SQLException("null is not a valid url");
        }
        Connection connection = null;
        if (acceptsURL(url)) {
            String boltUrl = url.replace(Neo4jDriver.JDBC_PREFIX, "").replaceAll("^(" + getPrefix() + ":)([^/])", "$1//$2");
            try {
                Properties info = parseUrlProperties(boltUrl, props);
                boltUrl = removeUrlProperties(boltUrl);
                Config.ConfigBuilder builder = build();
                if (info.containsKey("nossl")) {
                    builder = builder.withoutEncryption();
                }
                builder = setTrustStrategy(info, builder);
                Config config = builder.toConfig();
                AuthToken authToken = getAuthToken(info);
                Properties routingContext = getRoutingContext(boltUrl, info);
                boltUrl = addRoutingPolicy(boltUrl, routingContext);
                List<URI> routingUris = buildRoutingUris(boltUrl, routingContext);
                Driver driver = getDriver(routingUris, config, authToken);
                connection = BoltNeo4jConnectionImpl.newInstance(driver, info, url);
            } catch (Exception e) {
                throw new SQLException(e);
            }
        }
        return connection;
    }

    protected abstract Driver getDriver(List<URI> routingUris, Config config, AuthToken authToken) throws URISyntaxException;

    private AuthToken getAuthToken(Properties properties) {
        if (properties.isEmpty() || (!properties.containsKey("user") && !properties.containsKey("password"))) {
            return AuthTokens.none();
        }
        return AuthTokens.basic(properties.getProperty("user"), properties.getProperty("password"));
    }

    private String removeUrlProperties(String url) {
        String boltUrl = url;
        if (boltUrl.indexOf('?') != -1) {
            boltUrl = url.substring(0, url.indexOf('?'));
        }
        return boltUrl;
    }

    protected abstract Properties getRoutingContext(String url, Properties properties);

    protected abstract String addRoutingPolicy(String url, Properties properties);

    protected abstract List<URI> buildRoutingUris(String boltUrl, Properties properties) throws URISyntaxException;

    private Config.ConfigBuilder setTrustStrategy(Properties properties, Config.ConfigBuilder builder) throws SQLException {
        Config.ConfigBuilder newBuilder = builder;
        if (properties.containsKey(TRUST_STRATEGY_KEY)) {
            Config.TrustStrategy.Strategy strategy;
            try {
                strategy = Config.TrustStrategy.Strategy.valueOf((String) properties.get(TRUST_STRATEGY_KEY));
            } catch (IllegalArgumentException e) {
                throw new SQLException("Invalid value for trust.strategy param.", e);
            }
            switch (strategy) {
                case TRUST_SYSTEM_CA_SIGNED_CERTIFICATES:
                    newBuilder = builder.withTrustStrategy(Config.TrustStrategy.trustSystemCertificates());
                    break;
                case TRUST_CUSTOM_CA_SIGNED_CERTIFICATES:
                case TRUST_ON_FIRST_USE:
                case TRUST_SIGNED_CERTIFICATES:
                    newBuilder = handleTrustStrategyWithFile(properties, strategy, builder);
                    break;
                case TRUST_ALL_CERTIFICATES:
                default:
                    newBuilder = builder.withTrustStrategy(Config.TrustStrategy.trustAllCertificates());
                    break;
            }
        }
        return newBuilder;
    }

    private Config.ConfigBuilder handleTrustStrategyWithFile(Properties properties, Config.TrustStrategy.Strategy strategy, Config.ConfigBuilder builder)
            throws SQLException {
        if (properties.containsKey(TRUSTED_CERTIFICATE_KEY)) {
            Object file = properties.get(TRUSTED_CERTIFICATE_KEY);
            if (file instanceof File) {
                Config.ConfigBuilder newBuilder;
                switch (strategy) {
                    case TRUST_CUSTOM_CA_SIGNED_CERTIFICATES:
                        newBuilder = builder.withTrustStrategy(Config.TrustStrategy.trustCustomCertificateSignedBy((File) file));
                        break;
                    case TRUST_ON_FIRST_USE:
                        newBuilder = builder.withTrustStrategy(Config.TrustStrategy.trustOnFirstUse((File) file));
                        break;
                    case TRUST_SIGNED_CERTIFICATES:
                        newBuilder = builder.withTrustStrategy(Config.TrustStrategy.trustSignedBy((File) file));
                        break;
                    default:
                        newBuilder = builder;
                        break;
                }
                return newBuilder;
            } else {
                throw new SQLException("Invalid parameter 'trusted.certificate.file' : NOT A VALID FILE");
            }
        } else {
            throw new SQLException("Missing parameter 'trusted.certificate.file' : A FILE IS REQUIRED");
        }
    }
}
