package net.nowhereatall.xfty.predicates;

import java.util.HashSet;
import java.util.Set;

import net.nowhereatall.xfty.Field;

/**
 * A {@link RecordPredicate} satisfied when a field's value is one of a fixed
 * set. A null set is treated as empty (nothing matches).
 *
 * <p>Obtain one through {@link #of} or the {@link FieldPredicateFactory} facade.
 */
public final class FieldInSetPredicate implements RecordPredicate {

    private final Field field;
    private final Set<Object> acceptedValues;

    private FieldInSetPredicate(Field field, Iterable<?> acceptedValues) {
        this.field = field;
        this.acceptedValues = new HashSet<>();
        if (acceptedValues != null) {
            acceptedValues.forEach(this.acceptedValues::add);
        }
    }

    public static FieldInSetPredicate of(Field field, Iterable<?> acceptedValues) {
        return new FieldInSetPredicate(field, acceptedValues);
    }

    @Override
    public boolean isSatisfiedBy(Object record) {
        Object actual = record == null ? null : this.field.get(record);
        return this.acceptedValues.contains(actual);
    }
}
