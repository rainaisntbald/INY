package net.iridiummc.iny;

import net.iridiummc.iny.api.Iny;
import net.iridiummc.iny.api.InyConfig;
import net.iridiummc.iny.api.InyIdentifier;
import net.iridiummc.iny.codec.InyDecodeContext;
import net.iridiummc.iny.codec.InyDecoder;
import net.iridiummc.iny.exception.InyArgumentCountException;
import net.iridiummc.iny.exception.InyDuplicateFactoryException;
import net.iridiummc.iny.exception.InyFactoryArgumentException;
import net.iridiummc.iny.exception.InyFactoryExecutionException;
import net.iridiummc.iny.exception.InyFactoryTypeException;
import net.iridiummc.iny.exception.InyInvalidFactoryResultException;
import net.iridiummc.iny.exception.InyUnknownFactoryException;
import net.iridiummc.iny.factory.InyFactory;
import net.iridiummc.iny.factory.InyFactoryRegistration;
import net.iridiummc.iny.value.InySection;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InyFactoryTest {

    @Test
    void lambdaAndStringRegistrationConstructArbitraryUnknownTypes() {
        Iny iny = configuredTypes().build();
        Point point = iny.parse("point: test:point(1, 2.5, -3)\n").get("point", Point.class);
        assertEquals(new Point(1, 2.5, -3), point);
    }

    @Test
    void identifierObjectRegistrationOverloadWorks() {
        CompletelyUnknownType value = Iny.builder()
                .registerFactory(InyIdentifier.parse("example:custom"), CompletelyUnknownType.class,
                        context -> new CompletelyUnknownType(context.arguments().get(0, String.class)))
                .build()
                .parse("value: example:custom(\"created\")\n")
                .get("value", CompletelyUnknownType.class);
        assertEquals(new CompletelyUnknownType("created"), value);
    }

    @Test
    void nestedCustomObjectsResolveThroughTypedArgumentAccess() {
        Box box = configuredTypes().build().parse("""
                box: test:box(
                  test:point(1, 2, 3),
                  test:point(4, 5, 6)
                )
                """).get("box", Box.class);
        assertEquals(new Box(new Point(1, 2, 3), new Point(4, 5, 6)), box);
    }

    @Test
    void compatibleInterfacesSuperclassesAndJdkInterfacesCanBeRequested() {
        Iny iny = configuredTypes()
                .registerFactory("test:child", Child.class, context -> new Child("child"))
                .registerFactory("test:list", ArrayList.class, context -> new ArrayList<>())
                .build();
        InyConfig config = iny.parse("""
                named: test:named("name")
                child: test:child()
                list: test:list()
                """);

        assertEquals("name", config.get("named", NamedValue.class).name());
        assertEquals("child", config.get("child", Parent.class).name);
        assertInstanceOf(ArrayList.class, config.get("list", List.class));
        assertInstanceOf(ArrayList.class, config.get("list", Object.class));
    }

    @Test
    void incompatibleRequestedTypeFailsBeforeFactoryExecution() {
        AtomicInteger calls = new AtomicInteger();
        InyConfig config = Iny.builder()
                .registerFactory("test:point", Point.class, context -> {
                    calls.incrementAndGet();
                    return new Point(1, 2, 3);
                })
                .build().parse("value: test:point()\n");

        InyFactoryTypeException failure = assertThrows(InyFactoryTypeException.class,
                () -> config.get("value", String.class));
        assertEquals("value", failure.path());
        assertEquals(String.class, failure.requestedType());
        assertEquals(Point.class, failure.registeredResultType());
        assertEquals(0, calls.get());
    }

    @Test
    void unknownCallsLoadAndFailOnlyWhenResolved() {
        InyConfig config = Iny.builder().build().parse("value: unavailable:thing(\"test\")\n");
        assertTrue(config.contains("value"));

        InyUnknownFactoryException failure = assertThrows(InyUnknownFactoryException.class,
                () -> config.get("value", Object.class));
        assertEquals("value", failure.path());
        assertEquals("unavailable:thing", failure.identifier().toString());
        assertEquals(Object.class, failure.requestedType());
    }

    @Test
    void duplicateRegistrationIsRejectedAndReplacementIsExplicit() {
        Iny.Builder builder = Iny.builder()
                .registerFactory("test:value", String.class, context -> "first");
        assertThrows(InyDuplicateFactoryException.class,
                () -> builder.registerFactory("test:value", String.class, context -> "second"));

        Iny replaced = builder.replaceFactory("test:value", String.class, context -> "replacement").build();
        assertEquals("replacement", replaced.parse("value: test:value()\n").get("value", String.class));
    }

    @Test
    void nullAndWrongRuntimeResultsAreRejected() {
        Iny nullIny = Iny.builder()
                .registerFactory("test:null", Point.class, context -> null)
                .build();
        InyInvalidFactoryResultException nullFailure = assertThrows(InyInvalidFactoryResultException.class,
                () -> nullIny.parse("value: test:null()\n").get("value", Point.class));
        assertNull(nullFailure.actualResultType());

        Iny wrongIny = wrongResultIny();
        InyInvalidFactoryResultException wrongFailure = assertThrows(InyInvalidFactoryResultException.class,
                () -> wrongIny.parse("value: test:wrong()\n").get("value", Point.class));
        assertEquals(String.class, wrongFailure.actualResultType());
        assertEquals(Point.class, wrongFailure.registeredResultType());
    }

    @Test
    void thrownFactoryExceptionsAreWrappedWithTheirCause() {
        IllegalStateException cause = new IllegalStateException("external system unavailable");
        InyConfig config = Iny.builder()
                .registerFactory("test:failure", Point.class, context -> { throw cause; })
                .build().parse("value: test:failure()\n");

        InyFactoryExecutionException failure = assertThrows(InyFactoryExecutionException.class,
                () -> config.get("value", Point.class));
        assertSame(cause, failure.getCause());
        assertEquals(Point.class, failure.requestedType());
        assertEquals(Point.class, failure.registeredResultType());
        assertTrue(failure.getMessage().contains("test:failure"));
        assertTrue(failure.getMessage().contains("value"));
    }

    @Test
    void countAndTypeErrorsAreContextualAndDoNotLeakLowLevelExceptions() {
        Iny iny = Iny.builder()
                .registerFactory("test:range", Range.class, context -> {
                    context.arguments().requireCount(2);
                    return new Range(
                            context.arguments().get(0, Integer.class),
                            context.arguments().get(1, Integer.class));
                })
                .build();

        InyArgumentCountException count = assertThrows(InyArgumentCountException.class,
                () -> iny.parse("value: test:range(1)\n").get("value", Range.class));
        assertEquals(1, count.actual());

        InyFactoryArgumentException type = assertThrows(InyFactoryArgumentException.class,
                () -> iny.parse("value: test:range(1, \"two\")\n").get("value", Range.class));
        assertEquals(1, type.argumentIndex());
        assertEquals(Integer.class, type.requestedType());
        assertEquals("string", type.actualType());
        assertTrue(type.getCause() != null);

        InyFactoryArgumentException missing = assertThrows(InyFactoryArgumentException.class,
                () -> Iny.builder()
                        .registerFactory("test:get", String.class,
                                context -> context.arguments().get(5, String.class))
                        .build().parse("value: test:get()\n").get("value", String.class));
        assertEquals(5, missing.argumentIndex());
        assertEquals("missing", missing.actualType());
    }

    @Test
    void optionalAndDefaultArgumentsAreErgonomic() {
        Iny iny = Iny.builder()
                .registerFactory("test:optional", OptionalValue.class, context -> {
                    context.arguments().requireCountBetween(1, 3);
                    return new OptionalValue(
                            context.arguments().get(0, String.class),
                            context.arguments().find(1, Integer.class),
                            context.arguments().getOrDefault(2, Boolean.class, true));
                }).build();

        InyConfig config = iny.parse("""
                short: test:optional("short")
                full: test:optional("full", 4, false)
                """);
        assertEquals(new OptionalValue("short", Optional.empty(), true),
                config.get("short", OptionalValue.class));
        assertEquals(new OptionalValue("full", Optional.of(4), false),
                config.get("full", OptionalValue.class));
    }

    @Test
    void factoryArgumentsCanUseAnExistingCustomDecoder() {
        Iny iny = Iny.builder()
                .registerDecoder(new LabelDecoder())
                .registerFactory("test:wrapped-label", WrappedLabel.class,
                        context -> new WrappedLabel(context.arguments().get(0, Label.class)))
                .build();
        WrappedLabel wrapped = iny.parse("""
                value: test:wrapped-label({
                  text: "decoded"
                })
                """).get("value", WrappedLabel.class);
        assertEquals(new WrappedLabel(new Label("decoded")), wrapped);
    }

    @Test
    void nestedResolutionFailureRetainsOuterArgumentAndInnerCause() {
        Iny iny = configuredTypes().build();
        InyFactoryArgumentException failure = assertThrows(InyFactoryArgumentException.class,
                () -> iny.parse("box: test:box(test:point(1, \"bad\", 3), test:point(4, 5, 6))\n")
                        .get("box", Box.class));

        assertEquals("test:box", failure.identifier().toString());
        assertEquals(0, failure.argumentIndex());
        InyFactoryArgumentException inner = assertInstanceOf(
                InyFactoryArgumentException.class, failure.getCause());
        assertEquals("test:point", inner.identifier().toString());
        assertEquals(1, inner.argumentIndex());
        assertTrue(failure.getMessage().contains("test:point"));
    }

    @Test
    void moduleCanInstallDecoderAndFactoryTogether() {
        Iny iny = Iny.builder().install(builder -> {
            builder.registerDecoder(new LabelDecoder());
            builder.registerFactory("test:wrapped-label", WrappedLabel.class,
                    context -> new WrappedLabel(context.arguments().get(0, Label.class)));
        }).build();
        assertEquals(new WrappedLabel(new Label("module")), iny.parse("""
                value: test:wrapped-label({text: "module"})
                """).get("value", WrappedLabel.class));
    }

    @Test
    void immutableRegistrySupportsConcurrentReadsAndResultsAreNotCached() throws Exception {
        AtomicInteger constructions = new AtomicInteger();
        Iny iny = Iny.builder()
                .registerFactory("test:counter", CounterValue.class,
                        context -> new CounterValue(constructions.incrementAndGet()))
                .build();
        InyConfig config = iny.parse("value: test:counter()\n");

        try (var executor = Executors.newFixedThreadPool(8)) {
            List<java.util.concurrent.Future<CounterValue>> futures = new ArrayList<>();
            for (int index = 0; index < 40; index++) {
                futures.add(executor.submit(() -> config.get("value", CounterValue.class)));
            }
            for (var future : futures) {
                assertInstanceOf(CounterValue.class, future.get());
            }
        }
        assertEquals(40, constructions.get());
        assertNotSame(config.get("value", CounterValue.class), config.get("value", CounterValue.class));
        assertEquals(42, constructions.get());
        assertEquals(3, iny.factories().size());
    }

    private static Iny.Builder configuredTypes() {
        return Iny.builder()
                .registerFactory("test:point", Point.class, context -> {
                    context.arguments().requireCount(3);
                    return new Point(
                            context.arguments().get(0, Double.class),
                            context.arguments().get(1, Double.class),
                            context.arguments().get(2, Double.class));
                })
                .registerFactory("test:box", Box.class, context -> new Box(
                        context.arguments().get(0, Point.class),
                        context.arguments().get(1, Point.class)))
                .registerFactory("test:named", CustomNamedValue.class,
                        context -> new CustomNamedValue(context.arguments().get(0, String.class)));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Iny wrongResultIny() {
        InyFactory rawFactory = context -> "not a point";
        InyFactoryRegistration registration = new InyFactoryRegistration(
                net.iridiummc.iny.api.InyIdentifier.parse("test:wrong"), Point.class, rawFactory);
        return Iny.builder().registerFactory(registration).build();
    }

    private record Point(double x, double y, double z) { }
    private record Box(Point first, Point second) { }
    private interface NamedValue { String name(); }
    private record CustomNamedValue(String name) implements NamedValue { }
    private static class Parent {
        private final String name;
        private Parent(String name) { this.name = name; }
    }
    private static final class Child extends Parent {
        private Child(String name) { super(name); }
    }
    private record Range(int minimum, int maximum) { }
    private record OptionalValue(String name, Optional<Integer> size, boolean enabled) { }
    private record Label(String text) { }
    private record WrappedLabel(Label label) { }
    private record CounterValue(int sequence) { }
    private record CompletelyUnknownType(String value) { }

    private static final class LabelDecoder implements InyDecoder<Label> {
        @Override
        public Class<Label> targetType() {
            return Label.class;
        }

        @Override
        public Label decode(Object value, InyDecodeContext context) {
            if (!(value instanceof InySection section)) {
                throw context.failure("a label must be a section");
            }
            return new Label(context.decodeChild("text", section.get("text"), String.class));
        }
    }
}
