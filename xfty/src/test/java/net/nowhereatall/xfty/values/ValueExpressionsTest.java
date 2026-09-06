package net.nowhereatall.xfty.values;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

/**
 * The self-contained value expressions. Process-static counters (the unique*
 * expressions) are asserted on <em>relative</em> behaviour, not absolute values -
 * a JVM-wide counter carries state across every test in the run.
 */
class ValueExpressionsTest {

    @Test
    void literalAlwaysReturnsTheSameValue() {
        // Arrange
        LiteralExpression expression = new LiteralExpression("fixed");

        // Act / Assert
        assertEquals("fixed", expression.get());
        assertEquals("fixed", expression.get());
    }

    @Test
    void literalReturnsNullWhenGivenNull() {
        // Arrange
        LiteralExpression expression = new LiteralExpression(null);

        // Act / Assert
        assertNull(expression.get());
    }

    @Test
    void incrementingDecimalCountsFromOnePerInstance() {
        // Arrange
        IncrementingDecimalExpression expression = new IncrementingDecimalExpression();

        // Act / Assert
        assertEquals(new BigDecimal("1"), expression.get());
        assertEquals(new BigDecimal("2"), expression.get());
        assertEquals(new BigDecimal("3"), expression.get());
    }

    @Test
    void incrementingStringSeparatesThePrefixByDefault() {
        // Arrange
        IncrementingStringExpression expression = new IncrementingStringExpression("Acme");

        // Act / Assert
        assertEquals("Acme 1", expression.get());
        assertEquals("Acme 2", expression.get());
    }

    @Test
    void incrementingStringCanRunThePrefixUpAgainstTheNumber() {
        // Arrange
        IncrementingStringExpression expression =
                new IncrementingStringExpression("Acme", IncrementingStringExpression.DONT_SEPARATE_PREFIX);

        // Act / Assert
        assertEquals("Acme1", expression.get());
    }

    @Test
    void uniqueStringNeverRepeatsWithinTheProcess() {
        // Arrange
        UniqueStringExpression expression = new UniqueStringExpression("user");

        // Act
        Object first = expression.get();
        Object second = expression.get();

        // Assert
        assertNotEquals(first, second);
        assertTrue(((String) first).startsWith("user "));
    }

    @Test
    void uniqueEmailProducesAWellFormedAddress() {
        // Arrange
        UniqueEmailExpression expression = new UniqueEmailExpression("test.user");

        // Act
        String address = (String) expression.get();

        // Assert
        assertTrue(address.matches("test\\.user\\d+@example\\.com"), address);
    }

    @Test
    void uniqueStringOfLengthProducesUppercaseStringsOfTheRequestedLength() {
        // Arrange
        UniqueStringOfLengthExpression expression = new UniqueStringOfLengthExpression(4);

        // Act
        String value = (String) expression.get();

        // Assert
        assertEquals(4, value.length());
        assertTrue(value.matches("[A-Z]{4}"), value);
    }

    @Test
    void uniqueStringOfLengthCountsSeparatelyPerLength() {
        // Arrange
        UniqueStringOfLengthExpression lengthTwo = new UniqueStringOfLengthExpression(2);
        UniqueStringOfLengthExpression lengthTwoAgain = new UniqueStringOfLengthExpression(2);

        // Act
        String first = (String) lengthTwo.get();
        String second = (String) lengthTwoAgain.get();

        // Assert
        assertNotEquals(first, second);
    }

    @Test
    void uniqueAcrossRunsSharesOneRunTokenButAdvancesTheCounter() {
        // Arrange
        UniqueAcrossRunsExpression expression = new UniqueAcrossRunsExpression("a", "@z");

        // Act
        String first = (String) expression.get();
        String second = (String) expression.get();

        // Assert
        assertTrue(first.startsWith("a") && first.endsWith("@z"));
        assertNotEquals(first, second);
    }
}
