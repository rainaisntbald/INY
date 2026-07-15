package net.iridiummc.iny.exception;

import net.iridiummc.iny.source.InySource;
import net.iridiummc.iny.source.SourcePosition;

import java.util.Objects;

/** Common structured base for lexing and parsing diagnostics. */
public abstract class InySyntaxException extends InyException {

    private final SourcePosition position;
    private final String encountered;
    private final String expected;
    private final String sourceLine;

    protected InySyntaxException(
            String kind,
            InySource source,
            SourcePosition position,
            String encountered,
            String expected,
            String explanation
    ) {
        super(format(kind, source, position, encountered, expected, explanation));
        this.position = Objects.requireNonNull(position, "position");
        this.encountered = Objects.requireNonNull(encountered, "encountered");
        this.expected = Objects.requireNonNull(expected, "expected");
        this.sourceLine = source.line(position.line());
    }

    private static String format(
            String kind,
            InySource source,
            SourcePosition position,
            String encountered,
            String expected,
            String explanation
    ) {
        String sourceLine = source.line(position.line());
        String caret = " ".repeat(Math.max(0, position.column() - 1)) + "^";
        return kind + " at " + position + ": " + explanation
                + " (encountered " + encountered + "; expected " + expected + ")"
                + System.lineSeparator() + sourceLine
                + System.lineSeparator() + caret;
    }

    public SourcePosition position() {
        return position;
    }

    public String sourceName() {
        return position.sourceName();
    }

    public int offset() {
        return position.offset();
    }

    public int line() {
        return position.line();
    }

    public int column() {
        return position.column();
    }

    public String encountered() {
        return encountered;
    }

    public String expected() {
        return expected;
    }

    public String sourceLine() {
        return sourceLine;
    }
}
