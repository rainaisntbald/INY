package net.iridiummc.iny;

import net.iridiummc.iny.api.Iny;
import net.iridiummc.iny.api.InyConfig;
import net.iridiummc.iny.exception.InyDuplicateFactoryException;
import net.iridiummc.iny.runtime.InyContextKey;
import net.iridiummc.iny.runtime.InyProvider;
import net.iridiummc.iny.runtime.InyRunnable;
import net.iridiummc.iny.runtime.InyRuntimeContext;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InyRuntimeFactoryTest {

    @Test
    void registersAndResolvesRunnable() {
        AtomicInteger calls = new AtomicInteger();
        InyConfig config = Iny.builder()
                .registerRunnable("test:action", arguments -> context -> calls.incrementAndGet())
                .build().parse("action: test:action()");

        InyRunnable action = config.get("action", InyRunnable.class);
        assertEquals(0, calls.get());
        action.run(InyRuntimeContext.empty());
        assertEquals(1, calls.get());
    }

    @Test
    void registersAndResolvesProvider() {
        InyConfig config = Iny.builder()
                .registerProvider("test:value", arguments -> context -> "provided")
                .build().parse("value: test:value()");

        @SuppressWarnings("unchecked")
        InyProvider<String> provider = config.get("value", InyProvider.class);
        assertEquals("provided", provider.resolve(InyRuntimeContext.empty()));
    }

    @Test
    void providerAndRunnableReceiveRuntimeContext() {
        InyContextKey<String> key = InyContextKey.of("test:value", String.class);
        AtomicReference<InyRuntimeContext> runnableContext = new AtomicReference<>();
        InyConfig config = Iny.builder()
                .registerProvider("test:provider", arguments -> context -> context.get(key))
                .registerRunnable("test:runnable", arguments -> runnableContext::set)
                .build().parse("provider: test:provider()\nrunnable: test:runnable()");
        InyRuntimeContext runtime = InyRuntimeContext.builder().put(key, "contextual").build();

        @SuppressWarnings("unchecked")
        InyProvider<String> provider = config.get("provider", InyProvider.class);
        assertEquals("contextual", provider.resolve(runtime));
        config.get("runnable", InyRunnable.class).run(runtime);
        assertEquals(runtime, runnableContext.get());
    }

    @Test
    void providerCanBeRetrievedAndUsedAsRunnable() {
        AtomicInteger calls = new AtomicInteger();
        InyConfig config = Iny.builder()
                .registerProvider("test:value", arguments -> context -> calls.incrementAndGet())
                .build().parse("value: test:value()");

        InyRunnable runnable = config.get("value", InyRunnable.class);
        assertInstanceOf(InyProvider.class, runnable);
        runnable.run(InyRuntimeContext.empty());
        assertEquals(1, calls.get());
    }

    @Test
    void duplicateRuntimeRegistrationIsRejectedAndReplacementIsExplicit() {
        Iny.Builder runnableBuilder = Iny.builder()
                .registerRunnable("test:action", arguments -> context -> { });
        assertThrows(InyDuplicateFactoryException.class,
                () -> runnableBuilder.registerRunnable("test:action", arguments -> context -> { }));
        InyRunnable replacedRunnable = runnableBuilder
                .replaceRunnable("test:action", arguments -> context -> { })
                .build().parse("value: test:action()").get("value", InyRunnable.class);
        assertInstanceOf(InyRunnable.class, replacedRunnable);

        Iny.Builder providerBuilder = Iny.builder()
                .registerProvider("test:value", arguments -> context -> "first");
        assertThrows(InyDuplicateFactoryException.class,
                () -> providerBuilder.registerProvider("test:value", arguments -> context -> "second"));
        @SuppressWarnings("unchecked")
        InyProvider<String> replacedProvider = providerBuilder
                .replaceProvider("test:value", arguments -> context -> "replacement")
                .build().parse("value: test:value()").get("value", InyProvider.class);
        assertEquals("replacement", replacedProvider.resolve(InyRuntimeContext.empty()));
    }
}
