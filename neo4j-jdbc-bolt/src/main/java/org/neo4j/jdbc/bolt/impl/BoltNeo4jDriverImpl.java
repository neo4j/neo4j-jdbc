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

import org.neo4j.driver.v1.AuthToken;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.Driver;
import org.neo4j.jdbc.Neo4jDriver;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.neo4j.driver.v1.Config.build;

/**
 * @author AgileLARUS
 * @since 3.3.1
 */
public abstract class BoltNeo4jDriverImpl extends Neo4jDriver {

    public static final String TRUST_STRATEGY_KEY       = "trust.strategy";
    public static final String TRUSTED_CERTIFICATE_KEY  = "trusted.certificate.file";
    public static final String CONNECTION_ACQUISITION_TIMEOUT = "connection.acquisition.timeout";
    public static final String CONNECTION_LIVENESS_CHECK_TIMEOUT = "connection.liveness.check.timeout";
    public static final String CONNECTION_TIMEOUT = "connection.timeout";
    public static final String ENCRYPTION = "encryption";
    public static final String LEAKED_SESSIONS_LOGGING = "leaked.sessions.logging";
    public static final String LOAD_BALANCING_STRATEGY = "load.balancing.strategy";
    public static final String MAX_CONNECTION_LIFETIME = "max.connection.lifetime";
    public static final String MAX_CONNECTION_POOLSIZE = "max.connection.poolsize";
    public static final String MAX_TRANSACTION_RETRY_TIME = "max.transaction.retry.time";

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
                Properties info = mergeUrlAndInfo(boltUrl, props);

                boltUrl = removeUrlProperties(boltUrl);
                Config.ConfigBuilder builder = build();
                if (info.containsKey("nossl")) {
                    builder = builder.withoutEncryption();
                }

                builder = setTrustStrategy(info, builder);
                builder = setConnectionAcquisitionTimeout(info, builder);
                builder = setIdleTimeBeforeConnectionTest(info, builder);
                builder = setConnectionTimeout(info, builder);
                builder = setEncryption(info, builder);
                builder = setLakedSessionLogging(info, builder);
                builder = setLoadBalancingStrategy(info, builder);
                builder = setMaxConnectionLifetime(info, builder);
                builder = setMaxConnectionPoolSize(info, builder);
                builder = setMaxTransactionRetryTime(info, builder);

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
        if (!properties.containsKey("user") ) {
            if(properties.containsKey("password")){
                //if only password is provided, try to authenticate with the default user: 'neo4j'
                return AuthTokens.basic("neo4j", properties.getProperty("password"));
            }
            //neither user nor password
            return AuthTokens.none();
        }
        //user provided, it need a password
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

    /**
     * Mix the properties from input and url, using the URL value when conflicts
     * @param url The url of the connection
     * @param params The properties passed in the connect method
     * @return the merge of the properties from url and input
     */
    protected Properties mergeUrlAndInfo(String url, Properties params) {
        Properties fromInput = (params==null)?new Properties():params;
        Properties fromUrl = super.parseUrlProperties(url, null);

        Properties merge = (Properties) fromInput.clone();

        Set<String> keys = fromUrl.stringPropertyNames();
        for (String key : keys) {
            merge.put(key, fromUrl.get(key));
        }

        setUserInfo(merge);

        return merge;
    }

    /**
     * If there're properties values for the Username, it sets the "user" property with that value
     * @param properties
     */
    protected void setUserInfo(Properties properties) {
        // 'username' key has highest priority over 'user' key
        String user = properties.getProperty("username");
        if(user == null){
            user = properties.getProperty("user");
        }

        if (user!=null && !user.trim().isEmpty()) {
            properties.setProperty("user",user);
        }
    }

    private Config.ConfigBuilder handleTrustStrategyWithFile(Properties properties, Config.TrustStrategy.Strategy strategy, Config.ConfigBuilder builder)
            throws SQLException {
        if (properties.containsKey(TRUSTED_CERTIFICATE_KEY)) {
            String value = properties.getProperty(TRUSTED_CERTIFICATE_KEY);
            File file = new File(value);
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
            throw new SQLException("Missing parameter 'trusted.certificate.file' : A FILE IS REQUIRED");
        }
    }

    /**
     * Get a value from the properties and try to apply it to the builder
     * @param info
     * @param builder
     * @param key
     * @param op
     * @param errorMessage
     * @return
     */
    private Config.ConfigBuilder setValueConfig(Properties info, Config.ConfigBuilder builder, String key, Function<String,Config.ConfigBuilder> op, String errorMessage) {
        if(info.containsKey(key)){
            String value = info.getProperty(key);
            try{
                return op.apply(value);
            }catch(Exception e){
                throw new IllegalArgumentException(key+": "+value+" "+errorMessage);
            }

        }
        return builder;
    }

