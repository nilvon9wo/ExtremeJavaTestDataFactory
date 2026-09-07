package net.nowhereatall.xfty.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.SerializableFunction;
import net.nowhereatall.xfty.XftyConfigurationException;
import net.nowhereatall.xfty.relationships.DefaultRelationshipLike;
import net.nowhereatall.xfty.values.ContextAwareExpressionLike;
import net.nowhereatall.xfty.values.LiteralExpression;
import net.nowhereatall.xfty.values.ValueExpressionLike;

/**
 * The recipe for one Provider's records: default values, context-aware values,
 * and required/optional relationships, keyed by field.
 *
 * <p>(Up-flowing "deferred" values and the typed {@code MasterTemplate<T>}
 * wrapper from the C# port are not ported yet.)
 */
public final class MasterTemplate {

    private final Field primaryTargetField;
    private final Map<Field, ValueExpressionLike> defaultByField;
    private final Map<Field, ContextAwareExpressionLike> contextAwareByField;
    private final Map<Field, DefaultRelationshipLike> requiredRelationshipByField;
    private final Map<Field, DefaultRelationshipLike> optionalRelationshipByField;
    private final List<Field> valueFieldOrder;

    public MasterTemplate(Field primaryTargetField) {
        this(primaryTargetField, new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>());
    }

    /** Start a template, naming the primary target field by {@code Type::accessor}. */
    public static <T, R> MasterTemplate of(SerializableFunction<T, R> primaryTargetField) {
        return new MasterTemplate(Field.of(primaryTargetField));
    }

    private MasterTemplate(
            Field primaryTargetField,
            Map<Field, ValueExpressionLike> defaultByField,
            Map<Field, DefaultRelationshipLike> requiredRelationshipByField,
            Map<Field, DefaultRelationshipLike> optionalRelationshipByField) {
        this.primaryTargetField = primaryTargetField;
        this.defaultByField = defaultByField;
        this.contextAwareByField = new LinkedHashMap<>();
        this.requiredRelationshipByField = requiredRelationshipByField;
        this.optionalRelationshipByField = optionalRelationshipByField;
        this.valueFieldOrder = new ArrayList<>(defaultByField.keySet());
    }

    public Field primaryTargetField() {
        return this.primaryTargetField;
    }

    public Map<Field, ValueExpressionLike> defaultByField() {
        return this.defaultByField;
    }

    public Map<Field, ContextAwareExpressionLike> contextAwareByField() {
        return this.contextAwareByField;
    }

    public Map<Field, DefaultRelationshipLike> requiredRelationshipByField() {
        return this.requiredRelationshipByField;
    }

    public Map<Field, DefaultRelationshipLike> optionalRelationshipByField() {
        return this.optionalRelationshipByField;
    }

    /** Every value field (plain + context-aware) in the order it was put. */
    public List<Field> orderedValueFields() {
        return new ArrayList<>(this.valueFieldOrder);
    }

    public MasterTemplate put(Field field, ValueExpressionLike valueTemplate) {
        trackFieldOrder(field);
        this.contextAwareByField.remove(field);
        this.defaultByField.put(field, valueTemplate);
        return this;
    }

    public MasterTemplate put(Field field, ContextAwareExpressionLike contextAwareExpression) {
        trackFieldOrder(field);
        this.defaultByField.remove(field);
        this.contextAwareByField.put(field, contextAwareExpression);
        return this;
    }

    /** Convenience overload, routed by runtime type; a relationship is rejected, anything else is a literal. */
    public MasterTemplate put(Field field, Object value) {
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
        return put(field, new LiteralExpression(value));
    }

    /** put(field, ...), naming the field by {@code Type::accessor}. */
    public <T, R> MasterTemplate put(SerializableFunction<T, R> field, Object value) {
        return put(Field.of(field), value);
    }

    public MasterTemplate putRequired(Field field, DefaultRelationshipLike relationshipTemplate) {
        this.requiredRelationshipByField.put(field, relationshipTemplate);
        return this;
    }

    public <T, R> MasterTemplate putRequired(SerializableFunction<T, R> field, DefaultRelationshipLike relationshipTemplate) {
        return putRequired(Field.of(field), relationshipTemplate);
    }

    public MasterTemplate putOptional(Field field, DefaultRelationshipLike relationshipTemplate) {
        this.optionalRelationshipByField.put(field, relationshipTemplate);
        return this;
    }

    public <T, R> MasterTemplate putOptional(SerializableFunction<T, R> field, DefaultRelationshipLike relationshipTemplate) {
        return putOptional(Field.of(field), relationshipTemplate);
    }

    /**
     * Whether this template touches {@code field} at all - a default, a
     * context-aware value, a required/optional relationship, or the primary
     * target field itself.
     */
    public boolean isConfigured(Field field) {
        return field.equals(this.primaryTargetField)
                || this.defaultByField.containsKey(field)
                || this.contextAwareByField.containsKey(field)
                || this.requiredRelationshipByField.containsKey(field)
                || this.optionalRelationshipByField.containsKey(field);
    }

    /** An independent copy - field maps recreated, expression instances shared (immutable config). */
    public MasterTemplate copy() {
        MasterTemplate theCopy = new MasterTemplate(
                this.primaryTargetField,
                new LinkedHashMap<>(this.defaultByField),
                new LinkedHashMap<>(this.requiredRelationshipByField),
                new LinkedHashMap<>(this.optionalRelationshipByField));
        theCopy.contextAwareByField.putAll(this.contextAwareByField);
        theCopy.valueFieldOrder.clear();
        theCopy.valueFieldOrder.addAll(this.valueFieldOrder);
        return theCopy;
    }

    public MasterTemplate remove(Field field) {
        this.defaultByField.remove(field);
        this.contextAwareByField.remove(field);
        this.requiredRelationshipByField.remove(field);
        this.optionalRelationshipByField.remove(field);
        this.valueFieldOrder.removeIf(each -> each.equals(field));
        return this;
    }

    private void trackFieldOrder(Field field) {
        if (!this.defaultByField.containsKey(field) && !this.contextAwareByField.containsKey(field)) {
            this.valueFieldOrder.add(field);
        }
    }
}
