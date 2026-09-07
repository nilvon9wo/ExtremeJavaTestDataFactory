package net.nowhereatall.xfty.examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import net.nowhereatall.xfty.Async;
import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.core.Bundle;
import net.nowhereatall.xfty.core.ChildProvider;
import net.nowhereatall.xfty.core.InsertMode;
import net.nowhereatall.xfty.core.MasterTemplate;
import net.nowhereatall.xfty.core.RecordProvider;
import net.nowhereatall.xfty.core.SimpleRecordProvider;
import net.nowhereatall.xfty.demo.Account;
import net.nowhereatall.xfty.demo.AccountDataProvider;
import net.nowhereatall.xfty.demo.Case;
import net.nowhereatall.xfty.demo.Contact;
import net.nowhereatall.xfty.demo.ContactDataProvider;
import net.nowhereatall.xfty.demo.DefaultProviderLookup;
import net.nowhereatall.xfty.lookup.LookupKey;
import net.nowhereatall.xfty.lookup.ProviderLookupLike;
import net.nowhereatall.xfty.lookup.ProviderLookups;
import org.junit.jupiter.api.Test;

/** Runs the exact code shown in {@code docs/use/child-records.md}. */
class ExChildRecordsTest {

    private static final DefaultProviderLookup LOOKUP = new DefaultProviderLookup();

    private static ProviderLookupLike lookupWithCase() {
        return ProviderLookups.of(Map.of(
                LookupKey.get(Account.class), new AccountDataProvider(),
                LookupKey.get(Contact.class), new ContactDataProvider(),
                LookupKey.get(Case.class), new BlankCaseProvider()));
    }

    @Test
    void theHeadlineExample() {
        // from docs/use/child-records.md, top of the page
        Bundle bundle = Async.await(new RecordProvider<>(Account.class, LOOKUP)
                .setInsertMode(InsertMode.MOCK)
                .with(ChildProvider.forField(Contact::accountId, Contact.builder().department("Buyer").build()).setQuantity(3))
                .supplyBundle());

        Account account = bundle.getPrimaries(Account.class).get(0);
        List<Contact> contacts = bundle.getChildList(Contact.class, Contact.class, "accountId");

        assertEquals(3, contacts.size());
        assertTrue(contacts.stream().allMatch(contact -> account.getId().equals(contact.accountId())));
    }

    @Test
    void childProviderConstructors() {
        // from docs/use/child-records.md "ChildProvider"
        ChildProvider blank = new ChildProvider(Field.of(Contact.class, "accountId"));
        ChildProvider withTemplate = new ChildProvider(Field.of(Contact.class, "accountId"),
                Contact.builder().department("Buyer").build());

        assertEquals(Contact.class, blank.childType());
        assertEquals(Contact.class, withTemplate.childType());
        blank.setQuantity(3);
    }

    @Test
    void attachingIt_Additive() {
        // from docs/use/child-records.md "Attaching it"
        Bundle bundle = Async.await(new RecordProvider<>(Account.class, lookupWithCase())
                .with(ChildProvider.forField(Contact::accountId, Contact.builder().department("A").build()).setQuantity(3))
                .with(ChildProvider.forField(Contact::accountId, Contact.builder().department("B").build()).setQuantity(2))
                .with(ChildProvider.forField(Case::getAccountId, new Case()).setQuantity(2))
                .setInsertMode(InsertMode.MOCK)
                .supplyBundle());

        assertEquals(5, bundle.getChildList(Contact.class, Contact.class, "accountId").size());
        assertEquals(2, bundle.getChildList(Case.class, Case.class, "accountId").size());
    }

    @Test
    void grandchildren_ChildProviderNests() {
        // from docs/use/child-records.md "Grandchildren"
        Bundle bundle = Async.await(new RecordProvider<>(Account.class, lookupWithCase())
                .setInsertMode(InsertMode.MOCK)
                .with(ChildProvider.forField(Contact::accountId, Contact.builder().build()).setQuantity(3)
                        .with(ChildProvider.forField(Case::getContactId, new Case()).setQuantity(2)))
                .supplyBundle());

        List<Case> cases = bundle.getChildBundle(Contact.class, "accountId")
                .getChildList(Case.class, Case.class, "contactId");

        assertEquals(3, bundle.getChildList(Contact.class, Contact.class, "accountId").size());
        assertEquals(6, cases.size());
    }

    static final class BlankCaseProvider extends SimpleRecordProvider {
        BlankCaseProvider() {
            super(MasterTemplate.of(Case::getId));
        }
    }
}
