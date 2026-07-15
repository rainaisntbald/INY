package net.iridiummc.iny;

import net.iridiummc.iny.readiness.Readiness;
import net.iridiummc.iny.readiness.ReadinessBlocker;
import net.iridiummc.iny.readiness.ReadinessBlockerState;
import net.iridiummc.iny.readiness.ReadinessState;
import net.iridiummc.iny.readiness.TimeoutPolicy;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Thread-safe implementation of the lifecycle-scoped readiness barrier. */
final class ReadinessController implements Readiness {

    private final Object monitor = new Object();
    private final ReadinessScheduler scheduler;
    private final Runnable readyAction;
    private final Logger logger;
    private final Set<Blocker> pending = new LinkedHashSet<>();
    private final List<Runnable> readyCallbacks = new ArrayList<>();

    private ReadinessState state = ReadinessState.INITIALISING;
    private boolean registrationClosed;
    private boolean notificationScheduled;
    private boolean notificationPublished;

    ReadinessController(Plugin inyPlugin, ReadinessScheduler scheduler, Runnable readyAction) {
        Objects.requireNonNull(inyPlugin, "inyPlugin");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.readyAction = Objects.requireNonNull(readyAction, "readyAction");
        Logger pluginLogger = inyPlugin.getLogger();
        this.logger = pluginLogger == null ? Logger.getLogger("INY") : pluginLogger;
    }

    /** Opens the startup registration phase. This is called by the lifecycle owner once. */
    void openRegistration() {
        synchronized (monitor) {
            if (state != ReadinessState.INITIALISING) {
                throw new IllegalStateException("INY readiness registration has already started");
            }
            state = ReadinessState.REGISTRATION_OPEN;
        }
    }

    @Override
    public ReadinessBlocker registerBlocker(Plugin owner, String name) {
        return registerBlocker(owner, name, Readiness.DEFAULT_TIMEOUT, Readiness.DEFAULT_TIMEOUT_POLICY);
    }

    @Override
    public ReadinessBlocker registerBlocker(Plugin owner, String name, Duration timeout) {
        return registerBlocker(owner, name, timeout, Readiness.DEFAULT_TIMEOUT_POLICY);
    }

