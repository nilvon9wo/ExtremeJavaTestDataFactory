package net.nowhereatall.xfty.predicates;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.demo.Account;
import org.junit.jupiter.api.Test;

/**
 * Proves {@link FieldEqualToPredicate} - isSatisfiedBy is true exactly when the
 * record's field equals the configured value, null included.
 */
class FieldEqualToPredicateTest {

    @Test
    void isSatisfiedBy_WhenFieldEqualsValue_ReturnsTrue() {
        assertIsSatisfiedBy("Technology", Account.builder().industry("Technology").build(), true);
    }

    @Test
    void isSatisfiedBy_WhenFieldDiffersFromValue_ReturnsFalse() {
        assertIsSatisfiedBy("Technology", Account.builder().industry("Retail").build(), false);
    }

    @Test
    void isSatisfiedBy_WhenConfiguredWithNullAndFieldIsBlank_ReturnsTrue() {
        assertIsSatisfiedBy(null, new Account(), true);
    }

    @Test
    void isSatisfiedBy_WhenConfiguredWithNullAndFieldIsSet_ReturnsFalse() {
        assertIsSatisfiedBy(null, Account.builder().industry("Technology").build(), false);
    }

    @Test
    void isSatisfiedBy_WhenRecordIsNull_ReturnsFalse() {
        assertIsSatisfiedBy("Technology", null, false);
    }

    private static void assertIsSatisfiedBy(Object configuredValue, Account record, boolean expectedResult) {
        // Arrange
        RecordPredicateLike predicate = FieldEqualToPredicate.of(Field.of(Account.class, "industry"), configuredValue);

        // Act
        boolean actualResult = predicate.isSatisfiedBy(record);

        // Assert
        assertEquals(expectedResult, actualResult);
    }
}
