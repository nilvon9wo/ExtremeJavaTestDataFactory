package net.nowhereatall.xfty.enrichment;

import java.util.List;

import net.nowhereatall.xfty.Field;

/**
 * A stable, comparable key for a relationship path - the declaring type and name
 * of each field, joined by "&gt;"; "" for an empty or null path.
 */
public final class PathKey {

    private PathKey() {
    }

    public static String of(List<Field> path) {
        if (path == null) {
            return "";
        }
        StringBuilder key = new StringBuilder();
        for (Field step : path) {
            key.append(step.recordType().getName()).append('.').append(step.name()).append('>');
        }
        return key.toString();
    }
}
