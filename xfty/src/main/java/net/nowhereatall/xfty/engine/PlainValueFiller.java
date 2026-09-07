package net.nowhereatall.xfty.engine;

import java.util.ArrayList;
import java.util.List;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.core.MasterTemplate;
import net.nowhereatall.xfty.reflect.RecordShape;
import net.nowhereatall.xfty.values.ValueExpressionLike;

/**
 * Fills the plain (non-context-aware) default values on a copy of the test's
 * template. A copy is taken first, so the caller's template is untouched; for a
 * record every fill produces a new instance, which is threaded through.
 */
public final class PlainValueFiller {

    private PlainValueFiller() {
    }

    public static Object cloneAndCompletePlainValues(MasterTemplate template, Object testTemplate) {
        RecordShape shape = RecordShape.of(testTemplate.getClass());
        Object record = shape.copy(testTemplate);
        for (Field field : template.orderedValueFields()) {
            record = fillPlainValue(template, shape, record, field);
        }
        return record;
    }

    public static List<Object> cloneAndCompletePlainValues(MasterTemplate template, List<Object> testTemplates) {
        List<Object> filled = new ArrayList<>(testTemplates.size());
        for (Object testTemplate : testTemplates) {
            filled.add(cloneAndCompletePlainValues(template, testTemplate));
        }
        return filled;
    }

    private static Object fillPlainValue(MasterTemplate template, RecordShape shape, Object record, Field field) {
        ValueExpressionLike strategy = template.defaultByField().get(field);
        if (strategy == null || field.get(record) != null) {
            return record;
        }
        return shape.set(record, field, strategy.get());
    }
}
