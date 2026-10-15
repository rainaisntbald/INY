package net.iridiummc.iny.internal.parser;

import net.iridiummc.iny.api.InyIdentifier;
import net.iridiummc.iny.exception.InyParseException;
import net.iridiummc.iny.internal.config.InyEntry;
import net.iridiummc.iny.internal.lexer.Token;
import net.iridiummc.iny.internal.lexer.TokenType;
import net.iridiummc.iny.source.InySource;
import net.iridiummc.iny.source.SourcePosition;
import net.iridiummc.iny.internal.value.InyBoolean;
import net.iridiummc.iny.internal.value.InyCall;
import net.iridiummc.iny.internal.value.InyDecimal;
import net.iridiummc.iny.internal.value.InyInteger;
import net.iridiummc.iny.internal.value.InyList;
import net.iridiummc.iny.internal.value.InyNull;
import net.iridiummc.iny.internal.value.InySectionValue;
import net.iridiummc.iny.internal.value.InyString;
import net.iridiummc.iny.internal.value.InyValue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Recursive-descent parser for the position-aware INY token stream. */
public final class Parser {

    private final InySource source;
    private final List<Token> tokens;
    private int current;

    public Parser(InySource source, List<Token> tokens) {
        this.source = Objects.requireNonNull(source, "source");
        this.tokens = List.copyOf(tokens);
    }

    public InySectionValue parse() {
        skipNewlines();
        InySectionValue root = parseEntries(TokenType.EOF);
        consume(TokenType.EOF, "end of input", "Unexpected content after the root section");
        return root;
    }

    private InySectionValue parseEntries(TokenType terminator) {
        Map<String, InyEntry> entries = new LinkedHashMap<>();
        Map<String, SourcePosition> keyPositions = new LinkedHashMap<>();

        while (!check(terminator) && !check(TokenType.EOF)) {
            Token key = consume(TokenType.IDENTIFIER, "a bare key",
                    "Every section entry must begin with a key");
            InyEntry value = parseEntry(key);

            SourcePosition previous = keyPositions.putIfAbsent(key.value(), key.position());
            if (previous != null) {
                throw error(key, "a unique key", "Duplicate key '" + key.value()
                        + "' (first declared at " + previous + ")");
            }
            entries.put(key.value(), value);

            if (check(terminator) || check(TokenType.EOF)) {
                continue;
            }
            if (isEntryStart() && lineBoundaryBeforeCurrent()) {
                continue;
            }
            if (!match(TokenType.NEWLINE)) {
                throw error(peek(), "a newline or " + display(terminator),
                        "Entries must be separated by a line boundary");
            }
            skipNewlines();
        }

        if (terminator != TokenType.EOF && check(TokenType.EOF)) {
            throw error(peek(), display(terminator), "Section was not closed");
        }
        return new InySectionValue(entries);
    }

    private InyEntry parseEntry(Token key) {
        SourcePosition start = null;
        InyValue value = null;

        if (check(TokenType.LEFT_BRACE)) {
            start = advance().position();
            value = parseBracedSection();
        }

        if (!check(TokenType.COLON) && value == null) {
            throw error(peek(), "':' or '{' after key '" + key.value() + "'",
                    "Missing structural separator after key");
        }

        if(value == null) {
            advance();
        }

        if (check(TokenType.LEFT_BRACE)) {
            start = advance().position();
            value = parseBracedSection();
        }

        if (check(TokenType.NEWLINE)) {
            skipNewlines();
            if (check(TokenType.LEFT_BRACE)) {
                start = advance().position();
                value = parseBracedSection();
            }
            if (isListMarker(peek())) {
                start = peek().position();
                value = parseList(false);
            }
            if(value == null) {
                throw error(peek(), "'{' or a dash list after the line boundary",
                        "A value placed after a key's newline must be structural");
            }
        }

        if(value == null) {
            start = peek().position();
            value = parseValue();
        }

        Token lastToken = peekPrevious();
        int lastTokenEndOffset = lastToken.position().offset() + lastToken.lexeme().length();
        int valueLength = lastTokenEndOffset - start.offset();

        return new InyEntry(value, start, valueLength);
    }

    private InySectionValue parseBracedSection() {
        skipNewlines();
        if (match(TokenType.RIGHT_BRACE)) {
            return new InySectionValue(Map.of());
        }
        InySectionValue section = parseEntries(TokenType.RIGHT_BRACE);
        consume(TokenType.RIGHT_BRACE, "'}'", "Section was not closed");
        return section;
    }

