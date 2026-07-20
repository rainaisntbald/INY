package net.iridiummc.iny;

import net.iridiummc.iny.api.Iny;
import net.iridiummc.iny.api.InyConfig;
import net.iridiummc.iny.api.InyIdentifier;
import net.iridiummc.iny.exception.InyFactoryArgumentException;
import net.iridiummc.iny.exception.InyInvalidProviderResultException;
import net.iridiummc.iny.exception.InyNotProviderException;
import net.iridiummc.iny.runtime.InyContextKey;
import net.iridiummc.iny.runtime.InyProvider;
import net.iridiummc.iny.runtime.InyRunnable;
import net.iridiummc.iny.runtime.InyRuntimeContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InyRuntimeCompositionTest {

    @Test
    void providerArgumentsResolveUsingTheOuterRuntimeContext() {
        InyContextKey<String> messageKey = InyContextKey.of("test:message", String.class);
        AtomicReference<String> received = new AtomicReference<>();

        Iny iny = Iny.builder()
                .registerContextKey(messageKey)
                .registerProvider("context:value", context -> {
                    context.arguments().requireCount(1);
                    var identifier = net.iridiummc.iny.api.InyIdentifier.parse(
                            context.arguments().get(0, String.class));
                    var key = context.arguments().contextKeys().require(identifier);
                    return runtime -> runtime.getUnchecked(key);
                })
                .registerRunnable("test:capture", context -> {
                    context.arguments().requireCount(1);
                    InyProvider<String> value = context.arguments().getProvider(0, String.class);
                    return runtime -> received.set(value.resolve(runtime));
                })
                .build();

        InyRunnable action = iny.parse("""
                action: test:capture(
                  context:value("test:message")
                )
                """).getRunnable("action");

        action.run(InyRuntimeContext.builder().put(messageKey, "first").build());
        assertEquals("first", received.get());
        action.run(InyRuntimeContext.builder().put(messageKey, "second").build());
        assertEquals("second", received.get());
    }

    @Test
    void builtInContextProviderResolvesRegisteredKeys() {
        InyContextKey<String> messageKey = InyContextKey.of("test:message", String.class);
        InyConfig config = Iny.builder().registerContextKey(messageKey).build()
                .parse("message: context:value(\"test:message\")");

        assertEquals("value", config.getProvider("message", String.class).resolve(
                InyRuntimeContext.builder().put(messageKey, "value").build()));
    }

    @Test
    void contextProviderIsAvailableWithoutDefaultKeys() {
        Iny iny = Iny.builder().build();

        assertEquals(0, iny.contextKeys().entries().size());
        assertTrue(iny.factories().contains(InyIdentifier.parse("context:value")));
    }

    @Test
    void staticArgumentsAreLiftedIntoConstantProviders() {
        AtomicReference<String> received = new AtomicReference<>();
        InyConfig config = captureService(received).parse("action: test:capture(\"constant\")");

        config.getRunnable("action").run(InyRuntimeContext.empty());

        assertEquals("constant", received.get());
    }

    @Test
    void ordinaryFactoryResultsBecomeConstantProvidersExactlyOnce() {
        AtomicInteger factoryCalls = new AtomicInteger();
        AtomicReference<String> received = new AtomicReference<>();
        InyConfig config = captureBuilder(received)
                .registerFactory("test:text", String.class, context -> {
                    factoryCalls.incrementAndGet();
                    return context.arguments().get(0, String.class);
                })
                .build().parse("action: test:capture(test:text(\"made\"))");

        InyRunnable action = config.getRunnable("action");
        assertEquals(1, factoryCalls.get());
        action.run(InyRuntimeContext.empty());
        action.run(InyRuntimeContext.empty());

        assertEquals(1, factoryCalls.get());
        assertEquals("made", received.get());
    }

    @Test
    void providerCanBePassedIntoAnotherProviderAndIsNotCached() {
        InyContextKey<String> key = InyContextKey.of("test:text", String.class);
        AtomicInteger providerCalls = new AtomicInteger();
        InyConfig config = Iny.builder()
                .registerContextKey(key)
                .registerProvider("test:counted", context -> runtime -> {
                    providerCalls.incrementAndGet();
                    return runtime.get(key);
                })
                .registerProvider("test:decorate", context -> {
                    InyProvider<String> inner = context.arguments().getProvider(0, String.class);
                    return runtime -> "[" + inner.resolve(runtime) + "]";
                })
                .build().parse("value: test:decorate(test:counted())");

        InyProvider<String> value = config.getProvider("value", String.class);
        assertEquals("[one]", value.resolve(InyRuntimeContext.builder().put(key, "one").build()));
        assertEquals("[two]", value.resolve(InyRuntimeContext.builder().put(key, "two").build()));
        assertEquals(2, providerCalls.get());
    }

    @Test
    void multipleProviderArgumentsReceiveTheSameRuntimeContext() {
        InyContextKey<String> key = InyContextKey.of("test:text", String.class);
        AtomicReference<InyRuntimeContext> firstSeen = new AtomicReference<>();
        AtomicReference<InyRuntimeContext> secondSeen = new AtomicReference<>();
        InyConfig config = Iny.builder()
                .registerProvider("test:first", context -> runtime -> {
                    firstSeen.set(runtime);
                    return runtime.get(key);
                })
                .registerProvider("test:second", context -> runtime -> {
                    secondSeen.set(runtime);
                    return runtime.get(key);
                })
                .registerProvider("test:join", context -> {
                    InyProvider<String> first = context.arguments().getProvider(0, String.class);
                    InyProvider<String> second = context.arguments().getProvider(1, String.class);
                    return runtime -> first.resolve(runtime) + second.resolve(runtime);
                })
                .build().parse("value: test:join(test:first(), test:second())");
        InyRuntimeContext runtime = InyRuntimeContext.builder().put(key, "x").build();

        assertEquals("xx", config.getProvider("value", String.class).resolve(runtime));
        assertSame(runtime, firstSeen.get());
        assertSame(runtime, secondSeen.get());
    }

    @Test
    void ordinaryArgumentGetDoesNotImplicitlyResolveProvider() {
        AtomicInteger resolutions = new AtomicInteger();
        InyConfig config = Iny.builder()
                .registerProvider("test:value", context -> runtime -> {
                    resolutions.incrementAndGet();
                    return "value";
                })
                .registerRunnable("test:bad", context -> {
                    context.arguments().get(0, String.class);
                    return runtime -> { };
                })
                .build().parse("action: test:bad(test:value())");

        assertThrows(InyFactoryArgumentException.class, () -> config.getRunnable("action"));
        assertEquals(0, resolutions.get());
    }

    @Test
    void plainRunnableCannotBeRetrievedOrUsedAsAProvider() {
        Iny iny = Iny.builder()
                .registerRunnable("test:action", context -> runtime -> { })
                .registerRunnable("test:outer", context -> {
                    context.arguments().getProvider(0, String.class);
                    return runtime -> { };
                })
                .build();

        InyNotProviderException direct = assertThrows(InyNotProviderException.class,
                () -> iny.parse("value: test:action()").getProvider("value", String.class));
        assertEquals("value", direct.path());

        InyFactoryArgumentException nested = assertThrows(InyFactoryArgumentException.class,
                () -> iny.parse("value: test:outer(test:action())").getRunnable("value"));
        assertInstanceOf(InyNotProviderException.class, nested.getCause());
    }

    @Test
    void providerResultTypeAndNullAreCheckedAtRuntime() {
        InyConfig wrong = Iny.builder()
                .registerProvider("test:wrong", context -> runtime -> "not a number")
                .build().parse("value: test:wrong()");
        InyInvalidProviderResultException wrongFailure = assertThrows(
                InyInvalidProviderResultException.class,
                () -> wrong.getProvider("value", Integer.class).resolve(InyRuntimeContext.empty()));
        assertEquals(Integer.class, wrongFailure.expectedType());
        assertEquals(String.class, wrongFailure.actualType());

        InyConfig nullable = Iny.builder()
                .registerProvider("test:null", context -> runtime -> null)
                .build().parse("value: test:null()");
        InyInvalidProviderResultException nullFailure = assertThrows(
                InyInvalidProviderResultException.class,
                () -> nullable.getProvider("value", String.class).resolve(InyRuntimeContext.empty()));
        assertEquals(null, nullFailure.actualType());
    }

    @Test
    void primitiveAndWrapperProviderTargetsAreConsistent() {
        InyConfig config = Iny.builder()
                .registerProvider("test:number", context -> runtime -> 12)
                .build().parse("value: test:number()");

        assertEquals(12, config.getProvider("value", int.class).resolve(InyRuntimeContext.empty()));
        assertEquals(12, config.getProvider("value", Integer.class).resolve(InyRuntimeContext.empty()));
    }

    @Test
    void nestedProviderFailureRetainsOuterArgumentContext() {
        IllegalStateException cause = new IllegalStateException("failed at runtime");
        InyConfig config = Iny.builder()
                .registerProvider("test:failing", context -> runtime -> { throw cause; })
                .registerProvider("test:outer", context -> {
                    InyProvider<String> inner = context.arguments().getProvider(0, String.class);
                    return inner::resolve;
                })
                .build().parse("value: test:outer(test:failing())");

        InyFactoryArgumentException failure = assertThrows(InyFactoryArgumentException.class,
                () -> config.getProvider("value", String.class).resolve(InyRuntimeContext.empty()));
        assertEquals("test:outer", failure.identifier().toString());
        assertEquals(0, failure.argumentIndex());
        assertSame(cause, failure.getCause());
    }

    @Test
    void ordinaryRetrievalNeverRunsRuntimeValues() {
        AtomicInteger providerCalls = new AtomicInteger();
        AtomicInteger runnableCalls = new AtomicInteger();
        InyConfig config = Iny.builder()
                .registerProvider("test:provider", context -> runtime -> providerCalls.incrementAndGet())
                .registerRunnable("test:runnable", context -> runtime -> runnableCalls.incrementAndGet())
                .build().parse("provider: test:provider()\nrunnable: test:runnable()");

        config.get("provider", Object.class);
        config.get("runnable", Object.class);
        assertEquals(0, providerCalls.get());
        assertEquals(0, runnableCalls.get());
    }

    @Test
    void runnableListsAcceptProvidersAndExecuteInDeclarationOrder() {
        List<String> calls = new ArrayList<>();
        InyConfig config = Iny.builder()
                .registerRunnable("test:run", context -> {
                    String value = context.arguments().get(0, String.class);
                    return runtime -> calls.add(value);
                })
                .registerProvider("test:provide", context -> runtime -> {
                    calls.add("provider");
                    return "discarded";
                })
                .build().parse("""
                        actions:
                          - test:run("first")
                          - test:provide()
                          - test:run("last")
                        """);

        List<InyRunnable> actions = config.getList("actions", InyRunnable.class);
        actions.forEach(action -> action.run(InyRuntimeContext.empty()));
        assertEquals(List.of("first", "provider", "last"), calls);
    }

    @Test
    void runnableListRejectsOrdinaryValues() {
        InyConfig config = Iny.builder().build().parse("""
                actions:
                  - "not runnable"
                """);
        assertThrows(RuntimeException.class,
                () -> config.getList("actions", InyRunnable.class));
    }

    @Test
    void contextNamespaceIsReservedExceptForValueAccess() {
        assertThrows(IllegalArgumentException.class,
                () -> Iny.builder().registerFactory("context:other", String.class, context -> "bad"));
        assertThrows(IllegalArgumentException.class,
                () -> Iny.builder().registerFactory("context:value", String.class, context -> "bad"));
    }

    private static Iny captureService(AtomicReference<String> received) {
        return captureBuilder(received).build();
    }

    private static Iny.Builder captureBuilder(AtomicReference<String> received) {
        return Iny.builder().registerRunnable("test:capture", context -> {
            context.arguments().requireCount(1);
            InyProvider<String> value = context.arguments().getProvider(0, String.class);
            return runtime -> received.set(value.resolve(runtime));
        });
    }
}
