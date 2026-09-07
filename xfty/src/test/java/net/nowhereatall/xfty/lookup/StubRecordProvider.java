package net.nowhereatall.xfty.lookup;

import java.util.List;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.core.Bundle;
import net.nowhereatall.xfty.core.GenerationContext;
import net.nowhereatall.xfty.core.MasterTemplate;
import net.nowhereatall.xfty.core.RecordProviderLike;

/** A routing stub: the lookup system only ever stores and returns providers, never runs them. */
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

    public Class<?> primaryType() {
        return this.primaryType;
    }

    public String label() {
        return this.label;
    }

    @Override
    public Field primaryTargetField() {
        throw new UnsupportedOperationException("routing stub");
    }

    @Override
    public MasterTemplate masterTemplate() {
        throw new UnsupportedOperationException("routing stub");
    }

    @Override
    public Bundle createBundle(GenerationContext context, List<Object> templateRecords) {
        throw new UnsupportedOperationException("routing stub");
    }
}
