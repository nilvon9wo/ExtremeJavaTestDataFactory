package net.nowhereatall.xfty.enrichment;

import java.util.List;
import java.util.stream.Collectors;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.XftyConfigurationException;
import net.nowhereatall.xfty.core.Bundle;

/**
 * Resolves the field passed to {@code bundle.inject(field, ...)} to the list of
 * records to enrich, the sub-bundle carrying their relationships, and whether
 * those records are a generated ancestor. An unknown field is a loud error
 * naming the fields the bundle actually holds.
 */
public final class EnrichmentTarget {

    private final List<Object> records;
    private final Bundle subBundle;
    private final boolean isGeneratedAncestor;

    private EnrichmentTarget(List<Object> records, Bundle subBundle, boolean isGeneratedAncestor) {
        this.records = records;
        this.subBundle = subBundle;
        this.isGeneratedAncestor = isGeneratedAncestor;
    }

    public List<Object> records() {
        return this.records;
    }

    public Bundle subBundle() {
        return this.subBundle;
    }

    public boolean isGeneratedAncestor() {
        return this.isGeneratedAncestor;
    }

    public static EnrichmentTarget locate(Bundle bundle, Field field) {
        if (field.equals(bundle.primaryTargetField())) {
            return new EnrichmentTarget(bundle.primaryRecords(), bundle, false);
        }
        if (bundle.relationshipFields().contains(field)) {
            return new EnrichmentTarget(bundle.getList(field), bundle.getBundle(field), true);
        }
        if (bundle.childRelationshipFields().contains(field)) {
            return new EnrichmentTarget(bundle.getChildList(field), bundle.getChildBundle(field), false);
        }
        throw new XftyConfigurationException("Inject: " + field.name()
                + " is not this bundle's primary field, a generated ancestor field ["
                + names(bundle.relationshipFields()) + "], or a child field ["
                + names(bundle.childRelationshipFields()) + "].");
    }

    /** True when the graph has any generated ancestor or child collection to inject. */
    public boolean hasAnythingToInject() {
        boolean hasParents = this.subBundle != null && !this.subBundle.relationshipFields().isEmpty();
        boolean hasChildren = (this.subBundle != null && !this.subBundle.childRelationshipFields().isEmpty())
                || this.isGeneratedAncestor;
        return hasParents || hasChildren;
    }

    private static String names(java.util.Set<Field> fields) {
        return fields.stream().map(Field::name).collect(Collectors.joining(", "));
    }
}
