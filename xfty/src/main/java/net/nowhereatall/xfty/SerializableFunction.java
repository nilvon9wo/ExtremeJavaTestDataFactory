package net.nowhereatall.xfty;

import java.io.Serializable;
import java.util.function.Function;

/**
 * A {@link Function} that is also {@link Serializable}, so a method reference
 * written as {@code Account::getName} can be decomposed back into the accessor
 * it names via {@link java.lang.invoke.SerializedLambda}.
 *
 * <p>This is the type {@link Field#of(SerializableFunction)} accepts. It exists
 * only so the compiler emits the {@code writeReplace} bridge that carries the
 * lambda metadata; there is nothing to implement beyond {@code apply}.
 */
@FunctionalInterface
public interface SerializableFunction<T, R> extends Function<T, R>, Serializable {
}
