package net.nowhereatall.xfty;

/**
 * Thrown when a {@code Field.of(Type::accessor)} method reference cannot be
 * cleanly decomposed into a single plain accessor call - a multi-statement
 * lambda, a reference to something that is not a zero-argument getter or record
 * accessor, or a synthetic lambda body.
 *
 * <p>The message always points at the string-based overload
 * ({@code Field.of(Type.class, "name")}) as the guaranteed fallback.
 */
public final class FieldReferenceException extends XftyConfigurationException {

    public FieldReferenceException(String message) {
        super(message);
    }

    public FieldReferenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
