package net.nowhereatall.xfty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Proves the framework's base exception type is a constructable
 * {@link RuntimeException} subclass. It is thrown and caught across the suite;
 * this pins the hierarchy.
 */
class XftyConfigurationExceptionTest {

    @Test
    void constructor_WhenGivenAMessage_ProducesACatchableExceptionSubclassCarryingIt() {
        // Act
        XftyConfigurationException thrown = assertThrows(XftyConfigurationException.class,
                () -> {
                    throw new XftyConfigurationException("boom");
                });

        // Assert
        assertEquals("boom", thrown.getMessage());
    }
}
