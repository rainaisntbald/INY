package net.iridiummc.iny.readiness;

/** Terminal and non-terminal states for one readiness blocker. */
public enum ReadinessBlockerState {
    PENDING,
    COMPLETED,
    FAILED,
    TIMED_OUT
}
