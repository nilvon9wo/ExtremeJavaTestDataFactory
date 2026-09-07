package net.nowhereatall.xfty.engine;

import java.util.List;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.XftyConfigurationException;
import net.nowhereatall.xfty.core.MasterTemplate;
import net.nowhereatall.xfty.relationships.DefaultRelationshipLike;

/**
 * Applies each {@code includeOptional(...)} path by promoting its head
 * relationship from optional to required, on a copy of the master template.
 */
public final class RelationshipForcer {

    private RelationshipForcer() {
    }

    public static MasterTemplate apply(List<List<Field>> paths, MasterTemplate template) {
        if (paths.isEmpty()) {
            return template;
        }
        MasterTemplate forced = template.copy();
        for (List<Field> path : paths) {
            promoteHead(path.get(0), forced, template);
        }
        return forced;
    }

    private static void promoteHead(Field head, MasterTemplate forced, MasterTemplate source) {
        DefaultRelationshipLike optional = source.optionalRelationshipByField().get(head);
        if (optional == null) {
            assertIsRelationship(source, head);
            return;
        }
        forced.remove(head);
        forced.putRequired(head, optional);
    }

    private static void assertIsRelationship(MasterTemplate template, Field head) {
        if (template.requiredRelationshipByField().containsKey(head)) {
            return;
        }
        throw new XftyConfigurationException(
                "includeOptional: " + head.name() + " is not a relationship on the Provider for "
                + template.primaryTargetField().name() + ".");
    }
}
