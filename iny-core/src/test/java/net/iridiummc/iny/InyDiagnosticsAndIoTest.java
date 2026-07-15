package net.iridiummc.iny;

import net.iridiummc.iny.api.Iny;
import net.iridiummc.iny.exception.InyLexException;
import net.iridiummc.iny.exception.InyParseException;
import net.iridiummc.iny.exception.InySourceLoadException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InyDiagnosticsAndIoTest {

    private final Iny iny = Iny.builder().build();

    @Test
    void missingBraceReportsEofAndExpectedBrace() {
        InyParseException exception = assertThrows(InyParseException.class,
                () -> iny.parse("missing-brace.iny", "outer {\nvalue: 1\n"));
        assertEquals("missing-brace.iny", exception.sourceName());
        assertEquals("end of input", exception.encountered());
        assertTrue(exception.expected().contains("}"));
    }

    @Test
    void missingColonAndUnexpectedDashesFailClearly() {
        InyParseException missingColon = assertThrows(InyParseException.class,
                () -> iny.parse("value 1\n"));
        assertTrue(missingColon.expected().contains(":"));

        InyParseException dash = assertThrows(InyParseException.class,
                () -> iny.parse("- value\n"));
        assertTrue(dash.getMessage().contains("Every section entry"));
    }

    @Test
    void duplicateSeparatorsAndInvalidKeysAreNotIgnored() {
        assertThrows(InyParseException.class, () -> iny.parse("value:: 1\n"));
        InyParseException invalidKey = assertThrows(InyParseException.class, () -> iny.parse("1key: true\n"));
        assertEquals(1, invalidKey.line());
        assertEquals(1, invalidKey.column());
    }

    @Test
    void unterminatedStringsAndInvalidEscapesAreLexingErrors() {
        InyLexException unterminated = assertThrows(InyLexException.class,
                () -> iny.parse("name: \"never closed"));
        assertEquals(1, unterminated.line());
        assertEquals(7, unterminated.column());

        InyLexException escape = assertThrows(InyLexException.class,
                () -> iny.parse("name: \"bad \\q escape\""));
        assertTrue(escape.getMessage().contains("Invalid string escape"));
    }

    @Test
    void syntaxLocationIncludesAccurateLineColumnOffsetAndContext() {
        InyParseException exception = assertThrows(InyParseException.class,
                () -> iny.parse("location.iny", "good: 1\nsecond value\n"));

        assertEquals("location.iny", exception.sourceName());
        assertEquals(2, exception.line());
        assertEquals(8, exception.column());
        assertEquals(15, exception.offset());
        assertEquals("second value", exception.sourceLine());
        assertTrue(exception.getMessage().contains("location.iny:2:8"));
    }

    @Test
    void readerAndUtf8PathLoadingUseTheSameParser(@TempDir Path directory) throws Exception {
        assertEquals("reader", iny.parse("reader.iny", new StringReader("value: \"reader\""))
                .get("value", String.class));

        Path path = directory.resolve("utf8.iny");
        Files.writeString(path, "value: \"Grüße\"\n", StandardCharsets.UTF_8);
        assertEquals("Grüße", iny.load(path).get("value", String.class));
    }

    @Test
    void loadingFailureHasSourceName(@TempDir Path directory) {
        Path missing = directory.resolve("missing.iny");
        InySourceLoadException exception = assertThrows(InySourceLoadException.class, () -> iny.load(missing));
        assertEquals(missing.toString(), exception.sourceName());
    }
}
