package net.nowhereatall.xfty.values;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Proves {@link UniqueStringExpression} - get never repeats, even across
 * instances, and every value starts with the supplied prefix.
 */
class UniqueStringExpressionTest {

    @Test
    void get_ForManyCalls_NeverRepeatsEvenAcrossInstances() {
        // Arrange - one call per instance, mirroring how callers actually use this
        List<Object> produced = new ArrayList<>();

        // Act
        for (int index = 0; index < 25; index++) {
            produced.add(new UniqueStringExpression("Prefix").get());
        }

        // Assert - the static counter keeps values distinct across instances
        assertEquals(25, produced.stream().distinct().count());
    }

    @Test
    void get_ForAUniqueString_StartsWithTheSuppliedPrefix() {
        // Arrange
        UniqueStringExpression expression = new UniqueStringExpression("Widget");

        // Act
        Object value = expression.get();

        // Assert
        assertTrue(((String) value).startsWith("Widget "));
    }
}
