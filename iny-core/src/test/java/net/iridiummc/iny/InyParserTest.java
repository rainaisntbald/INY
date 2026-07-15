package net.iridiummc.iny;

import net.iridiummc.iny.api.Iny;
import net.iridiummc.iny.api.InyConfig;
import net.iridiummc.iny.exception.InyParseException;
import net.iridiummc.iny.value.InySection;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
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

        assertEquals("Example", root.get("string"));
        assertEquals("identifier-value", root.get("bare"));
        assertEquals(BigInteger.valueOf(12), root.get("integer"));
        assertEquals(new BigInteger("9000000000"), root.get("large"));
        assertEquals(new BigDecimal("1.5"), root.get("decimal"));
        assertEquals(true, root.get("enabled"));
        assertEquals(false, root.get("disabled"));
        assertNull(root.get("missing"));
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
        List<Object> list = iny.parse("""
                values:
                  - "one"
                  - "two"
                  - {
                      name: "three"
                      enabled: true
                    }
                after: 4
                """).getList("values");

        assertEquals("one", list.get(0));
        assertEquals("two", list.get(1));
        InySection section = assertInstanceOf(InySection.class, list.get(2));
        assertEquals("three", section.get("name"));
        assertEquals(true, section.get("enabled"));
    }

    @Test
    void parsesNestedListsUsingStandaloneDashBoundaries() {
        List<Object> matrix = iny.parse("""
                matrix:
                  -
                    - 1
                    - 2
                  -
                    - 3
                    - 4
                """).getList("matrix");

        assertEquals(2, matrix.size());
        assertEquals(List.of(BigInteger.ONE, BigInteger.TWO), matrix.get(0));
        assertEquals(List.of(BigInteger.valueOf(3), BigInteger.valueOf(4)), matrix.get(1));
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
