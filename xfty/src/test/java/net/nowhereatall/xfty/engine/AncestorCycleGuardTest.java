package net.nowhereatall.xfty.engine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Proves {@link AncestorCycleGuard} - the key-chain tracking that stops an infinite A → A → A ... ancestor cycle. */
class AncestorCycleGuardTest {

    @Test
    void wouldCycleOn_ForAnEmptyGuard_ReturnsFalse() {
        // Arrange
        AncestorCycleGuard guard = new AncestorCycleGuard(false);

        // Act / Assert
        assertFalse(guard.wouldCycleOn("Account"));
    }

    @Test
    void wouldCycleOn_WhenTheKeyIsAlreadyInProgress_ReturnsTrue() {
        // Arrange
        AncestorCycleGuard guard = new AncestorCycleGuard(false).descendingInto("Account");

        // Act / Assert - the same key one level up is a cycle
        assertTrue(guard.wouldCycleOn("Account"));
    }

    @Test
    void wouldCycleOn_ForADifferentKeyThanThoseInProgress_ReturnsFalse() {
        // Arrange
        AncestorCycleGuard guard = new AncestorCycleGuard(false).descendingInto("Account");

        // Act / Assert
        assertFalse(guard.wouldCycleOn("Contact"));
    }

    @Test
    void descendingInto_AccumulatesTheKeyChain() {
        // Arrange
        AncestorCycleGuard parent = new AncestorCycleGuard(false).descendingInto("Account");

        // Act
        AncestorCycleGuard child = parent.descendingInto("Contact");

        // Assert
        assertTrue(child.wouldCycleOn("Account"));
        assertTrue(child.wouldCycleOn("Contact"));
    }

    @Test
    void descendingInto_DoesNotMutateTheParentGuard() {
        // Arrange
        AncestorCycleGuard parent = new AncestorCycleGuard(false).descendingInto("Account");

        // Act
        parent.descendingInto("Contact");

        // Assert - the parent guard is unchanged
        assertFalse(parent.wouldCycleOn("Contact"));
    }

    @Test
    void wouldCycleOn_WhenCyclesAreAllowed_ReturnsFalseEvenForAKeyInProgress() {
        // Arrange
        AncestorCycleGuard guard = new AncestorCycleGuard(true).descendingInto("Account");

        // Act / Assert - allowAncestorCycles() lets the repeat through
        assertFalse(guard.wouldCycleOn("Account"));
    }
}
