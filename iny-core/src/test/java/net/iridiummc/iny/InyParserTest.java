package net.iridiummc.iny;

import net.iridiummc.iny.api.Iny;
import net.iridiummc.iny.api.InyConfig;
import net.iridiummc.iny.exception.InyParseException;
import net.iridiummc.iny.value.InyBoolean;
import net.iridiummc.iny.value.InyDecimal;
import net.iridiummc.iny.value.InyInteger;
import net.iridiummc.iny.value.InyList;
import net.iridiummc.iny.value.InyNull;
import net.iridiummc.iny.value.InySection;
import net.iridiummc.iny.value.InyString;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InyParserTest {

    private final Iny iny = Iny.builder().build();

    @Test
    void parsesEveryScalarKindAndRootEntries() {
        InySection root = iny.parse("""
                string: "Example"
                bare: identifier-value
                integer: 12
                large: 9000000000
                decimal: 1.50
                enabled: true
                disabled: false
                missing: null
                """).root();

        assertEquals(new InyString("Example"), root.get("string"));
        assertEquals(new InyString("identifier-value"), root.get("bare"));
        assertEquals(new InyInteger(BigInteger.valueOf(12)), root.get("integer"));
        assertEquals(new InyInteger(new BigInteger("9000000000")), root.get("large"));
        assertEquals(new InyDecimal(new BigDecimal("1.50")), root.get("decimal"));
        assertEquals(new InyBoolean(true), root.get("enabled"));
        assertEquals(new InyBoolean(false), root.get("disabled"));
        assertSame(InyNull.INSTANCE, root.get("missing"));
    }

    @Test
    void parsesBothSectionFormsAndEmptySections() {
        InyConfig config = iny.parse("""
                direct {
                  value: 1
                }
                colon: {
                  nested: {}
                }
                """);

        assertEquals(1, config.get("direct.value", Integer.class));
        assertTrue(config.getSection("colon.nested").entries().isEmpty());
    }

    @Test
    void parsesScalarListsAndSectionsInLists() {
        InyList list = iny.parse("""
                values:
                  - "one"
                  - "two"
                  - {
                      name: "three"
                      enabled: true
                    }
                after: 4
                """).getList("values");

        assertEquals(new InyString("one"), list.values().get(0));
        assertEquals(new InyString("two"), list.values().get(1));
        InySection section = assertInstanceOf(InySection.class, list.values().get(2));
        assertEquals(new InyString("three"), section.get("name"));
        assertEquals(new InyBoolean(true), section.get("enabled"));
    }

    @Test
    void parsesNestedListsUsingStandaloneDashBoundaries() {
        InyList matrix = iny.parse("""
                matrix:
                  -
                    - 1
                    - 2
                  -
                    - 3
                    - 4
                """).getList("matrix");

        assertEquals(2, matrix.values().size());
        assertEquals(new InyList(List.of(new InyInteger(1), new InyInteger(2))), matrix.values().get(0));
        assertEquals(new InyList(List.of(new InyInteger(3), new InyInteger(4))), matrix.values().get(1));
    }

    @Test
    void commentsAndEscapesAreHandledByTheLexer() {
        InyConfig config = iny.parse("""
                # full-line comment
                escaped: "quote: \\" slash: \\\\ newline: \\n tab: \\t unicode: \\u0041" # trailing comment
                value: 1
                """);

        assertEquals("quote: \" slash: \\ newline: \n tab: \t unicode: A",
                config.get("escaped", String.class));
        assertEquals(1, config.get("value", Integer.class));
    }

    @Test
    void lfAndCrlfProduceTheSameTree() {
        String lf = "outer {\nvalue: 1\nlist:\n- true\n- null\n}\n";
        String crlf = lf.replace("\n", "\r\n");
        assertEquals(iny.parse(lf).root(), iny.parse(crlf).root());
    }

    @Test
    void emptyFilesAreEmptyRootSections() {
        assertTrue(iny.parse("").root().entries().isEmpty());
        assertTrue(iny.parse("# comment only\n\n").root().entries().isEmpty());
    }

    @Test
    void duplicateKeysAreRejected() {
        InyParseException exception = assertThrows(InyParseException.class,
                () -> iny.parse("value: 1\nvalue: 2\n"));
        assertTrue(exception.getMessage().contains("Duplicate key 'value'"));
        assertEquals(2, exception.line());
    }

    @Test
    void indentationHasNoSemanticMeaning() {
        String readable = """
                outer {
                  inner_one: 1
                  inner_two: {
                    inner_three:
                      - "One"
                      - "TwO"
                  }
                }
                """;
        String irregular = """
                outer {
                inner_one: 1
                        inner_two: {
                 inner_three:
                - "One"
                             - "TwO"
                }
                }
                """;
        String tabs = "outer {\n\tinner_one: 1\n inner_two: {\n\t\tinner_three:\n - \"One\"\n\t- \"TwO\"\n}\n}";

        InySection expected = iny.parse(readable).root();
        assertEquals(expected, iny.parse(irregular).root());
        assertEquals(expected, iny.parse(tabs).root());
    }

    @Test
    void spacingAfterAListDashHasNoSemanticMeaning() {
        InySection spaced = iny.parse("values:\n- 1\n- 2\n").root();
        InySection compact = iny.parse("values:\n-1\n-2\n").root();
        assertEquals(spaced, compact);
    }
}
