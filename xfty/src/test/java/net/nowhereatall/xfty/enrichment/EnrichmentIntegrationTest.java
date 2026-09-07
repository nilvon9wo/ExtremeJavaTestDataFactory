package net.nowhereatall.xfty.enrichment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import net.nowhereatall.xfty.Async;
import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.XftyConfigurationException;
import net.nowhereatall.xfty.core.Bundle;
import net.nowhereatall.xfty.core.InsertInclusivity;
import net.nowhereatall.xfty.core.InsertMode;
import net.nowhereatall.xfty.core.RecordProvider;
import net.nowhereatall.xfty.demo.Account;
import net.nowhereatall.xfty.demo.AccountDataProvider;
import net.nowhereatall.xfty.demo.Contact;
import net.nowhereatall.xfty.demo.ContactDataProvider;
import net.nowhereatall.xfty.lookup.LookupKey;
import net.nowhereatall.xfty.lookup.ProviderLookupLike;
import net.nowhereatall.xfty.lookup.ProviderLookups;
import org.junit.jupiter.api.Test;

/**
 * End-to-end proof that {@link BundleEnricher} works through the real engine: a
 * generated ancestor / child collection, injected via reflection onto the demo
 * domain's {@code Account.contacts} / {@code Contact.account} navigation slots.
 */
class EnrichmentIntegrationTest {

    private static ProviderLookupLike lookup() {
        return ProviderLookups.of(Map.of(
                LookupKey.get(Account.class), new AccountDataProvider(),
                LookupKey.get(Contact.class), new ContactDataProvider()));
    }

    @Test
    void injectAll_ForAGeneratedAncestor_PopulatesTheNavigationSlotOnNewInstances() {
        // Arrange
        Bundle bundle = Async.await(new RecordProvider<>(Contact.class, lookup())
                .setInsertMode(InsertMode.MOCK)
                .setInclusivity(InsertInclusivity.REQUIRED)
                .supplyBundle());

        // Act
        List<Object> enriched = bundle.injectAll(Field.of(Contact.class, "id"));

        // Assert - the enriched copy carries the populated Account; the original does not
        Contact enrichedContact = (Contact) enriched.get(0);
        assertNotNull(enrichedContact.account());
        assertEquals(enrichedContact.accountId(), enrichedContact.account().getId());
        Contact original = (Contact) bundle.primaryRecords().get(0);
        assertNull(original.account());
    }

    @Test
    void injectAll_ForAGeneratedChildCollection_PopulatesTheCollectionNavigationSlot() {
        // Arrange - downward generation: an Account with three Contact children
        Bundle bundle = Async.await(new RecordProvider<>(Account.class, lookup())
                .setInsertMode(InsertMode.MOCK)
                .withChildren(Field.of(Contact.class, "accountId"), 3)
                .supplyBundle());

        // Act
        List<Object> enriched = bundle.injectAll(Field.of(Account.class, "id"));

        // Assert
        Account enrichedAccount = (Account) enriched.get(0);
        assertNotNull(enrichedAccount.getContacts());
        assertEquals(3, enrichedAccount.getContacts().size());
        assertTrue(enrichedAccount.getContacts().stream()
                .allMatch(contact -> enrichedAccount.getId().equals(contact.accountId())));
    }

    @Test
    void injectAll_WhenTheGraphHasNothingToInject_Throws() {
        // Arrange - no ancestor generated, no children configured
        Bundle bundle = Async.await(new RecordProvider<>(Contact.class, lookup())
                .setInsertMode(InsertMode.MOCK)
                .setInclusivity(InsertInclusivity.NONE)
                .supplyBundle());

        // Act
        XftyConfigurationException thrown = assertThrows(XftyConfigurationException.class,
                () -> bundle.injectAll(Field.of(Contact.class, "id")));

        // Assert
        assertTrue(thrown.getMessage().contains("has no generated ancestor or child collection"));
    }

    @Test
    void inject_WithInjectValue_ForcesAScalarOntoEveryRow() {
        // Arrange
        Bundle bundle = Async.await(new RecordProvider<>(Contact.class, lookup())
                .setInsertMode(InsertMode.MOCK)
                .setQuantityPerTemplate(2)
                .supplyBundle());
        InjectConfig config = InjectConfig.nothing().injectValue(Field.of(Contact.class, "department"), "Sales");

        // Act
        List<Object> enriched = bundle.inject(Field.of(Contact.class, "id"), config);

        // Assert
        assertTrue(enriched.stream().allMatch(contact -> "Sales".equals(((Contact) contact).department())));
    }
}
