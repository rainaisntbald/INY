package net.iridiummc.iny.runtime;

/** A deferred value resolved with a caller-supplied runtime context. */
@FunctionalInterface
public interface InyProvider<T> extends InyRunnable {

    /** Resolves one value. INY does not cache provider results. */
    T resolve(InyRuntimeContext context);

    /** Resolves this provider and discards its result. */
    @Override
    default void run(InyRuntimeContext context) {
        resolve(context);
    }
}
