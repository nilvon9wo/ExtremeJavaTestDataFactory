package net.nowhereatall.xfty.values;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.SerializableFunction;
import net.nowhereatall.xfty.XftyConfigurationException;
import net.nowhereatall.xfty.core.GenerationContext;

/**
 * A context-aware value that copies another field from the <em>same</em> record.
 *
 * <p>The sibling must be resolvable when this runs: a plain value (always), or
 * another context-aware value that was put <em>before</em> this one. Reading a
 * context-aware sibling put <em>after</em> this one (or a circular pair) throws
 * loudly from {@link GenerationContext#siblingValue} - never a silent null.
 */
public final class CopyFromSiblingExpression implements ContextAwareExpressionLike {

    private final Field sourceField;

    public CopyFromSiblingExpression(Field sourceField) {
        if (sourceField == null) {
            throw new XftyConfigurationException("CopyFromSiblingExpression needs a source field.");
        }
        this.sourceField = sourceField;
    }

    /** {@code new CopyFromSiblingExpression(field)}, naming the field by {@code Type::accessor}. */
    public static <T, R> CopyFromSiblingExpression from(SerializableFunction<T, R> sourceField) {
        return new CopyFromSiblingExpression(Field.of(sourceField));
    }

    @Override
    public Object get(GenerationContext context) {
        return context.siblingValue(this.sourceField);
    }
}
