package net.nowhereatall.xfty.engine;

import java.util.ArrayList;
import java.util.List;

import net.nowhereatall.xfty.core.MasterTemplate;
import net.nowhereatall.xfty.core.PathValue;

/**
 * Applies the {@code put(List<Field> path, value)} overrides that have reached
 * their target ({@code isAtTarget()}) by landing each value on a copy of the
 * master template for the level being generated.
 */
public final class PathValueApplier {

    private PathValueApplier() {
    }

    public static MasterTemplate apply(List<PathValue> pathValues, MasterTemplate template) {
        List<PathValue> atTarget = new ArrayList<>();
        for (PathValue pathValue : pathValues) {
            if (pathValue.isAtTarget()) {
                atTarget.add(pathValue);
            }
        }
        if (atTarget.isEmpty()) {
            return template;
        }
        MasterTemplate overlaid = template.copy();
        for (PathValue pathValue : atTarget) {
            pathValue.applyTo(overlaid);
        }
        return overlaid;
    }
}
