package net.nowhereatall.xfty.engine;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.core.Bundle;
import net.nowhereatall.xfty.core.GenerationContext;
import net.nowhereatall.xfty.core.MasterTemplate;
import net.nowhereatall.xfty.reflect.RecordShape;
import net.nowhereatall.xfty.values.ContextAwareExpressionLike;

/**
 * The second value pass: the context-aware expressions, run once the plain
 * values, ancestors and lookups are all in place. Rewrites the bundle's primary
 * list with the completed records (a record is rebuilt per field set).
 */
public final class ContextAwareValuePass {

    private final Bundle bundle;
    private final GenerationContext context;
    private final MasterTemplate template;

    public ContextAwareValuePass(Bundle bundle, GenerationContext context, MasterTemplate template) {
        this.bundle = bundle;
        this.context = context;
        this.template = template;
    }

    public void complete() {
        if (this.template.contextAwareByField().isEmpty()) {
            return;
        }
        List<Object> records = this.bundle.primaryRecords();
        List<Object> completed = new ArrayList<>(records.size());
        for (int row = 0; row < records.size(); row++) {
            completed.add(completeRow(records.get(row), row));
        }
        this.bundle.putPrimaries(this.template.primaryTargetField(), completed);
    }

    private Object completeRow(Object record, int row) {
        RecordShape shape = RecordShape.of(record.getClass());
        Set<Field> pending = new LinkedHashSet<>(this.template.contextAwareByField().keySet());
        Object current = record;
        for (Field field : this.template.orderedValueFields()) {
            GenerationContext rowContext = this.context.forRecord(current, this.bundle, row);
            GenerationContext scoped = rowContext.forValueField(field, pending);
            current = completeField(shape, current, scoped, field);
            pending.remove(field);
        }
        return current;
    }

    private Object completeField(RecordShape shape, Object record, GenerationContext scoped, Field field) {
        ContextAwareExpressionLike expression = this.template.contextAwareByField().get(field);
        if (expression == null || field.get(record) != null) {
            return record;
        }
        return shape.with(record, field, expression.get(scoped));
    }
}
