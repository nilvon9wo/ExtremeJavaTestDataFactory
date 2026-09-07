package net.nowhereatall.xfty.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.XftyConfigurationException;

/**
 * The child collections one {@link RecordProvider} call generates - downward
 * generation, the mirror of the usual upward ancestor generation.
 */
final class RecordProviderChildConfig {

    private final List<ChildProvider> childProviders = new ArrayList<>();

    void add(ChildProvider childProvider) {
        if (childProvider == null) {
            throw new XftyConfigurationException("with(...) needs a ChildProvider.");
        }
        this.childProviders.add(childProvider);
    }

    boolean hasAny() {
        return !this.childProviders.isEmpty();
    }

    CompletableFuture<Void> generateAll(Bundle bundle, boolean structural, RecordProviderExecutionState state) {
        return generateRemaining(bundle, this.childProviders, structural, state);
    }

    private static CompletableFuture<Void> generateRemaining(
            Bundle bundle, List<ChildProvider> childProviders, boolean structural, RecordProviderExecutionState state) {
        if (childProviders.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return generateOne(bundle, childProviders.get(0), structural, state)
                .thenCompose(ignored -> generateRemaining(
                        bundle, childProviders.subList(1, childProviders.size()), structural, state));
    }

    private static CompletableFuture<Void> generateOne(
            Bundle bundle, ChildProvider childProvider, boolean structural, RecordProviderExecutionState state) {
        Field primaryField = state.factoryOutlet().primaryTargetField();
        List<ChildRow> childRows = childRowsFor(bundle, primaryField, childProvider, structural);
        RecordProvider<Object> childInstance = buildChildInstance(childProvider, structural, childRows, state);
        return childInstance.supplyBundle().thenAccept(childBundle -> {
            List<Integer> parentRows = new ArrayList<>();
            for (ChildRow row : childRows) {
                parentRows.add(row.parentRow());
            }
            bundle.putChild(childProvider.relationshipField(), childBundle, parentRows);
        });
    }

    private static List<ChildRow> childRowsFor(
            Bundle bundle, Field primaryField, ChildProvider childProvider, boolean structural) {
        List<Object> primaries = bundle.getList(primaryField);
        List<ChildRow> rows = new ArrayList<>();
        for (int parentRow = 0; parentRow < primaries.size(); parentRow++) {
            Object parentId = structural ? null : idOf(primaries.get(parentRow));
            for (Object template : childProvider.templatesForParent(parentId)) {
                rows.add(new ChildRow(template, parentRow));
            }
        }
        return rows;
    }

    private static RecordProvider<Object> buildChildInstance(
            ChildProvider childProvider, boolean structural, List<ChildRow> childRows, RecordProviderExecutionState state) {
        InsertMode childMode = structural ? InsertMode.NEVER : childProvider.effectiveInsertMode(state.insertMode());
        List<Object> templates = new ArrayList<>();
        for (ChildRow row : childRows) {
            templates.add(row.template());
        }
        RecordProvider<Object> childInstance = childProvider.newProvider(state.providerLookup())
                .setOverrideTemplateList(templates)
                .setInsertMode(childMode)
                .setInclusivity(childProvider.effectiveInclusivity(state.inclusivity()));
        if (state.persistenceGateway() != null) {
            childInstance.setPersistenceGateway(state.persistenceGateway());
        }
        if (structural) {
            childInstance
                    .excludeRelationshipIfPresent(childProvider.relationshipField())
                    .forceStructuralChildGeneration();
        }
        return childInstance;
    }

    private static Object idOf(Object record) {
        try {
            return Field.of(record.getClass(), "id").get(record);
        } catch (RuntimeException absent) {
            return null;
        }
    }

    private record ChildRow(Object template, int parentRow) {
    }
}
