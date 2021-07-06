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

import org.neo4j.driver.AuthToken;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * A cache for the driver instances in order to reduce the number of them.
 * In the JDBC specification the connection has the properties meanwhile in Neo4j they are on the driver.
 */
public class BoltDriverCache {

    private final Map<BoltDriverCacheKey, Driver> cache;
    private final Function<BoltDriverCacheKey, Driver> builder;

    /**
     * Setup the cache for the specific building function
     * @param builder
     */
    public BoltDriverCache(Function<BoltDriverCacheKey, Driver> builder){
        this.builder = builder;
        this.cache = new ConcurrentHashMap<>();
    }

    /**
     * Get driver from cache, building it if necessary
     * @param routingUris
     * @param config
     * @param authToken
     * @param info
     * @return
     */
    public Driver getDriver(List<URI> routingUris, Config config, AuthToken authToken, Properties info) {
        return cache.computeIfAbsent(new BoltDriverCacheKey(routingUris, config, authToken, info),
            key -> new BoltDriverCached(builder.apply(key), this, key));
    }

    public Driver removeFromCache(BoltDriverCacheKey key){
        return cache.remove(key);
    }

    /**
     * The internal cache, for inspection only
     * @return
     */
    public Map<BoltDriverCacheKey, Driver> getCache() {
        return Collections.unmodifiableMap(cache);
    }

    // visible for testing
    public void clear() {
        cache.clear();
    }
}
