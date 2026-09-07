package net.nowhereatall.xfty.demo;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.nowhereatall.xfty.core.RecordProviderLike;
import net.nowhereatall.xfty.lookup.LookupKey;
import net.nowhereatall.xfty.lookup.LookupKeyLike;
import net.nowhereatall.xfty.lookup.ProviderLookupLike;
import net.nowhereatall.xfty.lookup.ProviderLookups;

/**
 * This library's own bundled Provider Lookup - the Account / Contact Providers
 * it ships - used by its own tests and offered as a starter kit.
 *
 * <p>Do not edit this class for your own project - copy it, swap the map entries
 * for your own Providers, and pass your class to
 * {@code new RecordProvider(type, new MyProjectLookup())}.
 */
public final class DefaultProviderLookup implements ProviderLookupLike {

    private static final Map<LookupKeyLike, Class<? extends RecordProviderLike>> PROVIDER_TYPE_BY_KEY = new LinkedHashMap<>();

    static {
        PROVIDER_TYPE_BY_KEY.put(LookupKey.get(Account.class), AccountDataProvider.class);
        PROVIDER_TYPE_BY_KEY.put(LookupKey.get(Contact.class), ContactDataProvider.class);
    }

    private final Map<LookupKeyLike, RecordProviderLike> instanceCache = new ConcurrentHashMap<>();

    @Override
    public RecordProviderLike get(Class<?> recordType) {
        return get(LookupKey.get(recordType));
    }

    @Override
    public RecordProviderLike get(LookupKeyLike lookupKey) {
        return ProviderLookups.get(PROVIDER_TYPE_BY_KEY, this.instanceCache, lookupKey);
    }

    @Override
    public Set<LookupKeyLike> keysFor(Object record) {
        return ProviderLookups.keysFor(new LinkedHashSet<>(PROVIDER_TYPE_BY_KEY.keySet()), record);
    }
}
