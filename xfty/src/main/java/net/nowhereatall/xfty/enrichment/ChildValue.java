package net.nowhereatall.xfty.enrichment;

import java.util.ArrayList;
import java.util.List;

import net.nowhereatall.xfty.Field;

/** A forced scalar on the records of a child collection the path reaches downward. */
public final class ChildValue {

    private final List<Field> path;
    private final Object value;

    public ChildValue(List<Field> path, Object value) {
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
