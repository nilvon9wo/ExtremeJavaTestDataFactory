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

    public static CompletableFuture<Void> insertMixed(PersistenceGatewayLike gateway, List<Object> records) {
        Map<Class<?>, List<Object>> byType = new LinkedHashMap<>();
        for (Object record : records) {
            byType.computeIfAbsent(record.getClass(), ignored -> new ArrayList<>()).add(record);
        }
        return insertGroups(gateway, new ArrayList<>(byType.entrySet()), 0);
    }

    private static CompletableFuture<Void> insertGroups(
            PersistenceGatewayLike gateway, List<Map.Entry<Class<?>, List<Object>>> groups, int index) {
        if (index >= groups.size()) {
            return CompletableFuture.completedFuture(null);
        }
        Map.Entry<Class<?>, List<Object>> group = groups.get(index);
        return gateway.insert(group.getValue(), Ids.fieldOf(group.getKey()))
                .thenCompose(ignored -> insertGroups(gateway, groups, index + 1));
    }
}
