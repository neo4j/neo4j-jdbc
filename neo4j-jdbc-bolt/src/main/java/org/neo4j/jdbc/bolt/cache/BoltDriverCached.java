package org.neo4j.jdbc.bolt.cache;

import org.neo4j.driver.BaseSession;
import org.neo4j.driver.BookmarkManager;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Metrics;
import org.neo4j.driver.QueryTask;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.async.AsyncSession;
import org.neo4j.driver.reactive.ReactiveSession;
import org.neo4j.driver.reactive.RxSession;
import org.neo4j.driver.types.TypeSystem;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A driver wrapper to keep connected the instance with the cache
 */
public class BoltDriverCached implements Driver {

    private static final UnsupportedOperationException UNSUPPORTED_OPERATION_EXCEPTION = new UnsupportedOperationException("Async operation are not supported over JDBC");
    private Driver internal;
    private BoltDriverCache cache;
    private BoltDriverCacheKey key;

    private final AtomicInteger sessionCounter = new AtomicInteger(0);

    /**
     *
     * @param internal
     * @param cache
     * @param key
     */
    public BoltDriverCached(Driver internal, BoltDriverCache cache, BoltDriverCacheKey key) {
        this.internal = internal;
        this.cache = cache;
        this.key = key;
    }

    @Override
    public QueryTask queryTask(String s) {
        throw UNSUPPORTED_OPERATION_EXCEPTION;
    }

    @Override
    public BookmarkManager queryTaskBookmarkManager() {
        throw UNSUPPORTED_OPERATION_EXCEPTION;
    }

    @Override
    public boolean isEncrypted() {
        return internal.isEncrypted();
    }

    @Override
    public Session session() {
        sessionCounter.incrementAndGet();
        return internal.session();
    }

    @Override
    public Session session(SessionConfig sessionConfig) {
        sessionCounter.incrementAndGet();
        return internal.session(sessionConfig);
    }

    @Override
    public <T extends BaseSession> T session(Class<T> aClass, SessionConfig sessionConfig) {
        throw UNSUPPORTED_OPERATION_EXCEPTION;
    }

    @Override
    public RxSession rxSession() {
        throw UNSUPPORTED_OPERATION_EXCEPTION;
    }

    @Override
    public RxSession rxSession(SessionConfig sessionConfig) {
        throw UNSUPPORTED_OPERATION_EXCEPTION;
    }

    @Override
    public ReactiveSession reactiveSession(SessionConfig sessionConfig) {
        throw UNSUPPORTED_OPERATION_EXCEPTION;
    }

    @Override
    public AsyncSession asyncSession() {
        throw UNSUPPORTED_OPERATION_EXCEPTION;
    }

    @Override
    public AsyncSession asyncSession(SessionConfig sessionConfig) {
        throw UNSUPPORTED_OPERATION_EXCEPTION;
    }

    @Override
    public void close() {
        if(sessionCounter.decrementAndGet() <= 0){
            cache.removeFromCache(key);
            internal.close();
        }
    }

    @Override
    public CompletionStage<Void> closeAsync() {
        throw UNSUPPORTED_OPERATION_EXCEPTION;
    }

    @Override
    public Metrics metrics() {
        return internal.metrics();
    }

    @Override
    public boolean isMetricsEnabled() {
        return internal.isMetricsEnabled();
    }

    @Override
    public TypeSystem defaultTypeSystem() {
        return internal.defaultTypeSystem();
    }

    @Override
    public void verifyConnectivity() {
        internal.verifyConnectivity();
    }

    @Override
    public CompletionStage<Void> verifyConnectivityAsync() {
        throw UNSUPPORTED_OPERATION_EXCEPTION;
    }

    @Override
    public boolean supportsMultiDb() {
        return internal.supportsMultiDb();
    }

    @Override
    public CompletionStage<Boolean> supportsMultiDbAsync() {
        throw UNSUPPORTED_OPERATION_EXCEPTION;
    }
}
