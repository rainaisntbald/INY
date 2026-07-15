package net.iridiummc.iny.readiness;

import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.List;

/**
 * Controls the startup readiness barrier for the shared INY service.
 *
 * <p>Integrations register blockers during plugin startup. Registration closes when INY
 * receives Bukkit's startup {@code ServerLoadEvent}; blockers registered after that point
 * are rejected.</p>
 */
public interface Readiness {

    /** The timeout and policy used by {@link #registerBlocker(Plugin, String)}. */
    Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    /** The default policy used by {@link #registerBlocker(Plugin, String)}. */
    TimeoutPolicy DEFAULT_TIMEOUT_POLICY = TimeoutPolicy.CONTINUE_WITH_WARNING;

    /**
     * Registers a blocker with the ten-second warning-and-continue defaults.
     *
     * @param owner plugin that owns the blocker
     * @param name human-readable blocker name
     * @return the registered blocker handle
     */
    ReadinessBlocker registerBlocker(Plugin owner, String name);

    /**
     * Registers a blocker with a custom timeout and the default warning-and-continue policy.
     *
     * @param owner plugin that owns the blocker
     * @param name human-readable blocker name
     * @param timeout maximum time to wait
     * @return the registered blocker handle
     */
    ReadinessBlocker registerBlocker(Plugin owner, String name, Duration timeout);

    /**
     * Registers a blocker with explicit timeout and timeout policy.
     *
     * @param owner plugin that owns the blocker
     * @param name human-readable blocker name
     * @param timeout maximum time to wait
     * @param timeoutPolicy action to take when the timeout expires
     * @return the registered blocker handle
     */
    ReadinessBlocker registerBlocker(
            Plugin owner,
            String name,
            Duration timeout,
            TimeoutPolicy timeoutPolicy
    );

    /**
     * Returns the current lifecycle state.
     *
     * @return current readiness state
     */
    ReadinessState state();

    /**
     * Returns whether INY has completed readiness successfully.
     *
     * @return {@code true} when the state is {@link ReadinessState#READY}
     */
    default boolean isReady() {
        return state() == ReadinessState.READY;
    }

    /**
     * Returns an immutable snapshot of blockers that have not resolved yet.
     *
     * @return immutable pending-blocker snapshot
     */
    List<ReadinessBlocker> pendingBlockers();
}
