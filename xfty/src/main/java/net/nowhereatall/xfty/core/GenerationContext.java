package net.nowhereatall.xfty.core;

import java.util.ArrayList;
import java.util.List;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.XftyConfigurationException;
import net.nowhereatall.xfty.engine.AncestorCycleGuard;
import net.nowhereatall.xfty.engine.ValueFieldPass;
import net.nowhereatall.xfty.lookup.ProviderLookupLike;
import net.nowhereatall.xfty.persistence.PersistenceGatewayLike;

/**
 * The state every step of a single generation run needs to see: which Provider
 * Lookup to resolve variants through, whether (and when) to insert, how far to
 * follow relationships, and - during the context-aware value pass - the record
 * currently being built and the graph generated so far.
 *
 * <p>Immutable. Derive a new one with the {@code with*} / {@code for*} methods
 * rather than mutating.
 */
public final class GenerationContext {

    private final ProviderLookupLike providerLookup;
    private final InsertMode insertMode;
    private final InsertInclusivity inclusivity;
    private final PersistenceGatewayLike persistenceGateway;
    private final UnsetFieldFillerLike unsetFieldFiller;
    private final Object recordBeingBuilt;
    private final Bundle bundleSoFar;
    private final int rowIndex;
    private final List<List<Field>> forcedRelationshipPaths;
    private final List<PathValue> pathValues;
    private final boolean batchedInsertPending;
    private final boolean excludePrimaryIds;
    private final ValueFieldPass valueFieldPass;
    private final AncestorCycleGuard cycleGuard;

    public GenerationContext(ProviderLookupLike providerLookup, InsertMode insertMode, InsertInclusivity inclusivity) {
        this(providerLookup, insertMode, inclusivity, null, null, null, null, -1,
                new ArrayList<>(), new ArrayList<>(), false, false, null, new AncestorCycleGuard(false));
    }

    private GenerationContext(
            ProviderLookupLike providerLookup, InsertMode insertMode, InsertInclusivity inclusivity,
            PersistenceGatewayLike persistenceGateway, UnsetFieldFillerLike unsetFieldFiller,
            Object recordBeingBuilt, Bundle bundleSoFar, int rowIndex,
            List<List<Field>> forcedRelationshipPaths, List<PathValue> pathValues,
            boolean batchedInsertPending, boolean excludePrimaryIds,
            ValueFieldPass valueFieldPass, AncestorCycleGuard cycleGuard) {
        if (providerLookup == null) {
            throw new XftyConfigurationException("A generation context requires a Provider Lookup.");
        }
        this.providerLookup = providerLookup;
        this.insertMode = insertMode == null ? InsertMode.NEVER : insertMode;
        this.inclusivity = inclusivity == null ? InsertInclusivity.NONE : inclusivity;
        this.persistenceGateway = persistenceGateway;
        this.unsetFieldFiller = unsetFieldFiller;
        this.recordBeingBuilt = recordBeingBuilt;
        this.bundleSoFar = bundleSoFar;
        this.rowIndex = rowIndex;
        this.forcedRelationshipPaths = forcedRelationshipPaths == null ? new ArrayList<>() : forcedRelationshipPaths;
        this.pathValues = pathValues == null ? new ArrayList<>() : pathValues;
        this.batchedInsertPending = batchedInsertPending;
        this.excludePrimaryIds = excludePrimaryIds;
        this.valueFieldPass = valueFieldPass;
        this.cycleGuard = cycleGuard;
    }

    public ProviderLookupLike providerLookup() {
        return this.providerLookup;
    }

    public InsertMode insertMode() {
        return this.insertMode;
    }

    public InsertInclusivity inclusivity() {
        return this.inclusivity;
    }

    public PersistenceGatewayLike persistenceGateway() {
        return this.persistenceGateway;
    }

    public UnsetFieldFillerLike unsetFieldFiller() {
        return this.unsetFieldFiller;
    }

    public Object recordBeingBuilt() {
        return this.recordBeingBuilt;
    }

    public Bundle bundleSoFar() {
        return this.bundleSoFar;
    }

    public int rowIndex() {
        return this.rowIndex;
    }

    public List<List<Field>> forcedRelationshipPaths() {
        return this.forcedRelationshipPaths;
    }

    public List<PathValue> pathValues() {
        return this.pathValues;
    }

