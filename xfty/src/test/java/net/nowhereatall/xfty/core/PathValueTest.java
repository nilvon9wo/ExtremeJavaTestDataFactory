package net.nowhereatall.xfty.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import net.nowhereatall.xfty.Async;
import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.XftyConfigurationException;
import net.nowhereatall.xfty.demo.Account;
import net.nowhereatall.xfty.demo.AccountDataProvider;
import net.nowhereatall.xfty.demo.Contact;
import net.nowhereatall.xfty.demo.User;
import net.nowhereatall.xfty.lookup.LookupKey;
import net.nowhereatall.xfty.lookup.LookupKeyLike;
import net.nowhereatall.xfty.lookup.ProviderLookupLike;
import net.nowhereatall.xfty.lookup.ProviderLookups;
import net.nowhereatall.xfty.relationships.DefaultRelationship;
import net.nowhereatall.xfty.relationships.SharedAncestor;
import net.nowhereatall.xfty.values.CopyFromSiblingExpression;
import net.nowhereatall.xfty.values.IncrementingStringExpression;
import net.nowhereatall.xfty.values.UniqueEmailExpression;
import org.junit.jupiter.api.Test;

/**
 * Proves {@code put(List<Field> path, value)} - path-scoped value overrides that
 * land on a generated ancestor, forcing every relationship on the way. MOCK mode
 * throughout.
 */
class PathValueTest {

    private static final Field CONTACT_ACCOUNT_ID = Field.of(Contact.class, "accountId");
    private static final Field ACCOUNT_INDUSTRY = Field.of(Account.class, "industry");
    private static final Field ACCOUNT_NAME = Field.of(Account.class, "name");
    private static final Field ACCOUNT_SITE = Field.of(Account.class, "site");
    private static final Field ACCOUNT_OWNER_ID = Field.of(Account.class, "ownerId");
    private static final Field ACCOUNT_PARENT_ID = Field.of(Account.class, "parentId");

    private static ProviderLookupLike lookup() {
        return ProviderLookups.of(Map.of(
                LookupKey.get(Account.class), new AccountDataProvider(),
                LookupKey.get(Contact.class), new ContactWithOptionalManagerProvider(),
                LookupKey.get(User.class), new LeafUserProvider()));
    }

    @Test
    void put_WithALiteralPathValue_LandsItOnTheGeneratedAncestor() {
        // Act
        Bundle bundle = Async.await(new RecordProvider<>(Contact.class, lookup())
                .setInclusivity(InsertInclusivity.REQUIRED)
                .setInsertMode(InsertMode.MOCK)
                .put(List.of(CONTACT_ACCOUNT_ID, ACCOUNT_INDUSTRY), "Aerospace")
                .supplyBundle());

        // Assert - the path literal overrode the Account Provider default
        Account generatedAccount = (Account) bundle.getBundle(Contact.class, "accountId").primaryRecords().get(0);
        assertEquals("Aerospace", generatedAccount.getIndustry());
    }

    @Test
    void put_WithAValueExpressionPathValue_RunsItOncePerGeneratedAncestor() {
        // Act
        Bundle bundle = Async.await(new RecordProvider<>(Contact.class, lookup())
                .setQuantityPerTemplate(3)
                .setInclusivity(InsertInclusivity.REQUIRED)
                .put(List.of(CONTACT_ACCOUNT_ID, ACCOUNT_NAME), new IncrementingStringExpression("Path Account"))
                .supplyBundle());

        // Assert
        List<Object> accounts = bundle.getBundle(Contact.class, "accountId").primaryRecords();
        assertEquals(3, accounts.size());
        assertEquals(3, accounts.stream().map(a -> ((Account) a).getName()).distinct().count());
    }

    @Test
    void put_WithAContextAwarePathValue_EvaluatesItOnTheGeneratedAncestor() {
        // Act - the generated Account's site copies its own name (a sibling on the ancestor)
        Bundle bundle = Async.await(new RecordProvider<>(Contact.class, lookup())
                .setInclusivity(InsertInclusivity.REQUIRED)
                .setInsertMode(InsertMode.MOCK)
                .put(List.of(CONTACT_ACCOUNT_ID, ACCOUNT_SITE), CopyFromSiblingExpression.from(Account::getName))
                .supplyBundle());

        // Assert
        Account generatedAccount = (Account) bundle.getBundle(Contact.class, "accountId").primaryRecords().get(0);
        assertEquals(generatedAccount.getName(), generatedAccount.getSite());
    }

    @Test
    void putRequired_WithARelationshipPathValue_GivesTheAncestorItsOwnGeneratedParent() {
        // Act
        Bundle bundle = Async.await(new RecordProvider<>(Contact.class, lookup())
                .setInclusivity(InsertInclusivity.REQUIRED)
                .setInsertMode(InsertMode.MOCK)
                .putRequired(List.of(CONTACT_ACCOUNT_ID, ACCOUNT_OWNER_ID), new DefaultRelationship(new User()))
                .supplyBundle());

        // Assert
        Account generatedAccount = (Account) bundle.getBundle(Contact.class, "accountId").primaryRecords().get(0);
        assertNotNull(generatedAccount.getOwnerId());
    }

