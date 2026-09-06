package net.nowhereatall.xfty.lookup;

import net.nowhereatall.xfty.core.RecordProviderLike;

/** A do-nothing provider that only reports its type - enough to test routing. */
public final class StubRecordProvider implements RecordProviderLike {

    private final Class<?> primaryType;
    private final String label;

    public StubRecordProvider(Class<?> primaryType, String label) {
        this.primaryType = primaryType;
        this.label = label;
    }

    public StubRecordProvider() {
        this(Object.class, "default");
    }

    @Override
    public Class<?> primaryType() {
        return this.primaryType;
    }

    public String label() {
        return this.label;
    }
}
