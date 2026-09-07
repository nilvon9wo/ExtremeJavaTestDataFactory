package net.nowhereatall.xfty.relationships;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.XftyConfigurationException;
import net.nowhereatall.xfty.core.Bundle;
import net.nowhereatall.xfty.core.GenerationContext;
import net.nowhereatall.xfty.core.InsertMode;
import net.nowhereatall.xfty.engine.SharedAncestorResolver;
import net.nowhereatall.xfty.lookup.LookupKeyLike;
import net.nowhereatall.xfty.lookup.ProviderLookupLike;
import net.nowhereatall.xfty.reflect.Ids;

/**
 * One record shared by every relationship that references it - the same instance
 * and id everywhere, generated at most once per test method.
 *
 * <p>{@code put(name, ...)} registers and returns a {@link SharedAncestorProvider};
 * {@code get(name)} only retrieves the token to hand to
 * {@code putRequired}/{@code putOptional}.
 *
 * <p>Flyweight - state is static. .NET/Java statics have no per-test-method
 * lifecycle, so nothing here resets automatically; call
 * {@link #resetAllForTesting()} from your own per-test setup if you rely on
 * shared ancestors across many tests.
 *
 * <p>(C# {@code SharedAncestor}, its partials merged.)
 */
public final class SharedAncestor implements SharedRelationshipLike {

    private static final Map<String, SharedAncestor> BY_NAME = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> DISABLED = new ConcurrentHashMap<>();
    private static volatile boolean manualResolution;

    private final String name;

    private SharedAncestorProvider source;
    private Object resolvedRecord;
    private Bundle resolvedBundle;
    private boolean resolvedRecordIsPersisted;

    private SharedAncestor(String name) {
        this.name = name;
    }

    /** The interned instance for {@code name} - the token for {@code putRequired(field, ...)}. Creates it on first use. */
    public static SharedAncestor get(String name) {
        if (name == null || name.isBlank()) {
            throw new XftyConfigurationException("A shared ancestor needs a non-blank name.");
        }
        return BY_NAME.computeIfAbsent(name, SharedAncestor::new);
    }

    public static Object getId(String name) {
        if (DISABLED.containsKey(name)) {
            throw new XftyConfigurationException("Shared ancestor \"" + name + "\" is disabled.");
        }
        SharedAncestor ancestor = get(name);
        if (ancestor.resolvedRecord == null) {
            throw new XftyConfigurationException("Shared ancestor \"" + name + "\" is not resolved yet. Reference it "
                    + "in a supply*() call first, or call SharedAncestor.get(\"" + name + "\").resolveNow(lookup, mode).");
        }
        return Ids.of(ancestor.resolvedRecord);
    }

    /** Clears every registered/disabled shared ancestor and the manual-resolution flag - for test isolation only. */
    public static void resetAllForTesting() {
        BY_NAME.clear();
        DISABLED.clear();
        manualResolution = false;
    }

    // Registration --------------------------------------------------

    /** Register a record. Disambiguates by id: with one, a fixed value; without, an override template. */
    public static SharedAncestorProvider put(String name, Object record) {
        return Ids.of(record) != null ? putAsValue(name, record) : putAsTemplate(name, record);
    }

    public static SharedAncestorProvider putAsTemplate(String name, Object template) {
        return get(name).provider().withTemplate(template);
    }

    public static SharedAncestorProvider putAsValue(String name, Object record) {
        SharedAncestor ancestor = get(name);
        ancestor.resolvedRecord = record;
        ancestor.resolvedBundle = null;
        ancestor.resolvedRecordIsPersisted = Ids.of(record) != null;
        return ancestor.provider();
    }

    public static SharedAncestorProvider put(String name, LookupKeyLike variantKey) {
        return get(name).provider().fromVariant(variantKey);
    }

    public static SharedAncestorProvider putIfAbsent(String name, Object record) {
        SharedAncestor ancestor = get(name);
        return ancestor.isUnregistered() ? put(name, record) : ancestor.provider();
    }

    public static SharedAncestorProvider putIfAbsent(String name, LookupKeyLike variantKey) {
        SharedAncestor ancestor = get(name);
        return ancestor.isUnregistered() ? put(name, variantKey) : ancestor.provider();
    }

    private SharedAncestorProvider provider() {
        if (this.source == null) {
            this.source = new SharedAncestorProvider(this);
        }
        return this.source;
    }

    // Control -----------------------------------------------------

