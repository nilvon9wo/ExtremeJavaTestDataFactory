package net.nowhereatall.xfty.enrichment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.XftyConfigurationException;
import net.nowhereatall.xfty.reflect.RecordShape;

/**
 * Writes onto record instances what an immutable field rejects after
 * construction - a populated parent relationship, a child collection, a forced
 * scalar - via reflection, one clone per row.
 *
 * <p>Standalone: no bundle, no generation. Collect the grafts fluently, then
 * {@link #result()} clones every row once and applies them all. Inputs are
 * untouched; the returned list is new instances.
 */
public final class RecordInjector {

    private final List<Object> records;
    private final Map<Field, List<Object>> parentsByRelationshipField = new LinkedHashMap<>();
    private final Map<Field, List<List<Object>>> childrenByRelationshipField = new LinkedHashMap<>();
    private final Map<Field, Object> uniformValueByField = new LinkedHashMap<>();
    private final Map<Field, List<Object>> perRowValuesByField = new LinkedHashMap<>();

    private RecordInjector(List<Object> records) {
        if (records == null) {
            throw new XftyConfigurationException("RecordInjector needs a records list, not null.");
        }
        this.records = records;
    }

    public static RecordInjector inject(List<Object> records) {
        return new RecordInjector(records);
    }

    /** Graft {@code parents[row]} onto {@code records[row]} under {@code relationshipField}. */
    public RecordInjector relationship(Field relationshipField, List<Object> parents) {
        this.parentsByRelationshipField.put(relationshipField, parents);
        return this;
    }

    /** Graft {@code childrenPerRow[row]} onto {@code records[row]} as {@code relationshipField}'s collection. */
    public RecordInjector childRelationship(Field relationshipField, List<List<Object>> childrenPerRow) {
        this.childrenByRelationshipField.put(relationshipField, childrenPerRow);
        return this;
    }

    /** Set {@code field} to the same value on every row. */
    public RecordInjector value(Field field, Object valueForEveryRow) {
        this.uniformValueByField.put(field, valueForEveryRow);
        return this;
    }

    /** Set {@code field} to {@code values[row]} on each row. */
    public RecordInjector valuePerRow(Field field, List<Object> values) {
        this.perRowValuesByField.put(field, values);
        return this;
    }

    public List<Object> result() {
        if (this.records.isEmpty()) {
            return new ArrayList<>();
        }
        rejectMisalignedGrafts();
        List<Object> grafted = new ArrayList<>(this.records.size());
        for (int row = 0; row < this.records.size(); row++) {
            grafted.add(graftedRow(this.records.get(row), row));
        }
        return grafted;
    }

    private Object graftedRow(Object record, int row) {
        RecordShape shape = RecordShape.of(record.getClass());
        Map<Field, Object> changes = new LinkedHashMap<>();
        this.parentsByRelationshipField.forEach((field, parents) -> changes.put(field, parents.get(row)));
        this.childrenByRelationshipField.forEach((field, perRow) -> changes.put(field, new ArrayList<>(perRow.get(row))));
        forcedValuesForRow(row).forEach(changes::put);
        return shape.setAll(shape.copy(record), changes);
    }

    private Map<Field, Object> forcedValuesForRow(int row) {
        Map<Field, Object> here = new LinkedHashMap<>(this.uniformValueByField);
        this.perRowValuesByField.forEach((field, values) -> here.put(field, values.get(row)));
        return here;
    }

    private void rejectMisalignedGrafts() {
        int rows = this.records.size();
        this.parentsByRelationshipField.forEach((field, parents) -> rejectWrongLength(field.name(), parents.size(), rows));
        this.childrenByRelationshipField.forEach((field, perRow) -> rejectWrongLength(field.name(), perRow.size(), rows));
        this.perRowValuesByField.forEach((field, values) -> rejectWrongLength("valuePerRow(" + field.name() + ")", values.size(), rows));
    }

    private static void rejectWrongLength(String label, int actual, int expected) {
        if (actual != expected) {
            throw new XftyConfigurationException(
                    "RecordInjector: " + label + " has " + actual + " entries but there are " + expected
                    + " records - grafts must align 1:1 with the records.");
        }
    }
}