    public boolean batchedInsertPending() {
        return this.batchedInsertPending;
    }

    public boolean excludePrimaryIds() {
        return this.excludePrimaryIds;
    }

    public ValueFieldPass valueFieldPass() {
        return this.valueFieldPass;
    }

    public AncestorCycleGuard cycleGuard() {
        return this.cycleGuard;
    }

    // Copy-with -----------------------------------------------------------

    public GenerationContext withPersistenceGateway(PersistenceGatewayLike gateway) {
        return copy().persistenceGateway(gateway).done();
    }

    public GenerationContext withUnsetFieldFiller(UnsetFieldFillerLike filler) {
        return copy().unsetFieldFiller(filler).done();
    }

    public GenerationContext withForcedRelationshipPaths(List<List<Field>> paths) {
        return copy().forcedRelationshipPaths(paths == null ? new ArrayList<>() : paths).done();
    }

    public GenerationContext withInclusivity(InsertInclusivity newInclusivity) {
        return copy().inclusivity(newInclusivity).done();
    }

    public GenerationContext withPathValues(List<PathValue> pathValues) {
        List<List<Field>> forced = new ArrayList<>(this.forcedRelationshipPaths);
        for (PathValue pathValue : pathValues) {
            forced.add(pathValue.relationshipPrefix());
        }
        return copy().forcedRelationshipPaths(forced).pathValues(pathValues).done();
    }

    public GenerationContext withAncestorCycleGuard(boolean cyclesAllowed) {
        return copy().cycleGuard(new AncestorCycleGuard(cyclesAllowed)).done();
    }

    public GenerationContext withPrimaryIdsExcluded(boolean excluded) {
        return copy().excludePrimaryIds(excluded).done();
    }

    public GenerationContext enteringProviderFor(String providerKeyHash) {
        return copy().cycleGuard(this.cycleGuard.descendingInto(providerKeyHash)).done();
    }

    public GenerationContext forBatchedInsert() {
        return copy().batchedInsertPending(true).done();
    }

    /** The context for generating one level of related (ancestor) records. */
    public GenerationContext forRelated() {
        return forRelated(null);
    }

    /** As {@link #forRelated()}, but for the child on {@code relationshipField}: only forced paths starting with it are carried, head dropped. */
    public GenerationContext forRelated(Field relationshipField) {
        List<List<Field>> childPaths = new ArrayList<>();
        for (List<Field> path : this.forcedRelationshipPaths) {
            if (relationshipField != null && path.size() > 1 && path.get(0).equals(relationshipField)) {
                childPaths.add(new ArrayList<>(path.subList(1, path.size())));
            }
        }
        List<PathValue> childPathValues = new ArrayList<>();
        for (PathValue pathValue : this.pathValues) {
            if (relationshipField != null && !pathValue.isAtTarget() && pathValue.head().equals(relationshipField)) {
                childPathValues.add(pathValue.tail());
            }
        }
        InsertInclusivity relatedInclusivity =
                this.inclusivity == InsertInclusivity.PREVENT_CASCADE ? InsertInclusivity.NONE : this.inclusivity;
        return new GenerationContext(
                this.providerLookup, this.insertMode, relatedInclusivity, this.persistenceGateway, this.unsetFieldFiller,
                null, null, -1, childPaths, childPathValues, this.batchedInsertPending, false, null, this.cycleGuard);
    }

    /** The context for evaluating a context-aware value on {@code record} (row {@code rowIndex}). */
    public GenerationContext forRecord(Object record, Bundle bundleSoFar, int rowIndex) {
        return new GenerationContext(
                this.providerLookup, this.insertMode, this.inclusivity, this.persistenceGateway, this.unsetFieldFiller,
                record, bundleSoFar, rowIndex, this.forcedRelationshipPaths, this.pathValues, this.batchedInsertPending,
                this.excludePrimaryIds, null, this.cycleGuard);
    }

    /** As {@link #forRecord}, narrowed to the one context-aware value field being generated now. */
    public GenerationContext forValueField(Field fieldBeingBuilt, java.util.Collection<Field> pendingContextAwareValues) {
        return new GenerationContext(
                this.providerLookup, this.insertMode, this.inclusivity, this.persistenceGateway, this.unsetFieldFiller,
                this.recordBeingBuilt, this.bundleSoFar, this.rowIndex, this.forcedRelationshipPaths, this.pathValues,
                this.batchedInsertPending, this.excludePrimaryIds,
                new ValueFieldPass(fieldBeingBuilt, pendingContextAwareValues), this.cycleGuard);
    }

