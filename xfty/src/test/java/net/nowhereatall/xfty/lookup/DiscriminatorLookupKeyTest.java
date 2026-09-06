package net.nowhereatall.xfty.lookup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.nowhereatall.xfty.SerializableFunction;
import net.nowhereatall.xfty.demo.Account;
import org.junit.jupiter.api.Test;

class DiscriminatorLookupKeyTest {

    @Test
    void matchesARecordWhoseDiscriminatorFieldEqualsTheValue() {
        // Arrange
        FlavouredLookupKey personKey =
                DiscriminatorLookupKey.get((SerializableFunction<Account, String>) Account::getType, "Person");
        Account person = new Account();
        person.setType("Person");

        // Act / Assert
        assertTrue(personKey.isInstanceOf(person));
    }

    @Test
    void doesNotMatchADifferentDiscriminatorValue() {
        // Arrange
        FlavouredLookupKey businessKey =
                DiscriminatorLookupKey.get((SerializableFunction<Account, String>) Account::getType, "Business");
        Account person = new Account();
        person.setType("Person");

        // Act / Assert
        assertFalse(businessKey.isInstanceOf(person));
    }

    @Test
    void isSafeToRequestMoreThanOnceWithoutStackingPredicates() {
        // Arrange
        FlavouredLookupKey first =
                DiscriminatorLookupKey.get((SerializableFunction<Account, String>) Account::getType, "Partner");
        int specificityAfterFirst = first.specificity();

        // Act
        FlavouredLookupKey second =
                DiscriminatorLookupKey.get((SerializableFunction<Account, String>) Account::getType, "Partner");

        // Assert
        assertSame(first, second);
        assertEquals(specificityAfterFirst, second.specificity());
    }
}
