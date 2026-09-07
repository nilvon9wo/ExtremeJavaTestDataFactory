package net.nowhereatall.xfty.persistence;

import net.nowhereatall.xfty.Field;

/**
 * {@code records[childIndex].field} should end up pointing at
 * {@code records[parentIndex]}. Also used by
 * {@link net.nowhereatall.xfty.engine.DeferredGraph} - the same link shape
 * either way.
 */
public final class DepthBatchedInserterParentLink {

    private final int childIndex;
    private final int parentIndex;
    private final Field field;

    public DepthBatchedInserterParentLink(int childIndex, int parentIndex, Field field) {
        this.childIndex = childIndex;
        this.parentIndex = parentIndex;
        this.field = field;
    }

    public int childIndex() {
        return this.childIndex;
    }

    public int parentIndex() {
        return this.parentIndex;
    }

    public Field field() {
        return this.field;
    }
}
