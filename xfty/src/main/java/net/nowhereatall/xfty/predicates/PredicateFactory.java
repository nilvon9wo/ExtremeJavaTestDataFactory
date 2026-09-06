package net.nowhereatall.xfty.predicates;

import java.util.List;

/**
 * Discoverable factory for the boolean combinators over {@link RecordPredicate}.
 * Each result is itself a {@link RecordPredicate}, so they nest.
 *
 * <p>The implementations are {@link AllOfPredicate}, {@link AnyOfPredicate} and
 * {@link NegationPredicate} - use those directly if you prefer; this facade only
 * saves an import.
 */
public final class PredicateFactory {

    private PredicateFactory() {
    }

    /** Satisfied only when every member predicate is. An empty list is vacuously satisfied. */
    public static RecordPredicate allOf(List<RecordPredicate> predicates) {
        return AllOfPredicate.of(predicates);
    }

    /** Satisfied when at least one member predicate is. An empty list is never satisfied. */
    public static RecordPredicate anyOf(List<RecordPredicate> predicates) {
        return AnyOfPredicate.of(predicates);
    }

    /** Satisfied exactly when {@code predicate} is not. */
    public static RecordPredicate negate(RecordPredicate predicate) {
        return NegationPredicate.of(predicate);
    }
}
