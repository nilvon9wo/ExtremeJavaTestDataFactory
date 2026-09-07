package net.nowhereatall.xfty.persistence;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.core.Bundle;
import net.nowhereatall.xfty.core.BundleChildEntry;
import net.nowhereatall.xfty.core.InsertMode;
import net.nowhereatall.xfty.engine.DescendantValuePass;

/**
 * Collects the records of one or more generated-but-unsaved bundles into the
 * flat list + parent links {@link DepthBatchedInserter} needs, and saves them.
 *
 * <p>Each bundle is a tree - the engine clones every template and generates a
 * distinct parent per child row - so a record never appears twice and the walk
 * needs no identity tracking. Just before the insert the up-flow value pass
 * ({@link DescendantValuePass}) fills any {@code CopyFromDescendantExpression}
 * field from its now-generated children.
 */
public final class DeferredInsertBuffer {

    private final List<DepthBatchedInserterParentLink> pendingLinks = new ArrayList<>();
    private final List<Object> pendingRecords = new ArrayList<>();
    private final List<PendingDeferredValue> pendingDeferredValues = new ArrayList<>();
    private final Set<Integer> excludedIndices = new LinkedHashSet<>();
    // Each collected bundle's own list, plus the flat index its first record occupies - so the
    // resolved records (new instances, for a record type) are threaded back into the bundle.
    private final List<WriteBack> writeBacks = new ArrayList<>();

    public static CompletableFuture<Void> insertGraph(Bundle bundle, PersistenceGatewayLike gateway, boolean excludePrimaryIds) {
        DeferredInsertBuffer buffer = new DeferredInsertBuffer();
        buffer.add(bundle, excludePrimaryIds);
        return buffer.insertAll(gateway);
    }

    public void add(Bundle bundle, boolean excludePrimaryIds) {
        int startIndex = this.pendingRecords.size();
        int primaryCount = bundle != null && bundle.primaryRecords() != null ? bundle.primaryRecords().size() : 0;
        collect(bundle);
        if (excludePrimaryIds) {
            for (int index = startIndex; index < startIndex + primaryCount; index++) {
                this.excludedIndices.add(index);
            }
        }
    }

    public void add(Bundle bundle) {
        add(bundle, false);
    }

    public int pendingCount() {
        return this.pendingRecords.size();
    }

    public List<Object> records() {
        return this.pendingRecords;
    }

    public List<DepthBatchedInserterParentLink> parentLinks() {
        return this.pendingLinks;
    }

    public CompletableFuture<Void> insertAll(PersistenceGatewayLike gateway) {
        resolveUpFlowValues();
        return DepthBatchedInserter.insertAll(this.pendingRecords, this.pendingLinks, gateway, this.excludedIndices)
                .thenRun(this::writeBackResolvedRecords);
    }

    /** Depth-batched resolution of every buffered bundle honouring {@code mode} (NOW/MOCK/NEVER). */
    public CompletableFuture<Void> resolveAll(InsertMode mode, PersistenceGatewayLike gateway) {
        resolveUpFlowValues();
        return DepthBatchedInserter.resolveAll(this.pendingRecords, this.pendingLinks, mode, gateway, this.excludedIndices)
                .thenRun(this::writeBackResolvedRecords);
    }

    private void writeBackResolvedRecords() {
        for (WriteBack writeBack : this.writeBacks) {
            for (int position = 0; position < writeBack.list.size(); position++) {
                writeBack.list.set(position, this.pendingRecords.get(writeBack.startIndex + position));
            }
        }
    }

    private record WriteBack(List<Object> list, int startIndex) {
    }

    public CompletableFuture<Void> resolveAll(InsertMode mode) {
        return resolveAll(mode, null);
    }

    private void resolveUpFlowValues() {
        new DescendantValuePass(this.pendingRecords, this.pendingLinks, this.pendingDeferredValues).complete();
    }

    private List<IndexedRecord> collect(Bundle bundle) {
        List<Object> primaries = bundle == null ? null : bundle.primaryRecords();
        if (primaries == null) {
            return new ArrayList<>();
        }
        List<IndexedRecord> theseRecords = append(primaries);
        captureDeferredValues(bundle, theseRecords);
        linkToParents(bundle, theseRecords);
        linkToChildCollections(bundle, theseRecords);
        return theseRecords;
    }

    private void captureDeferredValues(Bundle bundle, List<IndexedRecord> primaries) {
        bundle.deferredValues().forEach(deferred -> this.pendingDeferredValues.add(new PendingDeferredValue(
                primaries.get(deferred.primaryRow()).index(), deferred.field(), deferred.strategy())));
    }

    private void linkToChildCollections(Bundle bundle, List<IndexedRecord> primaries) {
        for (Field childField : bundle.childRelationshipFields()) {
            for (BundleChildEntry entry : bundle.childEntries(childField)) {
                List<IndexedRecord> childRecords = collect(entry.bundle());
                for (int childRow = 0; childRow < childRecords.size(); childRow++) {
                    linkChild(childRecords.get(childRow),
                            primaries.get(entry.parentRowByChildRow().get(childRow)), childField);
                }
            }
        }
    }

    private void linkToParents(Bundle bundle, List<IndexedRecord> children) {
        for (Field parentField : bundle.relationshipFields()) {
            Bundle parentBundle = bundle.getBundle(parentField);
            if (parentBundle == null) {
                continue;
            }
            linkRows(children, collect(parentBundle), parentField);
        }
    }

    private void linkRows(List<IndexedRecord> children, List<IndexedRecord> parents, Field field) {
        if (parents.isEmpty()) {
            return;
        }
        if (parents.size() == 1 && children.size() > 1) {
            children.forEach(child -> linkChild(child, parents.get(0), field));
            return;
        }
        int rows = Math.min(children.size(), parents.size());
        for (int row = 0; row < rows; row++) {
            linkChild(children.get(row), parents.get(row), field);
        }
    }

    private void linkChild(IndexedRecord child, IndexedRecord parent, Field field) {
        if (field.get(child.record()) != null) {
            return;
        }
        this.pendingLinks.add(new DepthBatchedInserterParentLink(child.index(), parent.index(), field));
    }

    private List<IndexedRecord> append(List<Object> records) {
        this.writeBacks.add(new WriteBack(records, this.pendingRecords.size()));
        List<IndexedRecord> appended = new ArrayList<>(records.size());
        for (Object record : records) {
            int index = this.pendingRecords.size();
            this.pendingRecords.add(record);
            appended.add(new IndexedRecord(index, record));
        }
        return appended;
    }
}
