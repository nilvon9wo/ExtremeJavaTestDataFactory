package net.nowhereatall.xfty.lookup;

import net.nowhereatall.xfty.XftyConfigurationException;

/** A lookup could not resolve, or resolved ambiguously - always with the fix in the message. */
public final class LookupException extends XftyConfigurationException {

    public LookupException(String message) {
        super(message);
    }
}
