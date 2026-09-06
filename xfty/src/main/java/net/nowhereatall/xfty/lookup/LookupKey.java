package net.nowhereatall.xfty.lookup;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.nowhereatall.xfty.XftyConfigurationException;

/**
 * The default lookup key: a record type and nothing else. Using only this key
 * reproduces XFTY's original behaviour - exactly one Provider per record type.
 *
 * <p>Instances are flyweights - obtain them with {@link #get(Class)}, never a
 * constructor.
 */
public final class LookupKey implements LookupKeyLike {

    private static final Map<Class<?>, LookupKey> INSTANCE_BY_TYPE = new ConcurrentHashMap<>();

    private final Class<?> recordType;

    private LookupKey(Class<?> recordType) {
        this.recordType = recordType;
    }

    public static LookupKey get(Class<?> recordType) {
        if (recordType == null) {
            throw new XftyConfigurationException("A lookup key requires a record type.");
        }
        return INSTANCE_BY_TYPE.computeIfAbsent(recordType, LookupKey::new);
    }

    public static LookupKey get(Object record) {
        return get(record == null ? null : record.getClass());
    }

    @Override
    public Class<?> recordType() {
        return this.recordType;
    }

    @Override
    public boolean isInstanceOf(Object record) {
        return record != null && record.getClass() == this.recordType;
    }

    @Override
    public String hashKey() {
        return this.recordType.toString();
    }

    @Override
    public int specificity() {
        return 0;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof LookupKeyLike key && key.hashKey().equals(this.hashKey());
    }

    @Override
    public int hashCode() {
        return this.hashKey().hashCode();
    }

    @Override
    public String toString() {
        return "LookupKey(" + this.recordType.getSimpleName() + ")";
    }
}
