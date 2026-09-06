package net.nowhereatall.xfty.demo;

/**
 * A Java record - the immutable half of the demo domain. Every place the engine
 * would reflectively set a field, it instead reconstructs a {@code Contact}
 * through this canonical constructor.
 */
public record Contact(
        String id,
        String firstName,
        String lastName,
        String email,
        String accountId) {
}
