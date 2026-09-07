package net.nowhereatall.xfty.values;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Proves {@link UniqueStringOfLengthExpression} - get produces fixed-length,
 * uppercase, unique values, counted separately per length.
 *
 * <p>Each test uses lengths no other test in this class touches. The per-length
 * counter is a process-static map, not reset between tests in the same run, so
 * asserting an exact "first value" would be order-dependent across the whole
 * suite.
 */
class UniqueStringOfLengthExpressionTest {

    @Test
    void get_ForManyCalls_ProducesFixedLengthUppercaseUniqueValues() {
        // Arrange
        UniqueStringOfLengthExpression expression = new UniqueStringOfLengthExpression(37);

        // Act
        List<String> produced = new ArrayList<>();
        for (int index = 0; index < 20; index++) {
            produced.add((String) expression.get());
        }

        // Assert
        assertEquals(20, produced.stream().distinct().count());
        assertTrue(produced.stream().allMatch(value -> value.matches("[A-Z]{37}")));
    }

    @Test
    void get_ForTwoInstancesOfTheSameLength_ShareTheCounter() {
        // Arrange
        UniqueStringOfLengthExpression first = new UniqueStringOfLengthExpression(41);
        UniqueStringOfLengthExpression second = new UniqueStringOfLengthExpression(41);
        Object firstValue = first.get();

        // Act
        Object secondValue = second.get();

        // Assert - the counter is per length, not per instance
        assertNotEquals(firstValue, secondValue);
    }

    @Test
    void get_ForDifferentLengths_CountsSeparately() {
        // Arrange
        UniqueStringOfLengthExpression lengthFortyThree = new UniqueStringOfLengthExpression(43);
        UniqueStringOfLengthExpression lengthFortyFour = new UniqueStringOfLengthExpression(44);

        // Act
        String firstOfFortyThree = (String) lengthFortyThree.get();
        String firstOfFortyFour = (String) lengthFortyFour.get();

        // Assert - each length's own first value has that length, independent of the other
        assertEquals(43, firstOfFortyThree.length());
        assertEquals(44, firstOfFortyFour.length());
    }
}
