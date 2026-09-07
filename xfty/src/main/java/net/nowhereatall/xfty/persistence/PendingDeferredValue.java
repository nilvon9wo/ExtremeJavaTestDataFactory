package net.nowhereatall.xfty.persistence;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.values.DeferredExpressionLike;

/**
 * One up-flowing value still to resolve: {@code records[recordIndex].field} will
 * be filled by {@code strategy} during the DEFERRED flush, once the whole forest
 * exists.
 */
public final class PendingDeferredValue {

    private final int recordIndex;
    private final Field field;
    private final DeferredExpressionLike strategy;

    public PendingDeferredValue(int recordIndex, Field field, DeferredExpressionLike strategy) {
        this.recordIndex = recordIndex;
        this.field = field;
        this.strategy = strategy;
    }

    public int recordIndex() {
        return this.recordIndex;
    }

    public Field field() {
        return this.field;
    }

    public DeferredExpressionLike strategy() {
        return this.strategy;
    }
}
