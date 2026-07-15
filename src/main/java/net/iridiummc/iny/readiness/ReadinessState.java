package net.iridiummc.iny.readiness;

/** Lifecycle states of the shared INY readiness barrier. */
public enum ReadinessState {
    INITIALISING,
    REGISTRATION_OPEN,
    WAITING_FOR_BLOCKERS,
    READY,
    FAILED
}
