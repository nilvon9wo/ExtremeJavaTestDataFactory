package net.nowhereatall.xfty.lookup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import net.nowhereatall.xfty.XftyConfigurationException;
import net.nowhereatall.xfty.core.RecordProviderLike;
import net.nowhereatall.xfty.demo.Account;
import net.nowhereatall.xfty.demo.AccountDataProvider;
import net.nowhereatall.xfty.demo.Contact;
import net.nowhereatall.xfty.predicates.FieldPredicateFactory;
import org.junit.jupiter.api.Test;

/**
 * Proves the lookup-key types ({@link LookupKey}, {@link FlavouredLookupKey}) and
 * {@link ProviderLookups} resolution. A discriminator-field variant is proven
 * separately in {@code DiscriminatorLookupKeyTest}.
 */
class LookupKeyTest {

    // Flavoured keys are interned flyweights whose matching(...) predicates
    // mutate the shared instance - build each exactly once, here.
    private static final FlavouredLookupKey ENTERPRISE_FLAVOUR =
            FlavouredLookupKey.get(Account.class, "enterprise")
                    .matching(FieldPredicateFactory.greaterThan(Account::getNumberOfEmployees, 500));

    private static final FlavouredLookupKey NAMED_FLAVOUR =
            FlavouredLookupKey.get(Account.class, "named-runner")
                    .matching(FieldPredicateFactory.isNotNull(Account::getName));

    private static final FlavouredLookupKey BIG_ACCOUNT =
            FlavouredLookupKey.get(Account.class, "big")
                    .matching(FieldPredicateFactory.greaterThan(Account::getNumberOfEmployees, 100));

    // LookupKey --------------------------------------------------------

    @Test
    void hashKey_ForAPlainKey_IsTheRecordTypeName() {
        // Arrange
        LookupKey key = LookupKey.get(Account.class);

        // Act
        String hashKey = key.hashKey();

        // Assert
        assertTrue(hashKey.contains("Account"));
    }

    @Test
    void recordType_ForAPlainKey_IsTheTypeItWasBuiltFor() {
        // Arrange
        LookupKey key = LookupKey.get(Account.class);

        // Act
        Class<?> type = key.recordType();

        // Assert
        assertEquals(Account.class, type);
    }

    @Test
    void specificity_ForAPlainKey_IsZero() {
        // Arrange
        LookupKey key = LookupKey.get(Account.class);

        // Act / Assert
        assertEquals(0, key.specificity());
    }

    @Test
    void isInstanceOf_WhenTheRecordIsOfThatType_ReturnsTrue() {
        assertPlainKeyIsInstanceOf(new Account(), true);
    }

    @Test
    void isInstanceOf_WhenTheRecordIsADifferentType_ReturnsFalse() {
        assertPlainKeyIsInstanceOf(Contact.builder().build(), false);
    }

    @Test
    void isInstanceOf_WhenTheRecordIsNull_ReturnsFalse() {
        assertPlainKeyIsInstanceOf(null, false);
    }

    @Test
    void get_ForATypeAndForARecordOfThatType_ReturnsTheOneInternedInstance() {
        // Act
        LookupKey fromType = LookupKey.get(Account.class);

        // Assert - get(type) and get(record) intern the same key
        assertEquals(fromType, LookupKey.get(new Account()));
        assertSame(fromType, LookupKey.get(new Account()));
    }

    @Test
    void get_WhenTheTypeIsNull_Throws() {
        // Act
        XftyConfigurationException thrown =
                assertThrows(XftyConfigurationException.class, () -> LookupKey.get((Class<?>) null));

        // Assert - a null type must be rejected
        assertTrue(thrown.getMessage().contains("requires a record type"));
    }

    // FlavouredLookupKey ---------------------------------------------

    @Test
    void isInstanceOf_WhenEveryPredicateHolds_ReturnsTrue() {
        assertEnterpriseFlavourIsInstanceOf(Account.builder().numberOfEmployees(1000).build(), true);
    }

    @Test
    void isInstanceOf_WhenAPredicateFails_ReturnsFalse() {
        assertEnterpriseFlavourIsInstanceOf(Account.builder().numberOfEmployees(10).build(), false);
    }

    @Test
    void isInstanceOf_WhenThePredicatedFieldIsBlank_ReturnsFalse() {
        assertEnterpriseFlavourIsInstanceOf(new Account(), false);
    }

    @Test
    void isInstanceOf_ForAFlavouredKey_WhenTheRecordIsADifferentType_ReturnsFalse() {
        assertEnterpriseFlavourIsInstanceOf(Contact.builder().build(), false);
    }

