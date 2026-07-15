package net.iridiummc.iny.exception;

import net.iridiummc.iny.source.InySource;
import net.iridiummc.iny.source.SourcePosition;

import java.util.Objects;

/** Common structured base for lexing and parsing diagnostics. */
public abstract class InySyntaxException extends InyException {

    /** Complete failing source position. */
    private final SourcePosition position;
    /** Description of the encountered source input. */
    private final String encountered;
    /** Description of the expected source input. */
    private final String expected;
    /** Complete source line containing the failure. */
    private final String sourceLine;

    /**
     * Creates a structured syntax failure.
     *
     * @param kind diagnostic heading
     * @param source source being processed
     * @param position failing source position
     * @param encountered description of the encountered input
     * @param expected description of the expected input
     * @param explanation explanation of the failure
     */
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

    /**
     * Returns the complete failing source position.
     *
     * @return source position
     */
    public SourcePosition position() {
        return position;
    }

    /**
     * Returns the diagnostic source name.
     *
     * @return source name
     */
    public String sourceName() {
        return position.sourceName();
    }

    /**
     * Returns the zero-based source offset.
     *
     * @return character offset
     */
    public int offset() {
        return position.offset();
    }

    /**
     * Returns the one-based source line number.
     *
     * @return line number
     */
    public int line() {
        return position.line();
    }

    /**
     * Returns the one-based source column number.
     *
     * @return column number
     */
    public int column() {
        return position.column();
    }

    /**
     * Returns the description of the encountered input.
     *
     * @return encountered input description
     */
    public String encountered() {
        return encountered;
    }

    /**
     * Returns the description of the expected input.
     *
     * @return expected input description
     */
    public String expected() {
        return expected;
    }

    /**
     * Returns the complete source line containing the failure.
     *
     * @return failing source line
     */
    public String sourceLine() {
        return sourceLine;
    }
}
