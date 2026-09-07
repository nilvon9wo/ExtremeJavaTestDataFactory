package net.nowhereatall.xfty.enrichment;

import java.util.ArrayList;
import java.util.List;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.XftyConfigurationException;
import net.nowhereatall.xfty.core.Bundle;
import net.nowhereatall.xfty.core.BundleChildEntry;
import net.nowhereatall.xfty.core.InverseAlignment;

/**
 * Re-expresses a generated graph in the shape an immutable field rejects, so
 * code under test reads relationships and generated child collections straight
 * off the record.
 *
 * <p>{@code enrichPosition} walks the graph deepest-first via the call stack: at
 * each bundle position it collects the enriched parents and child subqueries
 * below it, then does one {@link RecordInjector} round-trip. Ancestors carry
 * their one-level inverse child; downward children carry their own ancestors
 * and, to childDepth, their own nested children. Returns the enriched target
 * records as new instances - the originals are untouched.
 */
public final class BundleEnricher {

    private final Bundle entryBundle;
    private final Field entryField;
    private final InjectConfig config;
    private final EnrichmentSelection selection;
    private final ForcedValues forcedValues;

    private BundleEnricher(Bundle bundle, Field field, InjectConfig config) {
        QueryableShapeValidator.validate(config);
        this.entryBundle = bundle;
        this.entryField = field;
        this.config = config;
        this.selection = new EnrichmentSelection(config);
        this.forcedValues = new ForcedValues(config);
    }

    public static List<Object> enrich(Bundle bundle, Field field, InjectConfig config) {
        return new BundleEnricher(bundle, field, config).run();
    }

    /** Enrich with {@link InjectConfig#everything()}; throws when the graph has nothing to inject. */
    public static List<Object> enrichEverything(Bundle bundle, Field field) {
        if (EnrichmentTarget.locate(bundle, field).hasAnythingToInject()) {
            return enrich(bundle, field, InjectConfig.everything());
        }
        throw new XftyConfigurationException("injectAll(" + field.name()
                + "): the graph has no generated ancestor or child collection to inject. Generate related records, "
                + "or use inject(field, config) with explicit values.");
    }

    private List<Object> run() {
        EnrichmentTarget target = EnrichmentTarget.locate(this.entryBundle, this.entryField);
        List<Object> result = enrichPosition(rootPosition(target));
        this.forcedValues.assertEveryPathWasReached();
        return result;
    }

    private List<Object> enrichPosition(EnrichmentPosition pos) {
        if (pos.records() == null || pos.records().isEmpty()) {
            return pos.records() == null ? new ArrayList<>() : pos.records();
        }
        RecordInjector injector = RecordInjector.inject(pos.records());
        graftAncestors(injector, pos);
        graftInverse(injector, pos);
        graftChildren(injector, pos);
        applyForcedValues(injector, pos);
        return injector.result();
    }

    private void applyForcedValues(RecordInjector injector, EnrichmentPosition pos) {
        int rowCount = pos.records().size();
        if (pos.isRoot) {
            this.forcedValues.applyRecordValues(injector, rowCount);
        }
        if (pos.pathFromEntry != null && !pos.pathFromEntry.isEmpty()) {
            this.forcedValues.applyAncestorValues(injector, pos.pathFromEntry, rowCount);
        }
        if (pos.childPathFromEntry != null && !pos.childPathFromEntry.isEmpty()) {
            this.forcedValues.applyChildValues(injector, pos.childPathFromEntry, rowCount);
        }
    }

    private void graftAncestors(RecordInjector injector, EnrichmentPosition pos) {
        if (pos.subBundle() == null || pos.parentDepthLeft <= 0) {
            return;
        }
        for (Field lookupField : pos.subBundle().relationshipFields()) {
            graftAncestor(injector, pos, lookupField);
        }
    }

    private void graftAncestor(RecordInjector injector, EnrichmentPosition pos, Field lookupField) {
        if (!this.selection.wantsAncestor(ancestorPath(pos, lookupField))) {
            return;
        }
        List<Object> parents = pos.subBundle().getList(lookupField);
        if (parents == null) {
            return;
        }
        injector.relationship(
                InjectionPathResolver.parentRelationshipField(lookupField),
                enrichPosition(ancestorPosition(pos, lookupField, parents)));
    }

