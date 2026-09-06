package net.nowhereatall.xfty.lookup;

/**
 * Identifies which Provider variant should generate a particular record.
 *
 * <p>The default key ({@link LookupKey}) is just a record type, reproducing the
 * original one-Provider-per-type behaviour. A refined key adds a discriminator -
 * arbitrary predicates on the record ({@link FlavouredLookupKey}), or a custom
 * implementation.
 *
 * <p>A single record can match several registered keys, so
 * {@link ProviderLookupLike#keysFor} returns a set; the most specific match
 * ({@link #specificity()}) wins.
 *
 * <p>Keys are compared by {@link #hashKey()} rather than by identity, so two
 * different instances describing the same variant resolve to the same Provider.
 * Implementations must define {@code equals}/{@code hashCode} in terms of
 * {@code hashKey()}.
 *
 * <p>(C# {@code ILookupKey}.)
 */
public interface LookupKeyLike {

    /** The record type this key selects a Provider for. */
    Class<?> recordType();

    /**
     * Whether {@code record} belongs to the variant this key describes. Used to
     * derive a key from a relationship's override template when none was
     * supplied explicitly.
     */
    boolean isInstanceOf(Object record);

    /** Value-equality identity. Two keys with the same hash key are the same key. */
    String hashKey();

    /** How specific this key is; higher wins when several keys match one record. Plain type key = 0. */
    int specificity();
}
