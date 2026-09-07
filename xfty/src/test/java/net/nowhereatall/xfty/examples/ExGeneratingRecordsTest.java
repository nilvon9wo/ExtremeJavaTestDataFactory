package net.nowhereatall.xfty.examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;

import net.nowhereatall.xfty.Async;
import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.core.Bundle;
import net.nowhereatall.xfty.core.InsertInclusivity;
import net.nowhereatall.xfty.core.InsertMode;
import net.nowhereatall.xfty.core.MasterTemplate;
import net.nowhereatall.xfty.core.RecordProvider;
import net.nowhereatall.xfty.core.SimpleRecordProvider;
import net.nowhereatall.xfty.demo.Account;
import net.nowhereatall.xfty.demo.AccountDataProvider;
import net.nowhereatall.xfty.demo.Case;
import net.nowhereatall.xfty.demo.Contact;
import net.nowhereatall.xfty.demo.DefaultProviderLookup;
import net.nowhereatall.xfty.lookup.LookupKey;
import net.nowhereatall.xfty.lookup.ProviderLookupLike;
import net.nowhereatall.xfty.lookup.ProviderLookups;
import net.nowhereatall.xfty.relationships.DefaultRelationship;
import org.junit.jupiter.api.Test;

/**
 * Runs the exact code shown in {@code docs/use/generating-records.md} and
 * {@code docs/getting-started.md}, proving those examples compile and behave as
 * documented. Checked by {@code docs/verify-doc-examples.py}.
 */
class ExGeneratingRecordsTest {

    private static final DefaultProviderLookup LOOKUP = new DefaultProviderLookup();

    @Test
    void supply_TheSimplestCase_ReturnsOneRecord() {
        // from docs/use/generating-records.md "One record"
        Contact result = Async.await(new RecordProvider<>(Contact.class, LOOKUP)
                .supply());

        assertNotNull(result);
        assertNull(result.id()); // not inserted by default
    }

    @Test
    void shorthandConstructors_FromDocs_AllWork() {
        // from docs/use/generating-records.md "Shorthand constructors"
        Contact fromTemplate = Async.await(
                new RecordProvider<>(Contact.builder().firstName("Alice").build(), LOOKUP).supply());
        List<Contact> fromList = Async.await(
                new RecordProvider<>(List.of(Contact.builder().build(), Contact.builder().build()), LOOKUP).supplyList());
        Contact fromKey = Async.await(
                new RecordProvider<Contact>(LookupKey.get(Contact.class), LOOKUP).supply());

        assertEquals("Alice", fromTemplate.firstName());
        assertEquals(2, fromList.size());
        assertNotNull(fromKey);
    }

    @Test
    void severalRecords() {
        // from docs/use/generating-records.md "Several records"
        List<Contact> contacts = Async.await(new RecordProvider<>(Contact.class, LOOKUP)
                .setQuantityPerTemplate(5)
                .supplyList());

        assertEquals(5, contacts.size());
    }

    @Test
    void gettingStarted_OverrideTemplates() {
        // from docs/getting-started.md "Override templates"
        Contact contact = Async.await(new RecordProvider<>(Contact.class, LOOKUP)
                .setOverrideTemplate(Contact.builder().firstName("Alice").lastName("Smith").build())
                .supply());

        assertEquals("Alice", contact.firstName());
        assertEquals("Smith", contact.lastName());
    }

    @Test
    void gettingStarted_UnderstandingBundles() {
        // from docs/getting-started.md "Understanding bundles" - a Case pulling in an Account
        ProviderLookupLike lookup = ProviderLookups.of(Map.of(
                LookupKey.get(Case.class), new CaseWithAccountProvider(),
                LookupKey.get(Account.class), new AccountDataProvider()));
        Bundle bundle = Async.await(new RecordProvider<>(Case.class, lookup)
                .setInsertMode(InsertMode.MOCK)
                .setInclusivity(InsertInclusivity.REQUIRED)
                .supplyBundle());

        List<Account> accounts = bundle.getList(Account.class, Case.class, "accountId");
        Bundle accountBundle = bundle.getBundle(Case.class, "accountId");

        assertEquals(1, accounts.size());
        assertNotNull(accountBundle);
    }

    static final class CaseWithAccountProvider extends SimpleRecordProvider {
        CaseWithAccountProvider() {
            super(MasterTemplate.of(Case::getId)
                    .putRequired(Field.of(Case.class, "accountId"), new DefaultRelationship(new Account())));
        }
    }
}
