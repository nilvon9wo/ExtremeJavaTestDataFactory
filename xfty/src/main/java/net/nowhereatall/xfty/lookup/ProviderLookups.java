package net.nowhereatall.xfty.lookup;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.nowhereatall.xfty.core.RecordProviderLike;

/**
 * The reusable mechanics behind a {@link ProviderLookupLike}, so a project's own
 * lookup stays a handful of one-line delegations over an explicit map. Nothing
 * here is stateful and nothing mutates a lookup: you pass a complete map in, you
 * get an answer out.
 */
public final class ProviderLookups {

    private ProviderLookups() {
    }

    // Resolving a Provider ---------------------------------------------------

    /** {@code lookup.get(recordType)}, kept for symmetry with the C# extension method. */
    public static RecordProviderLike get(ProviderLookupLike lookup, Class<?> recordType) {
        return lookup.get(recordType);
    }

    /** Look up (and lazily instantiate + cache) a Provider for {@code key}. */
    public static RecordProviderLike get(
            Map<LookupKeyLike, Class<? extends RecordProviderLike>> providerTypeByKey,
            Map<LookupKeyLike, RecordProviderLike> instanceCache,
            LookupKeyLike key) {
        requireKey(key);
        return instanceCache.computeIfAbsent(key, missing -> {
            Class<? extends RecordProviderLike> providerType = providerTypeByKey.get(missing);
            if (providerType == null) {
                throw notRegistered(missing);
            }
            return instantiate(providerType);
        });
    }

    /** Look up an already-constructed Provider for {@code key}. */
    public static RecordProviderLike get(Map<LookupKeyLike, RecordProviderLike> providerByKey, LookupKeyLike key) {
        requireKey(key);
        RecordProviderLike provider = providerByKey.get(key);
        if (provider == null) {
            throw notRegistered(key);
        }
        return provider;
    }

    // Deriving a key from a record ------------------------------------------

    /** The subset of {@code registeredKeys} whose {@code isInstanceOf(record)} is true. */
    public static Set<LookupKeyLike> keysFor(Set<LookupKeyLike> registeredKeys, Object record) {
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
    public static LookupKeyLike resolve(ProviderLookupLike providerLookup, Object record) {
        Set<LookupKeyLike> matches = providerLookup.keysFor(record);
        return matches.isEmpty()
                ? LookupKey.get(record == null ? null : record.getClass())
                : bestOf(matches, record);
    }

    private static LookupKeyLike bestOf(Set<LookupKeyLike> matches, Object record) {
        int topSpecificity = matches.stream().mapToInt(LookupKeyLike::specificity).max().orElse(0);
        List<LookupKeyLike> topTier = matches.stream()
                .filter(key -> key.specificity() == topSpecificity)
                .toList();
        List<String> topTierHashes = topTier.stream().map(LookupKeyLike::hashKey).distinct().toList();
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
    public static LookupKeyLike reconcile(ProviderLookupLike providerLookup, LookupKeyLike explicitKey, Object overrideTemplate) {
        if (explicitKey == null && overrideTemplate == null) {
            return null;
        }
        if (explicitKey == null) {
            return resolve(providerLookup, overrideTemplate);
        }
        if (contradictsTemplate(providerLookup, explicitKey, overrideTemplate)) {
            LookupKeyLike fromTemplate = resolve(providerLookup, overrideTemplate);
            throw new LookupException(
                    "Explicit variant " + explicitKey.hashKey() + " contradicts the override template, which matches "
                    + fromTemplate.hashKey() + ". Supply only one.");
        }
        return explicitKey;
    }

    private static boolean contradictsTemplate(ProviderLookupLike providerLookup, LookupKeyLike explicitKey, Object overrideTemplate) {
        if (overrideTemplate == null) {
            return false;
        }
        LookupKeyLike fromTemplate = resolve(providerLookup, overrideTemplate);
        return fromTemplate.specificity() > 0 && !fromTemplate.hashKey().equals(explicitKey.hashKey());
    }

    // Ready-made map-backed lookups ---------------------------------------

    /** A lookup over a complete map of already-constructed Providers. */
    public static ProviderLookupLike of(Map<LookupKeyLike, RecordProviderLike> providerByKey) {
        return new MapBackedLookup(null, providerByKey, null);
    }

    /** As {@link #of(Map)}, plus the shared-ancestor defaults the Providers rely on. */
    public static ProviderLookupLike of(Map<LookupKeyLike, RecordProviderLike> providerByKey, Map<String, Object> sharedAncestorDefaults) {
        return new MapBackedLookup(null, providerByKey, sharedAncestorDefaults);
    }

    /** A lookup over a complete map of Provider types (instantiated lazily). */
    public static ProviderLookupLike ofTypes(Map<LookupKeyLike, Class<? extends RecordProviderLike>> providerTypeByKey) {
        return new MapBackedLookup(providerTypeByKey, null, null);
    }

    // ---------------------------------------------------------------------

    private static RecordProviderLike instantiate(Class<? extends RecordProviderLike> providerType) {
        try {
            return providerType.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new LookupException(
                    providerType.getSimpleName() + " needs a public no-argument constructor to be registered by type.");
        }
    }

    private static void requireKey(LookupKeyLike key) {
        if (key == null) {
            throw new LookupException("A lookup key is required.");
        }
    }

    private static LookupException notRegistered(LookupKeyLike key) {
        return new LookupException("No data provider registered for " + key.recordType() + " (key: " + key.hashKey() + ").");
    }

    private static String typeOf(Object record) {
        return record == null ? "null" : record.getClass().toString();
    }
}
