package net.nowhereatall.xfty.values;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link ValueExpressionLike} unique within one process's lifetime (the
 * counter is process-static, not per-instance) - not across persisted runs;
 * see {@link UniqueAcrossRunsExpression} for that.
 */
public final class UniqueStringExpression implements ValueExpressionLike {

    private static final AtomicInteger COUNTER = new AtomicInteger(1);

    private final String prefix;

    public UniqueStringExpression(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public Object get() {
        return this.prefix + " " + COUNTER.getAndIncrement();
    }
}
