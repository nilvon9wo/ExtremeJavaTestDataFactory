package net.nowhereatall.xfty.values;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link ValueExpressionLike} producing fixed-length, uppercase,
 * unique-within-one-process strings ("AAA", "AAB", ... for length 3) - a
 * base-26 counter over A-Z, counted separately per requested length.
 */
public final class UniqueStringOfLengthExpression implements ValueExpressionLike {

    private static final int A_ASCII_CODE = 65;
    private static final int ALPHABET_LENGTH = 26;

    private static final Map<Integer, AtomicInteger> LENGTH_TO_COUNTER = new ConcurrentHashMap<>();

    private final int length;

    public UniqueStringOfLengthExpression(int length) {
        this.length = length;
    }

    @Override
    public Object get() {
        int counter = LENGTH_TO_COUNTER
                .computeIfAbsent(this.length, ignored -> new AtomicInteger(0))
                .getAndIncrement();
        return generateNextString(counter, this.length);
    }

    private static String generateNextString(int counter, int remainingLength) {
        return remainingLength == 0
                ? ""
                : (char) (A_ASCII_CODE + (counter % ALPHABET_LENGTH))
                  + generateNextString(counter / ALPHABET_LENGTH, remainingLength - 1);
    }
}