    @Override
    public ReadinessBlocker registerBlocker(
            Plugin owner,
            String name,
            Duration timeout,
            TimeoutPolicy timeoutPolicy
    ) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(timeout, "timeout");
        Objects.requireNonNull(timeoutPolicy, "timeoutPolicy");
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("Readiness blocker timeout must be positive");
        }
        String validatedName = validateName(name);
        Blocker blocker = new Blocker(owner, validatedName, timeout, timeoutPolicy);
        synchronized (monitor) {
            ensureRegistrationOpen();
            pending.add(blocker);
            blocker.timeoutTask = scheduler.scheduleTimeout(() -> timeout(blocker), timeout);
        }
        return blocker;
    }

    @Override
    public ReadinessState state() {
        synchronized (monitor) {
            return state;
        }
    }

    @Override
    public List<ReadinessBlocker> pendingBlockers() {
        synchronized (monitor) {
            return List.copyOf(pending);
        }
    }

    /** Closes blocker registration and transitions to waiting or ready. */
    void closeRegistration() {
        boolean logWaiting = false;
        boolean dispatchReady = false;
        List<Blocker> snapshot = List.of();
        synchronized (monitor) {
            if (registrationClosed) {
                return;
            }
            registrationClosed = true;
            if (state == ReadinessState.FAILED) {
                return;
            }
            if (pending.isEmpty()) {
                state = ReadinessState.READY;
                dispatchReady = true;
            } else {
                state = ReadinessState.WAITING_FOR_BLOCKERS;
                logWaiting = true;
                snapshot = List.copyOf(pending);
            }
        }
        if (logWaiting) {
            logger.info("INY is waiting for readiness blockers: " + describe(snapshot));
        }
        if (dispatchReady) {
            dispatchReadyNotification();
        }
    }

    /** Registers a late-safe callback for the successful INY-ready transition. */
    void registerReadyCallback(Runnable callback) {
        Objects.requireNonNull(callback, "callback");
        boolean schedule = false;
        synchronized (monitor) {
            if (state == ReadinessState.FAILED) {
                return;
            }
            if (!notificationPublished) {
                readyCallbacks.add(callback);
            } else {
                schedule = true;
            }
        }
        if (schedule) {
            executeCallback(callback);
        }
    }

    /** Resolves pending blockers belonging to a plugin that Bukkit has disabled. */
    void handlePluginDisable(Plugin disabledPlugin) {
        Objects.requireNonNull(disabledPlugin, "disabledPlugin");
        List<Blocker> affected;
        synchronized (monitor) {
            affected = pending.stream()
                    .filter(blocker -> blocker.plugin() == disabledPlugin)
                    .toList();
        }
        if (affected.isEmpty()) {
            return;
        }
        logger.warning("Plugin '" + pluginName(disabledPlugin) + "' was disabled while owning INY readiness blockers: "
                + describe(affected));
        IllegalStateException cause = new IllegalStateException(
                "Owning plugin '" + pluginName(disabledPlugin) + "' was disabled");
        affected.forEach(blocker -> blocker.fail(cause));
    }

    /** Releases pending state when the owning INY plugin is disabled. */
    void shutdown() {
        List<Blocker> affected;
        synchronized (monitor) {
            if (state == ReadinessState.FAILED && registrationClosed) {
                return;
            }
            registrationClosed = true;
            state = ReadinessState.FAILED;
            readyCallbacks.clear();
            affected = List.copyOf(pending);
        }
        IllegalStateException cause = new IllegalStateException("The owning INY plugin was disabled");
        affected.forEach(blocker -> blocker.fail(cause));
    }

    private void timeout(Blocker blocker) {
        transition(blocker, ReadinessBlockerState.TIMED_OUT,
                new TimeoutException("Readiness blocker timed out after " + blocker.timeout()));
    }

    private void transition(Blocker blocker, ReadinessBlockerState terminalState, Throwable cause) {
        ReadinessScheduler.Cancellable timeoutTask;
        boolean dispatchReady = false;
        boolean failReadiness = false;
        synchronized (monitor) {
            if (blocker.state != ReadinessBlockerState.PENDING) {
                return;
            }
            blocker.state = terminalState;
            blocker.failureCause = cause;
            pending.remove(blocker);
            timeoutTask = blocker.timeoutTask;
            blocker.timeoutTask = null;

            if (terminalState != ReadinessBlockerState.COMPLETED
                    && blocker.timeoutPolicy() == TimeoutPolicy.FAIL_READINESS) {
                state = ReadinessState.FAILED;
                readyCallbacks.clear();
                failReadiness = true;
            } else if (registrationClosed && pending.isEmpty() && state != ReadinessState.FAILED) {
                state = ReadinessState.READY;
                dispatchReady = true;
            }
        }
        if (timeoutTask != null) {
            timeoutTask.cancel();
        }
        logTransition(blocker, terminalState, cause, failReadiness);
        if (dispatchReady) {
            dispatchReadyNotification();
        }
    }

    private void dispatchReadyNotification() {
        synchronized (monitor) {
            if (state != ReadinessState.READY || notificationScheduled || notificationPublished) {
                return;
            }
            notificationScheduled = true;
        }
        scheduler.executeOnMain(this::publishReadyNotification);
    }

    private void publishReadyNotification() {
        List<Runnable> callbacks;
        synchronized (monitor) {
            if (state != ReadinessState.READY || notificationPublished) {
                return;
            }
            notificationPublished = true;
            callbacks = List.copyOf(readyCallbacks);
            readyCallbacks.clear();
        }
        try {
            readyAction.run();
        } catch (RuntimeException exception) {
            logger.log(Level.SEVERE, "INY ready notification failed", exception);
        }
        callbacks.forEach(this::executeCallbackNow);
        logger.info("All INY readiness blockers have resolved; INY is ready");
    }

    private void executeCallback(Runnable callback) {
        scheduler.executeOnMain(() -> executeCallbackNow(callback));
    }

    private void executeCallbackNow(Runnable callback) {
        try {
            callback.run();
        } catch (RuntimeException exception) {
            logger.log(Level.SEVERE, "An INY ready callback failed", exception);
        }
    }

    private void logTransition(Blocker blocker, ReadinessBlockerState terminalState,
                               Throwable cause, boolean failReadiness) {
        if (terminalState == ReadinessBlockerState.COMPLETED) {
            return;
        }
        String details = "INY readiness blocker '" + blocker.name() + "' owned by plugin '"
                + blocker.ownerName + "'";
        if (terminalState == ReadinessBlockerState.TIMED_OUT) {
            String message = details + " timed out after " + blocker.timeout() + ".";
            if (failReadiness) {
                logger.log(Level.SEVERE, message + " INY readiness has failed.", cause);
            } else {
                logger.log(Level.WARNING, message + " Continuing INY readiness.", cause);
            }
            return;
        }
        if (failReadiness) {
            logger.log(Level.SEVERE, details + " failed. INY readiness has failed.", cause);
        } else {
            logger.log(Level.WARNING, details + " failed. Continuing INY readiness.", cause);
        }
    }

    private void ensureRegistrationOpen() {
        if (state != ReadinessState.REGISTRATION_OPEN || registrationClosed) {
            throw new IllegalStateException(
                    "INY readiness blocker registration is closed (state: " + state + ")");
        }
    }

    private static String validateName(String name) {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Readiness blocker name cannot be blank");
        }
        return name;
    }

    private String describe(List<Blocker> blockers) {
        StringBuilder description = new StringBuilder();
        for (Blocker blocker : blockers) {
            if (description.length() > 0) {
                description.append(", ");
            }
            description.append("'").append(blocker.name()).append("' (").append(blocker.ownerName).append(")");
        }
        return description.isEmpty() ? "none" : description.toString();
    }

    private static String pluginName(Plugin plugin) {
        String name = plugin.getName();
        return name == null || name.isBlank() ? plugin.getClass().getSimpleName() : name;
    }

    private final class Blocker implements ReadinessBlocker {

        private final java.lang.ref.WeakReference<Plugin> plugin;
        private final String ownerName;
        private final String name;
        private final Instant createdAt = Instant.now();
        private final Duration timeout;
        private final TimeoutPolicy timeoutPolicy;

        private volatile ReadinessBlockerState state = ReadinessBlockerState.PENDING;
        private volatile Throwable failureCause;
        private volatile ReadinessScheduler.Cancellable timeoutTask;

        private Blocker(Plugin plugin, String name, Duration timeout, TimeoutPolicy timeoutPolicy) {
            this.plugin = new java.lang.ref.WeakReference<>(plugin);
            this.ownerName = pluginName(plugin);
            this.name = name;
            this.timeout = timeout;
            this.timeoutPolicy = timeoutPolicy;
        }

        @Override
        public void complete() {
            transition(this, ReadinessBlockerState.COMPLETED, null);
        }

        @Override
        public void fail(Throwable cause) {
            transition(this, ReadinessBlockerState.FAILED, Objects.requireNonNull(cause, "cause"));
        }

        @Override
        public boolean isResolved() {
            return state != ReadinessBlockerState.PENDING;
        }

        @Override
        public Plugin plugin() {
            return plugin.get();
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Instant createdAt() {
            return createdAt;
        }

        @Override
        public Duration timeout() {
            return timeout;
        }

        @Override
        public TimeoutPolicy timeoutPolicy() {
            return timeoutPolicy;
        }

        @Override
        public ReadinessBlockerState state() {
            return state;
        }

        @Override
        public Optional<Throwable> failureCause() {
            return Optional.ofNullable(failureCause);
        }
    }
}
