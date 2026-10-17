package net.iridiummc.iny;

import net.iridiummc.iny.api.Iny;
import net.iridiummc.iny.api.InyIdentifier;
import net.iridiummc.iny.exception.InyMissingContextValueException;
import net.iridiummc.iny.exception.InyUnknownContextKeyException;
import net.iridiummc.iny.runtime.InyContextKey;
import net.iridiummc.iny.runtime.InyRuntimeContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InyRuntimeContextTest {

    private static final InyContextKey<String> MESSAGE =
            InyContextKey.of("test:message", String.class);

    @Test
    void runtimeContextStoresAndRetrievesTypedValues() {
        InyRuntimeContext context = InyRuntimeContext.builder().put(MESSAGE, "hello").build();

        assertEquals("hello", context.get(MESSAGE));
        assertEquals("hello", context.getUnchecked(MESSAGE));
        assertEquals("hello", context.find(MESSAGE).orElseThrow());
        assertTrue(context.contains(MESSAGE));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void runtimeContextRejectsWrongValueTypes() {
        InyContextKey raw = MESSAGE;
        assertThrows(ClassCastException.class,
                () -> InyRuntimeContext.builder().put(raw, 12));
    }

    @Test
    void runtimeContextThrowsForMissingValues() {
        InyMissingContextValueException failure = assertThrows(
                InyMissingContextValueException.class,
                () -> InyRuntimeContext.empty().get(MESSAGE));

        assertEquals(MESSAGE, failure.key());
        assertTrue(failure.getMessage().contains("test:message"));
        assertTrue(failure.getMessage().contains("java.lang.String"));
        assertFalse(InyRuntimeContext.empty().contains(MESSAGE));
    }

    @Test
    void runtimeContextIsImmutableAfterBuild() {
        InyRuntimeContext.Builder builder = InyRuntimeContext.builder().put(MESSAGE, "first");
        InyRuntimeContext first = builder.build();
        builder.put(MESSAGE, "second");

        assertEquals("first", first.get(MESSAGE));
        assertEquals("second", builder.build().get(MESSAGE));
    }

    @Test
    void derivedRuntimeContextDoesNotMutateItsParent() {
        InyRuntimeContext parent = InyRuntimeContext.builder().put(MESSAGE, "parent").build();
        InyRuntimeContext child = parent.with(MESSAGE, "child");

        assertEquals("parent", parent.get(MESSAGE));
        assertEquals("child", child.get(MESSAGE));
    }

    @Test
    void sameIdentifierCannotSilentlyUseDifferentTypes() {
        InyContextKey<Integer> incompatible = InyContextKey.of("test:message", Integer.class);
        InyRuntimeContext context = InyRuntimeContext.builder().put(MESSAGE, "value").build();

        assertThrows(IllegalArgumentException.class, () -> context.contains(incompatible));
        assertThrows(IllegalArgumentException.class, () -> context.with(incompatible, 1));
    }

    @Test
    void serviceExposesAnImmutableTypedContextKeyRegistry() {
        Iny iny = Iny.builder().registerContextKey(MESSAGE).build();

        assertEquals(MESSAGE, iny.contextKeys().require(InyIdentifier.parse("test:message")));
        assertThrows(UnsupportedOperationException.class,
                () -> iny.contextKeys().entries().clear());
        assertThrows(InyUnknownContextKeyException.class,
                () -> iny.contextKeys().require(InyIdentifier.parse("test:missing")));
    }

    @Test
    void duplicateContextKeyIdentifiersAreRejected() {
        Iny.Builder builder = Iny.builder().registerContextKey(MESSAGE);

        assertThrows(IllegalArgumentException.class, () -> builder.registerContextKey(MESSAGE));
        assertThrows(IllegalArgumentException.class, () -> builder.registerContextKey(
                InyContextKey.of("test:message", Integer.class)));
    }
}
