package net.nowhereatall.xfty.lookup;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.nowhereatall.xfty.core.RecordProvider;

/**
 * The reusable mechanics behind a {@link ProviderLookup}, so a project's own
 * lookup stays a handful of one-line delegations over an explicit map. Nothing
 * here is stateful and nothing mutates a lookup: you pass a complete map in, you
 * get an answer out.
 */
public final class ProviderLookups {

    private ProviderLookups() {
    }

    // Resolving a Provider ---------------------------------------------------

    /** {@code lookup.get(recordType)}, kept for symmetry with the C# extension method. */
    public static RecordProvider get(ProviderLookup lookup, Class<?> recordType) {
        return lookup.get(recordType);
    }

    /** Look up (and lazily instantiate + cache) a Provider for {@code key}. */
    public static RecordProvider get(
            Map<LookupKey, Class<? extends RecordProvider>> providerTypeByKey,
            Map<LookupKey, RecordProvider> instanceCache,
            LookupKey key) {
        requireKey(key);
        return instanceCache.computeIfAbsent(key, missing -> {
            Class<? extends RecordProvider> providerType = providerTypeByKey.get(missing);
            if (providerType == null) {
                throw notRegistered(missing);
            }
            return instantiate(providerType);
        });
    }

    /** Look up an already-constructed Provider for {@code key}. */
    public static RecordProvider get(Map<LookupKey, RecordProvider> providerByKey, LookupKey key) {
        requireKey(key);
        RecordProvider provider = providerByKey.get(key);
        if (provider == null) {
            throw notRegistered(key);
        }
        return provider;
    }

    // Deriving a key from a record ------------------------------------------

    /** The subset of {@code registeredKeys} whose {@code isInstanceOf(record)} is true. */
    public static Set<LookupKey> keysFor(Set<LookupKey> registeredKeys, Object record) {
        if (record == null) {
            throw new LookupException("A record is required to derive a lookup key.");
        }
        return registeredKeys.stream()
                .filter(key -> key.recordType() == record.getClass() && key.isInstanceOf(record))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * The single key to generate a parent from, given a lookup and a record:
     * the most specific match, or the plain type key when nothing refined
     * matched. Two equally-specific matches is an error - the caller must supply
     * an explicit key.
     */
    public static LookupKey resolve(ProviderLookup providerLookup, Object record) {
        Set<LookupKey> matches = providerLookup.keysFor(record);
        return matches.isEmpty()
                ? TypeLookupKey.get(record == null ? null : record.getClass())
                : bestOf(matches, record);
    }

    private static LookupKey bestOf(Set<LookupKey> matches, Object record) {
        int topSpecificity = matches.stream().mapToInt(LookupKey::specificity).max().orElse(0);
        List<LookupKey> topTier = matches.stream()
                .filter(key -> key.specificity() == topSpecificity)
                .toList();
        List<String> topTierHashes = topTier.stream().map(LookupKey::hashKey).distinct().toList();
        if (topTierHashes.size() > 1) {
            throw new LookupException(
                    "Ambiguous Provider variant for " + typeOf(record) + ": " + String.join(", ", topTierHashes)
                    + ". Supply an explicit lookup key.");
        }
        return topTier.get(0);
    }

    /**
     * The single variant key to generate from, given an optional explicit key
     * and an optional override template - the two ways a caller can name a
     * variant.
     */
    public static LookupKey reconcile(ProviderLookup providerLookup, LookupKey explicitKey, Object overrideTemplate) {
        if (explicitKey == null && overrideTemplate == null) {
            return null;
        }
        if (explicitKey == null) {
            return resolve(providerLookup, overrideTemplate);
        }
        if (contradictsTemplate(providerLookup, explicitKey, overrideTemplate)) {
            LookupKey fromTemplate = resolve(providerLookup, overrideTemplate);
            throw new LookupException(
                    "Explicit variant " + explicitKey.hashKey() + " contradicts the override template, which matches "
                    + fromTemplate.hashKey() + ". Supply only one.");
        }
        return explicitKey;
    }

    private static boolean contradictsTemplate(ProviderLookup providerLookup, LookupKey explicitKey, Object overrideTemplate) {
        if (overrideTemplate == null) {
            return false;
        }
        LookupKey fromTemplate = resolve(providerLookup, overrideTemplate);
        return fromTemplate.specificity() > 0 && !fromTemplate.hashKey().equals(explicitKey.hashKey());
    }

    // Ready-made map-backed lookups ---------------------------------------

    /** A lookup over a complete map of already-constructed Providers. */
    public static ProviderLookup of(Map<LookupKey, RecordProvider> providerByKey) {
        return new MapBackedLookup(null, providerByKey, null);
    }

    /** As {@link #of(Map)}, plus the shared-ancestor defaults the Providers rely on. */
    public static ProviderLookup of(Map<LookupKey, RecordProvider> providerByKey, Map<String, Object> sharedAncestorDefaults) {
        return new MapBackedLookup(null, providerByKey, sharedAncestorDefaults);
    }

    /** A lookup over a complete map of Provider types (instantiated lazily). */
    public static ProviderLookup ofTypes(Map<LookupKey, Class<? extends RecordProvider>> providerTypeByKey) {
        return new MapBackedLookup(providerTypeByKey, null, null);
    }

    // ---------------------------------------------------------------------

    private static RecordProvider instantiate(Class<? extends RecordProvider> providerType) {
        try {
            return providerType.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new LookupException(
                    providerType.getSimpleName() + " needs a public no-argument constructor to be registered by type.");
        }
    }

    private static void requireKey(LookupKey key) {
        if (key == null) {
            throw new LookupException("A lookup key is required.");
        }
    }

    private static LookupException notRegistered(LookupKey key) {
        return new LookupException("No data provider registered for " + key.recordType() + " (key: " + key.hashKey() + ").");
    }

    private static String typeOf(Object record) {
        return record == null ? "null" : record.getClass().toString();
    }
}
