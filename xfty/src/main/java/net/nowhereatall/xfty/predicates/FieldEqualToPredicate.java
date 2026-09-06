package net.nowhereatall.xfty.predicates;

import java.util.Objects;

import net.nowhereatall.xfty.Field;

/**
 * A {@link RecordPredicateLike} satisfied when a record's field equals a fixed
 * value - null included, so {@code of(field, null)} is an "is null" check. Wrap
 * it in {@link NegationPredicate} for "not equal" / "is not null".
 *
 * <p>Obtain one through {@link #of} or the {@link FieldPredicateFactory} facade.
 */
public final class FieldEqualToPredicate implements RecordPredicateLike {

    private final Field field;
    private final Object comparisonValue;

    private FieldEqualToPredicate(Field field, Object comparisonValue) {
        this.field = field;
        this.comparisonValue = comparisonValue;
    }

    public static FieldEqualToPredicate of(Field field, Object comparisonValue) {
        return new FieldEqualToPredicate(field, comparisonValue);
    }

    @Override
    public boolean isSatisfiedBy(Object record) {
        Object actual = record == null ? null : this.field.get(record);
        return Objects.equals(actual, this.comparisonValue);
    }
}
