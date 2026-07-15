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

    /** Registers a blocker with the ten-second warning-and-continue defaults. */
    ReadinessBlocker registerBlocker(Plugin owner, String name);

    /** Registers a blocker with a custom timeout and the default warning-and-continue policy. */
    ReadinessBlocker registerBlocker(Plugin owner, String name, Duration timeout);

    /** Registers a blocker with explicit timeout and timeout policy. */
    ReadinessBlocker registerBlocker(
            Plugin owner,
            String name,
            Duration timeout,
            TimeoutPolicy timeoutPolicy
    );

    /** Returns the current lifecycle state. */
    ReadinessState state();

    /** Returns whether INY has completed readiness successfully. */
    default boolean isReady() {
        return state() == ReadinessState.READY;
    }

    /** Returns an immutable snapshot of blockers that have not resolved yet. */
    List<ReadinessBlocker> pendingBlockers();
}
