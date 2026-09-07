package net.nowhereatall.xfty.core;

import java.util.List;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.XftyConfigurationException;

/**
 * Reads one field several relationship hops up a generated ancestor graph,
 * without the caller holding on to every intermediate bundle and list.
 *
 * <p>{@code path} is one or more relationship fields then the field to read.
 * Returns null when a hop was not generated or {@code rowIndex} is out of range.
 * Only walks generated ancestors - child relationships are not followed. Throws
 * only when the path itself is malformed.
 */
public final class AncestorPathWalker {

    private AncestorPathWalker() {
    }

    public static Object read(Bundle source, List<Field> path, int rowIndex) {
        rejectMalformed(path);
        Bundle owningBundle = descend(source, path);
        if (owningBundle == null || rowIndex < 0) {
            return null;
        }
        return valueAt(owningBundle, path, rowIndex);
    }

    private static Bundle descend(Bundle source, List<Field> path) {
        Bundle bundle = source;
        for (Field hop : path.subList(0, path.size() - 2)) {
            if (bundle == null) {
                return null;
            }
            bundle = bundle.getBundle(hop);
        }
        return bundle;
    }

    private static Object valueAt(Bundle owningBundle, List<Field> path, int rowIndex) {
        Field lastRelationshipField = path.get(path.size() - 2);
        Field fieldToRead = path.get(path.size() - 1);
        List<Object> parents = owningBundle.getList(lastRelationshipField);
        boolean noParentAtRow = parents == null || rowIndex >= parents.size() || parents.get(rowIndex) == null;
        return noParentAtRow ? null : fieldToRead.get(parents.get(rowIndex));
    }

    private static void rejectMalformed(List<Field> path) {
        if (path == null || path.size() < 2) {
            throw new XftyConfigurationException(
                    "getValue needs a path of at least one relationship field then the field to read.");
        }
        if (path.stream().anyMatch(step -> step == null)) {
            throw new XftyConfigurationException("getValue path steps cannot be null.");
        }
    }
}