    private void graftInverse(RecordInjector injector, EnrichmentPosition pos) {
        if (pos.inverseChildField() == null) {
            return;
        }
        injector.childRelationship(
                InjectionPathResolver.childRelationshipField(pos.positionType(), pos.inverseChildField()),
                pos.inverseChildrenPerRow());
    }

    private void graftChildren(RecordInjector injector, EnrichmentPosition pos) {
        if (pos.subBundle() == null || pos.childDepthLeft <= 0) {
            return;
        }
        for (Field childField : this.selection.childFieldsOn(pos.subBundle(), childPathOf(pos))) {
            injector.childRelationship(
                    InjectionPathResolver.childRelationshipField(pos.positionType(), childField),
                    childrenPerRow(pos, childField));
        }
    }

    private List<List<Object>> childrenPerRow(EnrichmentPosition pos, Field childField) {
        List<List<Object>> perRow = emptyListsFor(pos.records().size());
        for (BundleChildEntry entry : pos.subBundle().childEntries(childField)) {
            enrichEntryInto(perRow, childrenPosition(pos, entry, childField), entry);
        }
        return perRow;
    }

    private void enrichEntryInto(List<List<Object>> perRow, EnrichmentPosition childPos, BundleChildEntry entry) {
        if (childPos.records() == null || childPos.records().isEmpty()) {
            return;
        }
        List<Object> enrichedChildren = enrichPosition(childPos);
        for (int childRow = 0; childRow < enrichedChildren.size(); childRow++) {
            perRow.get(entry.parentRowByChildRow().get(childRow)).add(enrichedChildren.get(childRow));
        }
    }

    private EnrichmentPosition rootPosition(EnrichmentTarget target) {
        EnrichmentPosition root = new EnrichmentPosition(target.subBundle(), target.records());
        root.pathFromEntry = new ArrayList<>();
        root.childPathFromEntry = new ArrayList<>();
        root.parentDepthLeft = this.config.parentDepthLimit();
        root.childDepthLeft = this.config.childDepthLimit();
        root.isRoot = true;
        if (target.isGeneratedAncestor()) {
            root.carryInverse(this.entryField, InverseAlignment.childrenPerParent(
                    target.records(), this.entryBundle.primaryRecords(), this.entryField));
        }
        return root;
    }

    private EnrichmentPosition ancestorPosition(EnrichmentPosition pos, Field lookupField, List<Object> parents) {
        EnrichmentPosition up = new EnrichmentPosition(pos.subBundle().getBundle(lookupField), parents);
        up.pathFromEntry = ancestorPath(pos, lookupField);
        up.parentDepthLeft = pos.parentDepthLeft - 1;
        if (this.selection.wantsInverse(lookupField)) {
            up.carryInverse(lookupField, InverseAlignment.childrenPerParent(parents, pos.records(), lookupField));
        }
        return up;
    }

    private EnrichmentPosition childrenPosition(EnrichmentPosition pos, BundleChildEntry entry, Field childField) {
        EnrichmentPosition down = new EnrichmentPosition(entry.bundle(), entry.bundle().primaryRecords());
        down.parentDepthLeft = this.config.parentDepthLimit();
        down.childDepthLeft = pos.childDepthLeft - 1;
        down.childPathFromEntry = append(childPathOf(pos), childField);
        return down;
    }

    private static List<Field> childPathOf(EnrichmentPosition pos) {
        return pos.childPathFromEntry == null ? new ArrayList<>() : pos.childPathFromEntry;
    }

    private static List<Field> ancestorPath(EnrichmentPosition pos, Field lookupField) {
        return pos.pathFromEntry == null ? null : append(pos.pathFromEntry, lookupField);
    }

    private static List<Field> append(List<Field> path, Field extra) {
        List<Field> extended = new ArrayList<>(path);
        extended.add(extra);
        return extended;
    }

    private static List<List<Object>> emptyListsFor(int size) {
        List<List<Object>> lists = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            lists.add(new ArrayList<>());
        }
        return lists;
    }
}
