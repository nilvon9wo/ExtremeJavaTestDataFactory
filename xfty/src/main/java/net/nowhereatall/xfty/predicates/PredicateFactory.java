package net.nowhereatall.xfty.predicates;

import java.util.List;

/**
 * Discoverable factory for the boolean combinators over {@link RecordPredicateLike}.
 * Each result is itself a {@link RecordPredicateLike}, so they nest.
 *
 * <p>The implementations are {@link AllOfPredicate}, {@link AnyOfPredicate} and
 * {@link NegationPredicate} - use those directly if you prefer; this facade only
 * saves an import.
 */
public final class PredicateFactory {

    private PredicateFactory() {
    }

    /** Satisfied only when every member predicate is. An empty list is vacuously satisfied. */
    public static RecordPredicateLike allOf(List<RecordPredicateLike> predicates) {
        return AllOfPredicate.of(predicates);
    }

    /** Satisfied when at least one member predicate is. An empty list is never satisfied. */
    public static RecordPredicateLike anyOf(List<RecordPredicateLike> predicates) {
        return AnyOfPredicate.of(predicates);
    }

    /** Satisfied exactly when {@code predicate} is not. */
    public static RecordPredicateLike negate(RecordPredicateLike predicate) {
        return NegationPredicate.of(predicate);
    }
}
