package net.nowhereatall.xfty.core;

/** How far a generation run follows relationships. */
public enum InsertInclusivity {
    ALL,
    REQUIRED,
    PREVENT_CASCADE,
    NONE,
}
