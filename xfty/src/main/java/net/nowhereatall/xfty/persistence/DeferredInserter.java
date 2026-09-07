package net.nowhereatall.xfty.persistence;

import java.util.concurrent.CompletableFuture;

import net.nowhereatall.xfty.core.Bundle;

/**
 * The registry behind the DEFERRED insert mode.
 *
 * <p>A DEFERRED Provider call generates its graph like NEVER and registers it
 * here instead of inserting. {@link #flush} then saves everything registered so
 * far - across every {@code supplyBundle()} call - in one depth-batched pass.
 */
public final class DeferredInserter {

    private static DeferredInsertBuffer buffer = new DeferredInsertBuffer();

    private DeferredInserter() {
    }

    /** Register {@code bundle} for the eventual {@link #flush}. */
    public static void register(Bundle bundle, boolean excludePrimaryIds) {
        buffer.add(bundle, excludePrimaryIds);
    }

    public static void register(Bundle bundle) {
        register(bundle, false);
    }

    public static int pendingCount() {
        return buffer.pendingCount();
    }

    /**
     * Save every registered record through {@code gateway}, back-fill its id, and
     * clear the registry. The registry only clears after a successful save, so a
     * failed flush never silently loses what was registered.
     */
    public static CompletableFuture<Void> flush(PersistenceGatewayLike gateway) {
        return buffer.insertAll(gateway).thenRun(() -> buffer = new DeferredInsertBuffer());
    }

    /** Test hygiene only: clears every registered record without inserting anything. */
    public static void resetForTesting() {
        buffer = new DeferredInsertBuffer();
    }
}
