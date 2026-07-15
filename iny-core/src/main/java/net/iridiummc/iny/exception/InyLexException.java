package net.iridiummc.iny.exception;

import net.iridiummc.iny.source.InySource;
import net.iridiummc.iny.source.SourcePosition;

/** Raised when source characters cannot form valid INY tokens. */
public final class InyLexException extends InySyntaxException {

    /**
     * Creates a structured lexing failure.
     *
     * @param source source being lexed
     * @param position failing source position
     * @param encountered description of the encountered input
     * @param expected description of the expected input
     * @param explanation explanation of the failure
     */
    public InyLexException(
            InySource source,
            SourcePosition position,
            String encountered,
            String expected,
            String explanation
    ) {
        super("INY lexing error", source, position, encountered, expected, explanation);
    }
}
