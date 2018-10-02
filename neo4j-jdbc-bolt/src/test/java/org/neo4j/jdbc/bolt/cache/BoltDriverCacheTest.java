package org.neo4j.jdbc.bolt.cache;

import org.junit.Test;
import org.neo4j.driver.v1.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BoltDriverCacheTest {

    Function<BoltDriverCacheKey, Driver> builder = (params -> {
        return new Driver() {
            @Override
            public boolean isEncrypted() {
                return false;
            }

            @Override
            public Session session() {
                return null;
            }

            @Override
            public Session session(AccessMode mode) {
                return null;
            }

            @Override
            public Session session(String bookmark) {
                return null;
            }

            @Override
            public Session session(AccessMode mode, String bookmark) {
                return null;
            }

            @Override
            public Session session(Iterable<String> bookmarks) {
                return null;
            }

            @Override
            public Session session(AccessMode mode, Iterable<String> bookmarks) {
                return null;
            }

            @Override
            public void close() {

            }

            @Override
            public CompletionStage<Void> closeAsync() {
                return null;
            }
        };
    });

    @Test
    public void shouldBeSameInstance() throws URISyntaxException {
        List<URI> url = Arrays.asList(URI.create("bolt://localhost"));
        Config.ConfigBuilder configBuilder = Config.build();
        AuthToken authToken = AuthTokens.basic("neo4j", "password");

        BoltDriverCache cache = new BoltDriverCache(builder);
        assertEquals(0, cache.getCache().size());

        Driver driver1 = cache.getDriver(url, configBuilder.toConfig(), authToken, new Properties());

        List<URI> url2 = Arrays.asList(URI.create("bolt://localhost"));
        Config.ConfigBuilder configBuilder2 = Config.build();
        AuthToken authToken2 = AuthTokens.basic("neo4j", "password");
        Driver driver2 = cache.getDriver(url2, configBuilder2.toConfig(), authToken2, new Properties());

        assertEquals(1, cache.getCache().size());
        assertTrue(driver1 == driver2);
    }

    @Test
    public void shouldCreateNewInstanceByURI() throws URISyntaxException {
        List<URI> url = Arrays.asList(URI.create("bolt://localhost"));
        Config.ConfigBuilder configBuilder = Config.build();
        AuthToken authToken = AuthTokens.basic("neo4j", "password");
        BoltDriverCache cache = new BoltDriverCache(builder);

        Driver driver1 = cache.getDriver(url, configBuilder.toConfig(), authToken, new Properties());

        url = Arrays.asList(URI.create("bolt://another"));

        Driver driver2 = cache.getDriver(url, configBuilder.toConfig(), authToken, new Properties());

        assertEquals(2, cache.getCache().size());
        assertTrue(driver1 != driver2);
    }

    @Test
    public void shouldCreateNewInstanceByAuth() throws URISyntaxException {
        List<URI> url = Arrays.asList(URI.create("bolt://localhost"));
        Config.ConfigBuilder configBuilder = Config.build();
        AuthToken authToken = AuthTokens.basic("neo4j", "password");
        BoltDriverCache cache = new BoltDriverCache(builder);

        Driver driver1 = cache.getDriver(url, configBuilder.toConfig(), authToken, new Properties());

        authToken = AuthTokens.basic("admin", "password");

        Driver driver2 = cache.getDriver(url, configBuilder.toConfig(), authToken, new Properties());

        assertEquals(2, cache.getCache().size());
        assertTrue(driver1 != driver2);
    }

    @Test
    public void shouldCreateNewInstanceByProperties() throws URISyntaxException {
        List<URI> url = Arrays.asList(URI.create("bolt://localhost"));
        Config.ConfigBuilder configBuilder = Config.build();
        AuthToken authToken = AuthTokens.basic("neo4j", "password");
        BoltDriverCache cache = new BoltDriverCache(builder);
        Properties info1 = new Properties();
        info1.setProperty("flatten", "1");

        Driver driver1 = cache.getDriver(url, configBuilder.toConfig(), authToken, info1);

        Properties info2 = new Properties();
        info2.setProperty("flatten", "2");

        Driver driver2 = cache.getDriver(url, configBuilder.toConfig(), authToken, info2);

        assertEquals(2, cache.getCache().size());
        assertTrue(driver1 != driver2);
    }

    @Test
    public void shouldCreateNewInstanceByConfigEncryption() throws URISyntaxException {
        List<URI> url = Arrays.asList(URI.create("bolt://localhost"));
        Config.ConfigBuilder configBuilder = Config.build();
        AuthToken authToken = AuthTokens.basic("neo4j", "password");
        BoltDriverCache cache = new BoltDriverCache(builder);

        Driver driver1 = cache.getDriver(url, configBuilder.toConfig(), authToken, new Properties());

        Config.ConfigBuilder configBuilder2 = Config.build().withoutEncryption();

        Driver driver2 = cache.getDriver(url, configBuilder2.toConfig(), authToken, new Properties());

        assertEquals(2, cache.getCache().size());
        assertTrue(driver1 != driver2);
    }

    @Test
    public void shouldCreateNewInstanceByConfigIdleTimeBeforeConnectionTest() throws URISyntaxException {
        List<URI> url = Arrays.asList(URI.create("bolt://localhost"));
        Config.ConfigBuilder configBuilder = Config.build();
        AuthToken authToken = AuthTokens.basic("neo4j", "password");
        BoltDriverCache cache = new BoltDriverCache(builder);

        Driver driver1 = cache.getDriver(url, configBuilder.toConfig(), authToken, new Properties());

        Config.ConfigBuilder configBuilder2 = Config.build().withConnectionLivenessCheckTimeout(1, TimeUnit.MILLISECONDS);

        Driver driver2 = cache.getDriver(url, configBuilder2.toConfig(), authToken, new Properties());

        assertEquals(2, cache.getCache().size());
        assertTrue(driver1 != driver2);
    }

    @Test
    public void shouldCreateNewInstanceByConfigMaxConnectionLifetimeMillis() throws URISyntaxException {
        List<URI> url = Arrays.asList(URI.create("bolt://localhost"));
        Config.ConfigBuilder configBuilder = Config.build();
        AuthToken authToken = AuthTokens.basic("neo4j", "password");
        BoltDriverCache cache = new BoltDriverCache(builder);

        Driver driver1 = cache.getDriver(url, configBuilder.toConfig(), authToken, new Properties());

        Config.ConfigBuilder configBuilder2 = Config.build().withMaxConnectionLifetime(1, TimeUnit.MILLISECONDS);

        Driver driver2 = cache.getDriver(url, configBuilder2.toConfig(), authToken, new Properties());

        assertEquals(2, cache.getCache().size());
        assertTrue(driver1 != driver2);
    }

    @Test
    public void shouldCreateNewInstanceByConfigMaxConnectionPoolSize() throws URISyntaxException {
        List<URI> url = Arrays.asList(URI.create("bolt://localhost"));
        Config.ConfigBuilder configBuilder = Config.build();
        AuthToken authToken = AuthTokens.basic("neo4j", "password");
        BoltDriverCache cache = new BoltDriverCache(builder);

        Driver driver1 = cache.getDriver(url, configBuilder.toConfig(), authToken, new Properties());

        Config.ConfigBuilder configBuilder2 = Config.build().withMaxConnectionPoolSize(1);

        Driver driver2 = cache.getDriver(url, configBuilder2.toConfig(), authToken, new Properties());

        assertEquals(2, cache.getCache().size());
        assertTrue(driver1 != driver2);
    }

    @Test
    public void shouldCreateNewInstanceByConfigConnectionAcquisitionTimeoutMillis() throws URISyntaxException {
        List<URI> url = Arrays.asList(URI.create("bolt://localhost"));
        Config.ConfigBuilder configBuilder = Config.build();
        AuthToken authToken = AuthTokens.basic("neo4j", "password");
        BoltDriverCache cache = new BoltDriverCache(builder);

        Driver driver1 = cache.getDriver(url, configBuilder.toConfig(), authToken, new Properties());

        Config.ConfigBuilder configBuilder2 = Config.build().withConnectionAcquisitionTimeout(1, TimeUnit.MILLISECONDS);

        Driver driver2 = cache.getDriver(url, configBuilder2.toConfig(), authToken, new Properties());

        assertEquals(2, cache.getCache().size());
        assertTrue(driver1 != driver2);
    }

    @Test
    public void shouldCreateNewInstanceByConfigLogLeakedSessions() throws URISyntaxException {
        List<URI> url = Arrays.asList(URI.create("bolt://localhost"));
        Config.ConfigBuilder configBuilder = Config.build();
        AuthToken authToken = AuthTokens.basic("neo4j", "password");
        BoltDriverCache cache = new BoltDriverCache(builder);

        Driver driver1 = cache.getDriver(url, configBuilder.toConfig(), authToken, new Properties());

        Config.ConfigBuilder configBuilder2 = Config.build().withLeakedSessionsLogging();

        Driver driver2 = cache.getDriver(url, configBuilder2.toConfig(), authToken, new Properties());

        assertEquals(2, cache.getCache().size());
        assertTrue(driver1 != driver2);
    }

    @Test
    public void shouldCreateNewInstanceByConfigTrustStrategy() throws URISyntaxException {
        List<URI> url = Arrays.asList(URI.create("bolt://localhost"));
        Config.ConfigBuilder configBuilder = Config.build();
        AuthToken authToken = AuthTokens.basic("neo4j", "password");
        BoltDriverCache cache = new BoltDriverCache(builder);

        Driver driver1 = cache.getDriver(url, configBuilder.toConfig(), authToken, new Properties());

        Config.ConfigBuilder configBuilder2 = Config.build().withTrustStrategy(Config.TrustStrategy.trustSystemCertificates());

        Driver driver2 = cache.getDriver(url, configBuilder2.toConfig(), authToken, new Properties());

        assertEquals(2, cache.getCache().size());
        assertTrue(driver1 != driver2);
    }

    // SESSION MANAGEMENT //

    @Test
    public void shouldRemoveDriver() throws URISyntaxException {
        List<URI> url = Arrays.asList(URI.create("bolt://localhost"));
        Config.ConfigBuilder configBuilder = Config.build();
        AuthToken authToken = AuthTokens.basic("neo4j", "password");

        BoltDriverCache cache = new BoltDriverCache(builder);

        Driver driver1 = cache.getDriver(url, configBuilder.toConfig(), authToken, new Properties());

        List<URI> url2 = Arrays.asList(URI.create("bolt://another"));
        Driver driver2 = cache.getDriver(url2, configBuilder.toConfig(), authToken, new Properties());

        assertEquals(2, cache.getCache().size());

        Session s1_1 = driver1.session(AccessMode.READ, "bookmark");
        Session s1_2 = driver1.session(AccessMode.READ, "bookmark");

        //it doesn't remove the driver from cache because the cache receive driver.close for each connection (session)
        //so it remains in the cache if there are other opened sessions
        driver1.close();
        assertEquals(2, cache.getCache().size());
        driver1.close();
        assertEquals(1, cache.getCache().size());
    }
}
