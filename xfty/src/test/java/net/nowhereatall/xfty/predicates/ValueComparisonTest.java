package net.nowhereatall.xfty.predicates;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.demo.Account;
import org.junit.jupiter.api.Test;

/**
 * Proves {@link ValueComparison}: compare returns the sign of the natural
 * ordering (numbers numerically, same-typed comparables chronologically,
 * otherwise lexicographically), and fieldToValue returns null for any pairing
 * that cannot be ordered.
 */
class ValueComparisonTest {

    private static final LocalDateTime NOON = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
    private static final Field NUMBER_OF_EMPLOYEES = Field.of(Account.class, "numberOfEmployees");

    @Test
    void compare_WhenTheLeftNumberIsGreater_ReturnsPositive() {
        assertCompare(900, 100, 1);
    }

    @Test
    void compare_WhenTheNumbersAreEqual_ReturnsZero() {
        assertCompare(100, 100, 0);
    }

    @Test
    void compare_WhenTheLeftNumberIsSmaller_ReturnsNegative() {
        assertCompare(5, 100, -1);
    }

    @Test
    void compare_WhenGivenDecimals_OrdersThemNumerically() {
        assertCompare(new BigDecimal("1000000.50"), new BigDecimal("1000000.25"), 1);
    }

    @Test
    void compare_WhenTheLeftMomentIsLater_ReturnsPositive() {
        assertCompare(NOON.plusHours(1), NOON, 1);
    }

    @Test
    void compare_WhenTheLeftMomentIsEarlier_ReturnsNegative() {
        assertCompare(NOON.minusHours(1), NOON, -1);
    }

    @Test
    void compare_WhenGivenStrings_OrdersThemLexicographically() {
        assertCompare("Acme", "Aardvark", 1);
    }

    @Test
    void fieldToValue_WhenTheFieldIsAbsent_ReturnsNull() {
        assertFieldToValue(new Account(), 100, null);
    }

    @Test
    void fieldToValue_WhenTheComparisonValueIsNull_ReturnsNull() {
        assertFieldToValue(Account.builder().numberOfEmployees(5).build(), null, null);
    }

    @Test
    void fieldToValue_WhenTheRecordIsNull_ReturnsNull() {
        assertFieldToValue(null, 100, null);
    }

    @Test
    void fieldToValue_WhenBothSidesArePresent_ReturnsTheirOrdering() {
        assertFieldToValue(Account.builder().numberOfEmployees(900).build(), 100, 1);
    }

    private static void assertCompare(Object left, Object right, int expectedSign) {
        // Arrange - the caller supplies the pair to order

        // Act
        int actualSign = ValueComparison.compare(left, right);

        // Assert
        assertEquals(expectedSign, actualSign);
    }

    private static void assertFieldToValue(Account record, Object comparisonValue, Integer expectedResult) {
        // Arrange - the caller supplies the record and comparison value

        // Act
        Integer actualResult = ValueComparison.fieldToValue(record, NUMBER_OF_EMPLOYEES, comparisonValue);

        // Assert
        assertEquals(expectedResult, actualResult);
    }
}
