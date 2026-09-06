package net.nowhereatall.xfty.values;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link ValueExpressionLike} producing well-formed, unique-within-one-process
 * "prefix1@example.com" style addresses.
 */
public final class UniqueEmailExpression implements ValueExpressionLike {

    private static final AtomicInteger COUNTER = new AtomicInteger(1);

    private final String prefix;

    public UniqueEmailExpression(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public Object get() {
        return this.prefix + COUNTER.getAndIncrement() + "@example.com";
    }
}
