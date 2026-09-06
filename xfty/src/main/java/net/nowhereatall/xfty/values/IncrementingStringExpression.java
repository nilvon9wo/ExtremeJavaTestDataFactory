package net.nowhereatall.xfty.values;

/**
 * A {@link ValueExpressionLike} producing "prefix 1", "prefix 2", ... per
 * instance - or "prefix1", "prefix2" with {@link #DONT_SEPARATE_PREFIX}.
 */
public final class IncrementingStringExpression implements ValueExpressionLike {

    public static final boolean SEPARATE_PREFIX = true;
    public static final boolean DONT_SEPARATE_PREFIX = false;

    private final String prefix;
    private final boolean separatePrefix;
    private int counter = 1;

    public IncrementingStringExpression(String prefix) {
        this(prefix, SEPARATE_PREFIX);
    }

    public IncrementingStringExpression(String prefix, boolean separatePrefix) {
        this.prefix = prefix;
        this.separatePrefix = separatePrefix;
    }

    @Override
    public Object get() {
        String separator = this.separatePrefix ? " " : "";
        return this.prefix + separator + this.counter++;
    }
}
