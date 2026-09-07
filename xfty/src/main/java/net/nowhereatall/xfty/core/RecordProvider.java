package net.nowhereatall.xfty.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.SerializableFunction;
import net.nowhereatall.xfty.XftyConfigurationException;
import net.nowhereatall.xfty.engine.RecordFactory;
import net.nowhereatall.xfty.lookup.LookupKey;
import net.nowhereatall.xfty.lookup.LookupKeyLike;
import net.nowhereatall.xfty.lookup.ProviderLookupLike;
import net.nowhereatall.xfty.lookup.ProviderLookups;
import net.nowhereatall.xfty.persistence.PersistenceGatewayLike;
import net.nowhereatall.xfty.reflect.RecordShape;
import net.nowhereatall.xfty.relationships.DefaultRelationshipLike;
import net.nowhereatall.xfty.values.ContextAwareExpressionLike;
import net.nowhereatall.xfty.values.ValueExpressionLike;

/**
 * The primary entry point: configure a {@code T}'s fields and relationships,
 * then {@link #supply()} / {@link #supplyList()} / {@link #supplyBundle()} it.
 * Each returns a {@link CompletableFuture} - generation is asynchronous end to
 * end. {@code supply()} / {@code supplyList()} are typed as {@code T}, so no
 * cast at the call site.
 *
 * <pre>{@code
 * Account account = new RecordProvider<>(Account.class, lookup)
 *     .put(Account::getName, "Acme")
 *     .supply().join();
 * }</pre>
 */
public final class RecordProvider<T> {

    private final Class<T> recordType;
    private final ProviderLookupLike providerLookup;
    private final RecordProviderTemplateConfig templateConfig;

    private List<T> overrideTemplateList;
    private LookupKeyLike explicitVariantKey;
    private int quantityPerListedTemplate = 1;
    private InsertMode insertMode = InsertMode.NEVER;
    private InsertInclusivity inclusivity = InsertInclusivity.NONE;
    private boolean ancestorCyclesAllowed;
    private boolean excludePrimaryIds;
    private PersistenceGatewayLike persistenceGateway;
    private UnsetFieldFillerLike unsetFieldFiller;
    private RecordProviderLike factoryOutlet;

    public RecordProvider(Class<T> recordType, ProviderLookupLike providerLookup) {
        if (recordType == null) {
            throw new XftyConfigurationException("A record type is required to request data.");
        }
        if (providerLookup == null) {
            throw new XftyConfigurationException("A Provider Lookup is required to request data.");
        }
        this.recordType = recordType;
        this.providerLookup = providerLookup;
        this.templateConfig = new RecordProviderTemplateConfig(() -> resolveFactoryOutlet().masterTemplate().copy());
    }

    /** Start from an override template; the record type is taken from it. */
    @SuppressWarnings("unchecked")
    public RecordProvider(T overrideTemplate, ProviderLookupLike providerLookup) {
        this((Class<T>) requireTemplateType(overrideTemplate), providerLookup);
        setOverrideTemplate(overrideTemplate);
    }

    /** Start from a list of override templates; the record type is taken from the first. */
    @SuppressWarnings("unchecked")
    public RecordProvider(List<T> overrideTemplateList, ProviderLookupLike providerLookup) {
        this((Class<T>) typeOf(overrideTemplateList), providerLookup);
        setOverrideTemplateList(overrideTemplateList);
    }

    /** Start from a variant key; the record type is taken from the key, pinned as the variant. */
    @SuppressWarnings("unchecked")
    public RecordProvider(LookupKeyLike variantKey, ProviderLookupLike providerLookup) {
        this((Class<T>) typeOf(variantKey), providerLookup);
        this.explicitVariantKey = variantKey;
    }

    // Configuration -----------------------------------------------------

    public RecordProvider<T> setQuantityPerTemplate(int quantityPerListedTemplate) {
        if (quantityPerListedTemplate < 1) {
            throw new XftyConfigurationException("It makes no sense to supply " + quantityPerListedTemplate + ".");
        }
        this.quantityPerListedTemplate = quantityPerListedTemplate;
        return this;
    }

    public RecordProvider<T> setOverrideTemplateList(List<T> overrideTemplateList) {
        assertNoRecordTypeConflict(overrideTemplateList);
        this.overrideTemplateList = overrideTemplateList;
        return this;
    }

    public RecordProvider<T> setOverrideTemplate(T overrideTemplate) {
        List<T> single = new ArrayList<>();
        single.add(overrideTemplate);
        return setOverrideTemplateList(single);
    }

