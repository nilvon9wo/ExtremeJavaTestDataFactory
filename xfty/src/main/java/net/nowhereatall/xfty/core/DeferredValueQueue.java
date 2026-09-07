package net.nowhereatall.xfty.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.values.DeferredExpressionLike;

/**
 * The up-flowing values a bundle's primaries still owe - one (row, field,
 * strategy) per primary row per deferred field. Collected while the bundle is
 * generated and drained during the DEFERRED flush.
 */
public final class DeferredValueQueue {

    private final List<BundleDeferredEntry> entries = new ArrayList<>();

    /** Queue each {@code byField} entry for every one of {@code rowCount} primary rows. */
    public void addForEachRow(int rowCount, Map<Field, DeferredExpressionLike> byField) {
        for (int row = 0; row < rowCount; row++) {
            for (Map.Entry<Field, DeferredExpressionLike> pair : byField.entrySet()) {
                this.entries.add(new BundleDeferredEntry(row, pair.getKey(), pair.getValue()));
            }
        }
    }

    public List<BundleDeferredEntry> entries() {
        return this.entries;
    }
}
