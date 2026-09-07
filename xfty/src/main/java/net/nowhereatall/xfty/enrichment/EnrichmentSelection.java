package net.nowhereatall.xfty.enrichment;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.core.Bundle;

/**
 * Answers "does this config want the ancestor / child / inverse at this
 * position?" for {@link BundleEnricher}'s recursive walk. Built once from the
 * config; paths are compared as {@link PathKey} strings.
 */
public final class EnrichmentSelection {

    private final InjectConfig config;
    private final Set<String> includedParentKeys = new LinkedHashSet<>();
    private final Set<String> excludedParentKeys = new LinkedHashSet<>();

    public EnrichmentSelection(InjectConfig config) {
        this.config = config;
        config.includedParentPaths().forEach(this::includeWithPrefixes);
        config.ancestorValues().forEach(ancestorValue -> includeWithPrefixes(ancestorValue.relationshipPrefix()));
        config.excludedParentPaths().forEach(path -> this.excludedParentKeys.add(PathKey.of(path)));
    }

    /** {@code pathFromEntry} is the upward-hop path; null once the walk has turned downward. */
    public boolean wantsAncestor(List<Field> pathFromEntry) {
        if (pathFromEntry == null) {
            return this.config.fromAllParents();
        }
        return !hasExcludedPrefix(pathFromEntry)
                && (this.config.fromAllParents() || this.includedParentKeys.contains(PathKey.of(pathFromEntry)));
    }

    /** Whether an ancestor generated for the level below should carry that level back as its child subquery. */
    public boolean wantsInverse(Field relationshipField) {
        return this.config.fromAllChildren() && !this.config.excludedChildFields().contains(relationshipField);
    }

    /** The child relationship fields to inject at this position. {@code childPathHere} is the child-hop path already walked. */
    public Set<Field> childFieldsOn(Bundle subBundle, List<Field> childPathHere) {
        Set<Field> present = new LinkedHashSet<>(subBundle.childRelationshipFields());
        Set<Field> wanted = this.config.fromAllChildren() ? new LinkedHashSet<>(present) : new LinkedHashSet<>();
        wanted.addAll(namedNextHopsFollowing(childPathHere, present));
        wanted.removeAll(this.config.excludedChildFields());
        return wanted;
    }

    private Set<Field> namedNextHopsFollowing(List<Field> childPathHere, Set<Field> present) {
        Set<Field> named = new LinkedHashSet<>();
        if (childPathHere.isEmpty()) {
            for (Field childField : this.config.includedChildFields()) {
                if (present.contains(childField)) {
                    named.add(childField);
                }
            }
        }
        for (ChildValue childValue : this.config.childValues()) {
            Field nextHop = nextHopAfter(childPathHere, childValue.relationshipPrefix());
            if (nextHop != null && present.contains(nextHop)) {
                named.add(nextHop);
            }
        }
        return named;
    }

    private static Field nextHopAfter(List<Field> walked, List<Field> fullPath) {
        if (fullPath.size() > walked.size() && fullPath.subList(0, walked.size()).equals(walked)) {
            return fullPath.get(walked.size());
        }
        return null;
    }

    private void includeWithPrefixes(List<Field> path) {
        for (int length = 1; length <= path.size(); length++) {
            this.includedParentKeys.add(PathKey.of(new ArrayList<>(path.subList(0, length))));
        }
    }

    private boolean hasExcludedPrefix(List<Field> path) {
        for (int length = 1; length <= path.size(); length++) {
            if (this.excludedParentKeys.contains(PathKey.of(new ArrayList<>(path.subList(0, length))))) {
                return true;
            }
        }
        return false;
    }
}