    @Test
    void isInstanceOf_WhenAPredicateHolds_ReturnsTrue() {
        assertNamedFlavourIsInstanceOf(Account.builder().name("x").build(), true);
    }

    @Test
    void isInstanceOf_WhenThePredicateFails_ReturnsFalse() {
        assertNamedFlavourIsInstanceOf(new Account(), false);
    }

    @Test
    void flavouredGet_IsTheSameInternedInstance() {
        // Act
        FlavouredLookupKey again = FlavouredLookupKey.get(Account.class, "named-runner");

        // Assert
        assertSame(NAMED_FLAVOUR, again);
    }

    @Test
    void hashKey_ForAFlavouredKey_IsTypeAndFlavour() {
        // Arrange
        FlavouredLookupKey key = FlavouredLookupKey.get(Account.class, "named");

        // Act
        String hashKey = key.hashKey();

        // Assert
        assertTrue(hashKey.contains("Account"));
        assertTrue(hashKey.contains("named"));
    }

    @Test
    void specificity_ForAFlavouredKey_GrowsWithEachPredicateAndBeatsAPlainKey() {
        // Arrange
        FlavouredLookupKey onePredicate = FlavouredLookupKey.get(Account.class, "hashkey-a")
                .matching(FieldPredicateFactory.isNotNull(Account::getName));
        FlavouredLookupKey twoPredicates = FlavouredLookupKey.get(Account.class, "hashkey-b")
                .matching(FieldPredicateFactory.isNotNull(Account::getName))
                .matching(FieldPredicateFactory.isNotNull(Account::getIndustry));

        // Act
        int oneSpecificity = onePredicate.specificity();

        // Assert
        assertTrue(twoPredicates.specificity() > oneSpecificity);
        assertTrue(oneSpecificity > LookupKey.get(Account.class).specificity());
    }

    @Test
    void isInstanceOf_WhenTheFlavourHasNoPredicates_ReturnsFalse() {
        // Arrange
        FlavouredLookupKey key = FlavouredLookupKey.get(Account.class, "no-discriminator");

        // Act / Assert - a flavour with nothing on the record to match can only be used explicitly
        assertFalse(key.isInstanceOf(new Account()));
    }

    // Lookup keys as map keys --------------------------------------

    @Test
    void get_OnAMapKeyedByLookupKey_FindsTheEntryByValueEquality() {
        // Arrange
        Map<LookupKeyLike, String> byKey = new LinkedHashMap<>();
        byKey.put(LookupKey.get(Account.class), "plain");
        byKey.put(FlavouredLookupKey.get(Account.class, "hashkey-map"), "flavoured");

        // Act / Assert
        assertEquals("plain", byKey.get(LookupKey.get(Account.class)));
        assertEquals("flavoured", byKey.get(FlavouredLookupKey.get(Account.class, "hashkey-map")));
    }

    // ProviderLookups ---------------------------------------------

    @Test
    void get_ForARegisteredKey_ReturnsTheProviderAndCachesTheInstance() {
        // Arrange
        ProviderLookupLike lookup = ProviderLookups.ofTypes(
                Map.of(LookupKey.get(Account.class), AccountDataProvider.class));

        // Act
        RecordProviderLike first = lookup.get(LookupKey.get(Account.class));

        // Assert
        assertTrue(first instanceof AccountDataProvider);
        assertSame(first, lookup.get(Account.class));
    }

    @Test
    void get_OnAnInstanceMapLookup_ReturnsTheRegisteredProvider() {
        // Arrange
        RecordProviderLike provider = new AccountDataProvider();
        ProviderLookupLike lookup = ProviderLookups.of(Map.of(LookupKey.get(Account.class), provider));

        // Act / Assert
        assertSame(provider, lookup.get(Account.class));
        assertSame(provider, ProviderLookups.get(lookup, Account.class));
    }

    @Test
    void get_ForAnUnregisteredKeyOnATypeMapLookup_Throws() {
        // Arrange
        ProviderLookupLike lookup = ProviderLookups.ofTypes(Map.of());

        // Act
        LookupException thrown = assertThrows(LookupException.class,
                () -> lookup.get(FlavouredLookupKey.get(Account.class, "unregistered")));

        // Assert
        assertTrue(thrown.getMessage().contains("Account"));
    }

    @Test
    void get_ForAnUnregisteredKeyOnAnInstanceMapLookup_Throws() {
        // Arrange
        ProviderLookupLike lookup = ProviderLookups.of(Map.of());

        // Act
        LookupException thrown = assertThrows(LookupException.class, () -> lookup.get(Contact.class));

        // Assert
        assertTrue(thrown.getMessage().contains("Contact"));
    }

