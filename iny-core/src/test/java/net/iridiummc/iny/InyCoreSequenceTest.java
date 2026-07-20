package net.iridiummc.iny;

import net.iridiummc.iny.api.Iny;
import net.iridiummc.iny.exception.InyFactoryArgumentException;
import net.iridiummc.iny.runtime.InyContextKey;
import net.iridiummc.iny.runtime.InyRunnable;
import net.iridiummc.iny.runtime.InyRuntimeContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InyCoreSequenceTest {

    @Test
    void sequenceRunsArbitraryActionsInDeclarationOrder() {
        List<String> calls = new ArrayList<>();
        Iny iny = Iny.builder()
                .registerRunnable("test:record", context -> {
                    String value = context.arguments().get(0, String.class);
                    return runtime -> calls.add(value);
                })
                .registerProvider("test:provider", context -> runtime -> {
                    calls.add("provider");
                    return "discarded";
                })
                .build();

        InyRunnable sequence = iny.parse("""
                action: core:sequence(
                  test:record("first"),
                  test:provider(),
                  core:sequence(
                    test:record("nested")
                  ),
                  test:record("last")
                )
                """).getRunnable("action");

        sequence.run(InyRuntimeContext.empty());

        assertEquals(List.of("first", "provider", "nested", "last"), calls);
    }

    @Test
    void sequencePassesTheSameRuntimeContextToEveryAction() {
        InyContextKey<String> key = InyContextKey.of("test:value", String.class);
        AtomicReference<InyRuntimeContext> first = new AtomicReference<>();
        AtomicReference<InyRuntimeContext> second = new AtomicReference<>();
        Iny iny = Iny.builder()
                .registerRunnable("test:first", context -> first::set)
                .registerRunnable("test:second", context -> second::set)
                .build();
        InyRuntimeContext runtime = InyRuntimeContext.builder().put(key, "value").build();

        iny.parse("action: core:sequence(test:first(), test:second())")
                .getRunnable("action")
                .run(runtime);

        assertSame(runtime, first.get());
        assertSame(runtime, second.get());
    }

    @Test
    void emptySequenceIsANoOp() {
        InyRunnable sequence = Iny.builder().build()
                .parse("action: core:sequence()")
                .getRunnable("action");

        sequence.run(InyRuntimeContext.empty());
    }

    @Test
    void sequenceRejectsNonRunnableArgumentsContextually() {
        InyFactoryArgumentException failure = assertThrows(
                InyFactoryArgumentException.class,
                () -> Iny.builder().build()
                        .parse("action: core:sequence(\"not runnable\")")
                        .getRunnable("action"));

        assertEquals("core:sequence", failure.identifier().toString());
        assertEquals(0, failure.argumentIndex());
    }

    @Test
    void coreNamespaceCannotBeExtendedOrReplaced() {
        IllegalArgumentException registration = assertThrows(
                IllegalArgumentException.class,
                () -> Iny.builder().registerRunnable("core:other", context -> runtime -> { }));
        assertTrue(registration.getMessage().contains("reserved"));

        assertThrows(IllegalArgumentException.class,
                () -> Iny.builder().replaceRunnable("core:sequence", context -> runtime -> { }));
    }
}
