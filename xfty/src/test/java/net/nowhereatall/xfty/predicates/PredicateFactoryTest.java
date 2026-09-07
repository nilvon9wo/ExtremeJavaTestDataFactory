package net.nowhereatall.xfty.predicates;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import net.nowhereatall.xfty.demo.Account;
import org.junit.jupiter.api.Test;

/** Proves the {@link PredicateFactory} combinator facade wires allOf/anyOf/negate to the right implementation. */
class PredicateFactoryTest {

    @Test
    void allOf_WhenAMemberIsNotSatisfied_ReturnsFalse() {
        assertIsSatisfiedBy(
                PredicateFactory.allOf(List.of(FieldPredicateFactory.equalTo(Account::getIndustry, "Technology"))),
                Account.builder().industry("Retail").build(), false);
    }

    @Test
    void anyOf_WhenAMemberIsSatisfied_ReturnsTrue() {
        assertIsSatisfiedBy(
                PredicateFactory.anyOf(List.of(FieldPredicateFactory.equalTo(Account::getIndustry, "Technology"))),
                Account.builder().industry("Technology").build(), true);
    }

    @Test
    void negate_WhenTheInnerPredicateIsNotSatisfied_ReturnsTrue() {
        assertIsSatisfiedBy(
                PredicateFactory.negate(FieldPredicateFactory.equalTo(Account::getType, "Prospect")),
                Account.builder().type("Customer").build(), true);
    }

    private static void assertIsSatisfiedBy(RecordPredicateLike predicate, Account record, boolean expectedResult) {
        // Arrange - the caller supplies the facade-built predicate and the record

        // Act
        boolean actualResult = predicate.isSatisfiedBy(record);

        // Assert
        assertEquals(expectedResult, actualResult);
    }
}
