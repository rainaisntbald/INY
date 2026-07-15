package net.iridiummc.iny.readiness;

/** Lifecycle states of the shared INY readiness barrier. */
public enum ReadinessState {
    /** The plugin is creating its shared services. */
    INITIALISING,

    /** Integrations may register readiness blockers and factories. */
    REGISTRATION_OPEN,

    /** Registration has closed and unresolved blockers remain. */
    WAITING_FOR_BLOCKERS,

    /** All blockers resolved successfully and configuration may be consumed. */
    READY,

    /** A blocker or lifecycle transition prevented readiness. */
    FAILED
}