    @Test
    void get_WhenTheKeyIsNull_Throws() {
        // Act
        LookupException thrown = assertThrows(LookupException.class,
                () -> ProviderLookups.get(new java.util.HashMap<LookupKeyLike, RecordProviderLike>(), (LookupKeyLike) null));

        // Assert
        assertTrue(thrown.getMessage().contains("lookup key is required"));
    }

    @Test
    void keysFor_WhenGivenANullRecord_Throws() {
        // Act
        LookupException thrown = assertThrows(LookupException.class,
                () -> ProviderLookups.keysFor(new LinkedHashSet<LookupKeyLike>(), null));

        // Assert
        assertTrue(thrown.getMessage().contains("record is required"));
    }

    @Test
    void keysFor_SkipsKeysRegisteredForOtherRecordTypes() {
        // Arrange
        Set<LookupKeyLike> registered = new LinkedHashSet<>(
                Set.of(LookupKey.get(Account.class), LookupKey.get(Contact.class)));

        // Act
        Set<LookupKeyLike> matches = ProviderLookups.keysFor(registered, new Account());

        // Assert
        assertEquals(1, matches.size());
        assertEquals(Account.class, matches.iterator().next().recordType());
    }

    @Test
    void keysFor_WhenARecordMatchesARefinedKey_ReturnsBothItAndThePlainKey() {
        assertKeysForCount(Account.builder().numberOfEmployees(500).build(), 2);
    }

    @Test
    void keysFor_WhenARecordMatchesOnlyThePlainKey_ReturnsJustThePlainKey() {
        assertKeysForCount(Account.builder().numberOfEmployees(1).build(), 1);
    }

    @Test
    void resolve_WhenARefinedKeyMatches_PicksTheMostSpecific() {
        // Arrange
        ProviderLookupLike lookup = ProviderLookups.ofTypes(providerTypeMap());

        // Act
        LookupKeyLike resolved = ProviderLookups.resolve(lookup, Account.builder().numberOfEmployees(500).build());

        // Assert
        assertEquals(BIG_ACCOUNT.hashKey(), resolved.hashKey());
    }

    @Test
    void resolve_WhenNothingRefinedMatches_PicksThePlainKey() {
        // Arrange
        ProviderLookupLike lookup = ProviderLookups.ofTypes(providerTypeMap());

        // Act
        LookupKeyLike resolved = ProviderLookups.resolve(lookup, Account.builder().numberOfEmployees(1).build());

        // Assert
        assertEquals(LookupKey.get(Account.class).hashKey(), resolved.hashKey());
    }

    @Test
    void resolve_WhenTwoEquallySpecificKeysMatch_Throws() {
        // Arrange
        Map<LookupKeyLike, Class<? extends RecordProviderLike>> byKey = new LinkedHashMap<>();
        byKey.put(FlavouredLookupKey.get(Account.class, "ambiguous-a")
                .matching(FieldPredicateFactory.isNotNull(Account::getName)), AccountDataProvider.class);
        byKey.put(FlavouredLookupKey.get(Account.class, "ambiguous-b")
                .matching(FieldPredicateFactory.isNotNull(Account::getName)), AccountDataProvider.class);
        ProviderLookupLike lookup = ProviderLookups.ofTypes(byKey);

        // Act
        LookupException thrown = assertThrows(LookupException.class,
                () -> ProviderLookups.resolve(lookup, Account.builder().name("x").build()));

        // Assert
        assertTrue(thrown.getMessage().contains("Ambiguous"));
    }

    // Helpers ---------------------------------------------------

    private static Map<LookupKeyLike, Class<? extends RecordProviderLike>> providerTypeMap() {
        Map<LookupKeyLike, Class<? extends RecordProviderLike>> byKey = new LinkedHashMap<>();
        byKey.put(LookupKey.get(Account.class), AccountDataProvider.class);
        byKey.put(BIG_ACCOUNT, AccountDataProvider.class);
        return byKey;
    }

    private static void assertPlainKeyIsInstanceOf(Object record, boolean expected) {
        assertEquals(expected, LookupKey.get(Account.class).isInstanceOf(record));
    }

    private static void assertEnterpriseFlavourIsInstanceOf(Object record, boolean expected) {
        assertEquals(expected, ENTERPRISE_FLAVOUR.isInstanceOf(record));
    }

    private static void assertNamedFlavourIsInstanceOf(Object record, boolean expected) {
        assertEquals(expected, NAMED_FLAVOUR.isInstanceOf(record));
    }

    private static void assertKeysForCount(Account record, int expectedCount) {
        ProviderLookupLike lookup = ProviderLookups.ofTypes(providerTypeMap());
        assertEquals(expectedCount, lookup.keysFor(record).size());
    }
}
