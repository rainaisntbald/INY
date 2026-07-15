package net.iridiummc.iny;

import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.Objects;

/** Bukkit scheduler adapter used by the live INY plugin. */
final class BukkitReadinessScheduler implements ReadinessScheduler {

    private final Plugin plugin;
    private final BukkitScheduler scheduler;

    BukkitReadinessScheduler(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.scheduler = plugin.getServer() == null ? null : plugin.getServer().getScheduler();
    }

    @Override
    public Cancellable scheduleTimeout(Runnable task, Duration timeout) {
        Objects.requireNonNull(task, "task");
        if (scheduler == null) {
            return () -> { };
        }
        BukkitTask scheduled = scheduler.runTaskLater(plugin, task, toTicks(timeout));
        return scheduled::cancel;
    }

    @Override
    public void executeOnMain(Runnable task) {
        Objects.requireNonNull(task, "task");
        if (scheduler == null) {
            task.run();
            return;
        }
        scheduler.runTask(plugin, task);
    }

    private static long toTicks(Duration duration) {
        long millis;
        try {
            millis = duration.toMillis();
        } catch (ArithmeticException exception) {
            return Long.MAX_VALUE;
        }
        if (millis >= Long.MAX_VALUE - 49) {
            return Long.MAX_VALUE;
        }
        return Math.max(1, (millis + 49) / 50);
    }
}
