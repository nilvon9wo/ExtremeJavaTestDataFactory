package net.nowhereatall.xfty.values;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link ValueExpressionLike} unique across processes, machines, and persisted
 * runs - not just within one process's lifetime like
 * {@link UniqueStringExpression} (whose counter starts fresh every run).
 *
 * <p>The difference matters only when the record is <em>persisted</em>: two seed
 * runs each generating "test.username.example1@example.com" collide on the
 * second insert. {@code prefix} + a per-run token (time + randomness) + a
 * counter + {@code suffix}. Keep {@code prefix}/{@code suffix} short - the token
 * adds ~14 characters.
 */
public final class UniqueAcrossRunsExpression implements ValueExpressionLike {

    private static final String RUN_TOKEN = buildRunToken();
    private static final AtomicInteger COUNTER = new AtomicInteger(1);

    private final String prefix;
    private final String suffix;

    public UniqueAcrossRunsExpression(String prefix, String suffix) {
        this.prefix = prefix == null ? "" : prefix;
        this.suffix = suffix == null ? "" : suffix;
    }

    @Override
    public Object get() {
        return this.prefix + RUN_TOKEN + COUNTER.getAndIncrement() + this.suffix;
    }

    private static String buildRunToken() {
        String nowMillis = Long.toString(System.currentTimeMillis());
        String entropy = Integer.toString(ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE));
        return right(nowMillis, 9) + right(entropy, 5);
    }

    private static String right(String text, int length) {
        return text.length() <= length ? text : text.substring(text.length() - length);
    }
}
