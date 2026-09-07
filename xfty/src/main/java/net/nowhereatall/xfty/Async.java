package net.nowhereatall.xfty;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Blocks on a {@link CompletableFuture} and hands back its value, unwrapping the
 * {@link CompletionException} that {@code join()} would otherwise wrap a thrown
 * exception in - so callers see the real exception XFTY throws
 * ({@link XftyConfigurationException} and friends), not a wrapper.
 *
 * <p>Generation is asynchronous end to end; a synchronous caller (a test, a
 * seeding script, a {@code @BeforeEach}) uses this to wait for a
 * {@code supply()} / {@code supplyBundle()} result.
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
