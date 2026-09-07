package net.nowhereatall.xfty.relationships;

import net.nowhereatall.xfty.Field;

/** One field configuration queued on a {@link SharedAncestorProvider}, applied once its Master Template is resolved. */
public final class SharedAncestorFieldValue {

    private final Field field;
    private final Object value;

    public SharedAncestorFieldValue(Field field, Object value) {
        this.field = field;
        this.value = value;
    }

    public Field field() {
        return this.field;
    }

    public Object value() {
        return this.value;
    }
}
