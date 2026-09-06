package net.nowhereatall.xfty.predicates;

/**
 * An arbitrary condition on a record. Conditions need not be equality -
 * "annual revenue over 1M", "industry in a set", "nickname is null" are all
 * fine.
 *
 * <p>Implement this yourself for anything {@link FieldPredicateFactory} does not
 * express - a one-method interface, no base class, no registration.
 *
 * <p>See {@link FieldPredicateFactory} for ready-made single-field conditions
 * and {@link PredicateFactory} for AND / OR / NOT combinators.
 *
 * <p>(C# {@code IRecordPredicate}.)
 */
@FunctionalInterface
public interface RecordPredicateLike {

    boolean isSatisfiedBy(Object record);
}
