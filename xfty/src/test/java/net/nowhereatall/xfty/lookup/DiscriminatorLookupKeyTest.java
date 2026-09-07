package net.nowhereatall.xfty.lookup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import net.nowhereatall.xfty.Async;
import net.nowhereatall.xfty.core.MasterTemplate;
import net.nowhereatall.xfty.core.RecordProvider;
import net.nowhereatall.xfty.core.RecordProviderLike;
import net.nowhereatall.xfty.core.SimpleRecordProvider;
import net.nowhereatall.xfty.demo.Account;
import org.junit.jupiter.api.Test;

/** Proves {@link DiscriminatorLookupKey} - the record-type-discriminator analog built over {@link FlavouredLookupKey}. */
class DiscriminatorLookupKeyTest {

    @Test
    void get_ForARecordMatchingTheDiscriminatorValue_IsInstanceOfIsTrue() {
        // Arrange
        LookupKeyLike key = DiscriminatorLookupKey.get(Account::getType, "Person");

        // Act / Assert
        assertTrue(key.isInstanceOf(Account.builder().type("Person").build()));
    }

    @Test
    void get_ForARecordWithADifferentDiscriminatorValue_IsInstanceOfIsFalse() {
        // Arrange
        LookupKeyLike key = DiscriminatorLookupKey.get(Account::getType, "Person");

        // Act / Assert
        assertFalse(key.isInstanceOf(Account.builder().type("Business").build()));
    }

    @Test
    void get_CalledTwiceForTheSameFieldAndValue_ReturnsTheSameFlyweightAndStaysCorrect() {
        // Arrange / Act - calling it twice must not double-register the predicate
        FlavouredLookupKey first = DiscriminatorLookupKey.get(Account::getIndustry, "Technology");
        FlavouredLookupKey second = DiscriminatorLookupKey.get(Account::getIndustry, "Technology");

        // Assert
        assertSame(first, second);
        assertTrue(second.isInstanceOf(Account.builder().industry("Technology").build()));
    }

    @Test
    void get_ResolvesTheRightProviderThroughAProviderLookup() {
        // Arrange
        LookupKeyLike personKey = DiscriminatorLookupKey.get(Account::getType, "PersonAcct");
        ProviderLookupLike lookup = ProviderLookups.of(Map.of(personKey, new PersonAccountProvider()));
        RecordProvider<Account> provider = new RecordProvider<>(Account.builder().type("PersonAcct").build(), lookup);

        // Act
        Account result = Async.await(provider.supply());

        // Assert
        assertEquals("Person Default", result.getName());
    }

    private static final class PersonAccountProvider extends SimpleRecordProvider {
        private PersonAccountProvider() {
            super(MasterTemplate.of(Account::getId).put(Account::getName, "Person Default"));
        }
    }
}
