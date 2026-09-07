package net.nowhereatall.xfty.values;

import net.nowhereatall.xfty.core.GenerationContext;

/**
 * A value expression that needs the surrounding generation context - sibling
 * fields on the record being built, or a field on a generated ancestor.
 *
 * <p>This is a <b>separate</b> interface from {@link ValueExpressionLike}, not a
 * subtype: a context-aware value genuinely cannot produce anything without a
 * context, so making it satisfy the no-argument {@code get()} contract would be
 * a lie.
 *
 * <p>(C# {@code IContextAwareExpression}.)
 */
@FunctionalInterface
public interface ContextAwareExpressionLike {

    Object get(GenerationContext context);
}
