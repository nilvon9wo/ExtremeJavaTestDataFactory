package net.nowhereatall.xfty.predicates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.nowhereatall.xfty.XftyConfigurationException;
import net.nowhereatall.xfty.demo.Account;
import org.junit.jupiter.api.Test;

/**
 * Proves {@link AnyOfPredicate} - isSatisfiedBy is true when at least one member
 * predicate is (an empty member list is never satisfied), and of rejects a null
 * list.
 */
class AnyOfPredicateTest {

    @Test
    void isSatisfiedBy_WhenOneMemberIsSatisfied_ReturnsTrue() {
        assertIsSatisfiedBy(bigOrTechPredicates(),
                Account.builder().numberOfEmployees(10).industry("Technology").build(), true);
    }

    @Test
    void isSatisfiedBy_WhenNoMemberIsSatisfied_ReturnsFalse() {
        assertIsSatisfiedBy(bigOrTechPredicates(),
                Account.builder().numberOfEmployees(10).industry("Retail").build(), false);
    }

    @Test
    void isSatisfiedBy_WhenTheMemberListIsEmpty_ReturnsFalse() {
        assertIsSatisfiedBy(List.of(), new Account(), false);
    }

    @Test
    void of_WhenTheMemberListIsNull_Throws() {
        // Arrange - nothing to arrange

        // Act
        XftyConfigurationException thrown =
                assertThrows(XftyConfigurationException.class, () -> AnyOfPredicate.of(null));

        // Assert
        assertTrue(thrown.getMessage().contains("predicate list is required"));
    }

    private static List<RecordPredicateLike> bigOrTechPredicates() {
        return List.of(
                FieldPredicateFactory.greaterThan(Account::getNumberOfEmployees, 5000),
                FieldPredicateFactory.equalTo(Account::getIndustry, "Technology"));
    }

    private static void assertIsSatisfiedBy(List<RecordPredicateLike> members, Account record, boolean expectedResult) {
        // Arrange
        RecordPredicateLike predicate = AnyOfPredicate.of(members);

        // Act
        boolean actualResult = predicate.isSatisfiedBy(record);

        // Assert
        assertEquals(expectedResult, actualResult);
    }
}
