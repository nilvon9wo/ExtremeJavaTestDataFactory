package net.nowhereatall.xfty.engine;

import java.util.HashSet;
import java.util.Set;

/**
 * Tracks the Provider lookup keys currently being generated up the ancestor
 * chain, so the ancestor generator can refuse to recurse into one already in
 * progress - an infinite A → A → A ... cycle. Keyed by a lookup key's hash key,
 * so a deliberate deep hierarchy built with distinct per-level Providers is not
 * a cycle.
 */
public final class AncestorCycleGuard {

    private final boolean cyclesAllowed;
    private final Set<String> providerKeyHashesInProgress;

    public AncestorCycleGuard(boolean cyclesAllowed) {
        this(cyclesAllowed, new HashSet<>());
    }

    private AncestorCycleGuard(boolean cyclesAllowed, Set<String> providerKeyHashesInProgress) {
        this.cyclesAllowed = cyclesAllowed;
        this.providerKeyHashesInProgress = providerKeyHashesInProgress;
    }

    /** True when descending into {@code providerKeyHash} would repeat a key already in progress. */
    public boolean wouldCycleOn(String providerKeyHash) {
        return !this.cyclesAllowed && this.providerKeyHashesInProgress.contains(providerKeyHash);
    }

    /** A guard for one level deeper, with {@code providerKeyHash} added to the chain. */
    public AncestorCycleGuard descendingInto(String providerKeyHash) {
        Set<String> deeper = new HashSet<>(this.providerKeyHashesInProgress);
        deeper.add(providerKeyHash);
        return new AncestorCycleGuard(this.cyclesAllowed, deeper);
    }
}
