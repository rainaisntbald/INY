package net.iridiummc.iny;

import java.time.Duration;

/** Small scheduler seam that keeps readiness state tests independent of a live Bukkit server. */
interface ReadinessScheduler {

    Cancellable scheduleTimeout(Runnable task, Duration timeout);

    void executeOnMain(Runnable task);

    interface Cancellable {
        void cancel();
    }
}
