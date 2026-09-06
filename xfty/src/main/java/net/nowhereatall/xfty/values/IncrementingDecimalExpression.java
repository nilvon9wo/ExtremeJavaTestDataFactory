package net.nowhereatall.xfty.values;

import java.math.BigDecimal;

/** A {@link ValueExpressionLike} producing ascending decimals - 1, 2, 3... per instance. */
public final class IncrementingDecimalExpression implements ValueExpressionLike {

    private BigDecimal counter = BigDecimal.ONE;

    @Override
    public Object get() {
        BigDecimal current = this.counter;
        this.counter = this.counter.add(BigDecimal.ONE);
        return current;
    }
}
