package net.nowhereatall.xfty.enrichment;

import java.util.List;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.core.Bundle;

/** One frame of {@link BundleEnricher}'s recursive walk. Populated only by BundleEnricher; everything else reads. */
final class EnrichmentPosition {

    private final Bundle subBundle;
    private final List<Object> records;

    List<Field> pathFromEntry;
    List<Field> childPathFromEntry;
    int parentDepthLeft;
    int childDepthLeft;
    boolean isRoot;

    private Field inverseChildField;
    private List<List<Object>> inverseChildrenPerRow;

    EnrichmentPosition(Bundle subBundle, List<Object> records) {
        this.subBundle = subBundle;
        this.records = records;
    }

    Bundle subBundle() {
        return this.subBundle;
    }

    List<Object> records() {
        return this.records;
    }

    Field inverseChildField() {
        return this.inverseChildField;
    }

    List<List<Object>> inverseChildrenPerRow() {
        return this.inverseChildrenPerRow;
    }

    void carryInverse(Field childField, List<List<Object>> perRow) {
        this.inverseChildField = childField;
        this.inverseChildrenPerRow = perRow;
    }

    Class<?> positionType() {
        return this.records == null || this.records.isEmpty() ? null : this.records.get(0).getClass();
    }
}
