package net.nowhereatall.xfty.predicates;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.demo.Account;
import org.junit.jupiter.api.Test;

/**
 * Proves {@link FieldGreaterThanPredicate} - isSatisfiedBy is true exactly when
 * the field orders strictly after the configured value, and false whenever the
 * two cannot be compared. The ordering itself is {@link ValueComparison}'s job.
 */
class FieldGreaterThanPredicateTest {

    @Test
    void isSatisfiedBy_WhenFieldExceedsTheValue_ReturnsTrue() {
        assertIsSatisfiedBy(100, Account.builder().numberOfEmployees(900).build(), true);
    }

    @Test
    void isSatisfiedBy_WhenFieldEqualsTheValue_ReturnsFalse() {
        assertIsSatisfiedBy(900, Account.builder().numberOfEmployees(900).build(), false);
    }

    @Test
    void isSatisfiedBy_WhenFieldIsBelowTheValue_ReturnsFalse() {
        assertIsSatisfiedBy(900, Account.builder().numberOfEmployees(5).build(), false);
    }

    @Test
    void isSatisfiedBy_WhenFieldValueIsNull_ReturnsFalse() {
        assertIsSatisfiedBy(1, new Account(), false);
    }

    @Test
    void isSatisfiedBy_WhenComparisonValueIsNull_ReturnsFalse() {
        assertIsSatisfiedBy(null, Account.builder().numberOfEmployees(5).build(), false);
    }

    @Test
    void isSatisfiedBy_WhenRecordIsNull_ReturnsFalse() {
        assertIsSatisfiedBy(1, null, false);
    }

    private static void assertIsSatisfiedBy(Object threshold, Account record, boolean expectedResult) {
        // Arrange
        RecordPredicateLike predicate =
                FieldGreaterThanPredicate.of(Field.of(Account.class, "numberOfEmployees"), threshold);

        // Act
        boolean actualResult = predicate.isSatisfiedBy(record);

        // Assert
        assertEquals(expectedResult, actualResult);
    }
}
