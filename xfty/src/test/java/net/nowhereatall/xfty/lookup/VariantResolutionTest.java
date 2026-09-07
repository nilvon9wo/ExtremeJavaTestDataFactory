package net.nowhereatall.xfty.lookup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import net.nowhereatall.xfty.Async;
import net.nowhereatall.xfty.core.InsertMode;
import net.nowhereatall.xfty.core.RecordProvider;
import net.nowhereatall.xfty.core.RecordProviderLike;
import net.nowhereatall.xfty.demo.Account;
import net.nowhereatall.xfty.demo.AccountDataProvider;
import net.nowhereatall.xfty.predicates.FieldPredicateFactory;
import org.junit.jupiter.api.Test;

/**
 * Proves how the two ways to name a Provider variant - an explicit lookup key
 * and an override template - are reconciled: {@link ProviderLookups#reconcile}
 * directly, and {@link RecordProvider} end to end.
 */
class VariantResolutionTest {

    private static final LookupKeyLike BIG = FlavouredLookupKey.get(Account.class, "reconcile-big")
            .matching(FieldPredicateFactory.greaterThan(Account::getNumberOfEmployees, 1000));

    private static final LookupKeyLike SMALL = FlavouredLookupKey.get(Account.class, "reconcile-small")
            .matching(FieldPredicateFactory.lessThan(Account::getNumberOfEmployees, 10));

    private static ProviderLookupLike lookup() {
        Map<LookupKeyLike, Class<? extends RecordProviderLike>> byKey = new LinkedHashMap<>();
        byKey.put(LookupKey.get(Account.class), AccountDataProvider.class);
        byKey.put(BIG, AccountDataProvider.class);
        byKey.put(SMALL, AccountDataProvider.class);
        return ProviderLookups.ofTypes(byKey);
    }

    @Test
    void reconcile_WhenGivenNeitherKeyNorTemplate_ReturnsNull() {
        assertReconcile(null, null, null);
    }

    @Test
    void reconcile_WhenGivenOnlyATemplate_DerivesTheKeyFromIt() {
        assertReconcile(null, Account.builder().numberOfEmployees(5000).build(), BIG.hashKey());
    }

    @Test
    void reconcile_WhenTheTemplateAgreesWithTheExplicitKey_KeepsTheExplicitKey() {
        assertReconcile(BIG, Account.builder().numberOfEmployees(5000).build(), BIG.hashKey());
    }

    @Test
    void reconcile_WhenTheTemplateCarriesNoDiscriminator_KeepsTheExplicitKey() {
        assertReconcile(BIG, new Account(), BIG.hashKey());
    }

    @Test
    void reconcile_WhenTheTemplateMatchesADifferentRefinedVariant_Throws() {
        // Act
        LookupException thrown = assertThrows(LookupException.class,
                () -> ProviderLookups.reconcile(lookup(), BIG, Account.builder().numberOfEmployees(2).build()));

        // Assert
        assertTrue(thrown.getMessage().contains("contradicts"));
    }

    @Test
    void supply_WhenWithVariantContradictsTheOverrideTemplate_Throws() {
        // Arrange
        RecordProvider<Account> provider = new RecordProvider<>(Account.class, lookup())
                .withVariant(BIG)
                .setOverrideTemplate(Account.builder().numberOfEmployees(2).build())
                .setInsertMode(InsertMode.MOCK);

        // Act
        LookupException thrown = assertThrows(LookupException.class, () -> Async.await(provider.supply()));

        // Assert
        assertTrue(thrown.getMessage().contains("contradicts"));
    }

    @Test
    void supply_WhenWithVariantAgreesWithTheOverrideTemplate_StillAppliesTheTemplate() {
        // Act
        Account result = Async.await(new RecordProvider<>(Account.class, lookup())
                .withVariant(BIG)
                .setOverrideTemplate(Account.builder().numberOfEmployees(5000).build())
                .setInsertMode(InsertMode.MOCK)
                .supply());

        // Assert
        assertEquals(5000, result.getNumberOfEmployees());
    }

    private static void assertReconcile(LookupKeyLike explicitKey, Object template, String expectedHash) {
        LookupKeyLike resolved = ProviderLookups.reconcile(lookup(), explicitKey, template);
        assertEquals(expectedHash, resolved == null ? null : resolved.hashKey());
    }
}
