package net.nowhereatall.xfty.predicates;

import net.nowhereatall.xfty.Field;

/**
 * A {@link RecordPredicateLike} satisfied when a record's field orders strictly
 * before a fixed value (see {@link ValueComparison}). A null record, null field
 * value, or null comparison value is never less.
 *
 * <p>Obtain one through {@link #of} or the {@link FieldPredicateFactory} facade.
 */
public final class FieldLessThanPredicate implements RecordPredicateLike {

    private final Field field;
    private final Object comparisonValue;

    private FieldLessThanPredicate(Field field, Object comparisonValue) {
        this.field = field;
        this.comparisonValue = comparisonValue;
    }

    public static FieldLessThanPredicate of(Field field, Object comparisonValue) {
        return new FieldLessThanPredicate(field, comparisonValue);
    }

    @Override
    public boolean isSatisfiedBy(Object record) {
        Integer ordering = ValueComparison.fieldToValue(record, this.field, this.comparisonValue);
        return ordering != null && ordering < 0;
    }
}
