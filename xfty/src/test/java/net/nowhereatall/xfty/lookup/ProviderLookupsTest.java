package net.nowhereatall.xfty.lookup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashMap;
import java.util.Map;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.core.RecordProvider;
import net.nowhereatall.xfty.demo.Account;
import net.nowhereatall.xfty.predicates.FieldPredicateFactory;
import org.junit.jupiter.api.Test;

class ProviderLookupsTest {

    @Test
    void resolvesThePlainTypeKeyWhenNothingRefinedMatches() {
        // Arrange
        ProviderLookup lookup = ProviderLookups.of(Map.of(
                TypeLookupKey.get(Account.class), new StubRecordProvider(Account.class, "default")));
        Account account = new Account();

        // Act
        LookupKey resolved = ProviderLookups.resolve(lookup, account);

        // Assert
        assertEquals(TypeLookupKey.get(Account.class).hashKey(), resolved.hashKey());
    }

    @Test
    void resolvesTheMostSpecificMatchingVariant() {
        // Arrange
        FlavouredLookupKey techKey = FlavouredLookupKey.get(Account.class, "resolve-tech")
                .matching(FieldPredicateFactory.equalTo(Field.of(Account.class, "industry"), "Technology"));
        Map<LookupKey, RecordProvider> providers = new LinkedHashMap<>();
        providers.put(TypeLookupKey.get(Account.class), new StubRecordProvider(Account.class, "default"));
        providers.put(techKey, new StubRecordProvider(Account.class, "tech"));
        ProviderLookup lookup = ProviderLookups.of(providers);
        Account tech = new Account();
        tech.setIndustry("Technology");

        // Act
        LookupKey resolved = ProviderLookups.resolve(lookup, tech);

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
        Map<LookupKey, RecordProvider> providers = new LinkedHashMap<>();
        providers.put(oneKey, new StubRecordProvider(Account.class, "a"));
        providers.put(twoKey, new StubRecordProvider(Account.class, "b"));
        ProviderLookup lookup = ProviderLookups.of(providers);
        Account both = new Account();
        both.setIndustry("Technology");
        both.setActive(true);

        // Act / Assert
        assertThrows(LookupException.class, () -> ProviderLookups.resolve(lookup, both));
    }

    @Test
    void reconcileReturnsNullWhenNeitherAKeyNorATemplateIsGiven() {
        // Arrange
        ProviderLookup lookup = ProviderLookups.of(Map.of());

        // Act
        LookupKey reconciled = ProviderLookups.reconcile(lookup, null, null);

        // Assert
        assertEquals(null, reconciled);
    }

    @Test
    void reconcileDerivesTheKeyFromAnOverrideTemplateWhenNoExplicitKeyIsGiven() {
        // Arrange
        FlavouredLookupKey techKey = FlavouredLookupKey.get(Account.class, "reconcile-tech")
                .matching(FieldPredicateFactory.equalTo(Field.of(Account.class, "industry"), "Technology"));
        Map<LookupKey, RecordProvider> providers = new LinkedHashMap<>();
        providers.put(TypeLookupKey.get(Account.class), new StubRecordProvider(Account.class, "default"));
        providers.put(techKey, new StubRecordProvider(Account.class, "tech"));
        ProviderLookup lookup = ProviderLookups.of(providers);
        Account template = new Account();
        template.setIndustry("Technology");

        // Act
        LookupKey reconciled = ProviderLookups.reconcile(lookup, null, template);

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
        Map<LookupKey, RecordProvider> providers = new LinkedHashMap<>();
        providers.put(techKey, new StubRecordProvider(Account.class, "tech"));
        providers.put(financeKey, new StubRecordProvider(Account.class, "finance"));
        ProviderLookup lookup = ProviderLookups.of(providers);
        Account financeTemplate = new Account();
        financeTemplate.setIndustry("Finance");

        // Act / Assert
        assertThrows(LookupException.class, () -> ProviderLookups.reconcile(lookup, techKey, financeTemplate));
    }

    @Test
    void lazilyInstantiatesAProviderRegisteredByTypeAndCachesIt() {
        // Arrange
        Map<LookupKey, Class<? extends RecordProvider>> byType = Map.of(
                TypeLookupKey.get(Account.class), StubRecordProvider.class);
        ProviderLookup lookup = ProviderLookups.ofTypes(byType);

        // Act
        RecordProvider first = lookup.get(Account.class);
        RecordProvider second = lookup.get(Account.class);

        // Assert
        assertSame(first, second);
    }

    @Test
    void raisesANamedErrorForAnUnregisteredKey() {
        // Arrange
        ProviderLookup lookup = ProviderLookups.of(Map.of());

        // Act / Assert
        assertThrows(LookupException.class, () -> lookup.get(Account.class));
    }
}
