package net.iridiummc.iny.readiness;

import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/** A handle owned by an integration for one INY startup readiness dependency. */
public interface ReadinessBlocker {

    /** Marks this blocker completed. Repeated terminal transitions are ignored. */
    void complete();

    /**
     * Marks this blocker failed. Repeated terminal transitions are ignored.
     *
     * @param cause failure that prevented readiness
     */
    void fail(Throwable cause);

    /**
     * Returns whether this blocker is completed, failed, or timed out.
     *
     * @return {@code true} when this blocker is in a terminal state
     */
    boolean isResolved();

    /**
     * Returns the plugin that registered this blocker, when it is still reachable.
     *
     * @return owning plugin
     */
    Plugin plugin();

    /**
     * Returns the human-readable blocker name.
     *
     * @return blocker name
     */
    String name();

    /**
     * Returns when this blocker was created.
     *
     * @return creation instant
     */
    Instant createdAt();

    /**
     * Returns the configured timeout.
     *
     * @return configured timeout
     */
    Duration timeout();

    /**
     * Returns the configured timeout policy.
     *
     * @return configured timeout policy
     */
    TimeoutPolicy timeoutPolicy();

    /**
     * Returns the current blocker state.
     *
     * @return current blocker state
     */
    ReadinessBlockerState state();

    /**
     * Returns the failure cause for an explicit failure or owner-disable failure.
     *
     * @return failure cause, or empty when no failure was recorded
     */
    Optional<Throwable> failureCause();
}
