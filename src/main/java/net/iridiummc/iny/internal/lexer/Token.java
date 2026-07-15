package net.iridiummc.iny.internal.lexer;

import net.iridiummc.iny.source.SourcePosition;

import java.util.Objects;

/** Internal lexical token. */
public record Token(
        TokenType type,
        String lexeme,
        String value,
        SourcePosition position,
        boolean firstOnLine
) {

    public Token {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(lexeme, "lexeme");
        Objects.requireNonNull(position, "position");
    }

    public String display() {
        return switch (type) {
            case EOF -> "end of input";
            case NEWLINE -> "newline";
            default -> "'" + lexeme + "'";
        };
    }
}
