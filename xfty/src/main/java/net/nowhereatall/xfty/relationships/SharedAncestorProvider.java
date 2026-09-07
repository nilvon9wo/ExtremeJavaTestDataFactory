package net.nowhereatall.xfty.relationships;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.SerializableFunction;
import net.nowhereatall.xfty.XftyConfigurationException;
import net.nowhereatall.xfty.core.Bundle;
import net.nowhereatall.xfty.core.GenerationContext;
import net.nowhereatall.xfty.core.InsertInclusivity;
import net.nowhereatall.xfty.core.InsertMode;
import net.nowhereatall.xfty.core.MasterTemplate;
import net.nowhereatall.xfty.core.PathValue;
import net.nowhereatall.xfty.core.RecordProviderLike;
import net.nowhereatall.xfty.engine.RecordFactory;
import net.nowhereatall.xfty.lookup.LookupKeyLike;
import net.nowhereatall.xfty.lookup.ProviderLookupLike;
import net.nowhereatall.xfty.lookup.ProviderLookups;
import net.nowhereatall.xfty.reflect.RecordShape;
import net.nowhereatall.xfty.values.ContextAwareExpressionLike;
import net.nowhereatall.xfty.values.ValueExpressionLike;

/**
 * The single recipe for one shared ancestor's record - what
 * {@link net.nowhereatall.xfty.engine.SharedAncestorResolver} builds from.
 * Obtained by chaining onto {@code SharedAncestor.put(...)}; never constructed
 * directly.
 */
public final class SharedAncestorProvider {

    private final SharedAncestor owner;
    private final List<SharedAncestorFieldValue> valuePuts = new ArrayList<>();
    private final List<SharedAncestorFieldValue> requiredRelationships = new ArrayList<>();
    private final List<SharedAncestorFieldValue> optionalRelationships = new ArrayList<>();
    private final List<List<Field>> forcedRelationshipPaths = new ArrayList<>();
    private final List<PathValue> pathValues = new ArrayList<>();

    private Object overrideTemplate;
    private LookupKeyLike explicitKey;
    private LookupKeyLike resolvedKey;
    private Field relatedField;
    private InsertInclusivity inclusivity;

    SharedAncestorProvider(SharedAncestor owner) {
        this.owner = owner;
    }

    public SharedAncestorProvider withTemplate(Object overrideTemplate) {
        this.owner.assertUnresolved("put(name, template)");
        this.overrideTemplate = overrideTemplate;
        return this;
    }

    public SharedAncestorProvider fromVariant(LookupKeyLike key) {
        this.owner.assertUnresolved("fromVariant(...)");
        this.explicitKey = key;
        return this;
    }

    public SharedAncestorProvider copyingRelatedField(Field relatedField) {
        this.owner.assertUnresolved("copyingRelatedField(...)");
        this.relatedField = relatedField;
        return this;
    }

    public <T, R> SharedAncestorProvider copyingRelatedField(SerializableFunction<T, R> relatedField) {
        return copyingRelatedField(Field.of(relatedField));
    }

    public SharedAncestorProvider setInclusivity(InsertInclusivity inclusivity) {
        this.owner.assertUnresolved("setInclusivity(...)");
        this.inclusivity = inclusivity;
        return this;
    }

    public SharedAncestorProvider put(Field field, Object value) {
        this.owner.assertUnresolved("put(...)");
        this.valuePuts.add(new SharedAncestorFieldValue(field, value));
        return this;
    }

    public <T, R> SharedAncestorProvider put(SerializableFunction<T, R> field, Object value) {
        return put(Field.of(field), value);
    }

    public SharedAncestorProvider putRequired(Field field, DefaultRelationshipLike relationship) {
        this.owner.assertUnresolved("putRequired(...)");
        this.requiredRelationships.add(new SharedAncestorFieldValue(field, relationship));
        return this;
    }

    public <T, R> SharedAncestorProvider putRequired(SerializableFunction<T, R> field, DefaultRelationshipLike relationship) {
        return putRequired(Field.of(field), relationship);
    }

