package net.nowhereatall.xfty.engine;

import java.util.Collection;
import java.util.Set;

import net.nowhereatall.xfty.Field;

/**
 * The narrowest scope of a generation run: the single value field whose
 * context-aware expression is running right now, plus the set of context-aware
 * value fields on the same record that have <em>not</em> been generated yet.
 *
 * <p>{@link #pendingContextAwareValues()} is what lets the engine tell "this
 * sibling has not been computed yet" apart from "this sibling was computed and
 * the answer is null".
 */
public final class ValueFieldPass {

    private final Field fieldBeingBuilt;
    private final Set<Field> pendingContextAwareValues;

    public ValueFieldPass(Field fieldBeingBuilt, Collection<Field> pendingContextAwareValues) {
        this.fieldBeingBuilt = fieldBeingBuilt;
        this.pendingContextAwareValues = Set.copyOf(pendingContextAwareValues);
    }

    public Field fieldBeingBuilt() {
        return this.fieldBeingBuilt;
    }

    public Set<Field> pendingContextAwareValues() {
        return this.pendingContextAwareValues;
    }
}
