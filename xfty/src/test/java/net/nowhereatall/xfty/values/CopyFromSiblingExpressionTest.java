package net.nowhereatall.xfty.values;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Set;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.XftyConfigurationException;
import net.nowhereatall.xfty.core.Bundle;
import net.nowhereatall.xfty.core.GenerationContext;
import net.nowhereatall.xfty.core.InsertInclusivity;
import net.nowhereatall.xfty.core.InsertMode;
import net.nowhereatall.xfty.demo.Account;
import net.nowhereatall.xfty.lookup.ProviderLookupLike;
import org.junit.jupiter.api.Test;

/**
 * Proves {@link CopyFromSiblingExpression} by building the {@link GenerationContext}
 * directly. The end-to-end path (ordering rule included) is proven separately in
 * {@code ContextAwareExpressionTest}.
 */
class CopyFromSiblingExpressionTest {

    private static final ProviderLookupLike LOOKUP = mock(ProviderLookupLike.class);

    @Test
    void get_TakesTheSiblingsPlainValue() {
        // Arrange - reading a sibling only makes sense while a context-aware value is being generated
        Account record = Account.builder().site("Berlin").build();
        GenerationContext context = new GenerationContext(LOOKUP, InsertMode.MOCK, InsertInclusivity.NONE)
                .forRecord(record, new Bundle(), 0)
                .forValueField(Field.of(Account.class, "description"), Set.of());
        CopyFromSiblingExpression expression = new CopyFromSiblingExpression(Field.of(Account.class, "site"));

        // Act
        Object actualResult = expression.get(context);

        // Assert
        assertEquals("Berlin", actualResult);
    }

    @Test
    void get_WhenTheSiblingGeneratedToNull_YieldsNullWithoutThrowing() {
        // Arrange - site is already resolved to null (absent from "pending")
        Account record = new Account();
        GenerationContext context = new GenerationContext(LOOKUP, InsertMode.MOCK, InsertInclusivity.NONE)
                .forRecord(record, new Bundle(), 0)
                .forValueField(Field.of(Account.class, "description"), Set.of());
        CopyFromSiblingExpression expression = new CopyFromSiblingExpression(Field.of(Account.class, "site"));

        // Act
        Object actualResult = expression.get(context);

        // Assert - copying a generated-null sibling yields null, and does not throw
        assertNull(actualResult);
    }

    @Test
    void constructor_WhenTheFieldIsNull_Throws() {
        // Act
        XftyConfigurationException thrown = assertThrows(XftyConfigurationException.class,
                () -> new CopyFromSiblingExpression(null));

        // Assert
        assertTrue(thrown.getMessage().contains("source field"));
    }

    @Test
    void type_IsAContextAwareExpressionNotAPlainValueExpression() {
        // Act
        Object expression = CopyFromSiblingExpression.from(Account::getName);

        // Assert - a context-aware value, not a plain one
        assertFalse(expression instanceof ValueExpressionLike);
        assertTrue(expression instanceof ContextAwareExpressionLike);
    }

    @Test
    void get_WhenRunOutsideTheValuePass_Throws() {
        // Arrange
        GenerationContext baseContext = new GenerationContext(LOOKUP, InsertMode.MOCK, InsertInclusivity.NONE);
        CopyFromSiblingExpression expression = new CopyFromSiblingExpression(Field.of(Account.class, "name"));

        // Act
        XftyConfigurationException thrown =
                assertThrows(XftyConfigurationException.class, () -> expression.get(baseContext));

        // Assert - there is no record being built
        assertTrue(thrown.getMessage().contains("context-aware value is being generated"));
    }
}
