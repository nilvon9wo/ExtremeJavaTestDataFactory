package net.nowhereatall.xfty.predicates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.nowhereatall.xfty.XftyConfigurationException;
import net.nowhereatall.xfty.demo.Account;
import org.junit.jupiter.api.Test;

/**
 * Proves {@link AllOfPredicate} - isSatisfiedBy is true only when every member
 * predicate is (an empty member list is vacuously true), and of rejects a null
 * list.
 */
class AllOfPredicateTest {

    @Test
    void isSatisfiedBy_WhenEveryMemberIsSatisfied_ReturnsTrue() {
        assertIsSatisfiedBy(bigTechPredicates(),
                Account.builder().numberOfEmployees(900).industry("Technology").build(), true);
    }

    @Test
    void isSatisfiedBy_WhenOneMemberIsNotSatisfied_ReturnsFalse() {
        assertIsSatisfiedBy(bigTechPredicates(),
                Account.builder().numberOfEmployees(900).industry("Retail").build(), false);
    }

    @Test
    void isSatisfiedBy_WhenTheMemberListIsEmpty_ReturnsTrue() {
        assertIsSatisfiedBy(List.of(), new Account(), true);
    }

    @Test
    void of_WhenTheMemberListIsNull_Throws() {
        // Arrange - nothing to arrange

        // Act
        XftyConfigurationException thrown =
                assertThrows(XftyConfigurationException.class, () -> AllOfPredicate.of(null));

        // Assert
        assertTrue(thrown.getMessage().contains("predicate list is required"));
    }

    private static List<RecordPredicateLike> bigTechPredicates() {
        return List.of(
                FieldPredicateFactory.greaterThan(Account::getNumberOfEmployees, 100),
                FieldPredicateFactory.equalTo(Account::getIndustry, "Technology"));
    }

    private static void assertIsSatisfiedBy(List<RecordPredicateLike> members, Account record, boolean expectedResult) {
        // Arrange
        RecordPredicateLike predicate = AllOfPredicate.of(members);

        // Act
        boolean actualResult = predicate.isSatisfiedBy(record);

        // Assert
        assertEquals(expectedResult, actualResult);
    }
}
