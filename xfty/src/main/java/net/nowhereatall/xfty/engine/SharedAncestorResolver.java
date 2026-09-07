package net.nowhereatall.xfty.engine;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

import net.nowhereatall.xfty.XftyConfigurationException;
import net.nowhereatall.xfty.core.Bundle;
import net.nowhereatall.xfty.core.InsertMode;
import net.nowhereatall.xfty.core.MasterTemplate;
import net.nowhereatall.xfty.lookup.ProviderLookupLike;
import net.nowhereatall.xfty.lookup.SharedAncestorDefaultsLike;
import net.nowhereatall.xfty.persistence.DeferredInsertBuffer;
import net.nowhereatall.xfty.relationships.DefaultRelationshipLike;
import net.nowhereatall.xfty.relationships.SharedAncestor;
import net.nowhereatall.xfty.relationships.SharedAncestorProvider;

/**
 * Resolves the shared ancestors configured in the current test method, before
 * the main graph build: nested shared ancestors first, each built in memory,
 * then persisted one dependency layer at a time. Cycles throw.
 *
 * <p>Every path that can trigger resolution funnels through {@link #resolveAllConfigured}
 * or {@link #resolve}, so serializing those - a single reentrant gate, not one
 * lock per name - makes the whole subsystem safe under concurrent test
 * execution. Resolution completes synchronously in practice (MOCK/NEVER, or NOW
 * against a synchronous gateway), so the gate holds a plain
 * {@link ReentrantLock} and the {@code CompletableFuture}s it joins are already
 * done; a genuinely non-blocking gateway would need an async gate here.
 */
public final class SharedAncestorResolver {

    private static final ReentrantLock GATE = new ReentrantLock();
    private static boolean running;
    private static final Set<String> IN_PROGRESS = new LinkedHashSet<>();

    private final ProviderLookupLike lookup;
    private final InsertMode mode;

    public SharedAncestorResolver(ProviderLookupLike lookup, InsertMode mode) {
        this.lookup = lookup;
        this.mode = eager(mode);
    }

    /** Every shared ancestor configured this test method, resolved against the triggering call's mode. */
    public static CompletableFuture<Void> resolveAllConfigured(ProviderLookupLike lookup, InsertMode callMode) {
        return withGate(() -> {
            if (running) {
                return;
            }
            applyLookupDefaults(lookup);
            if (SharedAncestor.isManualResolutionOnly()) {
                return;
            }
            List<SharedAncestor> configured = SharedAncestor.configuredUnresolved();
            if (!configured.isEmpty()) {
                new SharedAncestorResolver(lookup, callMode).resolveUnderGate(configured);
            }
        });
    }

    /** Let a lookup that implements {@link SharedAncestorDefaultsLike} register its shared-ancestor defaults. */
    public static void applyLookupDefaults(ProviderLookupLike lookup) {
        if (lookup instanceof SharedAncestorDefaultsLike defaults) {
            defaults.registerSharedAncestorDefaults();
        }
    }

    public CompletableFuture<Void> resolve(List<SharedAncestor> ancestors) {
        return withGate(() -> resolveUnderGate(ancestors));
    }

    private void resolveUnderGate(List<SharedAncestor> ancestors) {
        boolean owns = !running;
        running = true;
        try {
            for (SharedAncestor ancestor : inDependencyOrder(ancestors)) {
                if (!ancestor.isResolved()) {
                    resolveOne(ancestor);
                }
            }
        } finally {
            if (owns) {
                running = false;
            }
        }
    }

    private static CompletableFuture<Void> withGate(Runnable action) {
        GATE.lock();
        try {
            action.run();
            return CompletableFuture.completedFuture(null);
        } finally {
            GATE.unlock();
        }
    }

    // Collect deepest-first --------------------------------------

    private List<SharedAncestor> inDependencyOrder(List<SharedAncestor> roots) {
        List<SharedAncestor> ordered = new ArrayList<>();
        Set<String> done = new LinkedHashSet<>();
        Set<String> onThePath = new LinkedHashSet<>();
        for (SharedAncestor root : roots) {
            visit(root, ordered, done, onThePath);
        }
        return ordered;
    }

    private void visit(SharedAncestor ancestor, List<SharedAncestor> ordered, Set<String> done, Set<String> onThePath) {
        String name = ancestor.sharedName();
        if (done.contains(name)) {
            return;
        }
        if (ancestor.isResolved()) {
            done.add(name);
            return;
        }
        if (onThePath.contains(name) || IN_PROGRESS.contains(name)) {
            throw cycle(name);
        }
        onThePath.add(name);
        for (SharedAncestor nested : nestedOf(ancestor)) {
            visit(nested, ordered, done, onThePath);
        }
        onThePath.remove(name);
        done.add(name);
        ordered.add(ancestor);
    }

    private List<SharedAncestor> nestedOf(SharedAncestor ancestor) {
        MasterTemplate template = ancestor.source().masterTemplate(this.lookup);
        List<SharedAncestor> nested = new ArrayList<>();
        for (DefaultRelationshipLike relationship : template.requiredRelationshipByField().values()) {
            if (relationship instanceof SharedAncestor sharedAncestor) {
                nested.add(sharedAncestor);
            }
        }
        for (DefaultRelationshipLike relationship : template.optionalRelationshipByField().values()) {
            if (relationship instanceof SharedAncestor sharedAncestor) {
                nested.add(sharedAncestor);
            }
        }
        return nested;
    }

    // Generate + depth-batched persist --------------------------

    private void resolveOne(SharedAncestor ancestor) {
        String name = ancestor.sharedName();
        if (!IN_PROGRESS.add(name)) {
            return;
        }
        try {
            buildAndPersist(ancestor);
        } finally {
            IN_PROGRESS.remove(name);
        }
    }

    private void buildAndPersist(SharedAncestor ancestor) {
        SharedAncestorProvider source = ancestor.source();
        Bundle graph = source.buildInMemory(this.lookup).join();

        DeferredInsertBuffer buffer = new DeferredInsertBuffer();
        buffer.add(graph);
        buffer.resolveAll(this.mode).join();

        Object record = graph.getList(source.primaryField(this.lookup)).get(0);
        ancestor.acceptResolved(record, graph, this.mode == InsertMode.NOW);
    }

    private static InsertMode eager(InsertMode callMode) {
        return callMode == null || callMode == InsertMode.DEFERRED ? InsertMode.NOW : callMode;
    }

    private static XftyConfigurationException cycle(String name) {
        return new XftyConfigurationException("Shared ancestors form a cycle involving \"" + name + "\". Break it by "
                + "pre-registering one side with SharedAncestor.put(\"" + name + "\", record).");
    }
}
