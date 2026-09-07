package net.nowhereatall.xfty.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.XftyConfigurationException;
import net.nowhereatall.xfty.core.Bundle;
import net.nowhereatall.xfty.core.GenerationContext;
import net.nowhereatall.xfty.core.InsertMode;
import net.nowhereatall.xfty.relationships.SharedRelationshipLike;

/**
 * Wires a shared ancestor into a bundle: one record stands in for every child at
 * its field, resolved once per test, then repeated {@code quantity} times.
 */
public final class SharedRelationshipWiring {

    private final GenerationContext context;
    private final SharedRelationshipLike shared;

    public SharedRelationshipWiring(GenerationContext context, SharedRelationshipLike shared) {
        this.context = context;
        this.shared = shared;
    }

    public CompletableFuture<Void> wire(Bundle bundle, Field field, int quantity) {
        return this.shared.resolveSharedRecord(this.context).thenAccept(record -> {
            assertSavedConsistently();
            List<Object> children = new ArrayList<>();
            for (int index = 0; index < quantity; index++) {
                children.add(record);
            }
            bundle.put(field, children);
            bundle.put(field, this.shared.getResolvedBundle());
        });
    }

    private void assertSavedConsistently() {
        boolean safe = this.context.insertMode() != InsertMode.NOW || this.shared.isResolvedRecordPersisted();
        if (safe) {
            return;
        }
        throw new XftyConfigurationException("Shared ancestor \"" + this.shared.sharedName() + "\" was resolved "
                + "without being inserted, but this NOW run would carry its id onto inserted records. Use a "
                + "consistent insert mode across the test, or register a saved record with "
                + "SharedAncestor.put(\"" + this.shared.sharedName() + "\", record).");
    }
}
