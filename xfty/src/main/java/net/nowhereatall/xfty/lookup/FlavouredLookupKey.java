package net.nowhereatall.xfty.lookup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.nowhereatall.xfty.predicates.RecordPredicate;

/**
 * Selects a Provider variant by record type and one or more arbitrary
 * predicates on the record ("annual revenue over 1M", "industry in a set"). The
 * "flavour" string labels the combination.
 *
 * <p>{@code isInstanceOf(record)} is true when the type matches and every
 * predicate is satisfied. A flavour with no predicates can only be used
 * explicitly.
 *
 * <p>Instances are flyweights, interned by record type + flavour (predicates
 * are <em>not</em> part of the identity). Obtain one with {@link #get} and add
 * its predicates with {@link #matching} <b>once</b>, in a single place - or use
 * {@link DiscriminatorLookupKey} for the common "match one field's value" case,
 * which enforces that for you.
 */
public final class FlavouredLookupKey implements LookupKey {

    private static final Map<String, FlavouredLookupKey> INSTANCE_BY_HASH = new ConcurrentHashMap<>();

    private final TypeLookupKey baseKey;
    private final String flavour;
    private final List<RecordPredicate> predicates = new ArrayList<>();

    private FlavouredLookupKey(Class<?> recordType, String flavour) {
        this.baseKey = TypeLookupKey.get(recordType);
        this.flavour = flavour;
    }

    public static FlavouredLookupKey get(Class<?> recordType, String flavour) {
        String hash = hashOf(TypeLookupKey.get(recordType), flavour);
        return INSTANCE_BY_HASH.computeIfAbsent(hash, ignored -> new FlavouredLookupKey(recordType, flavour));
    }

    /** Add a condition the record must satisfy to belong to this flavour. Chainable. */
    public FlavouredLookupKey matching(RecordPredicate predicate) {
        this.predicates.add(predicate);
        return this;
    }

    @Override
    public Class<?> recordType() {
        return this.baseKey.recordType();
    }

    @Override
    public boolean isInstanceOf(Object record) {
        return !this.predicates.isEmpty()
                && this.baseKey.isInstanceOf(record)
                && this.predicates.stream().allMatch(predicate -> predicate.isSatisfiedBy(record));
    }

    @Override
    public String hashKey() {
        return hashOf(this.baseKey, this.flavour);
    }

    /** More specific than the plain type key, and more so with more predicates. */
    @Override
    public int specificity() {
        return 20 + this.predicates.size();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof LookupKey key && key.hashKey().equals(this.hashKey());
    }

    @Override
    public int hashCode() {
        return this.hashKey().hashCode();
    }

    @Override
    public String toString() {
        return "FlavouredLookupKey(" + this.hashKey() + ")";
    }

    private static String hashOf(TypeLookupKey baseKey, String flavour) {
        return baseKey.hashKey() + "::flavour=" + flavour;
    }
}
