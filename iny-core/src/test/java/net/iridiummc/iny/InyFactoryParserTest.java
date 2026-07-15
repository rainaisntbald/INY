package net.iridiummc.iny;

import net.iridiummc.iny.api.Iny;
import net.iridiummc.iny.api.InyConfig;
import net.iridiummc.iny.api.InyIdentifier;
import net.iridiummc.iny.exception.InyParseException;
import net.iridiummc.iny.internal.lexer.Lexer;
import net.iridiummc.iny.internal.parser.Parser;
import net.iridiummc.iny.internal.value.InyCall;
import net.iridiummc.iny.internal.value.InyInteger;
import net.iridiummc.iny.internal.value.InyList;
import net.iridiummc.iny.internal.value.InyNull;
import net.iridiummc.iny.internal.value.InySectionValue;
import net.iridiummc.iny.internal.value.InyString;
import net.iridiummc.iny.source.InySource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InyFactoryParserTest {

    private final Iny iny = Iny.builder().build();

    @Test
    void parsesZeroOneAndMultipleArgumentsWithoutRegistration() {
        InySectionValue root = parse("""
                empty: example:empty()
                one: example:one("value")
                many: example:many(1, 2.5, true, null)
                """);

        assertEquals(new InyCall(InyIdentifier.parse("example:empty"), List.of()), call(root, "empty"));
        assertEquals(List.of(new InyString("value")), call(root, "one").arguments());
        assertEquals(4, call(root, "many").arguments().size());
        assertSame(InyNull.INSTANCE, call(root, "many").arguments().get(3));
        assertTrue(call(root, "empty").position().isPresent());
    }

    @Test
    void parsesNestedCallsAndCallsInsideListsAndSections() {
        InySectionValue root = parse("""
                nested: example:outer(example:inner("test"))
                values:
                  - example:value(1)
                  - example:value(2)
                section: {
                  constructed: example:value(3)
                }
                """);

        InyCall inner = assertInstanceOf(InyCall.class, call(root, "nested").arguments().getFirst());
        assertEquals(InyIdentifier.parse("example:inner"), inner.identifier());
        InyList values = assertInstanceOf(InyList.class, root.entries().get("values"));
        assertEquals(2, values.values().size());
        InySectionValue section = assertInstanceOf(InySectionValue.class, root.entries().get("section"));
        assertInstanceOf(InyCall.class, section.entries().get("constructed"));
    }

    @Test
    void parsesScalarsListsSectionsAndNullAsArguments() {
        InyCall call = call(parse("""
                value: example:all(
                  "scalar",
                  - 1
                  - 2,
                  {
                    key: "value"
                  },
                  null
                )
                """), "value");

        assertEquals(new InyString("scalar"), call.arguments().get(0));
        assertEquals(new InyList(List.of(new InyInteger(1), new InyInteger(2))), call.arguments().get(1));
        assertInstanceOf(InySectionValue.class, call.arguments().get(2));
        assertSame(InyNull.INSTANCE, call.arguments().get(3));
    }

    @Test
    void newlinesIndentationLfAndCrlfAreSemanticallyIrrelevant() {
        String compact = "value: example:outer(example:inner(1), 2)\n";
        String readable = """
                value: example:outer(
                    example:inner(
                       1
                    ),
                         2
                )
                """;
        String irregular = "value: example:outer(\nexample:inner(\n 1\n),\n        2\n)\n";

        assertEquals(parse(compact), parse(readable));
        assertEquals(parse(compact), parse(irregular));
        assertEquals(parse(readable), parse(readable.replace("\n", "\r\n")));
    }

    @Test
    void ordinaryColonAssignmentsAndBareIdentifiersRemainDistinct() {
        InyConfig config = iny.parse("value: normal_identifier\ncompact:normal_identifier\n");
        assertEquals("normal_identifier", config.get("value", String.class));
        assertEquals("normal_identifier", config.get("compact", String.class));
    }

    @Test
    void malformedCallsReportAccurateSyntaxErrors() {
        InyParseException missingArgument = assertThrows(InyParseException.class,
                () -> iny.parse("value: example:thing(, 1)\n"));
        assertEquals(1, missingArgument.line());
        assertTrue(missingArgument.expected().contains("argument"));

        InyParseException missingComma = assertThrows(InyParseException.class,
                () -> iny.parse("value: example:thing(1 2)\n"));
        assertTrue(missingComma.expected().contains(","));

        InyParseException missingClose = assertThrows(InyParseException.class,
                () -> iny.parse("value: example:thing(1\n"));
        assertEquals("end of input", missingClose.encountered());
        assertTrue(missingClose.expected().contains(")"));
    }

    private static InySectionValue parse(String content) {
        InySource source = new InySource("<test>", content);
        return new Parser(source, new Lexer(source).lex()).parse();
    }

    private static InyCall call(InySectionValue section, String key) {
        return assertInstanceOf(InyCall.class, section.entries().get(key));
    }
}
