package net.nowhereatall.xfty.relationships;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import net.nowhereatall.xfty.Async;
import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.XftyConfigurationException;
import net.nowhereatall.xfty.core.InsertInclusivity;
import net.nowhereatall.xfty.core.InsertMode;
import net.nowhereatall.xfty.core.RecordProvider;
import net.nowhereatall.xfty.demo.Account;
import net.nowhereatall.xfty.demo.AccountDataProvider;
import net.nowhereatall.xfty.demo.Contact;
import net.nowhereatall.xfty.demo.ContactDataProvider;
import net.nowhereatall.xfty.lookup.LookupKey;
import net.nowhereatall.xfty.lookup.LookupKeyLike;
import net.nowhereatall.xfty.lookup.ProviderLookupLike;
import net.nowhereatall.xfty.lookup.ProviderLookups;
import net.nowhereatall.xfty.core.RecordProviderLike;
import org.junit.jupiter.api.Test;

/**
 * End-to-end proof that {@link SharedAncestor} / {@code SharedAncestorResolver}
 * resolve through the real engine: every child that references a shared ancestor
 * gets the exact same generated record. The registry is process-static, so each
 * test uses its own never-reused name.
 */
class SharedAncestorIntegrationTest {

    private static ProviderLookupLike lookup() {
        return ProviderLookups.of(Map.of(
                LookupKey.get(Account.class), new AccountDataProvider(),
                LookupKey.get(Contact.class), new ContactDataProvider()));
    }

    @Test
    void supplyList_WithASharedAncestor_PointsEveryChildAtTheSameGeneratedParent() {
        // Arrange - two Contacts sharing one Account
        String sharedName = "shared-ancestor-test-two-contacts";
        SharedAncestor.putAsTemplate(sharedName, Account.builder().name("ACME HQ").build());
        RecordProvider<Contact> provider = new RecordProvider<>(Contact.class, lookup())
                .setInsertMode(InsertMode.MOCK)
                .setInclusivity(InsertInclusivity.REQUIRED)
                .putRequired(Field.of(Contact.class, "accountId"), SharedAncestor.get(sharedName))
                .setQuantityPerTemplate(2);

        // Act
        List<Contact> results = Async.await(provider.supplyList());

        // Assert - both contacts point at the very same generated Account id
        long distinctAccountIds = results.stream().map(Contact::accountId).distinct().count();
        assertEquals(1, distinctAccountIds);
        assertNotNull(results.get(0).accountId());
    }

    @Test
    void getId_AfterResolveNow_ReturnsTheGeneratedId() {
        // Arrange
        String sharedName = "shared-ancestor-test-resolve-now";
        SharedAncestor.putAsTemplate(sharedName, Account.builder().name("Resolved Up Front").build());

        // Act
        Async.await(SharedAncestor.get(sharedName).resolveNow(lookup(), InsertMode.MOCK));

        // Assert
        assertNotNull(SharedAncestor.getId(sharedName));
    }

    @Test
    void getId_WhenNeverResolved_Throws() {
        // Arrange
        String sharedName = "shared-ancestor-test-unresolved";
        SharedAncestor.get(sharedName);

        // Act
        XftyConfigurationException thrown =
                assertThrows(XftyConfigurationException.class, () -> SharedAncestor.getId(sharedName));

        // Assert
        assertTrue(thrown.getMessage().contains("not resolved yet"));
    }

    @Test
    void supply_WhenTheLookupRegistersASharedAncestorDefault_ResolvesItWithoutBeingPutExplicitly() {
        // Arrange - the lookup itself supplies the default template, not the test
        String sharedName = "shared-ancestor-test-lookup-default";
        Map<LookupKeyLike, RecordProviderLike> providers = Map.of(
                LookupKey.get(Account.class), new AccountDataProvider(),
                LookupKey.get(Contact.class), new ContactDataProvider());
        ProviderLookupLike lookup = ProviderLookups.of(providers,
                Map.of(sharedName, Account.builder().name("Lookup-Default HQ").build()));
        RecordProvider<Contact> provider = new RecordProvider<>(Contact.class, lookup)
                .setInsertMode(InsertMode.MOCK)
                .setInclusivity(InsertInclusivity.REQUIRED)
                .putRequired(Field.of(Contact.class, "accountId"), SharedAncestor.get(sharedName))
                .setQuantityPerTemplate(2);

        // Act
        List<Contact> results = Async.await(provider.supplyList());

        // Assert
        assertEquals(1, results.stream().map(Contact::accountId).distinct().count());
    }
}
