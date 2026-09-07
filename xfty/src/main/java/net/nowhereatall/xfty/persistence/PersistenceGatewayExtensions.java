package net.nowhereatall.xfty.persistence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import net.nowhereatall.xfty.reflect.Ids;

/**
 * {@link PersistenceGatewayLike#insert} takes one record type at a time; a
 * depth-batched layer can mix several. This groups a mixed layer by each
 * record's own type and calls the gateway once per type - still one call per
 * type, never one call per record - awaited sequentially, since a real gateway
 * typically wraps a single non-thread-safe connection.
 */
public final class PersistenceGatewayExtensions {

    private PersistenceGatewayExtensions() {
    }

    /** Persist a mixed-type batch, one gateway call per type; returns the persisted records in the input order. */
    public static CompletableFuture<List<Object>> insertMixed(PersistenceGatewayLike gateway, List<Object> records) {
        Map<Class<?>, List<Integer>> positionsByType = new LinkedHashMap<>();
        for (int position = 0; position < records.size(); position++) {
            positionsByType.computeIfAbsent(records.get(position).getClass(), ignored -> new ArrayList<>()).add(position);
        }
        List<Object> result = new ArrayList<>(records);
        return insertGroups(gateway, new ArrayList<>(positionsByType.entrySet()), 0, records, result)
                .thenApply(ignored -> result);
    }

    private static CompletableFuture<Void> insertGroups(
            PersistenceGatewayLike gateway, List<Map.Entry<Class<?>, List<Integer>>> groups, int index,
            List<Object> source, List<Object> result) {
        if (index >= groups.size()) {
            return CompletableFuture.completedFuture(null);
        }
        Map.Entry<Class<?>, List<Integer>> group = groups.get(index);
        List<Object> batch = new ArrayList<>();
        for (int position : group.getValue()) {
            batch.add(source.get(position));
        }
        return gateway.insert(batch, Ids.fieldOf(group.getKey())).thenCompose(persisted -> {
            for (int k = 0; k < group.getValue().size(); k++) {
                result.set(group.getValue().get(k), persisted.get(k));
            }
            return insertGroups(gateway, groups, index + 1, source, result);
        });
    }
}