    public RecordProvider<T> withVariant(LookupKeyLike variantKey) {
        if (this.templateConfig.hasCustomTemplate()) {
            throw new XftyConfigurationException("Call withVariant(...) before customizing the template with put(...).");
        }
        if (variantKey == null || variantKey.recordType() != this.recordType) {
            throw new RecordProviderConflictException(
                    "Variant key is for " + (variantKey == null ? "null" : variantKey.recordType())
                    + " but this Provider requests " + this.recordType + ".");
        }
        this.explicitVariantKey = variantKey;
        return this;
    }

    public RecordProvider<T> setInsertMode(InsertMode insertMode) {
        this.insertMode = insertMode;
        return this;
    }

    public RecordProvider<T> setInclusivity(InsertInclusivity inclusivity) {
        this.inclusivity = inclusivity;
        return this;
    }

    public RecordProvider<T> setPersistenceGateway(PersistenceGatewayLike gateway) {
        this.persistenceGateway = gateway;
        return this;
    }

    public RecordProvider<T> setUnsetFieldFiller(UnsetFieldFillerLike filler) {
        this.unsetFieldFiller = filler;
        return this;
    }

    public RecordProvider<T> allowAncestorCycles() {
        this.ancestorCyclesAllowed = true;
        return this;
    }

    public RecordProvider<T> excludePrimaryIds() {
        this.excludePrimaryIds = true;
        return this;
    }

    public RecordProvider<T> includePrimaryIds() {
        this.excludePrimaryIds = false;
        return this;
    }

    // Field / relationship configuration -------------------------------

    public RecordProvider<T> put(Field field, Object value) {
        this.templateConfig.put(field, value);
        return this;
    }

    public <R> RecordProvider<T> put(SerializableFunction<T, R> field, Object value) {
        return put(Field.of(field), value);
    }

    public RecordProvider<T> putRequired(Field field, DefaultRelationshipLike relationship) {
        this.templateConfig.putRequired(field, relationship);
        return this;
    }

    public <R> RecordProvider<T> putRequired(SerializableFunction<T, R> field, DefaultRelationshipLike relationship) {
        return putRequired(Field.of(field), relationship);
    }

    public RecordProvider<T> putOptional(Field field, DefaultRelationshipLike relationship) {
        this.templateConfig.putOptional(field, relationship);
        return this;
    }

    public <R> RecordProvider<T> putOptional(SerializableFunction<T, R> field, DefaultRelationshipLike relationship) {
        return putOptional(Field.of(field), relationship);
    }

    public RecordProvider<T> removeFromMasterTemplate(Field field) {
        this.templateConfig.removeFromMasterTemplate(field);
        return this;
    }

    public <R> RecordProvider<T> removeFromMasterTemplate(SerializableFunction<T, R> field) {
        return removeFromMasterTemplate(Field.of(field));
    }

    public RecordProvider<T> includeOptional(Field field) {
        List<Field> single = new ArrayList<>();
        single.add(field);
        return includeOptional(single);
    }

    public RecordProvider<T> includeOptional(List<Field> relationshipPath) {
        this.templateConfig.includeOptional(relationshipPath);
        return this;
    }

    public <R> RecordProvider<T> includeOptional(SerializableFunction<T, R> field) {
        return includeOptional(Field.of(field));
    }

    public RecordProvider<T> excludeRelationship(Field field) {
        this.templateConfig.excludeRelationship(field, this.recordType);
        return this;
    }

    public <R> RecordProvider<T> excludeRelationship(SerializableFunction<T, R> field) {
        return excludeRelationship(Field.of(field));
    }

    public RecordProvider<T> excludeRelationshipIfPresent(Field field) {
        this.templateConfig.excludeRelationshipIfPresent(field);
        return this;
    }

    // Path-scoped value overrides -------------------------------------

    public RecordProvider<T> putExpression(List<Field> path, ValueExpressionLike valueExpression) {
        this.templateConfig.addPathValue(PathValue.ofExpression(path, valueExpression));
        return this;
    }

    public RecordProvider<T> putContextAware(List<Field> path, ContextAwareExpressionLike contextAwareExpression) {
        this.templateConfig.addPathValue(PathValue.ofContextAware(path, contextAwareExpression));
        return this;
    }

    public RecordProvider<T> putLiteral(List<Field> path, Object literal) {
        this.templateConfig.addPathValue(PathValue.ofLiteral(path, literal));
        return this;
    }

    public RecordProvider<T> putRequired(List<Field> path, DefaultRelationshipLike relationship) {
        this.templateConfig.addPathValue(PathValue.ofRequiredRelationship(path, relationship));
        return this;
    }

    public RecordProvider<T> putOptional(List<Field> path, DefaultRelationshipLike relationship) {
        this.templateConfig.addPathValue(PathValue.ofOptionalRelationship(path, relationship));
        return this;
    }

