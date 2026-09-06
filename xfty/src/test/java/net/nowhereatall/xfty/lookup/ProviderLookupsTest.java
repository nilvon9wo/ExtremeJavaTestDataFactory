package net.nowhereatall.xfty.lookup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashMap;
import java.util.Map;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.core.RecordProviderLike;
import net.nowhereatall.xfty.demo.Account;
import net.nowhereatall.xfty.predicates.FieldPredicateFactory;
import org.junit.jupiter.api.Test;

class ProviderLookupsTest {

    @Test
    void resolvesThePlainTypeKeyWhenNothingRefinedMatches() {
        // Arrange
        ProviderLookupLike lookup = ProviderLookups.of(Map.of(
                LookupKey.get(Account.class), new StubRecordProvider(Account.class, "default")));
        Account account = new Account();

        // Act
        LookupKeyLike resolved = ProviderLookups.resolve(lookup, account);

        // Assert
        assertEquals(LookupKey.get(Account.class).hashKey(), resolved.hashKey());
    }

    @Test
    void resolvesTheMostSpecificMatchingVariant() {
        // Arrange
        FlavouredLookupKey techKey = FlavouredLookupKey.get(Account.class, "resolve-tech")
                .matching(FieldPredicateFactory.equalTo(Field.of(Account.class, "industry"), "Technology"));
        Map<LookupKeyLike, RecordProviderLike> providers = new LinkedHashMap<>();
        providers.put(LookupKey.get(Account.class), new StubRecordProvider(Account.class, "default"));
        providers.put(techKey, new StubRecordProvider(Account.class, "tech"));
        ProviderLookupLike lookup = ProviderLookups.of(providers);
        Account tech = new Account();
        tech.setIndustry("Technology");

        // Act
        LookupKeyLike resolved = ProviderLookups.resolve(lookup, tech);

        // Assert
        assertEquals(techKey.hashKey(), resolved.hashKey());
    }

    @Test
    void rejectsTwoEquallySpecificMatchesAsAmbiguous() {
        // Arrange
        FlavouredLookupKey oneKey = FlavouredLookupKey.get(Account.class, "ambiguous-a")
                .matching(FieldPredicateFactory.equalTo(Field.of(Account.class, "industry"), "Technology"));
        FlavouredLookupKey twoKey = FlavouredLookupKey.get(Account.class, "ambiguous-b")
                .matching(FieldPredicateFactory.equalTo(Field.of(Account.class, "active"), true));
        Map<LookupKeyLike, RecordProviderLike> providers = new LinkedHashMap<>();
        providers.put(oneKey, new StubRecordProvider(Account.class, "a"));
        providers.put(twoKey, new StubRecordProvider(Account.class, "b"));
        ProviderLookupLike lookup = ProviderLookups.of(providers);
        Account both = new Account();
        both.setIndustry("Technology");
        both.setActive(true);

        // Act / Assert
        assertThrows(LookupException.class, () -> ProviderLookups.resolve(lookup, both));
    }

    @Test
    void reconcileReturnsNullWhenNeitherAKeyNorATemplateIsGiven() {
        // Arrange
        ProviderLookupLike lookup = ProviderLookups.of(Map.of());

        // Act
        LookupKeyLike reconciled = ProviderLookups.reconcile(lookup, null, null);

        // Assert
        assertEquals(null, reconciled);
    }

    @Test
    void reconcileDerivesTheKeyFromAnOverrideTemplateWhenNoExplicitKeyIsGiven() {
        // Arrange
        FlavouredLookupKey techKey = FlavouredLookupKey.get(Account.class, "reconcile-tech")
                .matching(FieldPredicateFactory.equalTo(Field.of(Account.class, "industry"), "Technology"));
        Map<LookupKeyLike, RecordProviderLike> providers = new LinkedHashMap<>();
        providers.put(LookupKey.get(Account.class), new StubRecordProvider(Account.class, "default"));
        providers.put(techKey, new StubRecordProvider(Account.class, "tech"));
        ProviderLookupLike lookup = ProviderLookups.of(providers);
        Account template = new Account();
        template.setIndustry("Technology");

        // Act
        LookupKeyLike reconciled = ProviderLookups.reconcile(lookup, null, template);

        // Assert
        assertEquals(techKey.hashKey(), reconciled.hashKey());
    }

    @Test
    void reconcileRejectsAnExplicitKeyThatContradictsTheTemplate() {
        // Arrange
        FlavouredLookupKey techKey = FlavouredLookupKey.get(Account.class, "contradict-tech")
                .matching(FieldPredicateFactory.equalTo(Field.of(Account.class, "industry"), "Technology"));
        FlavouredLookupKey financeKey = FlavouredLookupKey.get(Account.class, "contradict-finance")
                .matching(FieldPredicateFactory.equalTo(Field.of(Account.class, "industry"), "Finance"));
        Map<LookupKeyLike, RecordProviderLike> providers = new LinkedHashMap<>();
        providers.put(techKey, new StubRecordProvider(Account.class, "tech"));
        providers.put(financeKey, new StubRecordProvider(Account.class, "finance"));
        ProviderLookupLike lookup = ProviderLookups.of(providers);
        Account financeTemplate = new Account();
        financeTemplate.setIndustry("Finance");

        // Act / Assert
        assertThrows(LookupException.class, () -> ProviderLookups.reconcile(lookup, techKey, financeTemplate));
    }

    @Test
    void lazilyInstantiatesAProviderRegisteredByTypeAndCachesIt() {
        // Arrange
        Map<LookupKeyLike, Class<? extends RecordProviderLike>> byType = Map.of(
                LookupKey.get(Account.class), StubRecordProvider.class);
        ProviderLookupLike lookup = ProviderLookups.ofTypes(byType);

        // Act
        RecordProviderLike first = lookup.get(Account.class);
        RecordProviderLike second = lookup.get(Account.class);

        // Assert
        assertSame(first, second);
    }

    @Test
    void raisesANamedErrorForAnUnregisteredKey() {
        // Arrange
        ProviderLookupLike lookup = ProviderLookups.of(Map.of());

        // Act / Assert
        assertThrows(LookupException.class, () -> lookup.get(Account.class));
    }
}
