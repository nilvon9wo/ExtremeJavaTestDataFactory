package net.nowhereatall.xfty.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import net.nowhereatall.xfty.Async;
import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.XftyConfigurationException;
import net.nowhereatall.xfty.core.Bundle;
import net.nowhereatall.xfty.core.InsertInclusivity;
import net.nowhereatall.xfty.core.InsertMode;
import net.nowhereatall.xfty.core.RecordProvider;
import net.nowhereatall.xfty.demo.Account;
import net.nowhereatall.xfty.demo.Contact;
import net.nowhereatall.xfty.demo.DefaultProviderLookup;
import net.nowhereatall.xfty.lookup.ProviderLookupLike;
import net.nowhereatall.xfty.persistence.DeferredInserter;
import net.nowhereatall.xfty.persistence.PersistenceGatewayLike;
import net.nowhereatall.xfty.reflect.RecordShape;
import net.nowhereatall.xfty.values.CopyFromDescendantExpression;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Proves the DEFERRED insert mode: a graph is registered, not saved, until {@link DeferredInserter#flush}. */
class DeferredInsertIntegrationTest {

    private final ProviderLookupLike lookup = new DefaultProviderLookup();

    @AfterEach
    void clearRegistry() {
        DeferredInserter.resetForTesting();
    }

    private static PersistenceGatewayLike idAssigningGateway() {
        PersistenceGatewayLike gateway = mock(PersistenceGatewayLike.class);
        doAnswer(invocation -> {
            List<Object> records = invocation.getArgument(0);
            Field idField = invocation.getArgument(1);
            List<Object> persisted = new ArrayList<>();
            for (Object record : records) {
                persisted.add(RecordShape.of(record.getClass()).set(record, idField, "real-" + UUID.randomUUID()));
            }
            return CompletableFuture.completedFuture(persisted);
        }).when(gateway).insert(any(), any());
        return gateway;
    }

    @Test
    void deferredMode_RegistersTheGraphAndInsertsNothingUntilFlush() {
        // Arrange
        PersistenceGatewayLike gateway = idAssigningGateway();

        // Act
        Async.await(new RecordProvider<>(Contact.class, this.lookup)
                .setInsertMode(InsertMode.DEFERRED)
                .setInclusivity(InsertInclusivity.REQUIRED)
                .supplyBundle());

        // Assert - nothing persisted yet, but the graph is queued
        assertTrue(DeferredInserter.pendingCount() > 0);
        Async.await(DeferredInserter.flush(gateway));
        assertEquals(0, DeferredInserter.pendingCount());
    }

    @Test
    void deferredMode_WiresForeignKeysAcrossTheWholeQueuedForestOnFlush() {
        // Arrange
        PersistenceGatewayLike gateway = idAssigningGateway();
        Bundle bundle = Async.await(new RecordProvider<>(Contact.class, this.lookup)
                .setInsertMode(InsertMode.DEFERRED)
                .setInclusivity(InsertInclusivity.REQUIRED)
                .supplyBundle());

        // Act
        Async.await(DeferredInserter.flush(gateway));

        // Assert
        Contact contact = bundle.getPrimaries(Contact.class).get(0);
        Account account = bundle.getList(Account.class, Contact.class, "accountId").get(0);
        assertNotNull(contact.id());
        assertNotNull(account.getId());
        assertEquals(account.getId(), contact.accountId());
    }

    @Test
    void anUpFlowValueOutsideDeferredMode_ThrowsAtGenerationTime() {
        // Arrange - CopyFromDescendant in plain MOCK mode: no forest exists to read up from
        RecordProvider<Account> provider = new RecordProvider<>(Account.class, this.lookup)
                .setInsertMode(InsertMode.MOCK)
                .put(Field.of(Account.class, "description"),
                        new CopyFromDescendantExpression(
                                Field.of(Contact.class, "accountId"), Field.of(Contact.class, "lastName")));

        // Act / Assert
        assertThrows(XftyConfigurationException.class, () -> Async.await(provider.supply()));
    }
}
