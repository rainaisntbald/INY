package net.iridiummc.iny.runtime;

/**
 * A deferred value resolved with a caller-supplied runtime context.
 *
 * @param <T> resolved value type
 */
@FunctionalInterface
public interface InyProvider<T> extends InyRunnable {

    /**
     * Resolves one value. INY does not cache provider results.
     *
     * @param context non-null runtime context
     * @return the non-null resolved value
     */
    T resolve(InyRuntimeContext context);

    /**
     * Resolves this provider and discards its result.
     *
     * @param context non-null runtime context
     */
    @Override
    default void run(InyRuntimeContext context) {
        resolve(context);
    }
}
