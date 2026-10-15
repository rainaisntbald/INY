package net.iridiummc.iny.internal.lexer;

/** Internal token kinds */
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
    LEFT_SQUARE,
    RIGHT_SQUARE,
    NEWLINE,
    EOF
}
