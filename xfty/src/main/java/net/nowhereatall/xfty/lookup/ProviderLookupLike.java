package net.nowhereatall.xfty.lookup;

import java.util.Set;

import net.nowhereatall.xfty.core.RecordProviderLike;

/**
 * Resolves which Provider should generate a given record.
 *
 * <p>(C# {@code IProviderLookup}.)
 */
public interface ProviderLookupLike {

    /** Convenience for the common case; equivalent to {@code get(LookupKeyLike.get(recordType))}. */
    RecordProviderLike get(Class<?> recordType);

    /** Resolve a Provider for an explicit variant key. */
    RecordProviderLike get(LookupKeyLike lookupKey);

    /** Every registered key whose {@code isInstanceOf(record)} is true. */
    Set<LookupKeyLike> keysFor(Object record);
}
