package net.nowhereatall.xfty.demo;

/**
 * A Java record - the immutable half of the demo domain. Every place the engine
 * would reflectively set a field, it instead reconstructs a {@code Contact}
 * through its canonical constructor.
 *
 * <p>The 5-argument constructor is a convenience for the common case; the
 * canonical (7-arg) one carries the two relationship-derived fields
 * ({@code reportsToId}, {@code department}) the engine tests exercise.
 */
public record Contact(
        String id,
        String firstName,
        String lastName,
        String email,
        String accountId,
        String reportsToId,
        String department) {

    public Contact(String id, String firstName, String lastName, String email, String accountId) {
        this(id, firstName, lastName, email, accountId, null, null);
    }
}
