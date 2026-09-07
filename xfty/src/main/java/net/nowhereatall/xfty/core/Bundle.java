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
 * The record(s) one {@code createBundle} call has produced: primary records,
 * their generated relationships (parents), and their generated child
 * collections.
 */
public final class Bundle {

    private final Map<Field, Bundle> bundleByField = new LinkedHashMap<>();
    private final Map<Field, List<Object>> recordListByField = new LinkedHashMap<>();
    private final Map<Field, List<BundleChildEntry>> childEntriesByRelationshipField = new LinkedHashMap<>();
    private final DeferredValueQueue deferredValueQueue = new DeferredValueQueue();

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

    public Bundle getBundle(Class<?> recordType, String fieldName) {
        return getBundle(Field.of(recordType, fieldName));
    }

    /** The records stored at {@code field}, untyped - the engine's own view. */
    public List<Object> getList(Field field) {
        return this.recordListByField.get(field);
    }

    /** The records stored at {@code field}, cast to {@code elementType}. */
    public <R> List<R> getList(Class<R> elementType, Field field) {
        return typed(elementType, this.recordListByField.get(field));
    }

    /** {@code getList(elementType, Field.of(ownerType, fieldName))} - name a field without building a token. */
    public <R> List<R> getList(Class<R> elementType, Class<?> ownerType, String fieldName) {
        return getList(elementType, Field.of(ownerType, fieldName));
    }

    /** {@code getList(elementType, Field.of(field))} - name a field by {@code Owner::accessor}. */
    public <T, F, R> List<R> getList(Class<R> elementType, SerializableFunction<T, F> field) {
        return getList(elementType, Field.of(field));
    }

    /** This bundle's primary records, cast to {@code recordType}. */
    public <R> List<R> getPrimaries(Class<R> recordType) {
        return typed(recordType, primaryRecords());
    }

    private static <R> List<R> typed(Class<R> elementType, List<Object> raw) {
        if (raw == null) {
            return null;
        }
        List<R> typed = new java.util.ArrayList<>(raw.size());
        for (Object record : raw) {
            typed.add(elementType.cast(record));
        }
        return typed;
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

    // Deferred (up-flowing) values -----------------------------------

    /** Record that each primary row's {@code byField} entries are still to be resolved up from descendants. */
    public void deferValues(Map<Field, net.nowhereatall.xfty.values.DeferredExpressionLike> byField) {
        this.deferredValueQueue.addForEachRow(primaryRecords().size(), byField);
    }

    public List<BundleDeferredEntry> deferredValues() {
        return this.deferredValueQueue.entries();
    }

    // Child collections -------------------------------------------------

    public Bundle putChild(Field childRelationshipField, Bundle childBundle, List<Integer> parentRowByChildRow) {
        this.childEntriesByRelationshipField
                .computeIfAbsent(childRelationshipField, ignored -> new ArrayList<>())
                .add(new BundleChildEntry(childBundle, parentRowByChildRow));
        return this;
    }

    /** Every child relationship field this bundle carries children for. */
    public Set<Field> childRelationshipFields() {
        return new LinkedHashSet<>(this.childEntriesByRelationshipField.keySet());
    }

    /** The configured child collections for a relationship field, in declaration order. */
    public List<BundleChildEntry> childEntries(Field childRelationshipField) {
        return this.childEntriesByRelationshipField.getOrDefault(childRelationshipField, new ArrayList<>());
    }

    /** The sub-bundles for a child relationship field, in config declaration order. */
    public List<Bundle> childBundles(Field childRelationshipField) {
        List<Bundle> bundles = new ArrayList<>();
        for (BundleChildEntry entry : childEntries(childRelationshipField)) {
            bundles.add(entry.bundle());
        }
        return bundles;
    }

    /** Every child generated for {@code childRelationshipField}, merged across configs. */
    public List<Object> getChildList(Field childRelationshipField) {
        List<Object> all = new ArrayList<>();
        for (Bundle childBundle : childBundles(childRelationshipField)) {
            if (childBundle.primaryRecords() != null) {
                all.addAll(childBundle.primaryRecords());
            }
        }
        return all;
    }

    public <T, R> List<Object> getChildList(SerializableFunction<T, R> childRelationshipField) {
        return getChildList(Field.of(childRelationshipField));
    }

    public <R> List<R> getChildList(Class<R> elementType, Class<?> ownerType, String fieldName) {
        return typed(elementType, getChildList(Field.of(ownerType, fieldName)));
    }

    /** The first child generated for {@code childRelationshipField}; null if none. */
    public Object getChild(Field childRelationshipField) {
        List<Object> all = getChildList(childRelationshipField);
        return all.isEmpty() ? null : all.get(0);
    }

    /** Just the children of one primary row - the slice of {@link #getChildList} that belongs to that row. */
    public List<Object> childRecordsOf(int parentRowIndex, Field childRelationshipField) {
        List<Object> here = new ArrayList<>();
        for (BundleChildEntry entry : childEntries(childRelationshipField)) {
            List<Object> childPrimaries = entry.bundle().primaryRecords();
            if (childPrimaries == null) {
                continue;
            }
            for (int childRow = 0; childRow < childPrimaries.size(); childRow++) {
                if (entry.parentRowByChildRow().get(childRow) == parentRowIndex) {
                    here.add(childPrimaries.get(childRow));
                }
            }
        }
        return here;
    }

    /** A single bundle of every child for {@code childRelationshipField}. Null if none. */
    public Bundle getChildBundle(Field childRelationshipField) {
        List<Bundle> bundles = childBundles(childRelationshipField);
        return switch (bundles.size()) {
            case 0 -> null;
            case 1 -> bundles.get(0);
            default -> BundleMerger.combine(bundles);
        };
    }
}
