package net.iridiummc.iny.internal.lexer;

import net.iridiummc.iny.exception.InyLexException;
import net.iridiummc.iny.api.InyIdentifier;
import net.iridiummc.iny.exception.InyInvalidIdentifierException;
import net.iridiummc.iny.source.InySource;
import net.iridiummc.iny.source.SourcePosition;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Converts INY source text into a position-aware token stream. */
public final class Lexer {

    private final InySource source;
    private final String text;
    private final List<Token> tokens = new ArrayList<>();
    private int offset;
    private int line = 1;
    private int column = 1;
    private boolean lineHasToken;

    public Lexer(InySource source) {
        this.source = Objects.requireNonNull(source, "source");
        this.text = source.text();
    }

    public List<Token> lex() {
        while (!atEnd()) {
            char character = peek();
            if (character == ' ' || character == '\t' || character == '\f') {
                advance();
            } else if (character == '\n' || character == '\r') {
                lexNewline();
            } else if (character == '#') {
                skipComment();
            } else if (character == ':') {
                single(TokenType.COLON);
            } else if (character == '{') {
                single(TokenType.LEFT_BRACE);
            } else if (character == '}') {
                single(TokenType.RIGHT_BRACE);
            } else if (character == '(') {
                single(TokenType.LEFT_PARENTHESIS);
            } else if (character == ')') {
                single(TokenType.RIGHT_PARENTHESIS);
            } else if (character == ',') {
                single(TokenType.COMMA);
            } else if (character == '"') {
                lexString();
            } else if (isIdentifierStart(character)) {
                lexIdentifier();
            } else if (character == '-' && !lineHasToken) {
                single(TokenType.DASH);
            } else if (isNumberStart()) {
                lexNumber();
            } else if (character == '-') {
                single(TokenType.DASH);
            } else {
                SourcePosition position = position();
                throw error(position, printable(character), "an identifier, value, or structural token",
                        "Unexpected character");
            }
        }

        tokens.add(new Token(TokenType.EOF, "", null, position(), !lineHasToken));
        return List.copyOf(tokens);
    }

    private void single(TokenType type) {
        SourcePosition position = position();
        boolean firstOnLine = !lineHasToken;
        char character = advance();
        tokens.add(new Token(type, Character.toString(character), null, position, firstOnLine));
        lineHasToken = true;
    }

    private void lexNewline() {
        SourcePosition position = position();
        int start = offset;
        if (peek() == '\r') {
            advanceRaw();
            if (!atEnd() && peek() == '\n') {
                advanceRaw();
            }
        } else {
            advanceRaw();
        }
        tokens.add(new Token(TokenType.NEWLINE, text.substring(start, offset), null, position, false));
        line++;
        column = 1;
        lineHasToken = false;
    }

    private void skipComment() {
        while (!atEnd() && peek() != '\n' && peek() != '\r') {
            advance();
        }
    }

    private void lexIdentifier() {
        if (tryLexNamespacedIdentifier()) {
            return;
        }
        SourcePosition position = position();
        boolean firstOnLine = !lineHasToken;
        int start = offset;
        advance();
        while (!atEnd() && isIdentifierPart(peek())) {
            advance();
        }
        String lexeme = text.substring(start, offset);
        tokens.add(new Token(TokenType.IDENTIFIER, lexeme, lexeme, position, firstOnLine));
        lineHasToken = true;
    }

    private boolean tryLexNamespacedIdentifier() {
        int end = offset;
        while (end < text.length() && isNamespacedIdentifierPart(text.charAt(end))) {
            end++;
        }
        String candidate = text.substring(offset, end);
        int after = end;
        while (after < text.length() && isHorizontalWhitespace(text.charAt(after))) {
            after++;
        }
        if (candidate.indexOf(':') < 0 || after >= text.length() || text.charAt(after) != '(') {
            return false;
        }
        try {
            InyIdentifier.parse(candidate);
        } catch (InyInvalidIdentifierException ignored) {
            return false;
        }

        SourcePosition position = position();
        boolean firstOnLine = !lineHasToken;
        while (offset < end) {
            advance();
        }
        tokens.add(new Token(TokenType.NAMESPACED_IDENTIFIER, candidate, candidate, position, firstOnLine));
        lineHasToken = true;
        return true;
    }

