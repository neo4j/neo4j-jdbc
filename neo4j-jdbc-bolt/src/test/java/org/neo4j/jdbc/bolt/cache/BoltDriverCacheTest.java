package org.neo4j.jdbc.bolt.cache;

import org.junit.Test;
import org.mockito.Mockito;
import org.neo4j.driver.*;
import org.neo4j.driver.internal.InternalBookmark;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BoltDriverCacheTest {

    Function<BoltDriverCacheKey, Driver> builder = (params -> Mockito.mock(Driver.class));

    @Test
    public void shouldBeSameInstance() throws URISyntaxException {
        List<URI> url = Arrays.asList(URI.create("bolt://localhost"));
        Config.ConfigBuilder configBuilder = Config.builder();
        AuthToken authToken = AuthTokens.basic("neo4j", "password");

        BoltDriverCache cache = new BoltDriverCache(builder);
        assertEquals(0, cache.getCache().size());

        Driver driver1 = cache.getDriver(url, configBuilder.build(), authToken, new Properties());

        List<URI> url2 = Arrays.asList(URI.create("bolt://localhost"));
        Config.ConfigBuilder configBuilder2 = Config.builder();
        AuthToken authToken2 = AuthTokens.basic("neo4j", "password");
        Driver driver2 = cache.getDriver(url2, configBuilder2.build(), authToken2, new Properties());

        assertEquals(1, cache.getCache().size());
        assertTrue(driver1 == driver2);
    }

    @Test
    public void shouldCreateNewInstanceByURI() throws URISyntaxException {
        List<URI> url = Arrays.asList(URI.create("bolt://localhost"));
        Config.ConfigBuilder configBuilder = Config.builder();
        AuthToken authToken = AuthTokens.basic("neo4j", "password");
        BoltDriverCache cache = new BoltDriverCache(builder);

        Driver driver1 = cache.getDriver(url, configBuilder.build(), authToken, new Properties());

        url = Arrays.asList(URI.create("bolt://another"));

        Driver driver2 = cache.getDriver(url, configBuilder.build(), authToken, new Properties());

        assertEquals(2, cache.getCache().size());
        assertTrue(driver1 != driver2);
    }

    @Test
    public void shouldCreateNewInstanceByNeo4jURI() throws URISyntaxException {
        List<URI> url = Arrays.asList(URI.create("neo4j://localhost"));
        Config.ConfigBuilder configBuilder = Config.builder();
        AuthToken authToken = AuthTokens.basic("neo4j", "password");
        BoltDriverCache cache = new BoltDriverCache(builder);

        Driver driver1 = cache.getDriver(url, configBuilder.build(), authToken, new Properties());

        url = Arrays.asList(URI.create("neo4j://another"));

        Driver driver2 = cache.getDriver(url, configBuilder.build(), authToken, new Properties());

        assertEquals(2, cache.getCache().size());
        assertTrue(driver1 != driver2);
    }

    @Test
    public void shouldCreateNewInstanceByAuth() throws URISyntaxException {
        List<URI> url = Arrays.asList(URI.create("bolt://localhost"));
        Config.ConfigBuilder configBuilder = Config.builder();
        AuthToken authToken = AuthTokens.basic("neo4j", "password");
        BoltDriverCache cache = new BoltDriverCache(builder);

        Driver driver1 = cache.getDriver(url, configBuilder.build(), authToken, new Properties());

        authToken = AuthTokens.basic("admin", "password");

        Driver driver2 = cache.getDriver(url, configBuilder.build(), authToken, new Properties());

        assertEquals(2, cache.getCache().size());
        assertTrue(driver1 != driver2);
    }

    @Test
    public void shouldCreateNewInstanceByProperties() throws URISyntaxException {
        List<URI> url = Arrays.asList(URI.create("bolt://localhost"));
        Config.ConfigBuilder configBuilder = Config.builder();
        AuthToken authToken = AuthTokens.basic("neo4j", "password");
        BoltDriverCache cache = new BoltDriverCache(builder);
        Properties info1 = new Properties();
        info1.setProperty("flatten", "1");

        Driver driver1 = cache.getDriver(url, configBuilder.build(), authToken, info1);

        Properties info2 = new Properties();
        info2.setProperty("flatten", "2");

        Driver driver2 = cache.getDriver(url, configBuilder.build(), authToken, info2);

        assertEquals(2, cache.getCache().size());
        assertTrue(driver1 != driver2);
    }

    @Test
    public void shouldCreateNewInstanceByConfigEncryption() throws URISyntaxException {
        List<URI> url = Arrays.asList(URI.create("bolt://localhost"));
        Config.ConfigBuilder configBuilder = Config.builder();
        AuthToken authToken = AuthTokens.basic("neo4j", "password");
        BoltDriverCache cache = new BoltDriverCache(builder);

        Driver driver1 = cache.getDriver(url, configBuilder.build(), authToken, new Properties());

        Config.ConfigBuilder configBuilder2 = Config.builder().withEncryption();

        Driver driver2 = cache.getDriver(url, configBuilder2.build(), authToken, new Properties());

        assertEquals(2, cache.getCache().size());
        assertTrue(driver1 != driver2);
    }

    @Test
    public void shouldCreateNewInstanceByConfigIdleTimeBeforeConnectionTest() throws URISyntaxException {
        List<URI> url = Arrays.asList(URI.create("bolt://localhost"));
        Config.ConfigBuilder configBuilder = Config.builder();
        AuthToken authToken = AuthTokens.basic("neo4j", "password");
        BoltDriverCache cache = new BoltDriverCache(builder);

        Driver driver1 = cache.getDriver(url, configBuilder.build(), authToken, new Properties());

        Config.ConfigBuilder configBuilder2 = Config.builder().withConnectionLivenessCheckTimeout(1, TimeUnit.MILLISECONDS);

        Driver driver2 = cache.getDriver(url, configBuilder2.build(), authToken, new Properties());

        assertEquals(2, cache.getCache().size());
        assertTrue(driver1 != driver2);
    }

    @Test
    public void shouldCreateNewInstanceByConfigMaxConnectionLifetimeMillis() throws URISyntaxException {
        List<URI> url = Arrays.asList(URI.create("bolt://localhost"));
        Config.ConfigBuilder configBuilder = Config.builder();
        AuthToken authToken = AuthTokens.basic("neo4j", "password");
        BoltDriverCache cache = new BoltDriverCache(builder);

        Driver driver1 = cache.getDriver(url, configBuilder.build(), authToken, new Properties());

        Config.ConfigBuilder configBuilder2 = Config.builder().withMaxConnectionLifetime(1, TimeUnit.MILLISECONDS);

        Driver driver2 = cache.getDriver(url, configBuilder2.build(), authToken, new Properties());

        assertEquals(2, cache.getCache().size());
        assertTrue(driver1 != driver2);
    }

    @Test
    public void shouldCreateNewInstanceByConfigMaxConnectionPoolSize() throws URISyntaxException {
        List<URI> url = Arrays.asList(URI.create("bolt://localhost"));
        Config.ConfigBuilder configBuilder = Config.builder();
        AuthToken authToken = AuthTokens.basic("neo4j", "password");
        BoltDriverCache cache = new BoltDriverCache(builder);

        Driver driver1 = cache.getDriver(url, configBuilder.build(), authToken, new Properties());

        Config.ConfigBuilder configBuilder2 = Config.builder().withMaxConnectionPoolSize(1);

        Driver driver2 = cache.getDriver(url, configBuilder2.build(), authToken, new Properties());

        assertEquals(2, cache.getCache().size());
        assertTrue(driver1 != driver2);
    }

    @Test
    public void shouldCreateNewInstanceByConfigConnectionAcquisitionTimeoutMillis() throws URISyntaxException {
        List<URI> url = Arrays.asList(URI.create("bolt://localhost"));
        Config.ConfigBuilder configBuilder = Config.builder();
        AuthToken authToken = AuthTokens.basic("neo4j", "password");
        BoltDriverCache cache = new BoltDriverCache(builder);

        Driver driver1 = cache.getDriver(url, configBuilder.build(), authToken, new Properties());

        Config.ConfigBuilder configBuilder2 = Config.builder().withConnectionAcquisitionTimeout(1, TimeUnit.MILLISECONDS);

        Driver driver2 = cache.getDriver(url, configBuilder2.build(), authToken, new Properties());

        assertEquals(2, cache.getCache().size());
        assertTrue(driver1 != driver2);
    }

    @Test
    public void shouldCreateNewInstanceByConfigLogLeakedSessions() throws URISyntaxException {
        List<URI> url = Arrays.asList(URI.create("bolt://localhost"));
        Config.ConfigBuilder configBuilder = Config.builder();
        AuthToken authToken = AuthTokens.basic("neo4j", "password");
        BoltDriverCache cache = new BoltDriverCache(builder);

        Driver driver1 = cache.getDriver(url, configBuilder.build(), authToken, new Properties());

        Config.ConfigBuilder configBuilder2 = Config.builder().withLeakedSessionsLogging();

        Driver driver2 = cache.getDriver(url, configBuilder2.build(), authToken, new Properties());

        assertEquals(2, cache.getCache().size());
        assertTrue(driver1 != driver2);
    }

    @Test
    public void shouldCreateNewInstanceByConfigTrustStrategy() throws URISyntaxException {
        List<URI> url = Arrays.asList(URI.create("bolt://localhost"));
        Config.ConfigBuilder configBuilder = Config.builder();
        AuthToken authToken = AuthTokens.basic("neo4j", "password");
        BoltDriverCache cache = new BoltDriverCache(builder);

        Driver driver1 = cache.getDriver(url, configBuilder.build(), authToken, new Properties());

        Config.ConfigBuilder configBuilder2 = Config.builder().withTrustStrategy(Config.TrustStrategy.trustAllCertificates());

        Driver driver2 = cache.getDriver(url, configBuilder2.build(), authToken, new Properties());

        assertEquals(2, cache.getCache().size());
        assertTrue(driver1 != driver2);
    }

    // SESSION MANAGEMENT //

    @Test
    public void shouldRemoveDriver() throws URISyntaxException {
        List<URI> url = Arrays.asList(URI.create("bolt://localhost"));
        Config.ConfigBuilder configBuilder = Config.builder();
        AuthToken authToken = AuthTokens.basic("neo4j", "password");

        BoltDriverCache cache = new BoltDriverCache(builder);

        Driver driver1 = cache.getDriver(url, configBuilder.build(), authToken, new Properties());

        List<URI> url2 = Arrays.asList(URI.create("bolt://another"));
        Driver driver2 = cache.getDriver(url2, configBuilder.build(), authToken, new Properties());

        assertEquals(2, cache.getCache().size());

        SessionConfig conf = SessionConfig.builder()
                .withBookmarks(InternalBookmark.parse("bookmark"))
                .withDefaultAccessMode(AccessMode.READ)
                .build();
        Session s1_1 = driver1.session(conf);
        Session s1_2 = driver1.session(conf);

        //it doesn't remove the driver from cache because the cache receive driver.close for each connection (session)
        //so it remains in the cache if there are other opened sessions
        driver1.close();
        assertEquals(2, cache.getCache().size());
        driver1.close();
        assertEquals(1, cache.getCache().size());
    }
}
