package net.nowhereatall.xfty.relationships;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.lookup.LookupKeyLike;
import net.nowhereatall.xfty.lookup.ProviderLookupLike;

/**
 * How a related record should be produced for a lookup field.
 *
 * <p>Whether the relationship is <em>required</em> or <em>optional</em> is not
 * part of this contract - that is decided by which slot it occupies on the
 * Master Template ({@code putRequired} vs {@code putOptional}).
 *
 * <p>(C# {@code IDefaultRelationship}.)
 */
public interface DefaultRelationshipLike {

    /** Override template for the generated parent; also identifies its record type. */
    Object overrideTemplate();

    /** The parent field whose value is copied into the child's lookup field, or null to use the parent's Id. */
    Field relatedField();

    /**
     * The lookup key identifying which Provider variant generates the parent.
     * Returns the explicit key if one was supplied, otherwise derives one from
     * the override template. The result is memoised on first call.
     */
    LookupKeyLike resolveLookupKey(ProviderLookupLike providerLookup);
}
