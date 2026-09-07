package net.nowhereatall.xfty.persistence;

/** A record paired with its position in the list or pass that is working on it. */
public final class IndexedRecord {

    private final int index;
    private final Object record;

    public IndexedRecord(int index, Object record) {
        this.index = index;
        this.record = record;
    }

    public int index() {
        return this.index;
    }

    public Object record() {
        return this.record;
    }
}
