package net.nowhereatall.xfty.values;

/**
 * A value that needs no context to produce - a literal, a counter, a random
 * unique token.
 *
 * <p>See {@code ContextAwareExpressionLike} (ported with the generation engine)
 * for values that read sibling fields or an ancestor record - that is a
 * <em>separate</em> interface, not a subtype: a context-aware value genuinely
 * cannot produce anything without a context.
 *
 * <p>(C# {@code IValueExpression}.)
 */
@FunctionalInterface
public interface ValueExpressionLike {

    Object get();
}
