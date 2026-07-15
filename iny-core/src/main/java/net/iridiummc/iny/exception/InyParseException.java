package net.iridiummc.iny.exception;

import net.iridiummc.iny.source.InySource;
import net.iridiummc.iny.source.SourcePosition;

/** Raised when valid tokens do not conform to the INY grammar. */
public final class InyParseException extends InySyntaxException {

    public InyParseException(
            InySource source,
            SourcePosition position,
            String encountered,
            String expected,
            String explanation
    ) {
        super("INY parsing error", source, position, encountered, expected, explanation);
    }
}