    public SharedAncestorProvider putOptional(Field field, DefaultRelationshipLike relationship) {
        this.owner.assertUnresolved("putOptional(...)");
        this.optionalRelationships.add(new SharedAncestorFieldValue(field, relationship));
        return this;
    }

    public SharedAncestorProvider includeOptional(Field relationshipField) {
        return includeOptional(new ArrayList<>(List.of(relationshipField)));
    }

    public SharedAncestorProvider includeOptional(List<Field> relationshipPath) {
        this.owner.assertUnresolved("includeOptional(...)");
        this.forcedRelationshipPaths.add(relationshipPath);
        return this;
    }

    // Used by SharedAncestor / SharedAncestorResolver -----------------

    public Field relatedField() {
        return this.relatedField;
    }

    public Object overrideTemplate() {
        return this.overrideTemplate;
    }

    /** This ancestor's whole graph, generated with no persistence, ready for the depth-batched insert. */
    public CompletableFuture<Bundle> buildInMemory(ProviderLookupLike lookup) {
        InsertInclusivity effectiveInclusivity = this.inclusivity != null ? this.inclusivity : InsertInclusivity.REQUIRED;
        GenerationContext context = new GenerationContext(lookup, InsertMode.NEVER, effectiveInclusivity)
                .withForcedRelationshipPaths(this.forcedRelationshipPaths)
                .withPathValues(this.pathValues);
        Object seed = RecordShape.of(recordTemplate(lookup).getClass()).copy(recordTemplate(lookup));
        return RecordFactory.createBundle(context, masterTemplate(lookup), new ArrayList<>(List.of(seed)));
    }

    public Field primaryField(ProviderLookupLike lookup) {
        return baseProvider(lookup).primaryTargetField();
    }

    /** The Master Template the pre-phase scans for nested shared ancestors - with this ancestor's puts applied. */
    public MasterTemplate masterTemplate(ProviderLookupLike lookup) {
        MasterTemplate template = baseProvider(lookup).masterTemplate().copy();
        this.valuePuts.forEach(put -> template.put(put.field(), put.value()));
        this.requiredRelationships.forEach(put -> template.putRequired(put.field(), (DefaultRelationshipLike) put.value()));
        this.optionalRelationships.forEach(put -> template.putOptional(put.field(), (DefaultRelationshipLike) put.value()));
        return template;
    }

    /** True when the shared record is a single row with no sub-graph of its own. */
    public boolean isLightweight(ProviderLookupLike lookup) {
        if (!this.requiredRelationships.isEmpty() || !this.optionalRelationships.isEmpty()
                || !this.forcedRelationshipPaths.isEmpty()) {
            return false;
        }
        for (PathValue pathValue : this.pathValues) {
            if (pathValue.isRelationshipKind()) {
                return false;
            }
        }
        MasterTemplate baseTemplate = baseProvider(lookup).masterTemplate();
        return baseTemplate.requiredRelationshipByField().isEmpty()
                && baseTemplate.optionalRelationshipByField().isEmpty();
    }

    /** The lookup key this ancestor resolves under. */
    public LookupKeyLike lookupKey(ProviderLookupLike lookup) {
        if (this.resolvedKey == null) {
            this.resolvedKey = this.explicitKey != null
                    ? this.explicitKey
                    : ProviderLookups.resolve(lookup, requireTemplate());
        }
        return this.resolvedKey;
    }

    private RecordProviderLike baseProvider(ProviderLookupLike lookup) {
        return lookup.get(lookupKey(lookup));
    }

    private Object recordTemplate(ProviderLookupLike lookup) {
        return this.overrideTemplate != null
                ? this.overrideTemplate
                : RecordShape.of(lookupKey(lookup).recordType()).instantiate();
    }

    private Object requireTemplate() {
        if (this.overrideTemplate == null) {
            throw new XftyConfigurationException("Shared ancestor \"" + this.owner.sharedName()
                    + "\" needs SharedAncestor.putAsTemplate(...) or put(name, key) before it can resolve.");
        }
        return this.overrideTemplate;
    }
}