    // Supply ----------------------------------------------------------

    public CompletableFuture<Bundle> supplyBundle() {
        warnIfMixingCustomTemplateWithOverrides();
        GenerationContext context = buildContext();
        List<Object> templates = templatesToFill();
        return generate(context, templates);
    }

    public CompletableFuture<List<T>> supplyList() {
        return supplyBundle().thenApply(bundle -> {
            List<Object> raw = bundle.getList(resolveFactoryOutlet().primaryTargetField());
            List<T> typed = new ArrayList<>(raw.size());
            for (Object record : raw) {
                typed.add(this.recordType.cast(record));
            }
            return typed;
        });
    }

    public CompletableFuture<T> supply() {
        return supplyList().thenApply(list -> list.get(0));
    }

    private CompletableFuture<Bundle> generate(GenerationContext context, List<Object> templates) {
        return this.templateConfig.hasCustomTemplate()
                ? RecordFactory.createBundle(context, this.templateConfig.resolveTemplate(), templates)
                : resolveFactoryOutlet().createBundle(context, templates);
    }

    private GenerationContext buildContext() {
        return new GenerationContext(this.providerLookup, this.insertMode, this.inclusivity)
                .withPersistenceGateway(this.persistenceGateway)
                .withUnsetFieldFiller(this.unsetFieldFiller)
                .withForcedRelationshipPaths(this.templateConfig.forcedRelationshipPaths())
                .withPathValues(this.templateConfig.pathValues())
                .withAncestorCycleGuard(this.ancestorCyclesAllowed)
                .withPrimaryIdsExcluded(this.excludePrimaryIds);
    }

    private List<Object> templatesToFill() {
        List<Object> templates = new ArrayList<>();
        if (hasOverrideTemplates()) {
            templates.addAll(this.overrideTemplateList);
        } else {
            templates.add(RecordShape.of(this.recordType).instantiate());
        }
        if (this.quantityPerListedTemplate <= 1) {
            return templates;
        }
        List<Object> multiplied = new ArrayList<>();
        for (int copy = 0; copy < this.quantityPerListedTemplate; copy++) {
            multiplied.addAll(templates);
        }
        return multiplied;
    }

    private boolean hasOverrideTemplates() {
        return this.overrideTemplateList != null && !this.overrideTemplateList.isEmpty();
    }

    private void warnIfMixingCustomTemplateWithOverrides() {
        if (this.templateConfig.hasCustomTemplate() && hasOverrideTemplates()) {
            System.err.println("Custom master template + overrides: overrides win all conflicts!");
        }
    }

    private RecordProviderLike resolveFactoryOutlet() {
        if (this.factoryOutlet == null) {
            this.factoryOutlet = this.providerLookup.get(resolveVariantKey());
        }
        return this.factoryOutlet;
    }

    private LookupKeyLike resolveVariantKey() {
        Object firstTemplate = hasOverrideTemplates() ? this.overrideTemplateList.get(0) : null;
        LookupKeyLike reconciled = ProviderLookups.reconcile(this.providerLookup, this.explicitVariantKey, firstTemplate);
        return reconciled != null ? reconciled : LookupKey.get(this.recordType);
    }

    private void assertNoRecordTypeConflict(List<T> overrideTemplateList) {
        if (overrideTemplateList == null) {
            return;
        }
        for (T overrideTemplate : overrideTemplateList) {
            if (overrideTemplate != null && overrideTemplate.getClass() != this.recordType) {
                throw new RecordProviderConflictException(
                        "This Provider requests " + this.recordType + " but was given a "
                        + overrideTemplate.getClass() + " override template.");
            }
        }
    }

    private static Class<?> requireTemplateType(Object overrideTemplate) {
        if (overrideTemplate == null) {
            throw new XftyConfigurationException("An override template cannot be null - use the (Class, lookup) constructor.");
        }
        return overrideTemplate.getClass();
    }

    private static Class<?> typeOf(LookupKeyLike variantKey) {
        if (variantKey == null) {
            throw new XftyConfigurationException("A lookup key is required to request data.");
        }
        return variantKey.recordType();
    }

    private static Class<?> typeOf(List<?> overrideTemplateList) {
        boolean hasAFirstTemplate = overrideTemplateList != null
                && !overrideTemplateList.isEmpty()
                && overrideTemplateList.get(0) != null;
        if (!hasAFirstTemplate) {
            throw new XftyConfigurationException(
                    "Cannot derive a record type from an empty or null template list - supply at least one "
                    + "concrete template, or use the (Class, lookup) constructor.");
        }
        return overrideTemplateList.get(0).getClass();
    }
}
