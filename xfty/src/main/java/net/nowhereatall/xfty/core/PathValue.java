package net.nowhereatall.xfty.core;

import java.util.ArrayList;
import java.util.List;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.XftyConfigurationException;
import net.nowhereatall.xfty.relationships.DefaultRelationshipLike;
import net.nowhereatall.xfty.values.ContextAwareExpressionLike;
import net.nowhereatall.xfty.values.ValueExpressionLike;

/**
 * One {@code put(List<Field> path, value)} on a Provider - a value targeted at a
 * field on a <b>generated ancestor</b>, reached by walking a path of
 * relationship fields, for this call only.
 *
 * <p>{@code path} is {@code [rel1, rel2, ..., targetField]}: every element but
 * the last is a relationship that is forced generated, and the last is the field
 * the value lands on.
 */
public final class PathValue {

    private final List<Field> path;
    private final PathTargetValue value;

    private PathValue(List<Field> path, PathTargetValue value) {
        assertPath(path);
        this.path = path;
        this.value = value;
    }

    public static PathValue ofExpression(List<Field> path, ValueExpressionLike expression) {
        assertUsablePath(path);
        return new PathValue(path, PathTargetValue.ofExpression(expression));
    }

    public static PathValue ofContextAware(List<Field> path, ContextAwareExpressionLike contextAware) {
        assertUsablePath(path);
        return new PathValue(path, PathTargetValue.ofContextAware(contextAware));
    }

    public static PathValue ofLiteral(List<Field> path, Object literal) {
        assertUsablePath(path);
        return new PathValue(path, PathTargetValue.ofLiteral(literal));
    }

    public static PathValue ofRequiredRelationship(List<Field> path, DefaultRelationshipLike relationship) {
        assertUsablePath(path);
        return new PathValue(path, PathTargetValue.ofRequiredRelationship(relationship));
    }

    public static PathValue ofOptionalRelationship(List<Field> path, DefaultRelationshipLike relationship) {
        assertUsablePath(path);
        return new PathValue(path, PathTargetValue.ofOptionalRelationship(relationship));
    }

    public List<Field> path() {
        return this.path;
    }

    public PathTargetValue value() {
        return this.value;
    }

    /** The relationship fields that must be forced generated to reach the target (path minus the target). */
    public List<Field> relationshipPrefix() {
        return new ArrayList<>(this.path.subList(0, this.path.size() - 1));
    }

    public Field head() {
        return this.path.get(0);
    }

    /** True when only the target field is left - this ancestor level is where the value lands. */
    public boolean isAtTarget() {
        return this.path.size() == 1;
    }

    public boolean isRelationshipKind() {
        return this.value.isRelationship();
    }

    public boolean isSharedRelationshipValue() {
        return this.value.isSharedRelationship();
    }

    /** The same value, one relationship deeper (head dropped). Only valid when not {@link #isAtTarget()}. */
    public PathValue tail() {
        return new PathValue(new ArrayList<>(this.path.subList(1, this.path.size())), this.value);
    }

    /** Land the value on {@code template} (call only when {@link #isAtTarget()}). */
    public void applyTo(MasterTemplate template) {
        this.value.applyTo(template, this.path.get(0));
    }

    private static void assertPath(List<Field> path) {
        if (path == null || path.isEmpty()) {
            throw new XftyConfigurationException("A path value cannot have an empty path.");
        }
        if (path.stream().anyMatch(step -> step == null)) {
            throw new XftyConfigurationException("A path value cannot contain a null field.");
        }
    }

    private static void assertUsablePath(List<Field> path) {
        if (path == null || path.size() < 2) {
            throw new XftyConfigurationException(
                    "A path value needs at least one relationship field plus the target field - use plain "
                    + "put(field, value) for a field on the record itself.");
        }
    }
}
