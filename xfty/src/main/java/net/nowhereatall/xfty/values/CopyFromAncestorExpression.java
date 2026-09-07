package net.nowhereatall.xfty.values;

import java.util.List;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.SerializableFunction;
import net.nowhereatall.xfty.XftyConfigurationException;
import net.nowhereatall.xfty.core.GenerationContext;

/**
 * A context-aware value that copies a field from a generated ancestor.
 *
 * <p>{@code path} is one or more relationship hops then the field to read.
 * Returns null if any hop of the relationship was not generated (e.g. an
 * optional one skipped by the current inclusivity).
 */
public final class CopyFromAncestorExpression implements ContextAwareExpressionLike {

    private final List<Field> path;

    public CopyFromAncestorExpression(Field relationshipField, Field sourceField) {
        this(List.of(relationshipField, sourceField));
    }

    public CopyFromAncestorExpression(List<Field> pathEndingInSourceField) {
        if (pathEndingInSourceField == null || pathEndingInSourceField.size() < 2) {
            throw new XftyConfigurationException(
                    "CopyFromAncestorExpression needs a path of at least one relationship field then the field to read.");
        }
        if (pathEndingInSourceField.stream().anyMatch(step -> step == null)) {
            throw new XftyConfigurationException("CopyFromAncestorExpression path steps cannot be null.");
        }
        this.path = List.copyOf(pathEndingInSourceField);
    }

    /** {@code new CopyFromAncestorExpression(relationshipField, sourceField)}, naming both by {@code Type::accessor}. */
    public static <TR, R1, TT, R2> CopyFromAncestorExpression from(
            SerializableFunction<TR, R1> relationshipField, SerializableFunction<TT, R2> sourceField) {
        return new CopyFromAncestorExpression(Field.of(relationshipField), Field.of(sourceField));
    }

    @Override
    public Object get(GenerationContext context) {
        return context.bundleSoFar() == null ? null : context.bundleSoFar().getValue(this.path, context.rowIndex());
    }
}
