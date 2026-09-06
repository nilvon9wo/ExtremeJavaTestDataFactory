package net.nowhereatall.xfty.values;

/** A {@link ValueExpressionLike} that always returns the same fixed value, null included. */
public final class LiteralExpression implements ValueExpressionLike {

    private final Object value;

    public LiteralExpression(Object value) {
        this.value = value;
    }

    @Override
    public Object get() {
        return this.value;
    }
}
