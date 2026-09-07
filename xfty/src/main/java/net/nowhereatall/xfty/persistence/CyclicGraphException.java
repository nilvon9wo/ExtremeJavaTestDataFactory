package net.nowhereatall.xfty.persistence;

/** The lookups leave no order in which every parent lands before its child. */
public final class CyclicGraphException extends RuntimeException {

    public CyclicGraphException(String message) {
        super(message);
    }
}
