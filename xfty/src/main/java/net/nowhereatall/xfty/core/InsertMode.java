package net.nowhereatall.xfty.core;

/**
 * How (and when) generated records reach a backing store.
 *
 * <p>{@link #NOW}, {@link #LATER} and {@link #DEFERRED} need the persistence /
 * deferred-insert machinery, which is not ported yet - use {@link #MOCK} or
 * {@link #NEVER} until it lands.
 */
public enum InsertMode {
    MOCK,
    NEVER,
    NOW,
    LATER,
    DEFERRED,
}
