package net.nowhereatall.xfty.values;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.nowhereatall.xfty.Async;
import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.XftyConfigurationException;
import net.nowhereatall.xfty.core.GenerationContext;
import net.nowhereatall.xfty.core.InsertInclusivity;
import net.nowhereatall.xfty.core.InsertMode;
import net.nowhereatall.xfty.core.MasterTemplate;
import net.nowhereatall.xfty.core.RecordProvider;
import net.nowhereatall.xfty.core.SimpleRecordProvider;
import net.nowhereatall.xfty.demo.Account;
import net.nowhereatall.xfty.demo.AccountDataProvider;
import net.nowhereatall.xfty.demo.Contact;
import net.nowhereatall.xfty.demo.ContactDataProvider;
import net.nowhereatall.xfty.demo.DefaultProviderLookup;
import net.nowhereatall.xfty.demo.User;
import net.nowhereatall.xfty.lookup.LookupKey;
import net.nowhereatall.xfty.lookup.ProviderLookupLike;
import net.nowhereatall.xfty.lookup.ProviderLookups;
import net.nowhereatall.xfty.relationships.DefaultRelationship;
import org.junit.jupiter.api.Test;

/**
 * Proves context-aware value generation driven end to end through
 * {@code RecordProvider.supply()} - the second value pass, {@code CopyFromSibling},
 * {@code CopyFromAncestor}, and a custom {@link ContextAwareExpressionLike}.
 */
class ContextAwareExpressionTest {

    private static final DefaultProviderLookup DEFAULT_LOOKUP = new DefaultProviderLookup();

    private static RecordProvider<Account> accountProvider() {
        return new RecordProvider<>(Account.class, DEFAULT_LOOKUP).setInsertMode(InsertMode.MOCK);
    }

    private static RecordProvider<Contact> contactProvider() {
        return new RecordProvider<>(Contact.class, DEFAULT_LOOKUP).setInsertMode(InsertMode.MOCK);
    }

    // CopyFromSiblingExpression ----------------------------------------

    @Test
    void supply_ForACopyFromSibling_TakesTheSiblingsPlainValue() {
        // Arrange
        RecordProvider<Account> provider = accountProvider()
                .put(Account::getShippingCity, "Berlin")
                .put(Account::getBillingCity, CopyFromSiblingExpression.from(Account::getShippingCity));

        // Act
        Account result = Async.await(provider.supply());

        // Assert
        assertEquals("Berlin", result.getBillingCity());
    }

    @Test
    void supply_ForACopyFromSibling_SeesAnEarlierContextAwareSibling() {
        // Arrange - ShippingCity (plain) -> BillingCity (reads ShippingCity) -> BillingStreet (reads BillingCity)
        RecordProvider<Account> provider = accountProvider()
                .put(Account::getShippingCity, "Munich")
                .put(Account::getBillingCity, CopyFromSiblingExpression.from(Account::getShippingCity))
                .put(Account::getBillingStreet, CopyFromSiblingExpression.from(Account::getBillingCity));

        // Act
        Account result = Async.await(provider.supply());

        // Assert
        assertEquals("Munich", result.getBillingStreet());
    }

    @Test
    void supply_ForACopyFromSibling_DoesNotOverrideAValueTheOverrideTemplateSupplied() {
        // Arrange
        RecordProvider<Account> provider = accountProvider()
                .put(Account::getShippingCity, "Hamburg")
                .put(Account::getBillingCity, CopyFromSiblingExpression.from(Account::getShippingCity))
                .setOverrideTemplate(Account.builder().build());
        provider.setOverrideTemplate(explicitBillingCity());

        // Act
        Account result = Async.await(provider.supply());

        // Assert - the override template still wins
        assertEquals("Explicit", result.getBillingCity());
    }

    private static Account explicitBillingCity() {
        Account template = new Account();
        template.setBillingCity("Explicit");
        return template;
    }

    @Test
    void supply_ForACopyFromSibling_WhenTheSiblingItReadsIsPutAfterIt_Throws() {
        // Arrange - Description (reader) is put before Site (a context-aware value it reads)
        RecordProvider<Account> provider = accountProvider()
                .put(Account::getDescription, CopyFromSiblingExpression.from(Account::getSite))
                .put(Account::getSite, CopyFromSiblingExpression.from(Account::getAccountNumber))
                .put(Account::getAccountNumber, "seed");

        // Act
        XftyConfigurationException thrown =
                assertThrows(XftyConfigurationException.class, () -> Async.await(provider.supply()));

        // Assert
        assertTrue(thrown.getMessage().contains("site"));
        assertTrue(thrown.getMessage().contains("has not been generated yet"));
        assertTrue(thrown.getMessage().contains("before"));
    }

    @Test
    void supply_ForACopyFromSibling_WhenTwoSiblingsReadEachOther_Throws() {
        // Arrange
        RecordProvider<Account> provider = accountProvider()
                .put(Account::getDescription, CopyFromSiblingExpression.from(Account::getSite))
                .put(Account::getSite, CopyFromSiblingExpression.from(Account::getDescription));

        // Act
        XftyConfigurationException thrown =
                assertThrows(XftyConfigurationException.class, () -> Async.await(provider.supply()));

        // Assert
        assertTrue(thrown.getMessage().contains("has not been generated yet"));
    }

    // CopyFromAncestorExpression --------------------------------------

