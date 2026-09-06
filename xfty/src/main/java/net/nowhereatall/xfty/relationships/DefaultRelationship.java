package net.nowhereatall.xfty.relationships;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.lookup.LookupKeyLike;
import net.nowhereatall.xfty.lookup.ProviderLookupLike;
import net.nowhereatall.xfty.lookup.ProviderLookups;

/**
 * The standard relationship implementation: generate a fresh parent record from
 * the given override template.
 */
public final class DefaultRelationship implements DefaultRelationshipLike {

    private final LookupKeyLike explicitLookupKey;
    private final Object overrideTemplate;
    private final Field relatedField;
    private LookupKeyLike resolvedLookupKey;

    public DefaultRelationship(LookupKeyLike lookupKey, Object overrideTemplate, Field relatedField) {
        this.explicitLookupKey = lookupKey;
        this.overrideTemplate = overrideTemplate;
        this.relatedField = relatedField;
    }

    public DefaultRelationship(Object overrideTemplate) {
        this(null, overrideTemplate, null);
    }

    public DefaultRelationship(Object overrideTemplate, Field relatedField) {
        this(null, overrideTemplate, relatedField);
    }

    public DefaultRelationship(LookupKeyLike lookupKey, Object overrideTemplate) {
        this(lookupKey, overrideTemplate, null);
    }

    @Override
    public Object overrideTemplate() {
        return this.overrideTemplate;
    }

    @Override
    public Field relatedField() {
        return this.relatedField;
    }

    @Override
    public LookupKeyLike resolveLookupKey(ProviderLookupLike providerLookup) {
        if (this.resolvedLookupKey == null) {
            this.resolvedLookupKey = ProviderLookups.reconcile(providerLookup, this.explicitLookupKey, this.overrideTemplate);
        }
        return this.resolvedLookupKey;
    }
}
