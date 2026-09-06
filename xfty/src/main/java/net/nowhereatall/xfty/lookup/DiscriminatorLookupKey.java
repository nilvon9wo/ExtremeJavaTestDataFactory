package net.nowhereatall.xfty.lookup;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.SerializableFunction;
import net.nowhereatall.xfty.predicates.FieldPredicateFactory;

/**
 * Selects a Provider variant by one property's value on the record - the direct
 * analog of a single-table discriminator: a JPA {@code @DiscriminatorColumn} on
 * a {@code SINGLE_TABLE} hierarchy, or any hand-rolled enum/string flag on one
 * table distinguishing several kinds of the same entity.
 *
 * <p>A thin, named convenience over {@link FlavouredLookupKey}'s general
 * predicate mechanism, which already does the actual matching -
 * {@code DiscriminatorLookupKey.get(Account::getAccountType, "Person")} reads
 * better than spelling out {@link FieldPredicateFactory#equalTo} by hand for the
 * common "one column, one value" case. Safe to call more than once for the same
 * (type, field, value) - later calls return the same flyweight without
 * re-adding the predicate.
 */
public final class DiscriminatorLookupKey {

    private static final Set<String> CONFIGURED_HASH_KEYS = ConcurrentHashMap.newKeySet();

    private DiscriminatorLookupKey() {
    }

    public static <T, R> FlavouredLookupKey get(SerializableFunction<T, R> discriminatorField, Object value) {
        Field field = Field.of(discriminatorField);
        FlavouredLookupKey key = FlavouredLookupKey.get(field.recordType(), field.name() + "=" + value);
        if (CONFIGURED_HASH_KEYS.add(key.hashKey())) {
            key.matching(FieldPredicateFactory.equalTo(field, value));
        }
        return key;
    }

    public static FlavouredLookupKey get(Field field, Object value) {
        FlavouredLookupKey key = FlavouredLookupKey.get(field.recordType(), field.name() + "=" + value);
        if (CONFIGURED_HASH_KEYS.add(key.hashKey())) {
            key.matching(FieldPredicateFactory.equalTo(field, value));
        }
        return key;
    }
}
