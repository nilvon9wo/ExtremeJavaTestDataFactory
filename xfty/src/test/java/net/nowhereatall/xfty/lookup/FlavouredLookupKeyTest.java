package net.nowhereatall.xfty.lookup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.demo.Account;
import net.nowhereatall.xfty.predicates.FieldPredicateFactory;
import org.junit.jupiter.api.Test;

class FlavouredLookupKeyTest {

    @Test
    void internsOneInstancePerTypeAndFlavour() {
        // Arrange / Act
        FlavouredLookupKey first = FlavouredLookupKey.get(Account.class, "person");
        FlavouredLookupKey second = FlavouredLookupKey.get(Account.class, "person");

        // Assert
        assertSame(first, second);
    }

    @Test
    void isMoreSpecificThanThePlainTypeKey() {
        // Arrange
        FlavouredLookupKey key = FlavouredLookupKey.get(Account.class, "enterprise-x");

        // Act / Assert
        assertTrue(key.specificity() > TypeLookupKey.get(Account.class).specificity());
    }

    @Test
    void growsMoreSpecificWithEachPredicate() {
        // Arrange
        FlavouredLookupKey key = FlavouredLookupKey.get(Account.class, "big-tech");
        int before = key.specificity();

        // Act
        key.matching(FieldPredicateFactory.equalTo(Field.of(Account.class, "industry"), "Technology"));

        // Assert
        assertEquals(before + 1, key.specificity());
    }

    @Test
    void matchesOnlyWhenTypeAndEveryPredicateHold() {
        // Arrange
        FlavouredLookupKey key = FlavouredLookupKey.get(Account.class, "active-tech")
                .matching(FieldPredicateFactory.equalTo(Field.of(Account.class, "industry"), "Technology"))
                .matching(FieldPredicateFactory.equalTo(Field.of(Account.class, "active"), true));
        Account matching = new Account();
        matching.setIndustry("Technology");
        matching.setActive(true);

        // Act / Assert
        assertTrue(key.isInstanceOf(matching));
    }

    @Test
    void doesNotMatchWhenOnePredicateFails() {
        // Arrange
        FlavouredLookupKey key = FlavouredLookupKey.get(Account.class, "tech-only")
                .matching(FieldPredicateFactory.equalTo(Field.of(Account.class, "industry"), "Technology"));
        Account other = new Account();
        other.setIndustry("Finance");

        // Act / Assert
        assertFalse(key.isInstanceOf(other));
    }

    @Test
    void aFlavourWithNoPredicatesNeverMatchesImplicitly() {
        // Arrange
        FlavouredLookupKey key = FlavouredLookupKey.get(Account.class, "explicit-only");

        // Act / Assert
        assertFalse(key.isInstanceOf(new Account()));
    }
}
