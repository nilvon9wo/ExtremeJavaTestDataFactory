package net.nowhereatall.xfty.lookup;

import java.util.Set;

import net.nowhereatall.xfty.core.RecordProvider;

/** Resolves which Provider should generate a given record. */
public interface ProviderLookup {

    /** Convenience for the common case; equivalent to {@code get(TypeLookupKey.get(recordType))}. */
    RecordProvider get(Class<?> recordType);

    /** Resolve a Provider for an explicit variant key. */
    RecordProvider get(LookupKey lookupKey);

    /** Every registered key whose {@code isInstanceOf(record)} is true. */
    Set<LookupKey> keysFor(Object record);
}