    private void lexNumber() {
        SourcePosition position = position();
        boolean firstOnLine = !lineHasToken;
        int start = offset;
        if (peek() == '+' || peek() == '-') {
            advance();
        }
        consumeDigits();

        if (!atEnd() && peek() == '.') {
            advance();
            if (atEnd() || !isDigit(peek())) {
                throw error(position(), atEnd() ? "end of input" : printable(peek()), "a digit after '.'",
                        "Incomplete decimal literal");
            }
            consumeDigits();
        }

        if (!atEnd() && (peek() == 'e' || peek() == 'E')) {
            advance();
            if (!atEnd() && (peek() == '+' || peek() == '-')) {
                advance();
            }
            if (atEnd() || !isDigit(peek())) {
                throw error(position(), atEnd() ? "end of input" : printable(peek()), "an exponent digit",
                        "Incomplete numeric exponent");
            }
            consumeDigits();
        }

        String lexeme = text.substring(start, offset);
        tokens.add(new Token(TokenType.NUMBER, lexeme, lexeme, position, firstOnLine));
        lineHasToken = true;
    }

    private void consumeDigits() {
        while (!atEnd() && isDigit(peek())) {
            advance();
        }
    }

    private void lexString() {
        SourcePosition position = position();
        boolean firstOnLine = !lineHasToken;
        int start = offset;
        advance();
        StringBuilder value = new StringBuilder();

        while (!atEnd() && peek() != '"') {
            char character = advance();
            if (character == '\n' || character == '\r') {
                throw error(position(), "newline", "a closing quote",
                        "Quoted strings may not span lines");
            }
            if (character != '\\') {
                value.append(character);
                continue;
            }

            if (atEnd()) {
                throw error(position(), "end of input", "an escape character and closing quote",
                        "Unterminated string escape");
            }
            SourcePosition escapePosition = position();
            char escaped = advance();
            switch (escaped) {
                case '"' -> value.append('"');
                case '\\' -> value.append('\\');
                case '/' -> value.append('/');
                case 'b' -> value.append('\b');
                case 'f' -> value.append('\f');
                case 'n' -> value.append('\n');
                case 'r' -> value.append('\r');
                case 't' -> value.append('\t');
                case 'u' -> value.append(readUnicodeEscape(escapePosition));
                default -> throw error(escapePosition, "'\\" + escaped + "'",
                        "a valid escape (\\\", \\\\, \\/, \\b, \\f, \\n, \\r, \\t, or \\uXXXX)",
                        "Invalid string escape");
            }
        }

        if (atEnd()) {
            throw error(position, "end of input", "a closing quote", "Unterminated quoted string");
        }
        advance();
        tokens.add(new Token(TokenType.STRING, text.substring(start, offset), value.toString(), position, firstOnLine));
        lineHasToken = true;
    }

    private char readUnicodeEscape(SourcePosition escapePosition) {
        if (offset + 4 > text.length()) {
            throw error(escapePosition, "incomplete unicode escape", "four hexadecimal digits",
                    "Invalid unicode escape");
        }
        int value = 0;
        for (int count = 0; count < 4; count++) {
            char character = advance();
            int digit = Character.digit(character, 16);
            if (digit < 0) {
                throw error(positionAt(offset - 1, line, column - 1), printable(character), "a hexadecimal digit",
                        "Invalid unicode escape");
            }
            value = value * 16 + digit;
        }
        return (char) value;
    }

    private boolean isNumberStart() {
        char character = peek();
        if (isDigit(character)) {
            return true;
        }
        return (character == '+' || character == '-') && offset + 1 < text.length()
                && isDigit(text.charAt(offset + 1));
    }

    private char advance() {
        char character = advanceRaw();
        column++;
        return character;
    }

    private char advanceRaw() {
        return text.charAt(offset++);
    }

    private char peek() {
        return text.charAt(offset);
    }

    private boolean atEnd() {
        return offset >= text.length();
    }

    private SourcePosition position() {
        return positionAt(offset, line, column);
    }

    private SourcePosition positionAt(int atOffset, int atLine, int atColumn) {
        return new SourcePosition(source.name(), atOffset, atLine, atColumn);
    }

    private InyLexException error(
            SourcePosition position,
            String encountered,
            String expected,
            String explanation
    ) {
        return new InyLexException(source, position, encountered, expected, explanation);
    }

    private static boolean isIdentifierStart(char character) {
        return character >= 'A' && character <= 'Z'
                || character >= 'a' && character <= 'z'
                || character == '_';
    }

    private static boolean isIdentifierPart(char character) {
        return isIdentifierStart(character) || isDigit(character) || character == '-';
    }

    private static boolean isNamespacedIdentifierPart(char character) {
        return isIdentifierPart(character) || character == ':' || character == '.' || character == '/';
    }

    private static boolean isHorizontalWhitespace(char character) {
        return character == ' ' || character == '\t' || character == '\f';
    }

    private static boolean isDigit(char character) {
        return character >= '0' && character <= '9';
    }

    private static String printable(char character) {
        return switch (character) {
            case '\t' -> "tab";
            case '\n' -> "newline";
            case '\r' -> "carriage return";
            default -> "'" + character + "'";
        };
    }
}