    @Test
    void put_WithADeepTwoRelationshipPath_WalksBothHopsAndSetsTheTargetField() {
        // Act - Contact -> Account -> Account (self-referencing parentId), set the grandparent's industry
        ProviderLookupLike deepLookup = ProviderLookups.of(Map.of(
                LookupKey.get(Account.class), new AccountWithOptionalParentProvider(),
                LookupKey.get(Contact.class), new ContactWithOptionalManagerProvider()));
        Bundle bundle = Async.await(new RecordProvider<>(Contact.class, deepLookup)
                .setInclusivity(InsertInclusivity.REQUIRED)
                .setInsertMode(InsertMode.MOCK)
                .allowAncestorCycles()
                .put(List.of(CONTACT_ACCOUNT_ID, ACCOUNT_PARENT_ID, ACCOUNT_INDUSTRY), "DeepValue")
                .supplyBundle());

        // Assert
        Bundle accountBundle = bundle.getBundle(Contact.class, "accountId");
        assertNotNull(((Account) accountBundle.primaryRecords().get(0)).getId());
        Account grandparent = (Account) accountBundle.getBundle(Account.class, "parentId").primaryRecords().get(0);
        assertEquals("DeepValue", grandparent.getIndustry());
    }

    @Test
    void put_AtTheDefaultNoneInclusivity_StillForcesTheNamedAncestor() {
        // Act
        Bundle bundle = Async.await(new RecordProvider<>(Contact.class, lookup())
                .setInsertMode(InsertMode.MOCK)
                .put(List.of(CONTACT_ACCOUNT_ID, ACCOUNT_INDUSTRY), "Aerospace")
                .supplyBundle());

        // Assert
        Account generatedAccount = (Account) bundle.getBundle(Contact.class, "accountId").primaryRecords().get(0);
        assertEquals("Aerospace", generatedAccount.getIndustry());
    }

    @Test
    void put_WhenAPathFieldIsNotARelationship_Throws() {
        // Arrange
        RecordProvider<Contact> provider = new RecordProvider<>(Contact.class, lookup())
                .put(List.of(Field.of(Contact.class, "firstName"), ACCOUNT_INDUSTRY), "x");

        // Act / Assert
        XftyConfigurationException thrown =
                assertThrows(XftyConfigurationException.class, () -> Async.await(provider.supplyBundle()));
        assertTrue(thrown.getMessage().toLowerCase().contains("relationship"));
    }

    @Test
    void put_WhenThePathTargetsASharedAncestor_Throws() {
        // Arrange
        String sharedName = "path-value-test-shared-acct";
        SharedAncestor.put(sharedName, Account.builder().name("Shared HQ").build());
        ProviderLookupLike sharedLookup = ProviderLookups.of(Map.of(
                LookupKey.get(Account.class), new AccountDataProvider(),
                LookupKey.get(Contact.class), new ContactUnderSharedAccountProvider(sharedName)));
        RecordProvider<Contact> provider = new RecordProvider<>(Contact.class, sharedLookup)
                .setInclusivity(InsertInclusivity.REQUIRED)
                .put(List.of(CONTACT_ACCOUNT_ID, ACCOUNT_INDUSTRY), "Aerospace");

        // Act / Assert
        XftyConfigurationException thrown =
                assertThrows(XftyConfigurationException.class, () -> Async.await(provider.supplyBundle()));
        assertTrue(thrown.getMessage().toLowerCase().contains("shared ancestor"));
    }

    @Test
    void put_WhenThePathHasNoRelationshipHop_Throws() {
        // Arrange
        RecordProvider<Contact> provider = new RecordProvider<>(Contact.class, lookup());

        // Act / Assert - a one-element path has no relationship to walk
        XftyConfigurationException thrown = assertThrows(XftyConfigurationException.class,
                () -> provider.put(List.of(ACCOUNT_INDUSTRY), "x"));
        assertTrue(thrown.getMessage().contains("at least one relationship"));
    }

    // In-test providers ------------------------------------------

    static final class ContactWithOptionalManagerProvider extends SimpleRecordProvider {
        ContactWithOptionalManagerProvider() {
            super(MasterTemplate.of(Contact::id)
                    .put(Contact::lastName, new IncrementingStringExpression("Contact"))
                    .put(Contact::email, new UniqueEmailExpression("test.contact"))
                    .putRequired(Contact::accountId, new DefaultRelationship(new Account()))
                    .putOptional(Contact::reportsToId, new DefaultRelationship(Contact.builder().build())));
        }
    }

    static final class ContactUnderSharedAccountProvider extends SimpleRecordProvider {
        ContactUnderSharedAccountProvider(String sharedName) {
            super(MasterTemplate.of(Contact::id)
                    .put(Contact::lastName, new IncrementingStringExpression("Contact"))
                    .putRequired(Field.of(Contact.class, "accountId"), SharedAncestor.get(sharedName)));
        }
    }

    static final class AccountWithOptionalParentProvider extends SimpleRecordProvider {
        AccountWithOptionalParentProvider() {
            super(MasterTemplate.of(Account::getId)
                    .put(Account::getName, new IncrementingStringExpression("Account"))
                    .putOptional(Account::getParentId, new DefaultRelationship(new Account())));
        }
    }

    static final class LeafUserProvider extends SimpleRecordProvider {
        LeafUserProvider() {
            super(MasterTemplate.of(User::getId)
                    .put(User::getLastName, new IncrementingStringExpression("User"))
                    .put(User::getEmail, new UniqueEmailExpression("test.user")));
        }
    }
}