    private InyList parseList(boolean stopBeforeStandaloneDash) {
        List<InyValue> values = new ArrayList<>();
        SourcePosition listPosition = peek().position();

        while (isListMarker(peek())) {
            advance();
            if (check(TokenType.NEWLINE)) {
                skipNewlines();
                if (!isListMarker(peek())) {
                    throw error(peek(), "a child dash list", "A standalone dash must introduce a nested list");
                }
                values.add(parseList(true));
            } else if (match(TokenType.LEFT_BRACE)) {
                values.add(parseBracedSection());
            } else if (isValueStart(peek())) {
                values.add(parseValue());
            } else {
                throw error(peek(), "a scalar, '{', or a newline followed by a child list",
                        "List entry after dash has no valid value");
            }

            if (isValueTerminator()) {
                break;
            }
            if (!lineBoundaryBeforeCurrent() && !match(TokenType.NEWLINE)) {
                throw error(peek(), "a newline after the list entry",
                        "Each list entry must occupy one logical line");
            }
            skipNewlines();

            if (stopBeforeStandaloneDash && !values.isEmpty() && isStandaloneDash()) {
                break;
            }
            if (isListMarker(peek())) {
                continue;
            }
            if (isEntryStart()) {
                break;
            }
            if (isValueTerminator()) {
                break;
            }
            throw error(peek(), "another dash entry, a section key, a closing brace, or end of input",
                    "Invalid token after list entry");
        }

        if (values.isEmpty()) {
            throw error(peek(), "at least one dash entry", "Empty dash lists have no representation in INY v1");
        }

        InyList list = new InyList(values);
        if(!mixedListChecker(list).isValid) mixedListWarning(listPosition);
        return list;
    }

    private InyList parseInlineList() {
        List<InyValue> values = new ArrayList<>();
        SourcePosition listPosition = peekPrevious().position();

        boolean continueFlag = true;

        while(continueFlag) {
            values.add(parseValue());
            if(!check(TokenType.RIGHT_SQUARE)) consume(TokenType.COMMA, ", or ]",
                    "Inline lists must be comma separated and surrounded by square brackets");
            else continueFlag = false;
        }
        advance();

        InyList list = new InyList(values);

        if(!mixedListChecker(list).isValid) mixedListWarning(listPosition);
        return list;
    }

    private MixedListResult mixedListChecker(InyValue value) {
        if(value instanceof InyList(List<InyValue> values)) {
            MixedListResult ERROR_RETURN = new MixedListResult(Optional.empty(), -1, false);

            if(values.isEmpty()) return new MixedListResult(Optional.empty(), 1, true);
            List<MixedListResult> results = values.stream().map(this::mixedListChecker).toList();

            int depth = results.getFirst().depth;
            Optional<Class<? extends InyValue>> clazz = Optional.empty();

            for(MixedListResult result : results) {
                if(!result.isValid) return ERROR_RETURN;
                if(result.depth != depth) return ERROR_RETURN;
                if(result.clazz.isPresent()) {
                    if(clazz.isEmpty()) clazz = result.clazz;
                    if(!clazz.equals(result.clazz)) return ERROR_RETURN;
                }
            }
            return new MixedListResult(clazz, depth + 1, true);
        }
        return new MixedListResult(Optional.of(value.getClass()), 0, true);
    }

    private record MixedListResult(Optional<Class<? extends InyValue>> clazz, int depth, boolean isValid) {
    };

    private void mixedListWarning(SourcePosition listPosition) {
        Logger.getLogger("net.iridiummc.INY").warning(String.format(
                "You are using a mixed list in\n%s at line %d, column %d\nMixed type lists will be deprecated in 2.0.0",
                listPosition.sourceName(),
                listPosition.line(),
                listPosition.column()
        ));
    }

