package net.nowhereatall.xfty.predicates;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.demo.Account;
import org.junit.jupiter.api.Test;

/**
 * Proves {@link FieldInSetPredicate} - isSatisfiedBy is true exactly when the
 * record's field is one of the configured set; a null set accepts nothing.
 */
class FieldInSetPredicateTest {

    private static final List<Object> FINANCE_OR_TECH = List.of("Finance", "Technology");

    @Test
    void isSatisfiedBy_WhenFieldIsAMemberOfTheSet_ReturnsTrue() {
        assertIsSatisfiedBy(FINANCE_OR_TECH, Account.builder().industry("Technology").build(), true);
    }

    @Test
    void isSatisfiedBy_WhenFieldIsNotAMemberOfTheSet_ReturnsFalse() {
        assertIsSatisfiedBy(FINANCE_OR_TECH, Account.builder().industry("Retail").build(), false);
    }

    @Test
    void isSatisfiedBy_WhenTheSetIsNull_ReturnsFalse() {
        assertIsSatisfiedBy(null, Account.builder().industry("Technology").build(), false);
    }

    @Test
    void isSatisfiedBy_WhenTheRecordIsNull_ReturnsFalse() {
        assertIsSatisfiedBy(FINANCE_OR_TECH, null, false);
    }

    private static void assertIsSatisfiedBy(Iterable<?> acceptedValues, Account record, boolean expectedResult) {
        // Arrange
        RecordPredicateLike predicate = FieldInSetPredicate.of(Field.of(Account.class, "industry"), acceptedValues);

        // Act
        boolean actualResult = predicate.isSatisfiedBy(record);

        // Assert
        assertEquals(expectedResult, actualResult);
    }
}
