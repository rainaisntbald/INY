package net.iridiummc.iny;

import net.iridiummc.iny.readiness.ReadinessBlocker;
import net.iridiummc.iny.readiness.ReadinessBlockerState;
import net.iridiummc.iny.readiness.ReadinessState;
import net.iridiummc.iny.readiness.TimeoutPolicy;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReadinessControllerTest {

    @Test
    void noBlockersBecomesReadyOnceRegistrationCloses() {
        TestScheduler scheduler = new TestScheduler();
        AtomicInteger ready = new AtomicInteger();
        ReadinessController controller = controller(scheduler, ready::incrementAndGet);

        controller.closeRegistration();

        assertEquals(ReadinessState.READY, controller.state());
        assertEquals(0, ready.get());
        scheduler.runMainTasks();
        scheduler.runMainTasks();
        assertEquals(1, ready.get());
    }

    @Test
    void oneBlockerKeepsInyWaitingUntilItCompletes() {
        TestScheduler scheduler = new TestScheduler();
        AtomicInteger ready = new AtomicInteger();
        ReadinessController controller = controller(scheduler, ready::incrementAndGet);
        ReadinessBlocker blocker = controller.registerBlocker(plugin("integration"), "items");

        controller.closeRegistration();
        assertEquals(ReadinessState.WAITING_FOR_BLOCKERS, controller.state());
        assertFalse(blocker.isResolved());
        assertEquals(0, ready.get());

        blocker.complete();
        assertEquals(ReadinessBlockerState.COMPLETED, blocker.state());
        scheduler.runMainTasks();
        assertEquals(1, ready.get());
    }

    @Test
    void multipleBlockersRequireEveryBlocker() {
        TestScheduler scheduler = new TestScheduler();
        AtomicInteger ready = new AtomicInteger();
        ReadinessController controller = controller(scheduler, ready::incrementAndGet);
        ReadinessBlocker first = controller.registerBlocker(plugin("first"), "first");
        ReadinessBlocker second = controller.registerBlocker(plugin("second"), "second");
        controller.closeRegistration();

        first.complete();
        scheduler.runMainTasks();
        assertEquals(0, ready.get());
        assertEquals(1, controller.pendingBlockers().size());

        second.complete();
        scheduler.runMainTasks();
        assertEquals(1, ready.get());
    }

    @Test
    void terminalTransitionsAreIdempotentAndFirstOneWins() {
        TestScheduler scheduler = new TestScheduler();
        ReadinessController controller = controller(scheduler, () -> { });
        ReadinessBlocker blocker = controller.registerBlocker(plugin("integration"), "items");
        RuntimeException failure = new RuntimeException("late failure");

        blocker.complete();
        blocker.complete();
        blocker.fail(failure);

        assertEquals(ReadinessBlockerState.COMPLETED, blocker.state());
        assertTrue(blocker.isResolved());
        assertTrue(blocker.failureCause().isEmpty());
        assertTrue(controller.pendingBlockers().isEmpty());
    }

    @Test
    void continueOnTimeoutAllowsReadinessToContinue() {
        TestScheduler scheduler = new TestScheduler();
        AtomicInteger ready = new AtomicInteger();
        ReadinessController controller = controller(scheduler, ready::incrementAndGet);
        ReadinessBlocker blocker = controller.registerBlocker(
                plugin("integration"), "items", Duration.ofSeconds(2), TimeoutPolicy.CONTINUE_WITH_WARNING);
        controller.closeRegistration();

        scheduler.runTimeouts();
        assertEquals(ReadinessBlockerState.TIMED_OUT, blocker.state());
        assertEquals(ReadinessState.READY, controller.state());
        scheduler.runMainTasks();
        assertEquals(1, ready.get());
    }

    @Test
    void failOnTimeoutSuppressesSuccessfulReadiness() {
        TestScheduler scheduler = new TestScheduler();
        AtomicInteger ready = new AtomicInteger();
        AtomicInteger callbacks = new AtomicInteger();
        ReadinessController controller = controller(scheduler, ready::incrementAndGet);
        ReadinessBlocker blocker = controller.registerBlocker(
                plugin("integration"), "items", Duration.ofSeconds(2), TimeoutPolicy.FAIL_READINESS);
        controller.registerReadyCallback(callbacks::incrementAndGet);
        controller.closeRegistration();

        scheduler.runTimeouts();
        scheduler.runMainTasks();

        assertEquals(ReadinessBlockerState.TIMED_OUT, blocker.state());
        assertEquals(ReadinessState.FAILED, controller.state());
        assertEquals(0, ready.get());
        assertEquals(0, callbacks.get());
    }

    @Test
    void explicitFailureUsesTheConfiguredPolicy() {
        TestScheduler warningScheduler = new TestScheduler();
        AtomicInteger warningReady = new AtomicInteger();
        ReadinessController warningController = controller(warningScheduler, warningReady::incrementAndGet);
        ReadinessBlocker warningBlocker = warningController.registerBlocker(
                plugin("warning"), "optional", Duration.ofSeconds(1), TimeoutPolicy.CONTINUE_WITH_WARNING);
        warningController.closeRegistration();
        warningBlocker.fail(new IllegalStateException("optional integration failed"));
        warningScheduler.runMainTasks();
        assertEquals(ReadinessState.READY, warningController.state());
        assertEquals(1, warningReady.get());

        TestScheduler failingScheduler = new TestScheduler();
        AtomicInteger failingReady = new AtomicInteger();
        ReadinessController failingController = controller(failingScheduler, failingReady::incrementAndGet);
        ReadinessBlocker failingBlocker = failingController.registerBlocker(
                plugin("required"), "required", Duration.ofSeconds(1), TimeoutPolicy.FAIL_READINESS);
        failingController.closeRegistration();
        failingBlocker.fail(new IllegalStateException("required integration failed"));
        failingScheduler.runMainTasks();
        assertEquals(ReadinessState.FAILED, failingController.state());
        assertEquals(0, failingReady.get());
    }

    @Test
    void concurrentCompletionEmitsReadinessExactlyOnce() throws InterruptedException {
        TestScheduler scheduler = new TestScheduler();
        AtomicInteger ready = new AtomicInteger();
        ReadinessController controller = controller(scheduler, ready::incrementAndGet);
        List<ReadinessBlocker> blockers = new ArrayList<>();
        for (int index = 0; index < 12; index++) {
            blockers.add(controller.registerBlocker(plugin("integration-" + index), "blocker-" + index));
        }
        controller.closeRegistration();

        CountDownLatch start = new CountDownLatch(1);
        List<Thread> threads = blockers.stream()
                .map(blocker -> new Thread(() -> {
                    await(start);
                    blocker.complete();
                }))
                .toList();
        threads.forEach(Thread::start);
        start.countDown();
        for (Thread thread : threads) {
            thread.join(2_000);
            assertFalse(thread.isAlive());
        }

        scheduler.runMainTasks();
        assertEquals(ReadinessState.READY, controller.state());
        assertEquals(1, ready.get());
    }

    @Test
    void registrationAfterClosureIsRejected() {
        ReadinessController controller = controller(new TestScheduler(), () -> { });
        controller.closeRegistration();

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> controller.registerBlocker(plugin("late"), "late"));
        assertTrue(exception.getMessage().contains("registration is closed"));
    }

    @Test
    void lateReadyCallbacksAreScheduledAndStillRun() {
        TestScheduler scheduler = new TestScheduler();
        ReadinessController controller = controller(scheduler, () -> { });
        controller.closeRegistration();
        scheduler.runMainTasks();

        AtomicInteger callbacks = new AtomicInteger();
        controller.registerReadyCallback(callbacks::incrementAndGet);
        assertEquals(0, callbacks.get());
        scheduler.runMainTasks();
        assertEquals(1, callbacks.get());
    }

    @Test
    void disablingAnOwnerResolvesItsPendingBlocker() {
        TestScheduler scheduler = new TestScheduler();
        AtomicInteger ready = new AtomicInteger();
        Plugin owner = plugin("integration");
        ReadinessController controller = controller(scheduler, ready::incrementAndGet);
        ReadinessBlocker blocker = controller.registerBlocker(owner, "items");
        controller.closeRegistration();

        controller.handlePluginDisable(owner);
        scheduler.runMainTasks();

        assertEquals(ReadinessBlockerState.FAILED, blocker.state());
        assertEquals(ReadinessState.READY, controller.state());
        assertEquals(1, ready.get());
    }

    @Test
    void blockerRetainsUsefulDiagnostics() {
        TestScheduler scheduler = new TestScheduler();
        Plugin owner = plugin("integration");
        ReadinessController controller = controller(scheduler, () -> { });
        Duration timeout = Duration.ofSeconds(4);
        ReadinessBlocker blocker = controller.registerBlocker(owner, "items", timeout, TimeoutPolicy.FAIL_READINESS);

        assertSame(owner, blocker.plugin());
        assertEquals("items", blocker.name());
        assertNotNull(blocker.createdAt());
        assertEquals(timeout, blocker.timeout());
        assertEquals(TimeoutPolicy.FAIL_READINESS, blocker.timeoutPolicy());
        assertEquals(ReadinessBlockerState.PENDING, blocker.state());
    }

    private static ReadinessController controller(TestScheduler scheduler, Runnable readyAction) {
        ReadinessController controller = new ReadinessController(plugin("INY"), scheduler, readyAction);
        controller.openRegistration();
        return controller;
    }

    private static Plugin plugin(String name) {
        Logger logger = Logger.getLogger("INY-test-" + name);
        return proxy(Plugin.class, (method, arguments) -> switch (method.getName()) {
            case "getName" -> name;
            case "getLogger" -> logger;
            default -> defaultValue(method.getReturnType());
        });
    }

    private static void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(2, TimeUnit.SECONDS));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }

    private static final class TestScheduler implements ReadinessScheduler {

        private final Queue<Runnable> mainTasks = new ConcurrentLinkedQueue<>();
        private final List<TestTask> timeoutTasks = new ArrayList<>();

        @Override
        public synchronized Cancellable scheduleTimeout(Runnable task, Duration timeout) {
            TestTask scheduled = new TestTask(task);
            timeoutTasks.add(scheduled);
            return scheduled;
        }

        @Override
        public void executeOnMain(Runnable task) {
            mainTasks.add(task);
        }

        private void runMainTasks() {
            Runnable task;
            while ((task = mainTasks.poll()) != null) {
                task.run();
            }
        }

        private synchronized void runTimeouts() {
            timeoutTasks.forEach(TestTask::run);
        }

        private static final class TestTask implements Cancellable {
            private final Runnable task;
            private boolean cancelled;

            private TestTask(Runnable task) {
                this.task = task;
            }

            @Override
            public synchronized void cancel() {
                cancelled = true;
            }

            private synchronized void run() {
                if (!cancelled) {
                    task.run();
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Invocation invocation) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, arguments) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> type.getSimpleName() + " test proxy";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == arguments[0];
                            default -> null;
                        };
                    }
                    return invocation.invoke(method, arguments);
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0.0f;
        if (type == double.class) return 0.0d;
        return null;
    }

    @FunctionalInterface
    private interface Invocation {
        Object invoke(java.lang.reflect.Method method, Object[] arguments);
    }
}