    @Test
    void supply_ForACopyFromAncestor_TakesAFieldFromTheGeneratedParent() {
        // Arrange
        RecordProvider<Contact> provider = contactProvider()
                .putRequired(Contact::accountId,
                        new DefaultRelationship(Account.builder().name("Wired Parent").build()))
                .put(Contact::department, CopyFromAncestorExpression.from(Contact::accountId, Account::getName))
                .setInclusivity(InsertInclusivity.REQUIRED);

        // Act
        Contact result = Async.await(provider.supply());

        // Assert
        assertEquals("Wired Parent", result.department());
    }

    @Test
    void supply_ForACopyFromAncestor_WhenTheRelationshipWasNotGenerated_IsNull() {
        // Arrange
        RecordProvider<Contact> provider = contactProvider()
                .removeFromMasterTemplate(Contact::accountId)
                .put(Contact::department, CopyFromAncestorExpression.from(Contact::accountId, Account::getName))
                .setInclusivity(InsertInclusivity.NONE);

        // Act
        Contact result = Async.await(provider.supply());

        // Assert - no ancestor generated -> null
        assertNull(result.department());
    }

    @Test
    void supplyList_ForACopyFromAncestor_WithQuantity_AppliesPerRow() {
        // Arrange - the bundled Account Provider gives each generated Account an incrementing name
        RecordProvider<Contact> provider = contactProvider()
                .put(Contact::department, CopyFromAncestorExpression.from(Contact::accountId, Account::getName))
                .setQuantityPerTemplate(3)
                .setInclusivity(InsertInclusivity.REQUIRED);

        // Act
        List<Contact> results = Async.await(provider.supplyList());

        // Assert
        long distinctDepartments = results.stream().map(Contact::department).distinct().count();
        assertEquals(3, distinctDepartments);
    }

    @Test
    void supply_ForACopyFromAncestor_FollowsAMultiHopPath() {
        // Arrange - Contact -> Account -> owner(User); copy the generated owner's lastName onto the Contact
        ProviderLookupLike lookup = ProviderLookups.of(Map.of(
                LookupKey.get(Contact.class), new ContactDataProvider(),
                LookupKey.get(Account.class), new AccountWithOwnerProvider(),
                LookupKey.get(User.class), new LeafUserProvider()));
        RecordProvider<Contact> provider = new RecordProvider<>(Contact.class, lookup)
                .put(Field.of(Contact.class, "department"), new CopyFromAncestorExpression(List.of(
                        Field.of(Contact.class, "accountId"),
                        Field.of(Account.class, "ownerId"),
                        Field.of(User.class, "lastName"))))
                .setInclusivity(InsertInclusivity.REQUIRED)
                .setInsertMode(InsertMode.MOCK);

        // Act
        Contact result = Async.await(provider.supply());

        // Assert - the Account owner's lastName was copied two hops up
        assertNotNull(result.department());
    }

    // A custom context-aware expression ------------------------------

    @Test
    void supply_ForACustomContextAwareExpression_CanDeriveFromASibling() {
        // Arrange
        RecordProvider<Contact> provider = contactProvider()
                .put(Contact::birthdate, LocalDate.of(2010, 1, 1))
                .put(Field.of(Contact.class, "department"), new IsMinorFlag(Field.of(Contact.class, "birthdate")));

        // Act
        Contact result = Async.await(provider.supply());

        // Assert
        assertEquals("MINOR", result.department());
    }

    @Test
    void supplyList_ForACustomContextAwareExpression_SeesTheSiblingPrimaryRecordsInBundleSoFar() {
        // Arrange
        RecordProvider<Account> provider = accountProvider()
                .put(Field.of(Account.class, "description"), new SiblingCountLabel())
                .setQuantityPerTemplate(3);

        // Act
        List<Account> accounts = Async.await(provider.supplyList());

        // Assert - each row sees all three sibling primaries and its own rowIndex
        List<String> labels = accounts.stream().map(Account::getDescription).collect(Collectors.toList());
        assertEquals(List.of("1 of 3", "2 of 3", "3 of 3"), labels);
    }

    /** An Account whose owner is generated, so multi-hop tests have a second level. */
    static final class AccountWithOwnerProvider extends SimpleRecordProvider {
        AccountWithOwnerProvider() {
            super(MasterTemplate.of(Account::getId)
                    .put(Account::getName, new IncrementingStringExpression("Acct"))
                    .putRequired(Account::getOwnerId, new DefaultRelationship(new User())));
        }
    }

    static final class LeafUserProvider extends SimpleRecordProvider {
        LeafUserProvider() {
            super(MasterTemplate.of(User::getId).put(User::getLastName, new IncrementingStringExpression("User")));
        }
    }

    /** Derives a MINOR / ADULT flag from a birthdate sibling. */
    static final class IsMinorFlag implements ContextAwareExpressionLike {
        private final Field birthdateField;

        IsMinorFlag(Field birthdateField) {
            this.birthdateField = birthdateField;
        }

        @Override
        public Object get(GenerationContext context) {
            LocalDate birthdate = (LocalDate) this.birthdateField.get(context.recordBeingBuilt());
            return birthdate != null && birthdate.plusYears(18).isAfter(LocalDate.now()) ? "MINOR" : "ADULT";
        }
    }

    /** Reads the whole batch of sibling primary records out of bundleSoFar. */
    static final class SiblingCountLabel implements ContextAwareExpressionLike {
        @Override
        public Object get(GenerationContext context) {
            int siblingCount = context.bundleSoFar().getList(Field.of(Account.class, "id")).size();
            return (context.rowIndex() + 1) + " of " + siblingCount;
        }
    }
}
