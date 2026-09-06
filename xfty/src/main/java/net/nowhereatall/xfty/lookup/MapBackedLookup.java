package net.nowhereatall.xfty.lookup;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.nowhereatall.xfty.core.RecordProvider;

/** The lookup {@link ProviderLookups#of(Map)} and friends build. */
public final class MapBackedLookup implements ProviderLookup, SharedAncestorDefaults {

    private final Map<LookupKey, Class<? extends RecordProvider>> providerTypeByKey;
    private final Map<LookupKey, RecordProvider> providerByKey;
    private final Map<String, Object> sharedAncestorDefaults;
    private final Map<LookupKey, RecordProvider> instanceCache = new ConcurrentHashMap<>();

    MapBackedLookup(
            Map<LookupKey, Class<? extends RecordProvider>> providerTypeByKey,
            Map<LookupKey, RecordProvider> providerByKey,
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
    public RecordProvider get(Class<?> recordType) {
        return get(TypeLookupKey.get(recordType));
    }

    @Override
    public RecordProvider get(LookupKey lookupKey) {
        return this.providerByKey != null
                ? ProviderLookups.get(this.providerByKey, lookupKey)
                : ProviderLookups.get(this.providerTypeByKey, this.instanceCache, lookupKey);
    }

    @Override
    public Set<LookupKey> keysFor(Object record) {
        Set<LookupKey> keys = new LinkedHashSet<>(
                this.providerByKey != null ? this.providerByKey.keySet() : this.providerTypeByKey.keySet());
        return ProviderLookups.keysFor(keys, record);
    }
}
