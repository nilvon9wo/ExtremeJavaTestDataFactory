package net.nowhereatall.xfty.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.XftyConfigurationException;
import net.nowhereatall.xfty.relationships.DefaultRelationshipLike;

/**
 * The field/relationship configuration one {@link RecordProvider} call
 * accumulates: direct {@code put}/{@code putRequired}/{@code putOptional} on its
 * own Master Template, per-call relationship overrides ({@code includeOptional},
 * {@code excludeRelationship}), and path-scoped value overrides on a generated
 * ancestor.
 */
final class RecordProviderTemplateConfig {

    private final Supplier<MasterTemplate> resolveBaseTemplate;
    private final List<List<Field>> forcedRelationshipPaths = new ArrayList<>();
    private final List<PathValue> pathValues = new ArrayList<>();

    private MasterTemplate ownTemplate;
    private boolean hasCustomTemplate;

    RecordProviderTemplateConfig(Supplier<MasterTemplate> resolveBaseTemplate) {
        this.resolveBaseTemplate = resolveBaseTemplate;
    }

    boolean hasCustomTemplate() {
        return this.hasCustomTemplate;
    }

    List<List<Field>> forcedRelationshipPaths() {
        return this.forcedRelationshipPaths;
    }

    List<PathValue> pathValues() {
        return this.pathValues;
    }

    MasterTemplate resolveTemplate() {
        if (this.ownTemplate == null) {
            this.ownTemplate = this.resolveBaseTemplate.get();
        }
        return this.ownTemplate;
    }

    void put(Field field, Object value) {
        mutate(() -> resolveTemplate().put(field, value));
    }

    void putRequired(Field field, DefaultRelationshipLike relationship) {
        mutate(() -> resolveTemplate().remove(field).putRequired(field, relationship));
    }

    void putOptional(Field field, DefaultRelationshipLike relationship) {
        mutate(() -> resolveTemplate().remove(field).putOptional(field, relationship));
    }

    void removeFromMasterTemplate(Field field) {
        mutate(() -> resolveTemplate().remove(field));
    }

    void includeOptional(List<Field> relationshipPath) {
        boolean valid = relationshipPath != null && !relationshipPath.isEmpty()
                && relationshipPath.stream().allMatch(step -> step != null);
        if (!valid) {
            throw new XftyConfigurationException("includeOptional(...) needs at least one non-null relationship field.");
        }
        this.forcedRelationshipPaths.add(relationshipPath);
    }

    void excludeRelationship(Field field, Class<?> recordType) {
        if (!isRelationshipOnTemplate(field)) {
            throw new XftyConfigurationException(
                    "excludeRelationship(" + field.name() + "): " + recordType.getSimpleName()
                    + " has no relationship on that field.");
        }
        mutate(() -> resolveTemplate().remove(field));
    }

    void excludeRelationshipIfPresent(Field field) {
        if (isRelationshipOnTemplate(field)) {
            mutate(() -> resolveTemplate().remove(field));
        }
    }

    void addPathValue(PathValue pathValue) {
        this.pathValues.add(pathValue);
    }

    private boolean isRelationshipOnTemplate(Field field) {
        MasterTemplate current = resolveTemplate();
        return current.requiredRelationshipByField().containsKey(field)
                || current.optionalRelationshipByField().containsKey(field);
    }

    private void mutate(Runnable mutation) {
        mutation.run();
        this.hasCustomTemplate = true;
    }
}
