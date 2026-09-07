package net.nowhereatall.xfty.engine;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.XftyConfigurationException;
import net.nowhereatall.xfty.core.Bundle;
import net.nowhereatall.xfty.core.GenerationContext;
import net.nowhereatall.xfty.core.InsertInclusivity;
import net.nowhereatall.xfty.core.MasterTemplate;
import net.nowhereatall.xfty.core.PathValue;
import net.nowhereatall.xfty.core.RecordProviderLike;
import net.nowhereatall.xfty.lookup.LookupKeyLike;
import net.nowhereatall.xfty.reflect.RecordShape;
import net.nowhereatall.xfty.relationships.DefaultRelationshipLike;

/** Generates the ancestor sub-bundle for each relationship the inclusivity covers. */
public final class AncestorGenerator {

    private final GenerationContext context;
    private final int quantity;
    private final MasterTemplate template;

    public AncestorGenerator(GenerationContext context, int quantity, MasterTemplate template) {
        this.context = context;
        this.quantity = quantity;
        this.template = template;
    }

    public Bundle generate() {
        Bundle bundle = new Bundle();
        Set<Field> forcedHeads = explicitlyRequestedRelationshipHeads();
        for (Field field : relationshipFields()) {
            addAncestor(bundle, field, forcedHeads.contains(field));
        }
        return bundle;
    }

    private List<Field> relationshipFields() {
        Set<Field> fields = this.context.inclusivity() == InsertInclusivity.NONE
                ? new LinkedHashSet<>()
                : requiredAndMaybeOptionalFields();
        fields.addAll(explicitlyRequestedRelationshipHeads());
        return new ArrayList<>(fields);
    }

    private Set<Field> requiredAndMaybeOptionalFields() {
        Set<Field> fields = new LinkedHashSet<>(this.template.requiredRelationshipByField().keySet());
        if (this.context.inclusivity() == InsertInclusivity.ALL) {
            fields.addAll(this.template.optionalRelationshipByField().keySet());
        }
        return fields;
    }

    private Set<Field> explicitlyRequestedRelationshipHeads() {
        Set<Field> heads = new LinkedHashSet<>();
        for (List<Field> path : this.context.forcedRelationshipPaths()) {
            if (!path.isEmpty() && isRelationshipHere(path.get(0))) {
                heads.add(path.get(0));
            }
        }
        for (PathValue pathValue : this.context.pathValues()) {
            if (isRelationshipHere(pathValue.head())
                    && (!pathValue.isAtTarget() || pathValue.isRelationshipKind())) {
                heads.add(pathValue.head());
            }
        }
        return heads;
    }

    private boolean isRelationshipHere(Field field) {
        return this.template.requiredRelationshipByField().containsKey(field)
                || this.template.optionalRelationshipByField().containsKey(field);
    }

    private void addAncestor(Bundle bundle, Field field, boolean isForced) {
        DefaultRelationshipLike relationship = relationshipOn(field);
        generateAncestor(bundle, field, relationship, isForced);
    }

    private void generateAncestor(Bundle bundle, Field field, DefaultRelationshipLike relationship, boolean isForced) {
        LookupKeyLike childKey = relationship.resolveLookupKey(this.context.providerLookup());
        assertNoAncestorCycle(field, childKey);
        RecordProviderLike provider = this.context.providerLookup().get(childKey);
        GenerationContext childContext = forcedChildContext(this.context.forRelated(field), isForced)
                .enteringProviderFor(childKey.hashKey());
        List<Object> templates = clonedTemplatesFor(relationship, this.quantity);
        Bundle generated = provider.createBundle(childContext, templates);
        List<Object> primaries = generated.getList(provider.primaryTargetField());
        bundle.put(field, generated);
        bundle.put(field, primaries);
    }

    /**
     * An explicitly forced ancestor is generated fully formed - its own required
     * relationships fill in - even when the surrounding call asked for NONE.
     */
    private GenerationContext forcedChildContext(GenerationContext childContext, boolean isForced) {
        boolean bumpNeeded = isForced && childContext.inclusivity() == InsertInclusivity.NONE;
        return bumpNeeded ? childContext.withInclusivity(InsertInclusivity.REQUIRED) : childContext;
    }

    private void assertNoAncestorCycle(Field field, LookupKeyLike childKey) {
        if (!this.context.cycleGuard().wouldCycleOn(childKey.hashKey())) {
            return;
        }
        throw new XftyConfigurationException(
                "Relationship " + field.name() + " would generate another " + childKey.recordType()
                + ", but one is already being generated further up this graph - a cycle. Use distinct per-level "
                + "Providers (different lookup keys), PREVENT_CASCADE, or allow ancestor cycles when the chain "
                + "terminates on its own.");
    }

    private static List<Object> clonedTemplatesFor(DefaultRelationshipLike relationship, int quantity) {
        Object overrideTemplate = relationship.overrideTemplate();
        return RecordShape.of(overrideTemplate.getClass()).copies(overrideTemplate, quantity);
    }

    private DefaultRelationshipLike relationshipOn(Field field) {
        DefaultRelationshipLike required = this.template.requiredRelationshipByField().get(field);
        return required != null ? required : this.template.optionalRelationshipByField().get(field);
    }
}
