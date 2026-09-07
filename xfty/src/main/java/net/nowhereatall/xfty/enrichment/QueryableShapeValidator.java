package net.nowhereatall.xfty.enrichment;

import net.nowhereatall.xfty.XftyConfigurationException;

/**
 * Rejects an {@link InjectConfig} that describes a shape no single query
 * round-trip could realistically return - a parent climb past a sane
 * relationship-hop limit, or a child subquery nested deeper than one level.
 * {@code allowDeeperGraph()} turns those checks off.
 */
public final class QueryableShapeValidator {

    private QueryableShapeValidator() {
    }

    public static void validate(InjectConfig config) {
        rejectChildValuesDeeperThanChildDepth(config);
        if (config.depthLimitsLifted()) {
            return;
        }
        rejectOverLimit("parentDepth " + config.parentDepthLimit(),
                config.parentDepthLimit(), InjectConfig.DEFAULT_PARENT_DEPTH_LIMIT);
        rejectOverLimit("childDepth " + config.childDepthLimit(),
                config.childDepthLimit(), InjectConfig.DEFAULT_CHILD_DEPTH_LIMIT);
        for (java.util.List<net.nowhereatall.xfty.Field> path : config.includedParentPaths()) {
            rejectOverLimit("an injectParent path of " + path.size() + " hops",
                    path.size(), InjectConfig.DEFAULT_PARENT_DEPTH_LIMIT);
        }
    }

    private static void rejectChildValuesDeeperThanChildDepth(InjectConfig config) {
        for (ChildValue childValue : config.childValues()) {
            int childLevels = childValue.relationshipPrefix().size();
            if (childLevels > config.childDepthLimit()) {
                throw new XftyConfigurationException(
                        "Inject: an injectChildValue path reaches " + childLevels + " child level(s) but childDepth is "
                        + config.childDepthLimit() + ". Raise childDepth (past " + InjectConfig.DEFAULT_CHILD_DEPTH_LIMIT
                        + " also needs allowDeeperGraph()).");
            }
        }
    }

    private static void rejectOverLimit(String label, int value, int defaultLimit) {
        if (value <= defaultLimit) {
            return;
        }
        throw new XftyConfigurationException(
                "Inject: " + label + " exceeds the " + defaultLimit + " a single query round-trip should reasonably "
                + "return. Call allowDeeperGraph() on the config to allow it.");
    }
}
