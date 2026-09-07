package net.nowhereatall.xfty.predicates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.nowhereatall.xfty.XftyConfigurationException;
import net.nowhereatall.xfty.demo.Account;
import org.junit.jupiter.api.Test;

/**
 * Proves {@link NegationPredicate} - isSatisfiedBy returns the opposite of the
 * wrapped predicate, and of rejects a null predicate.
 */
class NegationPredicateTest {

    @Test
    void isSatisfiedBy_WhenTheInnerPredicateIsSatisfied_ReturnsFalse() {
        assertIsSatisfiedBy(Account.builder().type("Prospect").build(), false);
    }

    @Test
    void isSatisfiedBy_WhenTheInnerPredicateIsNotSatisfied_ReturnsTrue() {
        assertIsSatisfiedBy(Account.builder().type("Customer").build(), true);
    }

    @Test
    void of_WhenThePredicateIsNull_Throws() {
        // Arrange - nothing to arrange

        // Act
        XftyConfigurationException thrown =
                assertThrows(XftyConfigurationException.class, () -> NegationPredicate.of(null));

        // Assert
        assertTrue(thrown.getMessage().contains("predicate to negate is required"));
    }

    private static void assertIsSatisfiedBy(Account record, boolean expectedResult) {
        // Arrange - negate "type is Prospect"
        RecordPredicateLike predicate =
                NegationPredicate.of(FieldPredicateFactory.equalTo(Account::getType, "Prospect"));

        // Act
        boolean actualResult = predicate.isSatisfiedBy(record);

        // Assert
        assertEquals(expectedResult, actualResult);
    }
}
