package net.nowhereatall.xfty.core;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.relationships.DefaultRelationshipLike;
import net.nowhereatall.xfty.values.ContextAwareExpressionLike;
import net.nowhereatall.xfty.values.ValueExpressionLike;

/**
 * The value half of a {@link PathValue} - what lands on the field a
 * {@code put(path, ...)} targets, in one of five kinds.
 */
public final class PathTargetValue {

    private final PathTargetValueKind valueKind;
    private final Object payload;

    private PathTargetValue(PathTargetValueKind kind, Object payload) {
        this.valueKind = kind;
        this.payload = payload;
    }

    public static PathTargetValue ofExpression(ValueExpressionLike expression) {
        return new PathTargetValue(PathTargetValueKind.VALUE_EXPRESSION, expression);
    }

    public static PathTargetValue ofContextAware(ContextAwareExpressionLike contextAware) {
        return new PathTargetValue(PathTargetValueKind.CONTEXT_AWARE, contextAware);
    }

    public static PathTargetValue ofLiteral(Object literal) {
        return new PathTargetValue(PathTargetValueKind.LITERAL, literal);
    }

    public static PathTargetValue ofRequiredRelationship(DefaultRelationshipLike relationship) {
        return new PathTargetValue(PathTargetValueKind.REQUIRED_RELATIONSHIP, relationship);
    }

    public static PathTargetValue ofOptionalRelationship(DefaultRelationshipLike relationship) {
        return new PathTargetValue(PathTargetValueKind.OPTIONAL_RELATIONSHIP, relationship);
    }

    public PathTargetValueKind valueKind() {
        return this.valueKind;
    }

    public boolean isRelationship() {
        return this.valueKind == PathTargetValueKind.REQUIRED_RELATIONSHIP
                || this.valueKind == PathTargetValueKind.OPTIONAL_RELATIONSHIP;
    }

    /** True when the value is a shared ancestor. */
    public boolean isSharedRelationship() {
        return this.payload instanceof net.nowhereatall.xfty.relationships.SharedRelationshipLike;
    }

    /** Land the value on {@code template.targetField} for the ancestor level being generated. */
    public void applyTo(MasterTemplate template, Field targetField) {
        template.remove(targetField);
        switch (this.valueKind) {
            case VALUE_EXPRESSION -> template.put(targetField, (ValueExpressionLike) this.payload);
            case CONTEXT_AWARE -> template.put(targetField, (ContextAwareExpressionLike) this.payload);
            case LITERAL -> template.put(targetField, this.payload);
            case REQUIRED_RELATIONSHIP -> template.putRequired(targetField, (DefaultRelationshipLike) this.payload);
            case OPTIONAL_RELATIONSHIP -> template.putOptional(targetField, (DefaultRelationshipLike) this.payload);
        }
    }
}
