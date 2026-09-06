package net.nowhereatall.xfty.predicates;

import java.math.BigDecimal;

import net.nowhereatall.xfty.Field;

/**
 * Orders two values the way the ordering field predicates
 * ({@link FieldGreaterThanPredicate}, {@link FieldLessThanPredicate}) need:
 * numbers numerically, same-typed {@link Comparable}s (dates, times) by their
 * natural order, everything else lexicographically by text.
 */
public final class ValueComparison {

    private ValueComparison() {
    }

    /**
     * -1 / 0 / 1 comparing {@code record}'s {@code field} against {@code value},
     * or {@code null} when either side is absent (so the caller decides what an
     * incomparable pair means).
     */
    public static Integer fieldToValue(Object record, Field field, Object value) {
        Object actual = record == null ? null : field.get(record);
        if (actual == null || value == null) {
            return null;
        }
        return compare(actual, value);
    }

    /** -1 / 0 / 1. Both arguments must be non-null and of comparable kinds. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static int compare(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) {
            return Integer.signum(toBigDecimal((Number) left).compareTo(toBigDecimal((Number) right)));
        }
        if (left instanceof Comparable && left.getClass() == right.getClass()) {
            return Integer.signum(((Comparable) left).compareTo(right));
        }
        return Integer.signum(String.valueOf(left).compareTo(String.valueOf(right)));
    }

    private static BigDecimal toBigDecimal(Number value) {
        return new BigDecimal(value.toString());
    }
}