    public static void disable(String name) {
        get(name).assertUnresolved("disable(...)");
        DISABLED.put(name, Boolean.TRUE);
    }

    public static void manualResolutionOnly() {
        manualResolution = true;
    }

    public static boolean isManualResolutionOnly() {
        return manualResolution;
    }

    /** Every registered ancestor not yet resolved. */
    public static List<SharedAncestor> configuredUnresolved() {
        List<SharedAncestor> unresolved = new ArrayList<>();
        for (SharedAncestor ancestor : BY_NAME.values()) {
            if (ancestor.source != null && ancestor.resolvedRecord == null && !DISABLED.containsKey(ancestor.name)) {
                unresolved.add(ancestor);
            }
        }
        return unresolved;
    }

    private boolean isUnregistered() {
        return this.source == null && this.resolvedRecord == null;
    }

    public void assertUnresolved(String call) {
        if (this.resolvedRecord != null) {
            throw new XftyConfigurationException(
                    "Shared ancestor \"" + this.name + "\" is already resolved; " + call + " would have no effect.");
        }
    }

    // Resolution ----------------------------------------------------

    public CompletableFuture<SharedAncestor> resolveNow(ProviderLookupLike lookup, InsertMode insertMode) {
        if (this.resolvedRecord != null) {
            return CompletableFuture.completedFuture(this);
        }
        SharedAncestorResolver.applyLookupDefaults(lookup);
        return new SharedAncestorResolver(lookup, insertMode)
                .resolve(new ArrayList<>(List.of(this)))
                .thenApply(ignored -> this);
    }

    public SharedAncestorProvider source() {
        if (this.source == null) {
            throw new XftyConfigurationException("Shared ancestor \"" + this.name + "\" was never registered - call "
                    + "SharedAncestor.put(\"" + this.name + "\", template / key).");
        }
        return this.source;
    }

    /** The resolver hands back the generated record and its graph. */
    public void acceptResolved(Object record, Bundle bundle, boolean persisted) {
        this.resolvedRecord = record;
        this.resolvedBundle = bundle;
        this.resolvedRecordIsPersisted = persisted;
    }

    // Relationship surface -----------------------------------------

    @Override
    public Object overrideTemplate() {
        return this.source == null ? null : this.source.overrideTemplate();
    }

    @Override
    public Field relatedField() {
        return this.source == null ? null : this.source.relatedField();
    }

    @Override
    public LookupKeyLike resolveLookupKey(ProviderLookupLike providerLookup) {
        return source().lookupKey(providerLookup);
    }

    @Override
    public String sharedName() {
        return this.name;
    }

    @Override
    public boolean isResolved() {
        return this.resolvedRecord != null;
    }

    @Override
    public boolean isResolvedRecordPersisted() {
        return this.resolvedRecordIsPersisted;
    }

    @Override
    public CompletableFuture<Object> resolveSharedRecord(GenerationContext context) {
        if (DISABLED.containsKey(this.name)) {
            return CompletableFuture.completedFuture(null);
        }
        if (this.resolvedRecord != null) {
            return CompletableFuture.completedFuture(this.resolvedRecord);
        }
        return resolveFresh(context);
    }

    private CompletableFuture<Object> resolveFresh(GenerationContext context) {
        if (manualResolution) {
            if (!source().isLightweight(context.providerLookup())) {
                throw new XftyConfigurationException("Shared ancestor \"" + this.name + "\" has a sub-graph of its own "
                        + "and auto-resolution is off (manual resolution only). Resolve it up front: "
                        + "SharedAncestor.get(\"" + this.name + "\").resolveNow(lookup, mode).");
            }
            return resolveNow(context.providerLookup(), context.insertMode()).thenApply(a -> a.resolvedRecord);
        }
        return SharedAncestorResolver
                .resolveAllConfigured(context.providerLookup(), context.insertMode())
                .thenCompose(ignored -> resolveNow(context.providerLookup(), context.insertMode()))
                .thenApply(a -> a.resolvedRecord);
    }

    @Override
    public Bundle getResolvedBundle() {
        if (this.resolvedBundle == null && this.resolvedRecord != null) {
            Bundle bundle = new Bundle();
            bundle.putPrimaries(Ids.fieldOf(this.resolvedRecord.getClass()), new ArrayList<>(List.of(this.resolvedRecord)));
            this.resolvedBundle = bundle;
        }
        return this.resolvedBundle;
    }
}
