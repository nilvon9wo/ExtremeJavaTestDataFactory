package net.nowhereatall.xfty.lookup;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.nowhereatall.xfty.core.RecordProviderLike;

/** The lookup {@link ProviderLookups#of(Map)} and friends build. */
public final class MapBackedLookup implements ProviderLookupLike, SharedAncestorDefaultsLike {

    private final Map<LookupKeyLike, Class<? extends RecordProviderLike>> providerTypeByKey;
    private final Map<LookupKeyLike, RecordProviderLike> providerByKey;
    private final Map<String, Object> sharedAncestorDefaults;
    private final Map<LookupKeyLike, RecordProviderLike> instanceCache = new ConcurrentHashMap<>();

    MapBackedLookup(
            Map<LookupKeyLike, Class<? extends RecordProviderLike>> providerTypeByKey,
            Map<LookupKeyLike, RecordProviderLike> providerByKey,
            Map<String, Object> sharedAncestorDefaults) {
        this.providerTypeByKey = providerTypeByKey;
        this.providerByKey = providerByKey;
        this.sharedAncestorDefaults = sharedAncestorDefaults;
    }

    @Override
    public void registerSharedAncestorDefaults() {
        if (this.sharedAncestorDefaults == null || this.sharedAncestorDefaults.isEmpty()) {
            return;
        }
        throw new UnsupportedOperationException(
                "Shared-ancestor defaults are declared on this lookup but the shared-ancestor subsystem is not ported yet.");
    }

    @Override
    public RecordProviderLike get(Class<?> recordType) {
        return get(LookupKey.get(recordType));
    }

    @Override
    public RecordProviderLike get(LookupKeyLike lookupKey) {
        return this.providerByKey != null
                ? ProviderLookups.get(this.providerByKey, lookupKey)
                : ProviderLookups.get(this.providerTypeByKey, this.instanceCache, lookupKey);
    }

    @Override
    public Set<LookupKeyLike> keysFor(Object record) {
        Set<LookupKeyLike> keys = new LinkedHashSet<>(
                this.providerByKey != null ? this.providerByKey.keySet() : this.providerTypeByKey.keySet());
        return ProviderLookups.keysFor(keys, record);
    }
}
