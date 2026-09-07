package net.nowhereatall.xfty.core;

import java.util.Collection;

import net.nowhereatall.xfty.Field;

/**
 * An optional collaborator, set via {@code RecordProvider.setUnsetFieldFiller},
 * that fills in fields a Provider's Master Template never configured at all -
 * not a field XFTY resolved to null, one nothing (no default, no override, no
 * put, no relationship) ever touched.
 *
 * <p>Runs once per generated record, after every other value/relationship pass,
 * before persistence.
 *
 * <p>(C# {@code IUnsetFieldFiller}. Returns the record because a record is
 * immutable - fill produces a new instance.)
 */
public interface UnsetFieldFillerLike {

    /**
     * Fill in as many of {@code unsetFields} on {@code record} as this filler
     * can/wants to - it need not fill every one. Returns the resulting record
     * (a new instance for a Java record; {@code record} itself for a class).
     */
    Object fill(Object record, Collection<Field> unsetFields);
}
