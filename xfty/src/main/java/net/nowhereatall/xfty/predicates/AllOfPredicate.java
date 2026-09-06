package net.nowhereatall.xfty.predicates;

import java.util.List;

import net.nowhereatall.xfty.XftyConfigurationException;

/**
 * A {@link RecordPredicate} satisfied only when every member predicate is
 * (logical AND). An empty member list is vacuously satisfied.
 *
 * <p>Obtain one through {@link #of} or the {@link PredicateFactory} facade.
 */
public final class AllOfPredicate implements RecordPredicate {

    private final List<RecordPredicate> members;

    private AllOfPredicate(List<RecordPredicate> members) {
        this.members = members;
    }

    public static AllOfPredicate of(List<RecordPredicate> members) {
        if (members == null) {
            throw new XftyConfigurationException("A predicate list is required.");
        }
        return new AllOfPredicate(List.copyOf(members));
    }

    @Override
    public boolean isSatisfiedBy(Object record) {
        return this.members.stream().allMatch(member -> member.isSatisfiedBy(record));
    }
}
