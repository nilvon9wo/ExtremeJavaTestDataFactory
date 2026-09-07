package net.nowhereatall.xfty.values;

import net.nowhereatall.xfty.engine.DeferredGraph;

/**
 * A value that reads <b>up</b> the graph - a field on a record derived from one
 * of its generated descendants. It cannot be evaluated when the record is built,
 * because the descendant does not exist yet, so it is resolved in a pass over
 * the whole in-memory forest just before the depth-batched insert.
 *
 * <p>That forest only exists under the DEFERRED insert mode; a Provider that
 * carries one of these in any other mode throws.
 *
 * <p>(C# {@code IDeferredExpression}.)
 */
@FunctionalInterface
public interface DeferredExpressionLike {

    /** The value for {@code records[recordIndex]}'s field, read from its descendants via {@code graph}. */
    Object get(DeferredGraph graph, int recordIndex);
}
