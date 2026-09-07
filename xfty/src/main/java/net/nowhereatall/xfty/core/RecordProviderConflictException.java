package net.nowhereatall.xfty.core;

import net.nowhereatall.xfty.XftyConfigurationException;

/** Thrown when a RecordProvider is given data for a record type other than the one it was constructed for. */
public final class RecordProviderConflictException extends XftyConfigurationException {

    public RecordProviderConflictException(String message) {
        super(message);
    }
}
