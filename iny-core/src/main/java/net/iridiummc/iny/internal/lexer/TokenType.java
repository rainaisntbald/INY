package net.iridiummc.iny.internal.lexer;

/** Internal token kinds. This package is not part of the public INY API. */
public enum TokenType {
    IDENTIFIER,
    NAMESPACED_IDENTIFIER,
    STRING,
    NUMBER,
    COLON,
    LEFT_BRACE,
    RIGHT_BRACE,
    LEFT_PARENTHESIS,
    RIGHT_PARENTHESIS,
    COMMA,
    DASH,
    NEWLINE,
    EOF
}
