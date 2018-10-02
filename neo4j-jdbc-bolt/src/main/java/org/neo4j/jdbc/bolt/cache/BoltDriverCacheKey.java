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
 * Created on 02/10/18
 */
package org.neo4j.jdbc.bolt.cache;

import org.neo4j.driver.v1.AuthToken;
import org.neo4j.driver.v1.Config;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/**
 * Implement the logic to cache driver instances (equals method)
 */
public class BoltDriverCacheKey {

    private List<URI> routingUris;
    private Config config;
    private AuthToken authToken;
    private Properties info;

    /**
     * Create the key as a combination of all the connection values
     * @param routingUris
     * @param config
     * @param authToken
     * @param info
     */
    public BoltDriverCacheKey(List<URI> routingUris, Config config, AuthToken authToken,Properties info) {
        this.routingUris = routingUris;
        this.config = config;
        this.authToken = authToken;
        this.info = info;
    }

    @Override
    public int hashCode() {
        // use equals
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BoltDriverCacheKey that = (BoltDriverCacheKey) o;
        return Objects.equals(routingUris, that.routingUris) &&
                compareConfig(config, that.config) &&
                Objects.equals(authToken, that.authToken) &&
                Objects.equals(info, that.info)
                ;
    }

    public List<URI> getRoutingUris() {
        return routingUris;
    }

    public Config getConfig() {
        return config;
    }

    public AuthToken getAuthToken() {
        return authToken;
    }

    public Properties getInfo() {
        return info;
    }

    /**
     * Config has no equals implemented method
     * @param o1
     * @param o2
     * @return
     */
    protected static boolean compareConfig(Config o1, Config o2){
        boolean equal = o1.encrypted() == o2.encrypted();
        equal = equal && o1.idleTimeBeforeConnectionTest() == o2.idleTimeBeforeConnectionTest();
        equal = equal && o1.maxConnectionLifetimeMillis() == o2.maxConnectionLifetimeMillis();
        equal = equal && o1.maxConnectionPoolSize() == o2.maxConnectionPoolSize();
        equal = equal && o1.connectionAcquisitionTimeoutMillis() == o2.connectionAcquisitionTimeoutMillis();
        equal = equal && o1.logLeakedSessions() == o2.logLeakedSessions();

        equal = equal && trustStrategyEquals(o1.trustStrategy(),o2.trustStrategy());

        return equal;
    }

    /**
     * TrustStrategy has not equals method
     * @param t1
     * @param t2
     * @return
     */
    protected static boolean trustStrategyEquals(Config.TrustStrategy t1, Config.TrustStrategy t2 ){

        if(t1 == t2){
            return true;
        }

        // if one is null
        if(t1 == null || t2 == null){
            return false;
        }

        return Objects.equals(t1.certFile(),t2.certFile()) && t1.strategy() == t2.strategy();
    }
}
