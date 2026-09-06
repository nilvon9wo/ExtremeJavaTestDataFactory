package net.nowhereatall.xfty.predicates;

import java.util.List;

import net.nowhereatall.xfty.XftyConfigurationException;

/**
 * A {@link RecordPredicateLike} satisfied when at least one member predicate is
 * (logical OR). An empty member list is never satisfied.
 *
 * <p>Obtain one through {@link #of} or the {@link PredicateFactory} facade.
 */
public final class AnyOfPredicate implements RecordPredicateLike {

    private final List<RecordPredicateLike> members;

    private AnyOfPredicate(List<RecordPredicateLike> members) {
        this.members = members;
    }

    public static AnyOfPredicate of(List<RecordPredicateLike> members) {
        if (members == null) {
            throw new XftyConfigurationException("A predicate list is required.");
        }
        return new AnyOfPredicate(List.copyOf(members));
    }

    @Override
    public boolean isSatisfiedBy(Object record) {
        return this.members.stream().anyMatch(member -> member.isSatisfiedBy(record));
    }
}
