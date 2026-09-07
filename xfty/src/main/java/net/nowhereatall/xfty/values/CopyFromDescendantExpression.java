package net.nowhereatall.xfty.values;

import java.util.List;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.SerializableFunction;
import net.nowhereatall.xfty.XftyConfigurationException;
import net.nowhereatall.xfty.engine.DeferredGraph;

/**
 * An up-flowing value: a field copied from a generated <b>descendant</b> - the
 * record that references this one through {@code childLookupField}, or (via the
 * path-list constructor) a chain of such lookups ending at a grandchild or
 * deeper. At every hop the <b>first</b> matching child is read; with no match at
 * any hop the value is {@code null}.
 *
 * <p>Needs the DEFERRED insert mode - a descendant must exist before it can be
 * read - and resolves when the deferred flush runs.
 */
public final class CopyFromDescendantExpression implements DeferredExpressionLike {

    private final List<Field> path;

    public CopyFromDescendantExpression(Field childLookupField, Field sourceField) {
        this(List.of(childLookupField, sourceField));
    }

    public CopyFromDescendantExpression(List<Field> pathEndingInSourceField) {
        if (pathEndingInSourceField == null || pathEndingInSourceField.size() < 2) {
            throw new XftyConfigurationException(
                    "CopyFromDescendantExpression needs a path of at least one child-lookup field then the field to read.");
        }
        if (pathEndingInSourceField.stream().anyMatch(step -> step == null)) {
            throw new XftyConfigurationException("CopyFromDescendantExpression path steps cannot be null.");
        }
        this.path = List.copyOf(pathEndingInSourceField);
    }

    public static <TChild, R1, R2> CopyFromDescendantExpression from(
            SerializableFunction<TChild, R1> childLookupField, SerializableFunction<TChild, R2> sourceField) {
        return new CopyFromDescendantExpression(Field.of(childLookupField), Field.of(sourceField));
    }

    @Override
    public Object get(DeferredGraph graph, int recordIndex) {
        Integer descendantIndex = walkHops(graph, recordIndex, 0);
        return descendantIndex == null ? null : this.path.get(this.path.size() - 1).get(graph.recordAt(descendantIndex));
    }

    private Integer walkHops(DeferredGraph graph, int currentIndex, int hopNumber) {
        if (hopNumber == this.path.size() - 1) {
            return currentIndex;
        }
        List<Integer> childIndices = graph.childIndicesOf(currentIndex, this.path.get(hopNumber));
        return childIndices.isEmpty() ? null : walkHops(graph, childIndices.get(0), hopNumber + 1);
    }
}
