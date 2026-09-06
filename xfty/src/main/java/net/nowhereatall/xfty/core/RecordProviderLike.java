package net.nowhereatall.xfty.core;

/**
 * Resolves to the set of definitions that generate one record type (and its
 * related graph).
 *
 * <p>This is the seam the lookup/variant system stores and returns. The full
 * generation surface - master template, primary target field, bundle creation -
 * is added as the engine is ported; for now a provider need only name the type
 * it is responsible for, which is all {@link net.nowhereatall.xfty.lookup}
 * needs to route by.
 *
 * <p>(C# {@code IRecordProvider}. The plain noun {@code RecordProvider} is
 * reserved for the concrete main-entry-point class, matching the C# port.)
 */
public interface RecordProviderLike {

    /** The record type this provider generates. */
    Class<?> primaryType();
}