    /**
     * The final value of a sibling field on {@link #recordBeingBuilt()}, for a
     * context-aware expression. A returned null means the sibling was genuinely
     * generated to null. Throws when {@code siblingField} is itself a
     * context-aware value that has not been generated yet.
     */
    public Object siblingValue(Field siblingField) {
        if (this.valueFieldPass == null) {
            throw new XftyConfigurationException(
                    "siblingValue(" + siblingField.name() + ") can only be read while a context-aware value is being generated.");
        }
        if (this.valueFieldPass.pendingContextAwareValues().contains(siblingField)) {
            throw new XftyConfigurationException(
                    "The context-aware value for " + this.valueFieldPass.fieldBeingBuilt().name() + " reads sibling field "
                    + siblingField.name() + ", which is itself a context-aware value that has not been generated yet. "
                    + "Context-aware values are generated in the order they are put, so .put(" + siblingField.name()
                    + ", ...) must come before .put(" + this.valueFieldPass.fieldBeingBuilt().name() + ", ...).");
        }
        return this.recordBeingBuilt == null ? null : siblingField.get(this.recordBeingBuilt);
    }

    // A tiny mutable copy holder to keep the with* methods to one line each.
    private Copy copy() {
        return new Copy(this);
    }

    private static final class Copy {
        private ProviderLookupLike providerLookup;
        private InsertMode insertMode;
        private InsertInclusivity inclusivity;
        private PersistenceGatewayLike persistenceGateway;
        private UnsetFieldFillerLike unsetFieldFiller;
        private Object recordBeingBuilt;
        private Bundle bundleSoFar;
        private int rowIndex;
        private List<List<Field>> forcedRelationshipPaths;
        private List<PathValue> pathValues;
        private boolean batchedInsertPending;
        private boolean excludePrimaryIds;
        private ValueFieldPass valueFieldPass;
        private AncestorCycleGuard cycleGuard;

        private Copy(GenerationContext from) {
            this.providerLookup = from.providerLookup;
            this.insertMode = from.insertMode;
            this.inclusivity = from.inclusivity;
            this.persistenceGateway = from.persistenceGateway;
            this.unsetFieldFiller = from.unsetFieldFiller;
            this.recordBeingBuilt = from.recordBeingBuilt;
            this.bundleSoFar = from.bundleSoFar;
            this.rowIndex = from.rowIndex;
            this.forcedRelationshipPaths = from.forcedRelationshipPaths;
            this.pathValues = from.pathValues;
            this.batchedInsertPending = from.batchedInsertPending;
            this.excludePrimaryIds = from.excludePrimaryIds;
            this.valueFieldPass = from.valueFieldPass;
            this.cycleGuard = from.cycleGuard;
        }

        private Copy persistenceGateway(PersistenceGatewayLike value) {
            this.persistenceGateway = value;
            return this;
        }

        private Copy unsetFieldFiller(UnsetFieldFillerLike value) {
            this.unsetFieldFiller = value;
            return this;
        }

        private Copy inclusivity(InsertInclusivity value) {
            this.inclusivity = value;
            return this;
        }

        private Copy forcedRelationshipPaths(List<List<Field>> value) {
            this.forcedRelationshipPaths = value;
            return this;
        }

        private Copy pathValues(List<PathValue> value) {
            this.pathValues = value;
            return this;
        }

        private Copy batchedInsertPending(boolean value) {
            this.batchedInsertPending = value;
            return this;
        }

        private Copy excludePrimaryIds(boolean value) {
            this.excludePrimaryIds = value;
            return this;
        }

        private Copy cycleGuard(AncestorCycleGuard value) {
            this.cycleGuard = value;
            return this;
        }

        private GenerationContext done() {
            return new GenerationContext(
                    this.providerLookup, this.insertMode, this.inclusivity, this.persistenceGateway, this.unsetFieldFiller,
                    this.recordBeingBuilt, this.bundleSoFar, this.rowIndex, this.forcedRelationshipPaths, this.pathValues,
                    this.batchedInsertPending, this.excludePrimaryIds, this.valueFieldPass, this.cycleGuard);
        }
    }
}
