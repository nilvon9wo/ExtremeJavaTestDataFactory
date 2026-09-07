package net.nowhereatall.xfty.enrichment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.XftyConfigurationException;
import net.nowhereatall.xfty.values.ValueExpressionLike;

/**
 * Applies the forced scalar values an {@link InjectConfig} carries onto the
 * {@link RecordInjector} for whichever position {@link BundleEnricher} is
 * enriching. No recursion; just value placement.
 */
public final class ForcedValues {

    private final InjectConfig config;
    private final Set<Integer> reachedAncestorValues = new LinkedHashSet<>();
    private final Set<Integer> reachedChildValues = new LinkedHashSet<>();

    public ForcedValues(InjectConfig config) {
        this.config = config;
    }

    /** The injectValue(field, v) scalars - the target record itself. */
    public void applyRecordValues(RecordInjector injector, int rowCount) {
        placeAll(injector, this.config.onRecordValues(), rowCount);
    }

    /** The injectValue(path, v) scalars whose relationship prefix is this ancestor position. */
    public void applyAncestorValues(RecordInjector injector, List<Field> pathFromEntry, int rowCount) {
        String hereKey = PathKey.of(pathFromEntry);
        Map<Field, Object> matched = new LinkedHashMap<>();
        for (int index = 0; index < this.config.ancestorValues().size(); index++) {
            AncestorValue ancestorValue = this.config.ancestorValues().get(index);
            if (PathKey.of(ancestorValue.relationshipPrefix()).equals(hereKey)) {
                matched.put(ancestorValue.targetField(), ancestorValue.value());
                this.reachedAncestorValues.add(index);
            }
        }
        placeAll(injector, matched, rowCount);
    }

    /** The injectChildValue(path, v) scalars whose relationship prefix is this child position. */
    public void applyChildValues(RecordInjector injector, List<Field> childPathFromEntry, int rowCount) {
        String hereKey = PathKey.of(childPathFromEntry);
        Map<Field, Object> matched = new LinkedHashMap<>();
        for (int index = 0; index < this.config.childValues().size(); index++) {
            ChildValue childValue = this.config.childValues().get(index);
            if (PathKey.of(childValue.relationshipPrefix()).equals(hereKey)) {
                matched.put(childValue.targetField(), childValue.value());
                this.reachedChildValues.add(index);
            }
        }
        placeAll(injector, matched, rowCount);
    }

    /** Throw if any injectValue(path) / injectChildValue never matched a visited position. */
    public void assertEveryPathWasReached() {
        List<String> unreached = new ArrayList<>();
        for (int index = 0; index < this.config.ancestorValues().size(); index++) {
            if (!this.reachedAncestorValues.contains(index)) {
                unreached.add("injectValue " + PathKey.of(this.config.ancestorValues().get(index).path()));
            }
        }
        for (int index = 0; index < this.config.childValues().size(); index++) {
            if (!this.reachedChildValues.contains(index)) {
                unreached.add("injectChildValue " + PathKey.of(this.config.childValues().get(index).path()));
            }
        }
        if (!unreached.isEmpty()) {
            throw new XftyConfigurationException("Inject: [" + String.join(", ", unreached)
                    + "] named a record the graph never produced or the walk never reached (check the path, that the "
                    + "ancestor / child was generated, and parentDepth / childDepth).");
        }
    }

    @SuppressWarnings("unchecked")
    private static void placeAll(RecordInjector injector, Map<Field, Object> valueByField, int rowCount) {
        valueByField.forEach((field, value) -> {
            if (value instanceof List<?>) {
                injector.valuePerRow(field, new ArrayList<>((List<Object>) value));
            } else if (value instanceof ValueExpressionLike expression) {
                injector.valuePerRow(field, resolvedPerRow(expression, rowCount));
            } else {
                injector.value(field, value);
            }
        });
    }

    private static List<Object> resolvedPerRow(ValueExpressionLike expression, int rowCount) {
        List<Object> perRow = new ArrayList<>(rowCount);
        for (int row = 0; row < rowCount; row++) {
            perRow.add(expression.get());
        }
        return perRow;
    }
}
