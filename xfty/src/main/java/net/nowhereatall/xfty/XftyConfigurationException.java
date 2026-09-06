package net.nowhereatall.xfty;

/**
 * Thrown when XFTY is misconfigured by its caller - a missing predicate list, a
 * null value where one is required, and similar. The framework must never make
 * a consumer debug it: this always names the misconfiguration and, where
 * possible, the fix, rather than surfacing a silent default or an opaque
 * downstream error.
 */
public class XftyConfigurationException extends RuntimeException {

    public XftyConfigurationException(String message) {
        super(message);
    }

    public XftyConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
