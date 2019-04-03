package org.neo4j.jdbc.bolt.cache;

import org.neo4j.driver.v1.AccessMode;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A driver wrapper to keep connected the instance with the cache
 */
public class BoltDriverCached implements Driver {

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
    public boolean isEncrypted() {
        return internal.isEncrypted();
    }

    @Override
    public Session session() {
        sessionCounter.incrementAndGet();
        return internal.session();
    }

    @Override
    public Session session(AccessMode mode) {
        sessionCounter.incrementAndGet();
        return internal.session(mode);
    }

    @Override
    public Session session(String bookmark) {
        sessionCounter.incrementAndGet();
        return internal.session(bookmark);
    }

    @Override
    public Session session(AccessMode mode, String bookmark) {
        sessionCounter.incrementAndGet();
        return internal.session(mode, bookmark);
    }

    @Override
    public Session session(Iterable<String> bookmarks) {
        sessionCounter.incrementAndGet();
        return internal.session(bookmarks);
    }

    @Override
    public Session session(AccessMode mode, Iterable<String> bookmarks) {
        sessionCounter.incrementAndGet();
        return internal.session(mode, bookmarks);
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
        return internal.closeAsync();
    }
}
