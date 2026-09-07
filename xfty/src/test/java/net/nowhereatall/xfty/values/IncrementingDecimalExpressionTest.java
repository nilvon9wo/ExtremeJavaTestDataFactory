package net.nowhereatall.xfty.values;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Proves {@link IncrementingDecimalExpression} - get returns ascending decimals, 1, 2, ... */
class IncrementingDecimalExpressionTest {

    @Test
    void get_ForAnIncrementingDecimal_ReturnsAscendingDecimals() {
        // Arrange
        IncrementingDecimalExpression expression = new IncrementingDecimalExpression();

        // Act
        List<Object> twoCalls = List.of(expression.get(), expression.get());

        // Assert
        assertEquals(List.of(new BigDecimal("1"), new BigDecimal("2")), twoCalls);
    }
}
