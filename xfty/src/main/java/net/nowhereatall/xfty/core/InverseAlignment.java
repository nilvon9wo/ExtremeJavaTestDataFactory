package net.nowhereatall.xfty.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.nowhereatall.xfty.Field;

/**
 * The inverse of the 1:1 parent alignment: for each parent record, the child
 * records whose foreign key points at it. Matched on the id when the parents
 * carry one, otherwise position for position.
 */
public final class InverseAlignment {

    private static final String ID_FIELD_NAME = "id";

    private InverseAlignment() {
    }

    public static List<List<Object>> childrenPerParent(List<Object> parents, List<Object> children, Field relationshipField) {
        List<List<Object>> perParent = new ArrayList<>(parents.size());
        for (int parentRow = 0; parentRow < parents.size(); parentRow++) {
            perParent.add(matchesFor(parents.get(parentRow), children, relationshipField, parentRow));
        }
        return perParent;
    }

    private static List<Object> matchesFor(Object parent, List<Object> children, Field relationshipField, int parentRow) {
        Object parentId = idOf(parent);
        return parentId != null
                ? foreignKeyMatch(children, relationshipField, parentId)
                : positionMatch(children, parentRow);
    }

    private static Object idOf(Object record) {
        if (record == null) {
            return null;
        }
        try {
            return Field.of(record.getClass(), ID_FIELD_NAME).get(record);
        } catch (RuntimeException absent) {
            return null;
        }
    }

    private static List<Object> foreignKeyMatch(List<Object> children, Field relationshipField, Object parentId) {
        List<Object> matches = new ArrayList<>();
        for (Object child : children) {
            if (child != null && Objects.equals(relationshipField.get(child), parentId)) {
                matches.add(child);
            }
        }
        return matches;
    }

    private static List<Object> positionMatch(List<Object> children, int parentRow) {
        return parentRow < children.size() ? new ArrayList<>(List.of(children.get(parentRow))) : new ArrayList<>();
    }
}
