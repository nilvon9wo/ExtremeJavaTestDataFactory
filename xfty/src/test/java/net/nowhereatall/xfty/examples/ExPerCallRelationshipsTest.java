package net.nowhereatall.xfty.examples;

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
import net.nowhereatall.xfty.demo.Contact;
import net.nowhereatall.xfty.demo.User;
import net.nowhereatall.xfty.lookup.LookupKey;
import net.nowhereatall.xfty.lookup.ProviderLookupLike;
import net.nowhereatall.xfty.lookup.ProviderLookups;
import net.nowhereatall.xfty.relationships.DefaultRelationship;
import org.junit.jupiter.api.Test;

/** Runs the exact code shown in {@code docs/use/per-call-relationships.md}. */
class ExPerCallRelationshipsTest {

    private static ProviderLookupLike lookup() {
        return ProviderLookups.of(Map.of(
                LookupKey.get(Contact.class), new ContactRequiringAccountProvider(),
                LookupKey.get(Account.class), new AccountWithOptionalOwnerAndParentProvider(),
                LookupKey.get(User.class), new LeafUserProvider()));
    }

    @Test
    void theSimplestCase() {
        // from docs/use/per-call-relationships.md "The simplest case"
        Account result = Async.await(new RecordProvider<>(Account.class, lookup())
                .includeOptional(Account::getOwnerId)         // generate this optional one too
                .excludeRelationship(Account::getParentId)    // do not generate this one
                .setInsertMode(InsertMode.MOCK)
                .supply());

        assertNotNull(result.getOwnerId());
        assertNull(result.getParentId());
    }

    @Test
    void reachingDeeper_APath() {
        // from docs/use/per-call-relationships.md "Reaching deeper - a path"
        Bundle bundle = Async.await(new RecordProvider<>(Contact.class, lookup())
                .includeOptional(List.of(Field.of(Contact.class, "accountId"), Field.of(Account.class, "ownerId")))
                .setInclusivity(InsertInclusivity.REQUIRED)
                .supplyBundle());

        Bundle accountBundle = bundle.getBundle(Contact.class, "accountId");
        assertNotNull(accountBundle.getList(Field.of(Account.class, "ownerId")));
    }

    static final class ContactRequiringAccountProvider extends SimpleRecordProvider {
        ContactRequiringAccountProvider() {
            super(MasterTemplate.of(Contact::id)
                    .putRequired(Contact::accountId, new DefaultRelationship(new Account())));
        }
    }

    static final class AccountWithOptionalOwnerAndParentProvider extends SimpleRecordProvider {
        AccountWithOptionalOwnerAndParentProvider() {
            super(MasterTemplate.of(Account::getId)
                    .putOptional(Account::getOwnerId, new DefaultRelationship(new User()))
                    .putOptional(Account::getParentId, new DefaultRelationship(new Account())));
        }
    }

    static final class LeafUserProvider extends SimpleRecordProvider {
        LeafUserProvider() {
            super(MasterTemplate.of(User::getId));
        }
    }
}
