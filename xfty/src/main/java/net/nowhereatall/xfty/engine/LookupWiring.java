package net.nowhereatall.xfty.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.core.Bundle;
import net.nowhereatall.xfty.core.GenerationContext;
import net.nowhereatall.xfty.core.MasterTemplate;
import net.nowhereatall.xfty.reflect.RecordShape;
import net.nowhereatall.xfty.relationships.DefaultRelationshipLike;

/** Points each primary record's lookup field at the matching generated ancestor. */
public final class LookupWiring {

    private final Bundle bundle;
    private final Map<Field, DefaultRelationshipLike> relationships;

    public LookupWiring(Bundle bundle, GenerationContext context, MasterTemplate template) {
        this.bundle = bundle;
        this.relationships = mergeRelationships(template);
    }

    public void wire() {
        List<Object> records = this.bundle.primaryRecords();
        List<Object> wired = new ArrayList<>(records.size());
        for (int row = 0; row < records.size(); row++) {
            wired.add(wireRecord(records.get(row), row));
        }
        this.bundle.putPrimaries(this.bundle.primaryTargetField(), wired);
    }

    private Object wireRecord(Object record, int row) {
        RecordShape shape = RecordShape.of(record.getClass());
        Object current = record;
        for (Field field : this.relationships.keySet()) {
            current = wireField(shape, current, row, field);
        }
        return current;
    }

    private Object wireField(RecordShape shape, Object record, int row, Field field) {
        if (field.get(record) != null) {
            return record;
        }
        Object parent = parentAt(field, row);
        if (parent == null) {
            return record;
        }
        Object value = readValue(parent, this.relationships.get(field).relatedField());
        return shape.set(record, field, value);
    }

    private Object parentAt(Field field, int row) {
        List<Object> parents = this.bundle.getList(field);
        return parents == null || row >= parents.size() ? null : parents.get(row);
    }

    private static Object readValue(Object parent, Field sourceField) {
        if (sourceField != null) {
            return sourceField.get(parent);
        }
        try {
            return Field.of(parent.getClass(), "id").get(parent);
        } catch (RuntimeException absent) {
            return null;
        }
    }

    private static Map<Field, DefaultRelationshipLike> mergeRelationships(MasterTemplate template) {
        Map<Field, DefaultRelationshipLike> merged = new LinkedHashMap<>(template.requiredRelationshipByField());
        merged.putAll(template.optionalRelationshipByField());
        return merged;
    }
}
