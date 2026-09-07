package net.nowhereatall.xfty.persistence;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.core.InsertMode;
import net.nowhereatall.xfty.reflect.Ids;
import net.nowhereatall.xfty.reflect.RecordShape;

/**
 * Inserts a set of not-yet-persisted records - any mix of record types - in one
 * batch per dependency layer, pointing each child's lookup at its parent's new
 * id as the layer above it lands.
 *
 * <p>Records are addressed by their index in the list: two records can be equal
 * by value, so an index is the only stable handle on one.
 */
public final class DepthBatchedInserter {

    private final List<List<DepthBatchedInserterParentLink>> linksByChild;
    private final List<Object> records;
    private final InsertMode mode;
    private final PersistenceGatewayLike gateway;
    private final Set<Integer> excludedIndices;

    private DepthBatchedInserter(
            List<Object> records, List<DepthBatchedInserterParentLink> links, InsertMode mode,
            PersistenceGatewayLike gateway, Set<Integer> excludedIndices) {
        this.records = records;
        this.mode = mode;
        this.gateway = gateway;
        this.excludedIndices = excludedIndices == null ? new LinkedHashSet<>() : excludedIndices;
        this.linksByChild = groupLinksByChild(records.size(), links);
    }

    /** Depth-batched real insert, via {@code gateway}. */
    public static CompletableFuture<Void> insertAll(
            List<Object> records, List<DepthBatchedInserterParentLink> links,
            PersistenceGatewayLike gateway, Set<Integer> excludedIndices) {
        return resolveAll(records, links, InsertMode.NOW, gateway, excludedIndices);
    }

    public static CompletableFuture<Void> insertAll(List<Object> records, List<DepthBatchedInserterParentLink> links) {
        return insertAll(records, links, null, null);
    }

    public static CompletableFuture<Void> resolveAll(
            List<Object> records, List<DepthBatchedInserterParentLink> links, InsertMode mode) {
        return resolveAll(records, links, mode, null, null);
    }

    /**
     * Depth-batched resolution honouring the mode: NOW inserts each depth layer
     * through {@code gateway}, MOCK gives it mock ids - either way the child
     * lookups are pointed at the layer above as it lands. NEVER does nothing.
     */
    public static CompletableFuture<Void> resolveAll(
            List<Object> records, List<DepthBatchedInserterParentLink> links, InsertMode mode,
            PersistenceGatewayLike gateway, Set<Integer> excludedIndices) {
        if (records.isEmpty() || mode == InsertMode.NEVER) {
            return CompletableFuture.completedFuture(null);
        }
        return new DepthBatchedInserter(records, links, mode, gateway, excludedIndices).insertLayerByLayer();
    }

    private CompletableFuture<Void> insertLayerByLayer() {
        Set<Integer> unpersisted = new LinkedHashSet<>();
        for (int index = 0; index < this.records.size(); index++) {
            unpersisted.add(index);
        }
        return insertRemainingLayers(unpersisted);
    }

    private CompletableFuture<Void> insertRemainingLayers(Set<Integer> unpersisted) {
        if (unpersisted.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        List<Integer> layer = takeNextLayer(unpersisted);
        return insertLayer(layer).thenCompose(ignored -> {
            Set<Integer> remaining = new LinkedHashSet<>(unpersisted);
            remaining.removeAll(layer);
            return insertRemainingLayers(remaining);
        });
    }

    private List<Integer> takeNextLayer(Set<Integer> unpersisted) {
        List<Integer> layer = new ArrayList<>();
        for (int index : unpersisted) {
            if (parentsPersisted(index, unpersisted)) {
                layer.add(index);
            }
        }
        if (layer.isEmpty()) {
            throw new CyclicGraphException("record lookups form a cycle - no insert order works");
        }
        return layer;
    }

    private boolean parentsPersisted(int child, Set<Integer> unpersisted) {
        for (DepthBatchedInserterParentLink link : this.linksByChild.get(child)) {
            if (unpersisted.contains(link.parentIndex())) {
                return false;
            }
        }
        return true;
    }

    private CompletableFuture<Void> insertLayer(List<Integer> indexes) {
        indexes.forEach(this::pointAtParents);
        List<Integer> toPersist = new ArrayList<>();
        for (int index : indexes) {
            if (!this.excludedIndices.contains(index) && Ids.of(this.records.get(index)) == null) {
                toPersist.add(index);
            }
        }
        if (toPersist.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return switch (this.mode) {
            case MOCK -> {
                for (int index : toPersist) {
                    Object record = this.records.get(index);
                    Field idField = Ids.fieldOf(record.getClass());
                    this.records.set(index, RecordShape.of(record.getClass()).set(record, idField, IdMocker.generateId()));
                }
                yield CompletableFuture.completedFuture(null);
            }
            case NOW -> insertNow(toPersist);
            default -> CompletableFuture.completedFuture(null);
        };
    }

    private CompletableFuture<Void> insertNow(List<Integer> indexes) {
        if (this.gateway == null) {
            throw new UnsupportedOperationException(
                    "InsertMode.NOW needs a persistence gateway - pass one to resolveAll(...)/insertAll(...), or "
                    + "RecordProvider.setPersistenceGateway(...) - use MOCK or NEVER when none is configured.");
        }
        List<Object> batch = new ArrayList<>();
        for (int index : indexes) {
            batch.add(this.records.get(index));
        }
        return PersistenceGatewayExtensions.insertMixed(this.gateway, batch).thenAccept(persisted -> {
            for (int k = 0; k < indexes.size(); k++) {
                this.records.set(indexes.get(k), persisted.get(k));
            }
        });
    }

    private void pointAtParents(int child) {
        for (DepthBatchedInserterParentLink link : this.linksByChild.get(child)) {
            Object childRecord = this.records.get(child);
            Object parentId = Ids.of(this.records.get(link.parentIndex()));
            this.records.set(child, RecordShape.of(childRecord.getClass()).set(childRecord, link.field(), parentId));
        }
    }

    private static List<List<DepthBatchedInserterParentLink>> groupLinksByChild(
            int recordCount, List<DepthBatchedInserterParentLink> links) {
        List<List<DepthBatchedInserterParentLink>> byChild = new ArrayList<>(recordCount);
        for (int index = 0; index < recordCount; index++) {
            byChild.add(new ArrayList<>());
        }
        if (links != null) {
            for (DepthBatchedInserterParentLink link : links) {
                byChild.get(link.childIndex()).add(link);
            }
        }
        return byChild;
    }
}
