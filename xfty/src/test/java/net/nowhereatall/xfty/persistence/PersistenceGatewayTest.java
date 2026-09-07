package net.nowhereatall.xfty.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import net.nowhereatall.xfty.Async;
import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.core.Bundle;
import net.nowhereatall.xfty.core.InsertInclusivity;
import net.nowhereatall.xfty.core.InsertMode;
import net.nowhereatall.xfty.core.RecordProvider;
import net.nowhereatall.xfty.demo.Account;
import net.nowhereatall.xfty.demo.Contact;
import net.nowhereatall.xfty.demo.DefaultProviderLookup;
import net.nowhereatall.xfty.reflect.RecordShape;
import org.junit.jupiter.api.Test;

/**
 * Proves {@code InsertMode.NOW} and {@code .depthBatched()} work end to end once
 * a gateway is configured - mocking the gateway itself, not the generation. The
 * real-database side lives in {@code xfty-jpa}'s {@code H2NowPersistenceTest}.
 */
class PersistenceGatewayTest {

    private static final DefaultProviderLookup LOOKUP = new DefaultProviderLookup();

    private static PersistenceGatewayLike idAssigningGateway() {
        PersistenceGatewayLike gateway = mock(PersistenceGatewayLike.class);
        doAnswer(invocation -> {
            List<Object> records = invocation.getArgument(0);
            Field idField = invocation.getArgument(1);
            List<Object> persisted = new java.util.ArrayList<>();
            for (Object record : records) {
                persisted.add(RecordShape.of(record.getClass()).set(record, idField, "real-" + UUID.randomUUID()));
            }
            return CompletableFuture.completedFuture(persisted);
        }).when(gateway).insert(any(), any());
        return gateway;
    }

    @Test
    void supply_InNowMode_WithAGateway_InsertsThroughItAndKeepsTheAssignedId() {
        // Arrange - the gateway assigns an id the way a real database would
        PersistenceGatewayLike gateway = idAssigningGateway();
        RecordProvider<Account> provider = new RecordProvider<>(Account.class, LOOKUP)
                .setInsertMode(InsertMode.NOW)
                .setPersistenceGateway(gateway);

        // Act
        Account result = Async.await(provider.supply());

        // Assert
        assertNotNull(result.getId());
        assertTrue(result.getId().startsWith("real-"));
        verify(gateway, times(1)).insert(any(), any());
    }

    @Test
    void supply_InNowMode_WithoutAGateway_StillThrows() {
        // Arrange - no setPersistenceGateway(...) call
        RecordProvider<Account> provider = new RecordProvider<>(Account.class, LOOKUP).setInsertMode(InsertMode.NOW);

        // Act
        UnsupportedOperationException thrown =
                assertThrows(UnsupportedOperationException.class, () -> Async.await(provider.supply()));

        // Assert
        assertTrue(thrown.getMessage().contains("persistence gateway"));
    }

    @Test
    void supplyBundle_InNowMode_WithAGateway_InsertsTheRequiredParentToo() {
        // Arrange
        PersistenceGatewayLike gateway = idAssigningGateway();
        RecordProvider<Contact> provider = new RecordProvider<>(Contact.class, LOOKUP)
                .setInclusivity(InsertInclusivity.REQUIRED)
                .setInsertMode(InsertMode.NOW)
                .setPersistenceGateway(gateway);

        // Act
        Bundle bundle = Async.await(provider.supplyBundle());

        // Assert - both the Contact and its required Account were inserted through the gateway
        Contact contact = bundle.getPrimaries(Contact.class).get(0);
        Account account = bundle.getList(Account.class, Contact.class, "accountId").get(0);
        assertNotNull(contact.id());
        assertNotNull(account.getId());
        assertEquals(account.getId(), contact.accountId());
        verify(gateway, times(2)).insert(any(), any());
    }

    @Test
    void supply_NowPlusDepthBatched_WithAGateway_InsertsOnePerLayerAndWiresTheForeignKey() {
        // Arrange
        PersistenceGatewayLike gateway = idAssigningGateway();
        RecordProvider<Contact> provider = new RecordProvider<>(Contact.class, LOOKUP)
                .setInclusivity(InsertInclusivity.REQUIRED)
                .setInsertMode(InsertMode.NOW)
                .setPersistenceGateway(gateway)
                .depthBatched();

        // Act
        Contact result = Async.await(provider.supply());

        // Assert - one insert per dependency layer (Account, then Contact)
        assertNotNull(result.id());
        assertNotNull(result.accountId());
        verify(gateway, times(2)).insert(any(), any());
    }
}