    private InyValue parseValue() {
        Token token = peek();
        if (match(TokenType.NAMESPACED_IDENTIFIER)) {
            return parseCall(token);
        }
        if (match(TokenType.LEFT_BRACE)) {
            return parseBracedSection();
        }
        if (isListMarker(token)) {
            return parseList(false);
        }
        if (match(TokenType.LEFT_SQUARE)) {
            return parseInlineList();
        }
        if (match(TokenType.STRING)) {
            return new InyString(token.value());
        }
        if (match(TokenType.NUMBER)) {
            try {
                if (token.lexeme().indexOf('.') >= 0 || token.lexeme().indexOf('e') >= 0
                        || token.lexeme().indexOf('E') >= 0) {
                    return new InyDecimal(new BigDecimal(token.lexeme()));
                }
                return new InyInteger(new BigInteger(token.lexeme()));
            } catch (NumberFormatException exception) {
                throw error(token, "a valid numeric literal", "Invalid numeric value");
            }
        }
        if (match(TokenType.IDENTIFIER)) {
            return switch (token.value()) {
                case "true" -> new InyBoolean(true);
                case "false" -> new InyBoolean(false);
                case "null" -> InyNull.INSTANCE;
                default -> new InyString(token.value());
            };
        }
        throw error(token, "a quoted string, number, boolean, null, or bare identifier",
                "Expected an INY value");
    }

    private InyCall parseCall(Token identifierToken) {
        InyIdentifier identifier = InyIdentifier.parse(identifierToken.value());
        consume(TokenType.LEFT_PARENTHESIS, "'(' after " + identifier,
                "A namespaced call must have an argument list");
        skipNewlines();

        List<InyValue> arguments = new ArrayList<>();
        if (match(TokenType.RIGHT_PARENTHESIS)) {
            return new InyCall(identifier, arguments, identifierToken.position());
        }

        while (true) {
            if (!isValueStart(peek()) && !check(TokenType.LEFT_BRACE) && !isListMarker(peek())) {
                throw error(peek(), "an argument value or ')'",
                        "Factory call argument is missing or malformed");
            }
            arguments.add(parseValue());
            skipNewlines();

            if (match(TokenType.RIGHT_PARENTHESIS)) {
                break;
            }
            consume(TokenType.COMMA, "',' or ')' after a factory argument",
                    "Factory arguments must be comma-separated");
            skipNewlines();
            if (check(TokenType.RIGHT_PARENTHESIS)) {
                throw error(peek(), "an argument after ','", "Trailing commas are not supported");
            }
        }
        return new InyCall(identifier, arguments, identifierToken.position());
    }

    private boolean isEntryStart() {
        if (!check(TokenType.IDENTIFIER)) {
            return false;
        }
        TokenType following = lookAhead(1).type();
        return following == TokenType.COLON || following == TokenType.LEFT_BRACE;
    }

    private boolean isStandaloneDash() {
        return isListMarker(peek()) && lookAhead(1).type() == TokenType.NEWLINE;
    }

    private boolean lineBoundaryBeforeCurrent() {
        return current > 0 && tokens.get(current - 1).type() == TokenType.NEWLINE;
    }

    private boolean isListMarker(Token token) {
        return token.type() == TokenType.DASH && token.firstOnLine();
    }

    private boolean isValueStart(Token token) {
        return token.type() == TokenType.STRING || token.type() == TokenType.NUMBER
                || token.type() == TokenType.IDENTIFIER
                || token.type() == TokenType.NAMESPACED_IDENTIFIER
                || token.type() == TokenType.LEFT_BRACE
                || isListMarker(token);
    }

    private boolean isValueTerminator() {
        return check(TokenType.RIGHT_BRACE) || check(TokenType.RIGHT_PARENTHESIS)
                || check(TokenType.COMMA) || check(TokenType.EOF);
    }

    private void skipNewlines() {
        while (match(TokenType.NEWLINE)) {
            // Line boundaries are separators; their quantity has no meaning.
        }
    }

    private Token consume(TokenType type, String expected, String explanation) {
        if (check(type)) {
            return advance();
        }
        throw error(peek(), expected, explanation);
    }

    private boolean match(TokenType type) {
        if (!check(type)) {
            return false;
        }
        advance();
        return true;
    }

    private boolean check(TokenType type) {
        return peek().type() == type;
    }

    private Token advance() {
        Token token = peek();
        if (token.type() != TokenType.EOF) {
            current++;
        }
        return token;
    }

    private Token peekPrevious() {
        return tokens.get(current - 1);
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token lookAhead(int distance) {
        return tokens.get(Math.min(current + distance, tokens.size() - 1));
    }

    private InyParseException error(Token token, String expected, String explanation) {
        return new InyParseException(source, token.position(), token.display(), expected, explanation);
    }

    private static String display(TokenType type) {
        return switch (type) {
            case RIGHT_BRACE -> "'}'";
            case EOF -> "end of input";
            default -> type.name().toLowerCase();
        };
    }
}
