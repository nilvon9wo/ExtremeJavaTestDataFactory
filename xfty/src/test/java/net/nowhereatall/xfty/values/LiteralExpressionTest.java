package net.nowhereatall.xfty.values;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Proves {@link LiteralExpression} - get always returns the same fixed value, null included. */
class LiteralExpressionTest {

    @Test
    void get_ForAStringLiteral_ReturnsItUnchanged() {
        assertReturnsItself("Customer");
    }

    @Test
    void get_ForAnIntLiteral_ReturnsItUnchanged() {
        assertReturnsItself(42);
    }

    @Test
    void get_ForANullLiteral_ReturnsNull() {
        assertReturnsItself(null);
    }

    @Test
    void get_ForALiteral_ReturnsTheSameValueEveryCall() {
        // Arrange
        LiteralExpression expression = new LiteralExpression("constant");

        // Act
        List<Object> twoCalls = List.of(expression.get(), expression.get());

        // Assert
        assertEquals(List.of("constant", "constant"), twoCalls);
    }

    private static void assertReturnsItself(Object value) {
        // Arrange
        LiteralExpression expression = new LiteralExpression(value);

        // Act
        Object result = expression.get();

        // Assert
        assertEquals(value, result);
    }
}
