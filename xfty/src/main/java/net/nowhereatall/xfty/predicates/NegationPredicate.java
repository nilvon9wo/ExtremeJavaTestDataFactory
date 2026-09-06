package net.nowhereatall.xfty.predicates;

import net.nowhereatall.xfty.XftyConfigurationException;

/**
 * A {@link RecordPredicate} satisfied exactly when the predicate it wraps is
 * not (logical NOT).
 *
 * <p>Obtain one through {@link #of} or the {@link PredicateFactory} facade.
 */
public final class NegationPredicate implements RecordPredicate {

    private final RecordPredicate negated;

    private NegationPredicate(RecordPredicate negated) {
        this.negated = negated;
    }

    public static NegationPredicate of(RecordPredicate predicate) {
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
