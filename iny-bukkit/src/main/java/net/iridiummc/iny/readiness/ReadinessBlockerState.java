package net.iridiummc.iny.readiness;

/** Terminal and non-terminal states for one readiness blocker. */
public enum ReadinessBlockerState {
    /** The blocker has not resolved yet. */
    PENDING,

    /** The owner completed the blocker successfully. */
    COMPLETED,

    /** The blocker failed explicitly or because its owner was disabled. */
    FAILED,

    /** The configured timeout elapsed before completion. */
    TIMED_OUT
}
