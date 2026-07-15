package net.iridiummc.iny.readiness;

/** Determines what a failed or timed-out readiness blocker does to INY startup. */
public enum TimeoutPolicy {
    /** Log the problem and allow INY to become ready once other blockers resolve. */
    CONTINUE_WITH_WARNING,

    /** Permanently fail INY readiness and suppress the successful ready notification. */
    FAIL_READINESS
}
