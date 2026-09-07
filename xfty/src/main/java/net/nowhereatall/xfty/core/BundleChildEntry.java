package net.nowhereatall.xfty.core;

import java.util.List;

/** One configured child collection on a {@link Bundle}: its bundle + which primary row each child row belongs to. */
public final class BundleChildEntry {

    private final Bundle bundle;
    private final List<Integer> parentRowByChildRow;

    public BundleChildEntry(Bundle bundle, List<Integer> parentRowByChildRow) {
        this.bundle = bundle;
        this.parentRowByChildRow = parentRowByChildRow;
    }

    public Bundle bundle() {
        return this.bundle;
    }

    public List<Integer> parentRowByChildRow() {
        return this.parentRowByChildRow;
    }
}