    /**
     * Get a long value from the properties and apply it to the builder
     * @param info
     * @param builder
     * @param key
     * @param op
     * @return
     */
    private Config.ConfigBuilder setLongConfig(Properties info, Config.ConfigBuilder builder, String key, Function<Long,Config.ConfigBuilder> op) {
        return setValueConfig(info, builder, key, (val)->op.apply(Long.parseLong(val)), "is not a number");
    }

    /**
     * Get a boolean value from the properties and apply it to the builder
     * @param info
     * @param builder
     * @param key
     * @param op
     * @return
     */
    private Config.ConfigBuilder setBooleanConfig(Properties info, Config.ConfigBuilder builder, String key, Function<Boolean,Config.ConfigBuilder> op) {
        return setValueConfig(info, builder, key, (val)->{
            if ("true".equalsIgnoreCase(val) || "false".equalsIgnoreCase(val)) {
                return op.apply(Boolean.parseBoolean(val));
            }else{
                throw new IllegalArgumentException();
            }
        }, "is not a boolean");
    }

    /**
     * Configure CONNECTION_ACQUISITION_TIMEOUT
     * @param info
     * @param builder
     * @return always a builder
     */
    private Config.ConfigBuilder setConnectionAcquisitionTimeout(Properties info, Config.ConfigBuilder builder) {
        return setLongConfig(info, builder, CONNECTION_ACQUISITION_TIMEOUT, (ms)->builder.withConnectionAcquisitionTimeout(ms, TimeUnit.MILLISECONDS));
    }

    /**
     * Configure CONNECTION_LIVENESS_CHECK_TIMEOUT
     * @param info
     * @param builder
     * @return always a builder
     */
    private Config.ConfigBuilder setIdleTimeBeforeConnectionTest(Properties info, Config.ConfigBuilder builder) {
        return setLongConfig(info, builder, CONNECTION_LIVENESS_CHECK_TIMEOUT, (ms)->builder.withConnectionLivenessCheckTimeout(ms, TimeUnit.MINUTES));
    }

    /**
     * Configure CONNECTION_TIMEOUT
     * @param info
     * @param builder
     * @return always a builder
     */
    private Config.ConfigBuilder setConnectionTimeout(Properties info, Config.ConfigBuilder builder) {
        return setLongConfig(info, builder, CONNECTION_TIMEOUT, (ms)->builder.withConnectionTimeout(ms, TimeUnit.MILLISECONDS));
    }

    /**
     * Configure ENCRYPTION
     * @param info
     * @param builder
     * @return always a builder
     */
    private Config.ConfigBuilder setEncryption(Properties info, Config.ConfigBuilder builder) {
        return setBooleanConfig(info, builder, ENCRYPTION, (condition)-> (condition)?builder.withEncryption():builder.withoutEncryption());
    }

    /**
     * Configure LEAKED_SESSIONS_LOGGING
     * @param info
     * @param builder
     * @return always a builder
     */
    private Config.ConfigBuilder setLakedSessionLogging(Properties info, Config.ConfigBuilder builder) {
        return setBooleanConfig(info, builder, LEAKED_SESSIONS_LOGGING, (condition)-> (condition)?builder.withLeakedSessionsLogging():builder);
    }

    /**
     * Configure LOAD_BALANCING_STRATEGY
     * @param info
     * @param builder
     * @return always a builder
     */
    private Config.ConfigBuilder setLoadBalancingStrategy(Properties info, Config.ConfigBuilder builder) {
        return setValueConfig(info, builder, LOAD_BALANCING_STRATEGY, (val)->builder.withLoadBalancingStrategy(Config.LoadBalancingStrategy.valueOf(val)),"is not a load balancing strategy");
    }

    /**
     * Configure MAX_CONNECTION_LIFETIME
     * @param info
     * @param builder
     * @return always a builder
     */
    private Config.ConfigBuilder setMaxConnectionLifetime(Properties info, Config.ConfigBuilder builder) {
        return setLongConfig(info, builder, MAX_CONNECTION_LIFETIME, (ms)->builder.withMaxConnectionLifetime(ms, TimeUnit.MILLISECONDS));
    }

    /**
     * Configure MAX_CONNECTION_POOLSIZE
     * @param info
     * @param builder
     * @return always a builder
     */
    private Config.ConfigBuilder setMaxConnectionPoolSize(Properties info, Config.ConfigBuilder builder) {
        return setValueConfig(info, builder, MAX_CONNECTION_POOLSIZE, (val)->builder.withMaxConnectionPoolSize(Integer.parseInt(val)),"is not a number");
    }

    /**
     * Configure MAX_TRANSACTION_RETRY_TIME
     * @param info
     * @param builder
     * @return always a builder
     */
    private Config.ConfigBuilder setMaxTransactionRetryTime(Properties info, Config.ConfigBuilder builder) {
        return setLongConfig(info, builder, MAX_TRANSACTION_RETRY_TIME, (ms)->builder.withMaxTransactionRetryTime(ms, TimeUnit.MILLISECONDS));
    }
}
