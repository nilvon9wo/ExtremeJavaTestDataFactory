package net.nowhereatall.xfty.enrichment;

import java.util.ArrayList;
import java.util.List;

import net.nowhereatall.xfty.Field;

/** A forced scalar on a record several relationship hops up. Path is the hops then the target field. */
public final class AncestorValue {

    private final List<Field> path;
    private final Object value;

    public AncestorValue(List<Field> path, Object value) {
        this.path = path;
        this.value = value;
    }

    public List<Field> path() {
        return this.path;
    }

    public Object value() {
        return this.value;
    }

    public List<Field> relationshipPrefix() {
        return new ArrayList<>(this.path.subList(0, this.path.size() - 1));
    }

    public Field targetField() {
        return this.path.get(this.path.size() - 1);
    }
}
