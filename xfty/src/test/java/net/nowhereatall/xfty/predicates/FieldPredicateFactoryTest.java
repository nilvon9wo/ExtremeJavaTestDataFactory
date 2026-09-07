package net.nowhereatall.xfty.predicates;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.demo.Account;
import org.junit.jupiter.api.Test;

/**
 * Proves the {@link FieldPredicateFactory} facade wires each factory method to
 * the right single-field predicate. notEqualTo/isNotNull are a negated equalTo,
 * so both directions are checked.
 */
class FieldPredicateFactoryTest {

    private static final Field INDUSTRY = Field.of(Account.class, "industry");
    private static final Field NUMBER_OF_EMPLOYEES = Field.of(Account.class, "numberOfEmployees");

    @Test
    void equalTo_WhenTheFieldMatches_ReturnsTrue() {
        assertIsSatisfiedBy(FieldPredicateFactory.equalTo(INDUSTRY, "Technology"),
                Account.builder().industry("Technology").build(), true);
    }

    @Test
    void notEqualTo_WhenTheFieldDiffers_ReturnsTrue() {
        assertIsSatisfiedBy(FieldPredicateFactory.notEqualTo(INDUSTRY, "Retail"),
                Account.builder().industry("Technology").build(), true);
    }

    @Test
    void notEqualTo_WhenTheFieldMatches_ReturnsFalse() {
        assertIsSatisfiedBy(FieldPredicateFactory.notEqualTo(INDUSTRY, "Retail"),
                Account.builder().industry("Retail").build(), false);
    }

    @Test
    void greaterThan_WhenTheFieldExceedsTheValue_ReturnsTrue() {
        assertIsSatisfiedBy(FieldPredicateFactory.greaterThan(NUMBER_OF_EMPLOYEES, 100),
                Account.builder().numberOfEmployees(900).build(), true);
    }

    @Test
    void lessThan_WhenTheFieldIsBelowTheValue_ReturnsTrue() {
        assertIsSatisfiedBy(FieldPredicateFactory.lessThan(NUMBER_OF_EMPLOYEES, 100),
                Account.builder().numberOfEmployees(5).build(), true);
    }

    @Test
    void isNull_WhenTheFieldIsBlank_ReturnsTrue() {
        assertIsSatisfiedBy(FieldPredicateFactory.isNull(INDUSTRY), new Account(), true);
    }

    @Test
    void isNotNull_WhenTheFieldIsSet_ReturnsTrue() {
        assertIsSatisfiedBy(FieldPredicateFactory.isNotNull(INDUSTRY),
                Account.builder().industry("Technology").build(), true);
    }

    @Test
    void isNotNull_WhenTheFieldIsBlank_ReturnsFalse() {
        assertIsSatisfiedBy(FieldPredicateFactory.isNotNull(INDUSTRY), new Account(), false);
    }

    @Test
    void inSet_WhenTheFieldIsAMember_ReturnsTrue() {
        assertIsSatisfiedBy(FieldPredicateFactory.inSet(INDUSTRY, List.of("Technology")),
                Account.builder().industry("Technology").build(), true);
    }

    private static void assertIsSatisfiedBy(RecordPredicateLike predicate, Account record, boolean expectedResult) {
        // Arrange - the caller supplies the facade-built predicate and the record

        // Act
        boolean actualResult = predicate.isSatisfiedBy(record);

        // Assert
        assertEquals(expectedResult, actualResult);
    }
}
