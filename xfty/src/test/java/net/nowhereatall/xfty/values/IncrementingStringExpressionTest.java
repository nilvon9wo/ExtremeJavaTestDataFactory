package net.nowhereatall.xfty.values;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Proves {@link IncrementingStringExpression} - get produces "prefix N" by
 * default, "prefixN" when told not to separate, and each instance counts
 * independently.
 */
class IncrementingStringExpressionTest {

    @Test
    void get_ByDefault_SeparatesThePrefixAndCounter() {
        // Arrange
        IncrementingStringExpression expression = new IncrementingStringExpression("Account");

        // Act
        List<Object> sequence = List.of(expression.get(), expression.get(), expression.get());

        // Assert
        assertEquals(List.of("Account 1", "Account 2", "Account 3"), sequence);
    }

    @Test
    void get_WithNoSeparator_JoinsThePrefixAndCounter() {
        // Arrange
        IncrementingStringExpression expression =
                new IncrementingStringExpression("ACME", IncrementingStringExpression.DONT_SEPARATE_PREFIX);

        // Act
        List<Object> sequence = List.of(expression.get(), expression.get());

        // Assert
        assertEquals(List.of("ACME1", "ACME2"), sequence);
    }

    @Test
    void get_ForTwoInstances_CountsIndependently() {
        // Arrange
        IncrementingStringExpression first = new IncrementingStringExpression("P");
        IncrementingStringExpression second = new IncrementingStringExpression("P");
        first.get();
        first.get();

        // Act
        Object secondsFirstValue = second.get();

        // Assert - each instance counts independently
        assertEquals("P 1", secondsFirstValue);
    }
}
