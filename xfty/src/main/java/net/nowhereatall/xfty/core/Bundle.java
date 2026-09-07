package net.nowhereatall.xfty.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.SerializableFunction;

/**
 * The record(s) one {@code createBundle} call has produced: primary records plus
 * their generated relationships (parents).
 *
 * <p>(Child collections, the deferred-value queue and enrichment from the C#
 * port are not ported yet.)
 */
public final class Bundle {

    private final Map<Field, Bundle> bundleByField = new LinkedHashMap<>();
    private final Map<Field, List<Object>> recordListByField = new LinkedHashMap<>();

    private Field primaryTargetField;

    public Bundle put(Field field, List<Object> records) {
        this.recordListByField.put(field, records);
        return this;
    }

    public Bundle put(Field field, Bundle bundle) {
        this.bundleByField.put(field, bundle);
        return this;
    }

    public Bundle getBundle(Field field) {
        return this.bundleByField.get(field);
    }

    public <T, R> Bundle getBundle(SerializableFunction<T, R> field) {
        return getBundle(Field.of(field));
    }

    public List<Object> getList(Field field) {
        return this.recordListByField.get(field);
    }

    public <T, R> List<Object> getList(SerializableFunction<T, R> field) {
        return getList(Field.of(field));
    }

    /** Read one field several relationship hops up the generated ancestor graph. */
    public Object getValue(List<Field> path, int rowIndex) {
        return AncestorPathWalker.read(this, path, rowIndex);
    }

    public Object getValue(List<Field> path) {
        return getValue(path, 0);
    }

    public void putPrimaries(Field primaryTargetField, List<Object> records) {
        this.primaryTargetField = primaryTargetField;
        put(primaryTargetField, records);
    }

    public Field primaryTargetField() {
        return this.primaryTargetField;
    }

    public List<Object> primaryRecords() {
        return this.primaryTargetField == null ? null : getList(this.primaryTargetField);
    }

    /** The relationship fields that carry a generated sub-bundle (the parents). */
    public Set<Field> relationshipFields() {
        return new LinkedHashSet<>(this.bundleByField.keySet());
    }

    /**
     * The primary records generated pointing at {@code getList(relationshipField)}
     * row {@code ancestorRowIndex} - the inverse of the 1:1 parent alignment.
     */
    public List<Object> primariesResolvingTo(Field relationshipField, int ancestorRowIndex) {
        List<Object> ancestors = getList(relationshipField);
        boolean cannotResolve = primaryRecords() == null
                || ancestors == null
                || ancestorRowIndex < 0
                || ancestorRowIndex >= ancestors.size();
        return cannotResolve
                ? new ArrayList<>()
                : InverseAlignment.childrenPerParent(ancestors, primaryRecords(), relationshipField).get(ancestorRowIndex);
    }
}
