package net.nowhereatall.xfty;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Test helper: block on a {@link CompletableFuture} and unwrap the
 * {@link CompletionException} {@code join()} would otherwise wrap a thrown
 * exception in, so tests can assert on the real exception type XFTY throws.
 */
public final class Async {

    private Async() {
    }

    public static <T> T await(CompletableFuture<T> future) {
        try {
            return future.join();
        } catch (CompletionException wrapped) {
            Throwable cause = wrapped.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw wrapped;
        }
    }
}
