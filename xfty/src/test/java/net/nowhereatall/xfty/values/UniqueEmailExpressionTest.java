package net.nowhereatall.xfty.values;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Proves {@link UniqueEmailExpression} - get produces well-formed, unique addresses. */
class UniqueEmailExpressionTest {

    @Test
    void get_ForManyCalls_ProducesWellFormedUniqueAddresses() {
        // Arrange - one call per instance, mirroring how callers actually use this
        List<String> produced = new ArrayList<>();

        // Act
        for (int index = 0; index < 25; index++) {
            produced.add((String) new UniqueEmailExpression("test.user").get());
        }

        // Assert
        assertEquals(25, produced.stream().distinct().count());
        assertTrue(produced.stream().allMatch(email -> email.startsWith("test.user") && email.endsWith("@example.com")));
    }
}
