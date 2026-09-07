package net.nowhereatall.xfty.engine;

import java.util.List;

import net.nowhereatall.xfty.persistence.DepthBatchedInserterParentLink;
import net.nowhereatall.xfty.persistence.PendingDeferredValue;
import net.nowhereatall.xfty.reflect.RecordShape;

/**
 * The up-flow value pass: runs over the whole DEFERRED forest, just before the
 * depth-batched insert, and fills every field a {@code DeferredExpressionLike}
 * left unresolved by reading it from that record's generated descendants.
 */
public final class DescendantValuePass {

    private final List<Object> records;
    private final DeferredGraph graph;
    private final List<PendingDeferredValue> pending;

    public DescendantValuePass(
            List<Object> records, List<DepthBatchedInserterParentLink> links, List<PendingDeferredValue> pending) {
        this.records = records;
        this.graph = new DeferredGraph(records, links);
        this.pending = pending;
    }

    public void complete() {
        this.pending.forEach(this::fill);
    }

    private void fill(PendingDeferredValue value) {
        Object target = this.records.get(value.recordIndex());
        if (value.field().get(target) != null) {
            return;
        }
        Object resolved = value.strategy().get(this.graph, value.recordIndex());
        this.records.set(value.recordIndex(),
                RecordShape.of(target.getClass()).set(target, value.field(), resolved));
    }
}
