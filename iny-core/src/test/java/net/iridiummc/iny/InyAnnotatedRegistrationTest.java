package net.iridiummc.iny;

import net.iridiummc.iny.annotation.InyFactory;
import net.iridiummc.iny.annotation.InyProvider;
import net.iridiummc.iny.annotation.InyRunnable;
import net.iridiummc.iny.api.Iny;
import net.iridiummc.iny.api.InyConfig;
import net.iridiummc.iny.api.InyIdentifier;
import net.iridiummc.iny.exception.InyArgumentCountException;
import net.iridiummc.iny.exception.InyDuplicateFactoryException;
import net.iridiummc.iny.exception.InyFactoryArgumentException;
import net.iridiummc.iny.runtime.InyRuntimeContext;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class InyAnnotatedRegistrationTest {

    @Test
    void registersAndInvokesPrivateRunnableWithDeclaredArgumentOrder() {
        RunnableLibrary library = new RunnableLibrary();
        InyConfig config = Iny.builder().registerAnnotated(library).build().parse(
                "action: annotated:capture(7, \"message\")");

        assertNull(library.received.get());
        config.getRunnable("action").run(InyRuntimeContext.empty());
        assertEquals("7:message", library.received.get());
    }

    @Test
    void registersAndInvokesFactoryWithInferredOutputTypeAndPrimitiveParameters() {
        Iny iny = Iny.builder().registerAnnotated(new FactoryLibrary()).build();

        NumberPair pair = iny.parse("value: annotated:pair(3, 2.5)").get("value", NumberPair.class);
        assertEquals(new NumberPair(3, 2.5), pair);
        assertEquals(NumberPair.class, iny.factories()
                .find(InyIdentifier.parse("annotated:pair")).orElseThrow().resultType());
    }

    @Test
    void registersAndInvokesDeferredProvider() {
        ProviderLibrary library = new ProviderLibrary();
        InyConfig config = Iny.builder().registerAnnotated(library).build().parse(
                "value: annotated:length(\"deferred\")");

        assertEquals(0, library.calls.get());
        assertEquals(8, config.getProvider("value", int.class).resolve(InyRuntimeContext.empty()));
        assertEquals(1, library.calls.get());
    }

    @Test
    void annotationIdentifierIsRuntimeVisibleAndUsedVerbatim() throws ReflectiveOperationException {
        Method method = FactoryLibrary.class.getDeclaredMethod("pair", int.class, double.class);
        assertEquals("annotated:pair", method.getAnnotation(InyFactory.class).value());

        Iny iny = Iny.builder().registerAnnotated(new FactoryLibrary()).build();
        assertTrue(iny.factories().contains(InyIdentifier.parse("annotated:pair")));
    }

    @Test
    void invalidRunnableReturnTypeFailsDuringRegistrationWithContext() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> Iny.builder().registerAnnotated(new InvalidRunnableLibrary()));

        assertInvalidDiagnostic(failure, InvalidRunnableLibrary.class, "invalidRunnable",
                "@InyRunnable(\"annotated:invalid_runnable\")", "must return void");
    }

    @Test
    void voidFactoryAndProviderFailDuringRegistration() {
        IllegalArgumentException factoryFailure = assertThrows(IllegalArgumentException.class,
                () -> Iny.builder().registerAnnotated(new VoidFactoryLibrary()));
        assertInvalidDiagnostic(factoryFailure, VoidFactoryLibrary.class, "voidFactory",
                "@InyFactory(\"annotated:void_factory\")", "must return a value");

        IllegalArgumentException providerFailure = assertThrows(IllegalArgumentException.class,
                () -> Iny.builder().registerAnnotated(new VoidProviderLibrary()));
        assertInvalidDiagnostic(providerFailure, VoidProviderLibrary.class, "voidProvider",
                "@InyProvider(\"annotated:void_provider\")", "must return a value");
    }

    @Test
    void multipleRegistrationAnnotationsOnOneMethodAreRejected() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> Iny.builder().registerAnnotated(new MultipleAnnotationsLibrary()));

        assertInvalidDiagnostic(failure, MultipleAnnotationsLibrary.class, "ambiguous",
                "@InyFactory(\"annotated:factory\")", "at most one");
        assertTrue(failure.getMessage().contains("@InyProvider(\"annotated:provider\")"));
    }

    @Test
    void wrongArgumentCountUsesExistingFactoryValidation() {
        InyConfig config = Iny.builder().registerAnnotated(new FactoryLibrary()).build()
                .parse("value: annotated:pair(1)");

        InyArgumentCountException failure = assertThrows(InyArgumentCountException.class,
                () -> config.get("value", NumberPair.class));
        assertEquals(2, failure.minimum());
        assertEquals(2, failure.maximum());
        assertEquals(1, failure.actual());
    }

    @Test
    void wrongArgumentTypeUsesExistingTypedArgumentValidation() {
        InyConfig config = Iny.builder().registerAnnotated(new FactoryLibrary()).build()
                .parse("value: annotated:pair(\"wrong\", 2)");

        InyFactoryArgumentException failure = assertThrows(InyFactoryArgumentException.class,
                () -> config.get("value", NumberPair.class));
        assertEquals(0, failure.argumentIndex());
        assertEquals(int.class, failure.requestedType());
    }

    @Test
    void nullIsRejectedForPrimitiveRunnableParameter() {
        RunnableLibrary library = new RunnableLibrary();
        InyConfig config = Iny.builder().registerAnnotated(library).build()
                .parse("action: annotated:capture(null, \"message\")");

        InyFactoryArgumentException failure = assertThrows(InyFactoryArgumentException.class,
                () -> config.getRunnable("action"));
        assertEquals(0, failure.argumentIndex());
        assertEquals(int.class, failure.requestedType());
        assertNull(library.received.get());
    }

    @Test
    void invocationTargetRuntimeExceptionIsUnwrapped() {
        IllegalStateException cause = new IllegalStateException("original failure");
        ThrowingLibrary library = new ThrowingLibrary(cause);
        net.iridiummc.iny.runtime.InyRunnable runnable = Iny.builder()
                .registerAnnotated(library)
                .build()
                .parse("action: annotated:throwing()")
                .getRunnable("action");

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> runnable.run(InyRuntimeContext.empty()));
        assertSame(cause, failure);
    }

    @Test
    void duplicateIdentifierUsesExistingDuplicateRegistrationException() {
        Iny.Builder builder = Iny.builder()
                .registerFactory("annotated:pair", NumberPair.class, context -> new NumberPair(0, 0));

        InyDuplicateFactoryException failure = assertThrows(InyDuplicateFactoryException.class,
                () -> builder.registerAnnotated(new FactoryLibrary()));
        assertEquals(InyIdentifier.parse("annotated:pair"), failure.identifier());
    }

    @Test
    void unannotatedMethodsAreIgnored() {
        IgnoredLibrary library = new IgnoredLibrary();
        Iny iny = Iny.builder().registerAnnotated(library).build();

        assertFalse(iny.factories().contains(InyIdentifier.parse("annotated:ignored")));
        assertEquals(0, library.calls.get());
    }

    private static void assertInvalidDiagnostic(
            IllegalArgumentException failure,
            Class<?> owner,
            String method,
            String annotation,
            String reason
    ) {
        assertTrue(failure.getMessage().contains(owner.getTypeName()));
        assertTrue(failure.getMessage().contains("#" + method + "("));
        assertTrue(failure.getMessage().contains(annotation));
        assertTrue(failure.getMessage().contains(reason));
    }

    private static final class RunnableLibrary {
        private final AtomicReference<String> received = new AtomicReference<>();

        @InyRunnable("annotated:capture")
        private void capture(int number, String message) {
            received.set(number + ":" + message);
        }
    }

    private static final class FactoryLibrary {
        @InyFactory("annotated:pair")
        private NumberPair pair(int first, double second) {
            return new NumberPair(first, second);
        }
    }

    private static final class ProviderLibrary {
        private final AtomicInteger calls = new AtomicInteger();

        @InyProvider("annotated:length")
        private int length(String value) {
            calls.incrementAndGet();
            return value.length();
        }
    }

    private static final class InvalidRunnableLibrary {
        @InyRunnable("annotated:invalid_runnable")
        private String invalidRunnable() {
            return "invalid";
        }
    }

    private static final class VoidFactoryLibrary {
        @InyFactory("annotated:void_factory")
        private void voidFactory() {
        }
    }

    private static final class VoidProviderLibrary {
        @InyProvider("annotated:void_provider")
        private void voidProvider() {
        }
    }

    private static final class MultipleAnnotationsLibrary {
        @InyFactory("annotated:factory")
        @InyProvider("annotated:provider")
        private String ambiguous() {
            return "invalid";
        }
    }

    private record ThrowingLibrary(IllegalStateException cause) {

        @InyRunnable("annotated:throwing")
            private void throwing() {
                throw cause;
            }
        }

    private static final class IgnoredLibrary {
        private final AtomicInteger calls = new AtomicInteger();

        private void ignored() {
            calls.incrementAndGet();
        }
    }

    private record NumberPair(int first, double second) {
    }
}
