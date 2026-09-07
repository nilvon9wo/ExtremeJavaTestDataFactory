package net.nowhereatall.xfty.engine;

import java.util.ArrayList;
import java.util.List;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.persistence.DepthBatchedInserterParentLink;

/**
 * The whole in-memory forest a DEFERRED flush has collected - every generated
 * record and every parent link - presented for a
 * {@link net.nowhereatall.xfty.values.DeferredExpressionLike} to read a value up
 * from a descendant.
 */
public final class DeferredGraph {

    private final List<Object> records;
    private final List<DepthBatchedInserterParentLink> links;

    public DeferredGraph(List<Object> records, List<DepthBatchedInserterParentLink> links) {
        this.records = records;
        this.links = links;
    }

    /** The generated records that reference {@code records[parentIndex]} through {@code childLookupField}. */
    public List<Object> childrenOf(int parentIndex, Field childLookupField) {
        List<Object> children = new ArrayList<>();
        for (int childIndex : childIndicesOf(parentIndex, childLookupField)) {
            children.add(recordAt(childIndex));
        }
        return children;
    }

    /** The flat indices of the records referencing {@code records[parentIndex]} through {@code childLookupField}. */
    public List<Integer> childIndicesOf(int parentIndex, Field childLookupField) {
        List<Integer> indices = new ArrayList<>();
        for (DepthBatchedInserterParentLink link : this.links) {
            if (link.parentIndex() == parentIndex && link.field().equals(childLookupField)) {
                indices.add(link.childIndex());
            }
        }
        return indices;
    }

    /** The generated record at this flat index - pairs with {@link #childIndicesOf} for a multi-hop walk. */
    public Object recordAt(int index) {
        return this.records.get(index);
    }
}
