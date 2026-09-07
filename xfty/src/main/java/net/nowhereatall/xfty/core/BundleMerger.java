package net.nowhereatall.xfty.core;

import java.util.ArrayList;
import java.util.List;

import net.nowhereatall.xfty.Field;

/**
 * Combines the sibling child bundles one relationship field can carry - one per
 * configured child provider - into a single bundle: every child across the
 * configs as primaries, and each generated parent merged so the whole collection
 * navigates as one unit.
 */
public final class BundleMerger {

    private BundleMerger() {
    }

    public static Bundle combine(List<Bundle> bundles) {
        Bundle merged = new Bundle();
        bundles.forEach(each -> combineParentsInto(merged, each));
        putMergedPrimaries(merged, bundles);
        return merged;
    }

    private static void combineParentsInto(Bundle merged, Bundle source) {
        for (Field parentField : source.relationshipFields()) {
            merged.put(parentField, resolveCombined(merged, source, parentField));
        }
    }

    private static Bundle resolveCombined(Bundle merged, Bundle source, Field parentField) {
        Bundle soFar = merged.getBundle(parentField);
        Bundle incoming = source.getBundle(parentField);
        return soFar == null ? incoming : combinedPrimaries(soFar, incoming);
    }

    private static Bundle combinedPrimaries(Bundle soFar, Bundle incoming) {
        List<Object> records = new ArrayList<>();
        if (soFar.primaryRecords() != null) {
            records.addAll(soFar.primaryRecords());
        }
        if (incoming.primaryRecords() != null) {
            records.addAll(incoming.primaryRecords());
        }
        Bundle rebuilt = new Bundle();
        rebuilt.putPrimaries(incoming.primaryTargetField(), records);
        return rebuilt;
    }

    private static void putMergedPrimaries(Bundle merged, List<Bundle> bundles) {
        Field primaryField = bundles.get(0).primaryTargetField();
        if (primaryField == null) {
            return;
        }
        List<Object> allPrimaries = new ArrayList<>();
        for (Bundle each : bundles) {
            if (each.primaryRecords() != null) {
                allPrimaries.addAll(each.primaryRecords());
            }
        }
        merged.putPrimaries(primaryField, allPrimaries);
    }
}
