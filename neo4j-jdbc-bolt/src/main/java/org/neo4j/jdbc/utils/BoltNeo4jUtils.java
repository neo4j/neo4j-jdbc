package org.neo4j.jdbc.utils;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.summary.SummaryCounters;
import org.neo4j.jdbc.bolt.BoltNeo4jConnection;
import org.neo4j.jdbc.bolt.impl.BoltNeo4jConnectionImpl;

import java.sql.SQLException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * A set of common functions for bolt connector
 */
public class BoltNeo4jUtils {

    public static final RetryConfig RETRY_CONFIG = RetryConfig
            .custom()
            .retryOnException(new RetryExceptionPredicate())
            .waitDuration(Duration.of(1, ChronoUnit.SECONDS))
            .maxAttempts(10)
            .build();

    /**
     * Calculate, using the summary, how many operations are executed in the statement
     * @param stats
     * @return
     */
    public static int calculateUpdateCount(SummaryCounters stats) {

		/*
		Neo4j has a detailed report to explain which entity is changed.
		But we need to collect all that value to only one.
		So a priority is defined:
		1 - nodes and relationships creation or deletion
		2 - schema changes
		3 - properties changes
		 */

        int objectCount = 0;
        objectCount += stats.nodesCreated();
        objectCount += stats.nodesDeleted();
        objectCount += stats.relationshipsCreated();
        objectCount += stats.relationshipsDeleted();

        int schemaCount = 0;
        schemaCount += stats.constraintsAdded();
        schemaCount += stats.constraintsRemoved();
        schemaCount += stats.indexesAdded();
        schemaCount += stats.indexesRemoved();
        schemaCount += stats.labelsAdded();
        schemaCount += stats.labelsRemoved();

        int updateCount = (objectCount == 0)?schemaCount:objectCount;

        return (updateCount == 0)?stats.propertiesSet():updateCount;
    }

    public static <R> R executeInTx(BoltNeo4jConnection connection,
                                    String sql,
                                    Map<String, Object> params,
                                    Function<Result, R> body) throws SQLException {
        try {
            Result statementResult = execute(connection, sql, params);
            R result = body.apply(statementResult);
            if (connection.getAutoCommit()) {
                connection.doCommit();
            }
            return result;
        }  catch (Exception e) {
            connection.doRollback();
            throw new SQLException(e);
        }
    }

    public static <R> R executeInTx(BoltNeo4jConnection connection,
                                    String sql,
                                    Function<Result, R> body) throws SQLException {
        return executeInTx(connection, sql, Collections.emptyMap(), body);
    }

    private static Result execute(BoltNeo4jConnection connection,
                                          String statement,
                                          Map<String, Object> params) throws Exception {
        return runTransactionWithRetries(connection.getTransaction(), statement, params);
    }

    public static Result runTransactionWithRetries(Transaction tx,
                                                   String statement,
                                                   Map<String, Object> params) throws Exception {
        return Retry.decorateCallable(Retry.of("retryPool", RETRY_CONFIG),
                () -> tx.run(statement, params))
                .call();
    }

    public static void closeSafely(AutoCloseable closeable, Logger logger) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception e) {
            if (logger == null) {
                return;
            }
            logger.warning("Exception while trying to close an AutoCloseable, because of the following exception: " +
                    ExceptionUtils.getStackTrace(e));
        }
    }

    public static boolean hasResultSet(BoltNeo4jConnection connection, String cypher) {
        if (StringUtils.isBlank(cypher)) {
            return false;
        }
        try (final Session session = connection.newNeo4jSession();
             final Transaction transaction = session.beginTransaction()) {
            final String toUpperCase = cypher.trim().toUpperCase(Locale.ROOT);
            final String cql;
            if (toUpperCase.startsWith("EXPLAIN") || toUpperCase.startsWith("PROFILE")) {
                cql = cypher;
            } else {
                cql = "EXPLAIN " + cypher;
            }
            return transaction
                    .run(cql)
                    .consume()
                    .plan()
                    .children()
                    .stream()
                    .map(f -> f.operatorType())
                    .noneMatch(o -> o.equalsIgnoreCase("EmptyResult"));
        } catch (Exception e) {
            return false;
        }
    }

}
