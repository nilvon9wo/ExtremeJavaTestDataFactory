package net.nowhereatall.xfty.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.SerializableFunction;
import net.nowhereatall.xfty.XftyConfigurationException;
import net.nowhereatall.xfty.core.Bundle;
import net.nowhereatall.xfty.core.InsertInclusivity;
import net.nowhereatall.xfty.core.InsertMode;
import net.nowhereatall.xfty.core.RecordProvider;
import net.nowhereatall.xfty.demo.Account;
import net.nowhereatall.xfty.demo.AccountDataProvider;
import net.nowhereatall.xfty.demo.Contact;
import net.nowhereatall.xfty.demo.DefaultProviderLookup;
import net.nowhereatall.xfty.lookup.ProviderLookupLike;
import net.nowhereatall.xfty.values.CopyFromAncestorExpression;
import net.nowhereatall.xfty.values.CopyFromSiblingExpression;
import org.junit.jupiter.api.Test;

class RecordProviderIntegrationTest {

    private final ProviderLookupLike lookup = new DefaultProviderLookup();

    @Test
    void suppliesARecordWithTheProvidersDefaultValues() {
        // Act
        Account account = (Account) new RecordProvider(Account.class, this.lookup).supply();

        // Assert
        assertTrue(account.getName().startsWith(AccountDataProvider.DEFAULT_NAME_PREFIX));
        assertEquals(AccountDataProvider.DEFAULT_INDUSTRY, account.getIndustry());
        assertEquals(AccountDataProvider.DEFAULT_TYPE, account.getType());
    }

    @Test
    void anOverrideValueWinsOverTheProvidersDefault() {
        // Act
        Account account = (Account) new RecordProvider(Account.class, this.lookup)
                .put((SerializableFunction<Account, String>) Account::getName, "Acme")
                .supply();

        // Assert
        assertEquals("Acme", account.getName());
    }

    @Test
    void mockInsertModeAssignsAPlaceholderId() {
        // Act
        Account account = (Account) new RecordProvider(Account.class, this.lookup)
                .setInsertMode(InsertMode.MOCK)
                .supply();

        // Assert
        assertNotNull(account.getId());
        assertTrue(account.getId().startsWith("mock-"));
    }

    @Test
    void neverInsertModeLeavesTheIdUnset() {
        // Act
        Account account = (Account) new RecordProvider(Account.class, this.lookup).supply();

        // Assert
        assertNull(account.getId());
    }

    @Test
    void generatesARequiredRelationshipAndWiresTheForeignKey() {
        // Act
        Bundle bundle = new RecordProvider(Contact.class, this.lookup)
                .setInsertMode(InsertMode.MOCK)
                .setInclusivity(InsertInclusivity.REQUIRED)
                .supplyBundle();

        // Assert
        Contact contact = (Contact) bundle.primaryRecords().get(0);
        Account account = (Account) bundle.getList(Field.of(Contact.class, "accountId")).get(0);
        assertNotNull(account.getId());
        assertEquals(account.getId(), contact.accountId());
    }

    @Test
    void inclusivityNoneSkipsTheRequiredRelationship() {
        // Act
        Contact contact = (Contact) new RecordProvider(Contact.class, this.lookup)
                .setInsertMode(InsertMode.MOCK)
                .setInclusivity(InsertInclusivity.NONE)
                .supply();

        // Assert
        assertNull(contact.accountId());
    }

    @Test
    void quantityProducesOneDistinctRecordPerRequested() {
        // Act
        List<Object> accounts = new RecordProvider(Account.class, this.lookup)
                .setQuantityPerTemplate(3)
                .supplyList();

        // Assert
        assertEquals(3, accounts.size());
        assertEquals(3, accounts.stream().map(a -> ((Account) a).getName()).distinct().count());
    }

    @Test
    void aContextAwareSiblingValueReadsAnEarlierField() {
        // Act
        Account account = (Account) new RecordProvider(Account.class, this.lookup)
                .put((SerializableFunction<Account, String>) Account::getName, "Globex")
                .put(Field.of(Account.class, "industry"),
                        CopyFromSiblingExpression.from((SerializableFunction<Account, String>) Account::getName))
                .supply();

        // Assert
        assertEquals("Globex", account.getIndustry());
    }

    @Test
    void aContextAwareSiblingValueReadingAStillPendingSiblingThrows() {
        // Arrange - department reads reportsToId, which is put later; both are context-aware.
        RecordProvider provider = new RecordProvider(Account.class, this.lookup)
                .put(Field.of(Account.class, "industry"),
                        CopyFromSiblingExpression.from((SerializableFunction<Account, String>) Account::getType))
                .put(Field.of(Account.class, "type"),
                        CopyFromSiblingExpression.from((SerializableFunction<Account, String>) Account::getIndustry));

        // Act / Assert
        assertThrows(XftyConfigurationException.class, provider::supply);
    }

    @Test
    void aContextAwareAncestorValueCopiesFromTheGeneratedParent() {
        // Act
        Contact contact = (Contact) new RecordProvider(Contact.class, this.lookup)
                .setInsertMode(InsertMode.MOCK)
                .setInclusivity(InsertInclusivity.REQUIRED)
                .put(Field.of(Contact.class, "department"),
                        new CopyFromAncestorExpression(
                                Field.of(Contact.class, "accountId"), Field.of(Account.class, "name")))
                .supply();

        // Assert
        assertNotNull(contact.department());
        assertTrue(contact.department().startsWith(AccountDataProvider.DEFAULT_NAME_PREFIX));
    }
}
