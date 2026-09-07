package net.nowhereatall.xfty.reflect;

import net.nowhereatall.xfty.Field;

/**
 * Reads the conventional {@code id} field off a generated record. XFTY has no
 * schema metadata to consult, so "the identity field" is by convention the one
 * named {@code id} - present on every record type XFTY generates.
 */
public final class Ids {

    private Ids() {
    }

    /** The value of {@code record}'s {@code id} field, or {@code null} if it has none / it is unset. */
    public static Object of(Object record) {
        if (record == null) {
            return null;
        }
        Field idField = fieldOf(record.getClass());
        return idField == null ? null : idField.get(record);
    }

    /** The {@code id} field token for {@code type}, or {@code null} if it has no such field. */
    public static Field fieldOf(Class<?> type) {
        try {
            return Field.of(type, "id");
        } catch (RuntimeException absent) {
            return null;
        }
    }
}
