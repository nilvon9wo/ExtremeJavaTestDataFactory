package net.nowhereatall.xfty.core;

import java.util.ArrayList;
import java.util.List;

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
 * The primary entry point: configure a record's fields and relationships, then
 * {@link #supply()} / {@link #supplyList()} / {@link #supplyBundle()} it.
 *
 * <p>Children, shared ancestors, deferred/depth-batched inserts and the typed
 * {@code MasterTemplate<T>} wrapper from the C# port are not ported yet.
 */
public final class RecordProvider {

    private final Class<?> recordType;
    private final ProviderLookupLike providerLookup;
    private final RecordProviderTemplateConfig templateConfig;

    private List<Object> overrideTemplateList;
    private LookupKeyLike explicitVariantKey;
    private int quantityPerListedTemplate = 1;
    private InsertMode insertMode = InsertMode.NEVER;
    private InsertInclusivity inclusivity = InsertInclusivity.NONE;
    private boolean ancestorCyclesAllowed;
    private boolean excludePrimaryIds;
    private PersistenceGatewayLike persistenceGateway;
    private UnsetFieldFillerLike unsetFieldFiller;
    private RecordProviderLike factoryOutlet;

    public RecordProvider(Class<?> recordType, ProviderLookupLike providerLookup) {
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

    public RecordProvider(LookupKeyLike variantKey, ProviderLookupLike providerLookup) {
        this(typeOf(variantKey), providerLookup);
        this.explicitVariantKey = variantKey;
    }

    public RecordProvider(Object overrideTemplate, ProviderLookupLike providerLookup) {
        this(new ArrayList<>(List.of(overrideTemplate)), providerLookup);
    }

    public RecordProvider(List<Object> overrideTemplateList, ProviderLookupLike providerLookup) {
        this(typeOf(overrideTemplateList), providerLookup);
        setOverrideTemplateList(overrideTemplateList);
    }

    // Configuration -----------------------------------------------------

    public RecordProvider setQuantityPerTemplate(int quantityPerListedTemplate) {
        if (quantityPerListedTemplate < 1) {
            throw new XftyConfigurationException("It makes no sense to supply " + quantityPerListedTemplate + ".");
        }
        this.quantityPerListedTemplate = quantityPerListedTemplate;
        return this;
    }

    public RecordProvider setOverrideTemplateList(List<Object> overrideTemplateList) {
        assertNoRecordTypeConflict(overrideTemplateList);
        this.overrideTemplateList = overrideTemplateList;
        return this;
    }

    public RecordProvider setOverrideTemplate(Object overrideTemplate) {
        return setOverrideTemplateList(new ArrayList<>(List.of(overrideTemplate)));
    }

    public RecordProvider withVariant(LookupKeyLike variantKey) {
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

    public RecordProvider setInsertMode(InsertMode insertMode) {
        this.insertMode = insertMode;
        return this;
    }

    public RecordProvider setInclusivity(InsertInclusivity inclusivity) {
        this.inclusivity = inclusivity;
        return this;
    }

    public RecordProvider setPersistenceGateway(PersistenceGatewayLike gateway) {
        this.persistenceGateway = gateway;
        return this;
    }

    public RecordProvider setUnsetFieldFiller(UnsetFieldFillerLike filler) {
        this.unsetFieldFiller = filler;
        return this;
    }

    public RecordProvider allowAncestorCycles() {
        this.ancestorCyclesAllowed = true;
        return this;
    }

    public RecordProvider excludePrimaryIds() {
        this.excludePrimaryIds = true;
        return this;
    }

    public RecordProvider includePrimaryIds() {
        this.excludePrimaryIds = false;
        return this;
    }

    // Field / relationship configuration -------------------------------

    public RecordProvider put(Field field, Object value) {
        this.templateConfig.put(field, value);
        return this;
    }

    public <T, R> RecordProvider put(SerializableFunction<T, R> field, Object value) {
        return put(Field.of(field), value);
    }

    public RecordProvider putRequired(Field field, DefaultRelationshipLike relationship) {
        this.templateConfig.putRequired(field, relationship);
        return this;
    }

    public <T, R> RecordProvider putRequired(SerializableFunction<T, R> field, DefaultRelationshipLike relationship) {
        return putRequired(Field.of(field), relationship);
    }

    public RecordProvider putOptional(Field field, DefaultRelationshipLike relationship) {
        this.templateConfig.putOptional(field, relationship);
        return this;
    }

    public <T, R> RecordProvider putOptional(SerializableFunction<T, R> field, DefaultRelationshipLike relationship) {
        return putOptional(Field.of(field), relationship);
    }

    public RecordProvider removeFromMasterTemplate(Field field) {
        this.templateConfig.removeFromMasterTemplate(field);
        return this;
    }

    public RecordProvider includeOptional(Field field) {
        return includeOptional(new ArrayList<>(List.of(field)));
    }

    public RecordProvider includeOptional(List<Field> relationshipPath) {
        this.templateConfig.includeOptional(relationshipPath);
        return this;
    }

    public <T, R> RecordProvider includeOptional(SerializableFunction<T, R> field) {
        return includeOptional(Field.of(field));
    }

    public RecordProvider excludeRelationship(Field field) {
        this.templateConfig.excludeRelationship(field, this.recordType);
        return this;
    }

    public <T, R> RecordProvider excludeRelationship(SerializableFunction<T, R> field) {
        return excludeRelationship(Field.of(field));
    }

    public RecordProvider excludeRelationshipIfPresent(Field field) {
        this.templateConfig.excludeRelationshipIfPresent(field);
        return this;
    }

    // Path-scoped value overrides -------------------------------------

    public RecordProvider put(List<Field> path, ValueExpressionLike valueExpression) {
        this.templateConfig.addPathValue(PathValue.ofExpression(path, valueExpression));
        return this;
    }

    public RecordProvider putContextAware(List<Field> path, ContextAwareExpressionLike contextAwareExpression) {
        this.templateConfig.addPathValue(PathValue.ofContextAware(path, contextAwareExpression));
        return this;
    }

    public RecordProvider putLiteral(List<Field> path, Object literal) {
        this.templateConfig.addPathValue(PathValue.ofLiteral(path, literal));
        return this;
    }

    public RecordProvider putRequired(List<Field> path, DefaultRelationshipLike relationship) {
        this.templateConfig.addPathValue(PathValue.ofRequiredRelationship(path, relationship));
        return this;
    }

    public RecordProvider putOptional(List<Field> path, DefaultRelationshipLike relationship) {
        this.templateConfig.addPathValue(PathValue.ofOptionalRelationship(path, relationship));
        return this;
    }

    // Supply ----------------------------------------------------------

    public Bundle supplyBundle() {
        warnIfMixingCustomTemplateWithOverrides();
        GenerationContext context = buildContext();
        List<Object> templates = templatesToFill();
        return generate(context, templates);
    }

    public List<Object> supplyList() {
        return supplyBundle().getList(resolveFactoryOutlet().primaryTargetField());
    }

    public Object supply() {
        return supplyList().get(0);
    }

    private Bundle generate(GenerationContext context, List<Object> templates) {
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
        List<Object> templates = hasOverrideTemplates()
                ? this.overrideTemplateList
                : new ArrayList<>(List.of(RecordShape.of(this.recordType).blank()));
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

    private void assertNoRecordTypeConflict(List<Object> overrideTemplateList) {
        if (overrideTemplateList == null) {
            return;
        }
        for (Object overrideTemplate : overrideTemplateList) {
            if (overrideTemplate != null && overrideTemplate.getClass() != this.recordType) {
                throw new RecordProviderConflictException(
                        "This Provider requests " + this.recordType + " but was given a "
                        + overrideTemplate.getClass() + " override template.");
            }
        }
    }

    private static Class<?> typeOf(LookupKeyLike variantKey) {
        if (variantKey == null) {
            throw new XftyConfigurationException("A lookup key is required to request data.");
        }
        return variantKey.recordType();
    }

    private static Class<?> typeOf(List<Object> overrideTemplateList) {
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
