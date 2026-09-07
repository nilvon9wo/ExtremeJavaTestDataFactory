package net.nowhereatall.xfty.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.demo.Account;
import net.nowhereatall.xfty.values.DeferredExpressionLike;
import org.junit.jupiter.api.Test;

/**
 * Proves {@link DeferredValueQueue} - collecting one pending value per primary
 * row per deferred field. The strategy reference is stored verbatim and never
 * called here, so the tests pass null for it.
 */
class DeferredValueQueueTest {

    @Test
    void entries_WhenNothingWasAdded_IsEmpty() {
        // Arrange / Act / Assert
        assertTrue(new DeferredValueQueue().entries().isEmpty());
    }

    @Test
    void addForEachRow_Always_QueuesOneEntryPerRowPerField() {
        // Arrange
        DeferredValueQueue queue = new DeferredValueQueue();
        Map<Field, DeferredExpressionLike> byField = new LinkedHashMap<>();
        byField.put(Field.of(Account.class, "name"), null);
        byField.put(Field.of(Account.class, "site"), null);

        // Act
        queue.addForEachRow(3, byField);

        // Assert - 3 rows x 2 fields
        assertEquals(6, queue.entries().size());
    }

    @Test
    void addForEachRow_WhenRowCountIsZero_QueuesNothing() {
        // Arrange
        DeferredValueQueue queue = new DeferredValueQueue();
        Map<Field, DeferredExpressionLike> byField = new LinkedHashMap<>();
        byField.put(Field.of(Account.class, "name"), null);

        // Act
        queue.addForEachRow(0, byField);

        // Assert
        assertTrue(queue.entries().isEmpty());
    }

    @Test
    void addForEachRow_Always_RecordsTheRowAndFieldOnEachEntry() {
        // Arrange
        DeferredValueQueue queue = new DeferredValueQueue();
        Map<Field, DeferredExpressionLike> byField = new LinkedHashMap<>();
        byField.put(Field.of(Account.class, "name"), null);

        // Act
        queue.addForEachRow(2, byField);

        // Assert
        List<BundleDeferredEntry> entries = queue.entries();
        assertEquals(0, entries.get(0).primaryRow());
        assertEquals(1, entries.get(1).primaryRow());
        assertEquals(Field.of(Account.class, "name"), entries.get(1).field());
    }
}
