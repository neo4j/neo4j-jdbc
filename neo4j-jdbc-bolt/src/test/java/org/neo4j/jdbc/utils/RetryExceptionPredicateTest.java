package org.neo4j.jdbc.utils;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.Invocation;
import org.neo4j.driver.Result;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.exceptions.TransientException;

import java.util.Collection;
import java.util.Collections;

public class RetryExceptionPredicateTest {

    @Test
    public void shouldRetry() throws Exception {
        final Transaction mockedTransaction = Mockito.mock(Transaction.class);
        final Result mockedResult = Mockito.mock(Result.class);
        Mockito.when(mockedTransaction.run(Mockito.anyString(), Mockito.anyMap()))
                .thenThrow(new TransientException("code.allowed.to.retry", ""))
                .thenReturn(mockedResult);
        final Result result = BoltNeo4jUtils.runTransactionWithRetries(mockedTransaction, "", Collections.emptyMap());
        final Collection<Invocation> invocations = Mockito.mockingDetails(mockedTransaction).getInvocations();
        Assert.assertEquals(mockedResult, result);
        Assert.assertEquals(2, invocations.size());
    }
}
