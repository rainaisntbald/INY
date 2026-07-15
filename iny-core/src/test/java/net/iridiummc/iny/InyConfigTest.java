package net.iridiummc.iny;

import net.iridiummc.iny.api.Iny;
import net.iridiummc.iny.api.InyConfig;
import net.iridiummc.iny.codec.InyDecodeContext;
import net.iridiummc.iny.codec.InyDecoder;
import net.iridiummc.iny.exception.InyDecodeException;
import net.iridiummc.iny.exception.InyInvalidPathException;
import net.iridiummc.iny.exception.InyMissingDecoderException;
import net.iridiummc.iny.exception.InyMissingValueException;
import net.iridiummc.iny.exception.InyPathTraversalException;
import net.iridiummc.iny.value.InySection;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InyConfigTest {

    private final Iny iny = Iny.builder().build();

    @Test
    void dottedLookupAndValueConveniencesWork() {
        InyConfig config = iny.parse("""
                outer {
                  inner {
                    value: "example"
                  }
                  values:
                    - 1
                }
                """);

        assertEquals("example", config.get("outer.inner.value", String.class));
        assertTrue(config.contains("outer.inner.value"));
        assertFalse(config.contains("outer.inner.absent"));
        assertInstanceOf(InySection.class, config.getValue("outer.inner"));
        assertEquals(1, config.getList("outer.values").size());
    }

    @Test
    void configIsItsRootSectionAndSubsectionsSupportTypedRelativeNavigation() {
        InyConfig config = iny.parse("""
                server {
                  host: "localhost"
                  limits {
                    players: 20
                  }
                  ports:
                    - 25565
                    - 25566
                }
                """);

        assertSame(config, config.root());
        assertInstanceOf(InySection.class, config);
        assertEquals("localhost", config.get("server.host", String.class));

        InySection server = config.getSection("server");
        assertEquals("localhost", server.get("host", String.class));
        assertEquals(20, server.get("limits.players", Integer.class));
        assertEquals(20, server.getSection("limits").get("players", Integer.class));
        assertEquals(List.of(25565, 25566), server.getList("ports", Integer.class));
        assertEquals(Optional.of(20), server.find("limits.players", Integer.class));
        assertTrue(server.contains("limits.players"));
        assertFalse(server.contains("limits.absent"));

        InyMissingValueException missing = assertThrows(
                InyMissingValueException.class, () -> server.get("limits.absent", Object.class));
        assertEquals("server.limits.absent", missing.path());
    }

    @Test
    void typedListLookupDecodesEachElement() {
        InyConfig config = iny.parse("""
                values:
                  - 1
                  - 2
                  - 3
                """);

        List<Integer> values = config.getList("values", Integer.class);
        List<Object> rawValues = config.getList("values");

        assertEquals(new ArrayList<>(List.of(1, 2, 3)), values);
        assertThrows(UnsupportedOperationException.class, () -> values.add(4));
        assertThrows(UnsupportedOperationException.class, () -> rawValues.add(4));
    }

    @Test
    void defaultSectionListMethodsReturnImmutableDefensiveCopies() {
        ArrayList<Object> backing = new ArrayList<>();
        backing.add("one");
        backing.add(null);
        InySection section = () -> Map.of("values", backing);

        List<Object> rawValues = section.getList("values");
        List<String> typedValues = section.getList("values", String.class);
        backing.add("two");

        assertEquals(2, rawValues.size());
        assertEquals(2, typedValues.size());
        assertNull(rawValues.get(1));
        assertNull(typedValues.get(1));
        assertThrows(UnsupportedOperationException.class, () -> rawValues.add("three"));
        assertThrows(UnsupportedOperationException.class, () -> typedValues.add("three"));
    }

    @Test
    void defaultSectionMethodsNavigateDottedPaths() {
        LinkedHashMap<String, Object> limitEntries = new LinkedHashMap<>();
        limitEntries.put("players", 20);
        limitEntries.put("motd", null);
        InySection limits = () -> Collections.unmodifiableMap(limitEntries);
        InySection server = () -> Map.of("limits", limits);
        InySection root = () -> Map.of("server", server);

        assertEquals(20, root.get("server.limits.players", Integer.class));
        assertEquals(Optional.of(20), root.find("server.limits.players", Integer.class));
        assertEquals(Optional.empty(), root.find("server.limits.motd"));
        assertTrue(root.contains("server.limits.motd"));
        assertFalse(root.contains("server.limits.absent"));
        assertSame(limits, root.getSection("server.limits"));

        InyMissingValueException missing = assertThrows(InyMissingValueException.class,
                () -> root.get("server.limits.absent"));
        assertEquals("server.limits.absent", missing.path());

        InyPathTraversalException traversal = assertThrows(InyPathTraversalException.class,
                () -> root.contains("server.limits.players.maximum"));
        assertEquals("players", traversal.segment());
        assertEquals("integer", traversal.actualType());

        assertThrows(InyInvalidPathException.class, () -> root.get(""));
        assertThrows(InyInvalidPathException.class, () -> root.find("server..limits"));
    }

    @Test
    void objectLookupReturnsOnlyOrdinaryJavaValues() {
        InyConfig config = iny.parse("""
                text: "Test"
                integer: 12
                decimal: 12.5
                enabled: true
                missing: null
                values:
                  - "entry"
                  - 2
                  - false
                nested {
                  name: "section"
                }
                """);

        Object text = config.get("text", Object.class);
        assertInstanceOf(String.class, text);
        assertEquals("Test", text);
        assertInstanceOf(java.math.BigInteger.class, config.getValue("integer"));
        assertInstanceOf(java.math.BigDecimal.class, config.getValue("decimal"));
        assertInstanceOf(Boolean.class, config.getValue("enabled"));
        assertNull(config.getValue("missing"));

        List<?> values = assertInstanceOf(List.class, config.getValue("values"));
        assertEquals("entry", values.get(0));
        assertInstanceOf(java.math.BigInteger.class, values.get(1));
        assertEquals(false, values.get(2));

        InySection section = assertInstanceOf(InySection.class, config.getValue("nested"));
        assertEquals(Map.of("name", "section"), section.entries());
    }

    @Test
    void optionalLookupReturnsEmptyOnlyForMissingValues() {
        InyConfig config = iny.parse("outer {\nvalue: 1\n}\n");
        assertEquals(Optional.of(1), config.find("outer.value", Integer.class));
        assertEquals(Optional.empty(), config.find("outer.absent", Integer.class));
        assertEquals(Optional.empty(), config.findValue("absent.child"));
    }

    @Test
    void requiredLookupDistinguishesFinalAndIntermediateMissingValues() {
        InyConfig config = iny.parse("outer {\nvalue: 1\n}\n");

        InyMissingValueException finalMissing = assertThrows(InyMissingValueException.class,
                () -> config.getValue("outer.absent"));
        assertTrue(finalMissing.finalSegment());
        assertEquals("outer.absent", finalMissing.path());

        InyMissingValueException intermediate = assertThrows(InyMissingValueException.class,
                () -> config.getValue("absent.child"));
        assertFalse(intermediate.finalSegment());
        assertEquals(0, intermediate.segmentIndex());
    }

    @Test
    void nonSectionIntermediateValueIsATraversalFailure() {
        InyConfig config = iny.parse("outer: 1\n");
        InyPathTraversalException exception = assertThrows(InyPathTraversalException.class,
                () -> config.findValue("outer.child"));
        assertEquals("outer", exception.segment());
        assertEquals("integer", exception.actualType());
    }

    @Test
    void invalidPathsAreRejectedBeforeNavigation() {
        InyConfig config = iny.parse("value: 1\n");
        assertThrows(InyInvalidPathException.class, () -> config.getValue(""));
        assertThrows(InyInvalidPathException.class, () -> config.getValue("one..two"));
        assertThrows(InyInvalidPathException.class, () -> config.getValue(" one"));
    }

    @Test
    void primitiveAndWrapperDecodersAreConsistent() {
        InyConfig config = iny.parse("""
                integer: 12
                decimal: 12.5
                bool: true
                """);

        assertEquals(12, config.get("integer", int.class));
        assertEquals(12, config.get("integer", Integer.class));
        assertEquals(12L, config.get("integer", long.class));
        assertEquals(12L, config.get("integer", Long.class));
        assertEquals(12.0d, config.get("integer", Double.class));
        assertEquals(12.5d, config.get("decimal", double.class));
        assertEquals(12.5f, config.get("decimal", Float.class));
        assertEquals(true, config.get("bool", boolean.class));
        assertEquals(true, config.get("bool", Boolean.class));
    }

    @Test
    void decodingIsStrictAndChecksNumericSafety() {
        InyConfig config = iny.parse("""
                text: "12"
                huge: 9000000000
                fractional: 12.5
                too_large_double: 1e9999
                """);

        InyDecodeException wrongType = assertThrows(InyDecodeException.class,
                () -> config.get("text", Integer.class));
        assertEquals("text", wrongType.path());
        assertEquals(Integer.class, wrongType.targetType());
        assertEquals(String.class.getTypeName(), wrongType.actualType());

        assertThrows(InyDecodeException.class, () -> config.get("huge", Integer.class));
        assertEquals(9_000_000_000L, config.get("huge", Long.class));
        assertThrows(InyDecodeException.class, () -> config.get("fractional", Integer.class));
        assertThrows(InyDecodeException.class, () -> config.get("too_large_double", Double.class));
    }

    @Test
    void missingDecoderIsContextual() {
        InyConfig config = iny.parse("value: \"x\"\n");
        InyMissingDecoderException exception = assertThrows(InyMissingDecoderException.class,
                () -> config.get("value", Unregistered.class));
        assertEquals("value", exception.path());
        assertEquals(Unregistered.class, exception.targetType());
    }

    @Test
    void customDecoderCanDecodeChildrenWithoutChangingConfig() {
        Iny configured = Iny.builder().registerDecoder(new PersonDecoder()).build();
        Person person = configured.parse("""
                person {
                  name: "Ada"
                  age: 37
                }
                """).get("person", Person.class);

        assertEquals(new Person("Ada", 37), person);
    }

    @Test
    void duplicateRegistrationRequiresExplicitReplacement() {
        assertThrows(IllegalArgumentException.class,
                () -> Iny.builder().registerDecoder(new DuplicateStringDecoder()));

        Iny overridden = Iny.builder().replaceDecoder(new DuplicateStringDecoder()).build();
        assertEquals("overridden", overridden.parse("value: \"original\"").get("value", String.class));
    }

    private record Unregistered(String value) {
    }

    private record Person(String name, int age) {
    }

    private static final class PersonDecoder implements InyDecoder<Person> {
        @Override
        public Class<Person> targetType() {
            return Person.class;
        }

        @Override
        public Person decode(Object value, InyDecodeContext context) {
            if (!(value instanceof InySection section)) {
                throw context.failure("a person must be a section");
            }
            String name = context.decodeChild("name", section.get("name"), String.class);
            int age = context.decodeChild("age", section.get("age"), Integer.class);
            return new Person(name, age);
        }
    }

    private static final class DuplicateStringDecoder implements InyDecoder<String> {
        @Override
        public Class<String> targetType() {
            return String.class;
        }

        @Override
        public String decode(Object value, InyDecodeContext context) {
            return "overridden";
        }
    }
}
