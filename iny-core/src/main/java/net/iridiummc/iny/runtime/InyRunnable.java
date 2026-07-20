package net.iridiummc.iny.runtime;

/** A deferred action executed with a caller-supplied runtime context. */
@FunctionalInterface
public interface InyRunnable {

    /**
     * Executes this action. INY does not cache, schedule, or memoise execution.
     *
     * @param context non-null runtime context
     */
    void run(InyRuntimeContext context);
}
