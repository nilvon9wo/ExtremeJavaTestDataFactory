package net.nowhereatall.xfty.values;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Proves {@link UniqueAcrossRunsExpression} - get wraps a counter in a
 * prefix/suffix, and carries a per-run token so persisted runs don't collide.
 */
class UniqueAcrossRunsExpressionTest {

    @Test
    void get_ForTwoCalls_WrapsACounterInAPrefixAndSuffix() {
        // Arrange
        UniqueAcrossRunsExpression expression = new UniqueAcrossRunsExpression("u.", "@example.com");

        // Act
        Object firstValue = expression.get();
        Object secondValue = expression.get();

        // Assert
        assertTrue(((String) firstValue).startsWith("u."));
        assertTrue(((String) firstValue).endsWith("@example.com"));
        assertNotEquals(firstValue, secondValue);
    }

    @Test
    void get_ForOneCall_CarriesAPerRunToken() {
        // Arrange - the token is what makes two persisted runs not collide
        UniqueAcrossRunsExpression expression = new UniqueAcrossRunsExpression("User Federation Id ", "");

        // Act
        Object value = expression.get();

        // Assert - prefix, then digits from the token + counter
        String stringValue = (String) value;
        assertTrue(stringValue.startsWith("User Federation Id "));
        assertTrue(stringValue.length() > "User Federation Id 1".length());
    }
}
