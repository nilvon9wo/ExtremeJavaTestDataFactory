package net.nowhereatall.xfty.core;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.values.DeferredExpressionLike;

/** One primary row's field, still to be filled by an up-flow strategy during the DEFERRED flush. */
public final class BundleDeferredEntry {

    private final int primaryRow;
    private final Field field;
    private final DeferredExpressionLike strategy;

    public BundleDeferredEntry(int primaryRow, Field field, DeferredExpressionLike strategy) {
        this.primaryRow = primaryRow;
        this.field = field;
        this.strategy = strategy;
    }

    public int primaryRow() {
        return this.primaryRow;
    }

    public Field field() {
        return this.field;
    }

    public DeferredExpressionLike strategy() {
        return this.strategy;
    }
}
