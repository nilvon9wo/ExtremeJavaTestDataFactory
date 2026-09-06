package net.nowhereatall.xfty.predicates;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.SerializableFunction;

/**
 * Discoverable factory for the ready-made single-field {@link RecordPredicate}
 * conditions. Each call returns a plain predicate you can combine with
 * {@link PredicateFactory} or evaluate directly.
 *
 * <p>These cover the common cases only. Implement {@link RecordPredicate}
 * yourself for anything they do not express. Each factory just wires up a
 * purpose-built class; {@code notEqualTo}/{@code isNotNull} are a negated
 * {@code equalTo}. Use those classes directly if you prefer; this facade only
 * saves an import.
 */
public final class FieldPredicateFactory {

    private FieldPredicateFactory() {
    }

    public static RecordPredicate equalTo(Field field, Object comparisonValue) {
        return FieldEqualToPredicate.of(field, comparisonValue);
    }

    public static RecordPredicate notEqualTo(Field field, Object comparisonValue) {
        return NegationPredicate.of(equalTo(field, comparisonValue));
    }

    public static RecordPredicate greaterThan(Field field, Object comparisonValue) {
        return FieldGreaterThanPredicate.of(field, comparisonValue);
    }

    public static RecordPredicate lessThan(Field field, Object comparisonValue) {
        return FieldLessThanPredicate.of(field, comparisonValue);
    }

    public static RecordPredicate isNull(Field field) {
        return equalTo(field, null);
    }

    public static RecordPredicate isNotNull(Field field) {
        return NegationPredicate.of(isNull(field));
    }

    public static RecordPredicate inSet(Field field, Iterable<?> acceptedValues) {
        return FieldInSetPredicate.of(field, acceptedValues);
    }

    // Method-reference overloads - naming the field by Type::accessor -----------

    public static <T, R> RecordPredicate equalTo(SerializableFunction<T, R> field, Object comparisonValue) {
        return equalTo(Field.of(field), comparisonValue);
    }

    public static <T, R> RecordPredicate notEqualTo(SerializableFunction<T, R> field, Object comparisonValue) {
        return notEqualTo(Field.of(field), comparisonValue);
    }

    public static <T, R> RecordPredicate greaterThan(SerializableFunction<T, R> field, Object comparisonValue) {
        return greaterThan(Field.of(field), comparisonValue);
    }

    public static <T, R> RecordPredicate lessThan(SerializableFunction<T, R> field, Object comparisonValue) {
        return lessThan(Field.of(field), comparisonValue);
    }

    public static <T, R> RecordPredicate isNull(SerializableFunction<T, R> field) {
        return isNull(Field.of(field));
    }

    public static <T, R> RecordPredicate isNotNull(SerializableFunction<T, R> field) {
        return isNotNull(Field.of(field));
    }

    public static <T, R> RecordPredicate inSet(SerializableFunction<T, R> field, Iterable<?> acceptedValues) {
        return inSet(Field.of(field), acceptedValues);
    }
}
