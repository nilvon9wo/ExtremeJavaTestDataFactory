package net.nowhereatall.xfty.core;

import java.util.ArrayList;
import java.util.List;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.SerializableFunction;
import net.nowhereatall.xfty.XftyConfigurationException;
import net.nowhereatall.xfty.lookup.LookupKeyLike;
import net.nowhereatall.xfty.lookup.ProviderLookupLike;
import net.nowhereatall.xfty.reflect.RecordShape;
import net.nowhereatall.xfty.relationships.DefaultRelationshipLike;
import net.nowhereatall.xfty.values.ContextAwareExpressionLike;
import net.nowhereatall.xfty.values.ValueExpressionLike;

/**
 * Configures one <b>child collection</b> hung off a {@link RecordProvider} - the
 * downward mirror of the framework's usual upward generation. The child type is
 * taken from the relationship field's declaring type.
 *
 * <p>A plain field carries no "what type does this foreign key reference"
 * metadata, so a misconfigured relationship field surfaces as a wrong/null
 * value rather than failing fast.
 */
public final class ChildProvider {

    private final Field relationshipField;
    private final Class<?> childType;
    private final Object template;
    private final List<ChildProviderPendingPut> pendingPuts = new ArrayList<>();
    private final List<ChildProvider> grandchildProviders = new ArrayList<>();

    private int quantity = 1;
    private InsertMode insertModeOverride;
    private InsertInclusivity inclusivityOverride;
    private LookupKeyLike variantKey;

    public ChildProvider(Field relationshipField) {
        this(relationshipField, null);
    }

    public ChildProvider(Field relationshipField, Object template) {
        if (relationshipField == null) {
            throw new XftyConfigurationException("ChildProvider needs the child relationship field.");
        }
        this.relationshipField = relationshipField;
        this.childType = relationshipField.recordType();
        if (template != null && template.getClass() != this.childType) {
            throw new XftyConfigurationException(
                    "Template is a " + template.getClass() + " but " + relationshipField.name() + " is on " + this.childType + ".");
        }
        this.template = template != null ? template : RecordShape.of(this.childType).instantiate();
    }

    public static <TChild, R> ChildProvider forField(SerializableFunction<TChild, R> relationshipField) {
        return new ChildProvider(Field.of(relationshipField));
    }

    public static <TChild, R> ChildProvider forField(SerializableFunction<TChild, R> relationshipField, TChild template) {
        return new ChildProvider(Field.of(relationshipField), template);
    }

    public Field relationshipField() {
        return this.relationshipField;
    }

    public Class<?> childType() {
        return this.childType;
    }

    // Fluent config -----------------------------------------------------

    public ChildProvider setQuantity(int quantity) {
        if (quantity < 1) {
            throw new XftyConfigurationException("setQuantity(" + quantity + "): at least 1.");
        }
        this.quantity = quantity;
        return this;
    }

    public ChildProvider put(Field field, ValueExpressionLike valueExpression) {
        return addPendingPut(ChildProviderPendingPut.ofValue(field, valueExpression));
    }

    public ChildProvider put(Field field, ContextAwareExpressionLike contextAwareExpression) {
        return addPendingPut(ChildProviderPendingPut.ofContextAware(field, contextAwareExpression));
    }

    public ChildProvider put(Field field, Object value) {
        if (value instanceof ContextAwareExpressionLike contextAware) {
            return put(field, contextAware);
        }
        if (value instanceof ValueExpressionLike valueExpression) {
            return put(field, valueExpression);
        }
        if (value instanceof DefaultRelationshipLike) {
            throw new XftyConfigurationException(
                    "Relationships must be added with putRequired(...) or putOptional(...), not put(...).");
        }
        return addPendingPut(ChildProviderPendingPut.ofLiteral(field, value));
    }

    public <TChild, R> ChildProvider put(SerializableFunction<TChild, R> field, Object value) {
        return put(Field.of(field), value);
    }

    public ChildProvider putRequired(Field field, DefaultRelationshipLike relationship) {
        return addPendingPut(ChildProviderPendingPut.ofRequiredRelationship(field, relationship));
    }

    public <TChild, R> ChildProvider putRequired(SerializableFunction<TChild, R> field, DefaultRelationshipLike relationship) {
        return putRequired(Field.of(field), relationship);
    }

    public ChildProvider putOptional(Field field, DefaultRelationshipLike relationship) {
        return addPendingPut(ChildProviderPendingPut.ofOptionalRelationship(field, relationship));
    }

    public <TChild, R> ChildProvider putOptional(SerializableFunction<TChild, R> field, DefaultRelationshipLike relationship) {
        return putOptional(Field.of(field), relationship);
    }

    public ChildProvider setInsertMode(InsertMode insertMode) {
        this.insertModeOverride = insertMode;
        return this;
    }

    public ChildProvider setInclusivity(InsertInclusivity inclusivity) {
        this.inclusivityOverride = inclusivity;
        return this;
    }

    public ChildProvider withVariant(LookupKeyLike variantKey) {
        this.variantKey = variantKey;
        return this;
    }

    /** Nest a further child collection under these children - grandchildren, and so on. */
    public ChildProvider with(ChildProvider grandchildProvider) {
        if (grandchildProvider == null) {
            throw new XftyConfigurationException("with(...) needs a ChildProvider.");
        }
        this.grandchildProviders.add(grandchildProvider);
        return this;
    }

    // Used by RecordProviderChildConfig -------------------------------

    public InsertMode effectiveInsertMode(InsertMode parentMode) {
        InsertMode mode = this.insertModeOverride != null ? this.insertModeOverride : parentMode;
        boolean mixesMockWithReal = (parentMode == InsertMode.MOCK && mode == InsertMode.NOW)
                || (parentMode == InsertMode.NOW && mode == InsertMode.MOCK);
        if (mixesMockWithReal) {
            throw new XftyConfigurationException(
                    "A child collection cannot mix mock ids with real DML - parent is " + parentMode + ", child is " + mode + ".");
        }
        return mode;
    }

    public InsertInclusivity effectiveInclusivity(InsertInclusivity parentInclusivity) {
        return this.inclusivityOverride != null ? this.inclusivityOverride : parentInclusivity;
    }

    /** Build the {@code quantity} child templates for one primary, with the back-reference set. */
    public List<Object> templatesForParent(Object parentId) {
        RecordShape shape = RecordShape.of(this.childType);
        List<Object> templates = new ArrayList<>(this.quantity);
        for (int index = 0; index < this.quantity; index++) {
            templates.add(shape.set(shape.copy(this.template), this.relationshipField, parentId));
        }
        return templates;
    }

    /** A fresh Provider for these children, with this child provider's puts/variant/nested children applied. */
    @SuppressWarnings("unchecked")
    public RecordProvider<Object> newProvider(ProviderLookupLike lookup) {
        RecordProvider<Object> provider = this.variantKey != null
                ? new RecordProvider<>(this.variantKey, lookup)
                : new RecordProvider<>((Class<Object>) this.childType, lookup);
        this.pendingPuts.forEach(pendingPut -> pendingPut.applyTo(provider));
        this.grandchildProviders.forEach(provider::with);
        return provider;
    }

    private ChildProvider addPendingPut(ChildProviderPendingPut pendingPut) {
        this.pendingPuts.add(pendingPut);
        return this;
    }
}
