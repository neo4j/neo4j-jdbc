package org.neo4j.jdbc.utils;

import org.neo4j.driver.exceptions.ServiceUnavailableException;
import org.neo4j.driver.exceptions.SessionExpiredException;
import org.neo4j.driver.exceptions.TransientException;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public final class RetryExceptionPredicate implements Predicate<Throwable> {

    private static final Set<String> RETRYABLE_ILLEGAL_STATE_MESSAGES = Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList("Transaction must be open, but has already been closed.",
                    "Session must be open, but has already been closed.")));

    @Override
    public boolean test(Throwable throwable) {
        if (throwable instanceof IllegalStateException) {
            String msg = throwable.getMessage();
            return RETRYABLE_ILLEGAL_STATE_MESSAGES.contains(msg);
        }

        if (throwable instanceof TransientException) {
            String code = ((TransientException) throwable).code();
            return !("Neo.TransientError.Transaction.Terminated".equals(code) ||
                    "Neo.TransientError.Transaction.LockClientStopped".equals(code));
        } else {
            return throwable instanceof SessionExpiredException || throwable instanceof ServiceUnavailableException;
        }
    }
}