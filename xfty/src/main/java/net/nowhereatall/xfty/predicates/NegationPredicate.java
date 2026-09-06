package net.nowhereatall.xfty.predicates;

import net.nowhereatall.xfty.XftyConfigurationException;

/**
 * A {@link RecordPredicateLike} satisfied exactly when the predicate it wraps is
 * not (logical NOT).
 *
 * <p>Obtain one through {@link #of} or the {@link PredicateFactory} facade.
 */
public final class NegationPredicate implements RecordPredicateLike {

    private final RecordPredicateLike negated;

    private NegationPredicate(RecordPredicateLike negated) {
        this.negated = negated;
    }

    public static NegationPredicate of(RecordPredicateLike predicate) {
        if (predicate == null) {
            throw new XftyConfigurationException("A predicate to negate is required.");
        }
        return new NegationPredicate(predicate);
    }

    @Override
    public boolean isSatisfiedBy(Object record) {
        return !this.negated.isSatisfiedBy(record);
    }
}
