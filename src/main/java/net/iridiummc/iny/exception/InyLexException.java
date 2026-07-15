package net.iridiummc.iny.exception;

import net.iridiummc.iny.source.InySource;
import net.iridiummc.iny.source.SourcePosition;

/** Raised when source characters cannot form valid INY tokens. */
public final class InyLexException extends InySyntaxException {

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
