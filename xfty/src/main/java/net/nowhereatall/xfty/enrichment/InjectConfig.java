package net.nowhereatall.xfty.enrichment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.nowhereatall.xfty.Field;

/**
 * What {@code bundle.inject(field, config)} should materialise. Start from a
 * breadth - {@link #nothing()}, {@link #allParents()}, {@link #allChildren()},
 * {@link #everything()} - then layer refiners on. A plain state carrier; the
 * enricher reads the fields, nothing here acts.
 */
public final class InjectConfig {

    public static final int DEFAULT_PARENT_DEPTH_LIMIT = 5;
    public static final int DEFAULT_CHILD_DEPTH_LIMIT = 1;

    private final boolean fromAllParents;
    private final boolean fromAllChildren;

    private final List<List<Field>> includedParentPaths = new ArrayList<>();
    private final List<List<Field>> excludedParentPaths = new ArrayList<>();
    private final Set<Field> includedChildFields = new LinkedHashSet<>();
    private final Set<Field> excludedChildFields = new LinkedHashSet<>();
    private final Map<Field, Object> onRecordValues = new LinkedHashMap<>();
    private final List<AncestorValue> ancestorValues = new ArrayList<>();
    private final List<ChildValue> childValues = new ArrayList<>();

    private int parentDepthLimit = DEFAULT_PARENT_DEPTH_LIMIT;
    private int childDepthLimit = DEFAULT_CHILD_DEPTH_LIMIT;
    private boolean depthLimitsLifted;

    private InjectConfig(boolean fromAllParents, boolean fromAllChildren) {
        this.fromAllParents = fromAllParents;
        this.fromAllChildren = fromAllChildren;
    }

    /** Name every parent and child to inject; nothing is materialised by default. */
    public static InjectConfig nothing() {
        return new InjectConfig(false, false);
    }

    /** Every generated ancestor, to parentDepth; children only if named. */
    public static InjectConfig allParents() {
        return new InjectConfig(true, false);
    }

    /** Every generated child collection, to childDepth; parents only if named. */
    public static InjectConfig allChildren() {
        return new InjectConfig(false, true);
    }

    /** Every generated ancestor and child collection. {@code bundle.injectAll(field)} uses this. */
    public static InjectConfig everything() {
        return new InjectConfig(true, true);
    }

    public boolean fromAllParents() {
        return this.fromAllParents;
    }

    public boolean fromAllChildren() {
        return this.fromAllChildren;
    }

    public List<List<Field>> includedParentPaths() {
        return this.includedParentPaths;
    }

    public List<List<Field>> excludedParentPaths() {
        return this.excludedParentPaths;
    }

    public Set<Field> includedChildFields() {
        return this.includedChildFields;
    }

    public Set<Field> excludedChildFields() {
        return this.excludedChildFields;
    }

    public Map<Field, Object> onRecordValues() {
        return this.onRecordValues;
    }

    public List<AncestorValue> ancestorValues() {
        return this.ancestorValues;
    }

    public List<ChildValue> childValues() {
        return this.childValues;
    }

    public int parentDepthLimit() {
        return this.parentDepthLimit;
    }

    public int childDepthLimit() {
        return this.childDepthLimit;
    }

    public boolean depthLimitsLifted() {
        return this.depthLimitsLifted;
    }

    public InjectConfig injectParent(List<Field> path) {
        this.includedParentPaths.add(path);
        return this;
    }

    public InjectConfig excludeParent(List<Field> path) {
        this.excludedParentPaths.add(path);
        return this;
    }

    /** Inject the child collection this lookup field defines (Contact.accountId → the Account's contacts). */
    public InjectConfig injectChild(Field childLookupField) {
        this.includedChildFields.add(childLookupField);
        return this;
    }

    public InjectConfig excludeChild(Field childLookupField) {
        this.excludedChildFields.add(childLookupField);
        return this;
    }

    /** A scalar on the target record - the formula / roll-up / system / read-only case. */
    public InjectConfig injectValue(Field field, Object value) {
        this.onRecordValues.put(field, value);
        return this;
    }

    /** A scalar on a record several relationship hops up - path is the hops then the target field. */
    public InjectConfig injectValue(List<Field> pathToField, Object value) {
        this.ancestorValues.add(new AncestorValue(pathToField, value));
        return this;
    }

    /** A scalar on every record of the child collection {@code childField} defines. */
    public InjectConfig injectChildValue(Field childField, Field leafField, Object value) {
        return injectChildValue(List.of(childField, leafField), value);
    }

    /** A scalar on a child (or grandchild) record - path is the child-lookup hops downward, then the field to set. */
    public InjectConfig injectChildValue(List<Field> pathToLeaf, Object value) {
        this.childValues.add(new ChildValue(pathToLeaf, value));
        return this;
    }

    public InjectConfig parentDepth(int hops) {
        this.parentDepthLimit = hops;
        return this;
    }

    public InjectConfig childDepth(int hops) {
        this.childDepthLimit = hops;
        return this;
    }

    /** Allow parentDepth, childDepth and the injectParent path length to exceed one query round-trip's reasonable size. */
    public InjectConfig allowDeeperGraph() {
        this.depthLimitsLifted = true;
        return this;
    }
}
