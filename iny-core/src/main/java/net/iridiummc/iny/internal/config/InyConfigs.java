package net.iridiummc.iny.internal.config;

import net.iridiummc.iny.api.Iny;
import net.iridiummc.iny.api.InyConfig;
import net.iridiummc.iny.exception.InyMissingValueException;
import net.iridiummc.iny.exception.InyPathTraversalException;
import net.iridiummc.iny.internal.Utils;
import net.iridiummc.iny.internal.codec.InyValueAccess;
import net.iridiummc.iny.internal.lexer.Token;
import net.iridiummc.iny.internal.lexer.TokenType;
import net.iridiummc.iny.internal.path.InyPath;
import net.iridiummc.iny.internal.value.*;
import net.iridiummc.iny.source.InySource;
import net.iridiummc.iny.value.InySection;
import net.iridiummc.iny.runtime.InyProvider;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Internal construction boundary for public configuration and section views. */
public final class InyConfigs {

    private InyConfigs() {
    }

    /** Creates an immutable root configuration view. */
    public static InyConfig create(InyValueAccess values, InySectionValue root, List<Token> tokens, InySource source, Iny context) {
        return new DefaultInyConfig(values, root, tokens, source, context);
    }

    /** Creates a navigable public view over an internal section node. */
    public static InySection view(InyValueAccess values, InySectionValue section, String path) {
        return new DefaultInySection(values, section, path);
    }

    private abstract static class AbstractInySection implements InySection {

        private final InyValueAccess values;
        private final InySectionValue section;
        private final String basePath;

        private AbstractInySection(InyValueAccess values, InySectionValue section, String basePath) {
            this.values = Objects.requireNonNull(values, "values");
            this.section = Objects.requireNonNull(section, "section");
            this.basePath = Objects.requireNonNull(basePath, "basePath");
        }

        @Override
        public Map<String, Object> entries() {
            LinkedHashMap<String, Object> entries = new LinkedHashMap<>();
            section.entries().forEach((key, value) ->
                    entries.put(key, values.resolve(value, Object.class, childPath(key))));
            return Collections.unmodifiableMap(entries);
        }

        @Override
        public Object get(String path) {
            return get(path, Object.class);
        }

        @Override
        public <T> T get(String path, Class<T> type) {
            Objects.requireNonNull(type, "type");
            String fullPath = fullPath(path);
            return values.resolve(resolve(path, fullPath, true).orElseThrow(), type, fullPath);
        }

        @Override
        public Optional<Object> find(String path) {
            return find(path, Object.class);
        }

        @Override
        public <T> Optional<T> find(String path, Class<T> type) {
            Objects.requireNonNull(type, "type");
            String fullPath = fullPath(path);
            return resolve(path, fullPath, false).map(value -> values.resolve(value, type, fullPath));
        }

        @Override
        public boolean contains(String path) {
            String fullPath = fullPath(path);
            return resolve(path, fullPath, false).isPresent();
        }

        @Override
        public InySection getSection(String path) {
            return get(path, InySection.class);
        }

        @Override
        public <T> InyProvider<T> getProvider(String path, Class<T> targetType) {
            Objects.requireNonNull(targetType, "targetType");
            String fullPath = fullPath(path);
            InyValue value = resolve(path, fullPath, true).orElseThrow();
            return values.provider(value, targetType, fullPath);
        }

        @Override
        public List<Object> getList(String path) {
            List<?> list = get(path, List.class);
            ArrayList<Object> copy = new ArrayList<>(list.size());
            copy.addAll(list);
            return Collections.unmodifiableList(copy);
        }

        @Override
        public <T> List<T> getList(String path, Class<T> type) {
            Objects.requireNonNull(type, "type");
            String fullPath = fullPath(path);
            InyValue value = resolve(path, fullPath, true).orElseThrow();
            if (!(value instanceof InyList list)) {
                values.resolve(value, List.class, fullPath);
                throw new AssertionError("List decoder accepted a non-list value");
            }
            ArrayList<T> decoded = new ArrayList<>(list.values().size());
            for (int index = 0; index < list.values().size(); index++) {
                decoded.add(values.resolve(
                        list.values().get(index), type, fullPath + "[" + index + "]"));
            }
            return Collections.unmodifiableList(decoded);
        }

        private Optional<InyValue> resolve(String relativePath, String fullPath, boolean required) {
            InyPath path = InyPath.parse(relativePath);
            InySectionValue current = section;

            for (int index = 0; index < path.segments().size(); index++) {
                String segment = path.segments().get(index);
                InyValue value = current.entries().get(segment);
                boolean finalSegment = index == path.segments().size() - 1;
                if (value == null) {
                    if (required) {
                        throw new InyMissingValueException(fullPath, segment, index, finalSegment);
                    }
                    return Optional.empty();
                }
                if (finalSegment) {
                    return Optional.of(value);
                }
                if (!(value instanceof InySectionValue childSection)) {
                    throw new InyPathTraversalException(fullPath, segment, value.actualType());
                }
                current = childSection;
            }
            throw new AssertionError("Validated paths always contain at least one segment");
        }

        private String fullPath(String relativePath) {
            Objects.requireNonNull(relativePath, "path");
            return basePath.isEmpty() ? relativePath : basePath + "." + relativePath;
        }

        private String childPath(String key) {
            return basePath.isEmpty() ? key : basePath + "." + key;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (object instanceof AbstractInySection other) {
                return section.equals(other.section);
            }
            return object instanceof InySection other && entries().equals(other.entries());
        }

        @Override
        public int hashCode() {
            return section.hashCode();
        }

        @Override
        public String toString() {
            return "InySection" + entries();
        }
    }

    private static final class DefaultInyConfig extends AbstractInySection implements InyConfig {

        private final List<Token> tokens;
        private final InySource source;
        private final Iny context;

        private DefaultInyConfig(InyValueAccess values, InySectionValue root, List<Token> tokens, InySource source, Iny context) {
            super(values, root, "");
            this.tokens = tokens;
            this.source = source;
            this.context = context;
        }

        @Override
        public InySection root() {
            return this;
        }

        @Override
        public Object getValue(String path) {
            return get(path);
        }

        @Override
        public Optional<Object> findValue(String path) {
            return find(path);
        }

        @Override
        public InyConfig setString(String path, String newValue) {
            InyString inyString = new InyString(newValue);
            return set(path, inyString);
        }

        @Override
        public InyConfig setBoolean(String path, Boolean newValue) {
            InyBoolean inyBoolean = new InyBoolean(newValue);
            return set(path, inyBoolean);
        }

        @Override
        public InyConfig setInteger(String path, BigInteger integer) {
            InyInteger inyInteger = new InyInteger(integer);
            return set(path, inyInteger);
        }

        @Override
        public InyConfig setDecimal(String path, BigDecimal decimal) {
            InyDecimal inyDecimal = new InyDecimal(decimal);
            return set(path, inyDecimal);
        }

        @Override
        public InyConfig setNull(String path) {
            InyNull inyNull = InyNull.INSTANCE;
            return set(path, inyNull);
        }

        @Override
        public void save(Path path) throws IOException {
            Files.writeString(path, source.text());
        }

        private InyConfig set(String path, InyValue newValue) {
            List<String> segments = Utils.segments(path);
            int scanIndex = getScanIndex(path, segments);
            while (scanIndex < tokens.size() && tokens.get(scanIndex).type() == TokenType.COLON) {
                scanIndex++;
            }

            if (scanIndex >= tokens.size()) {
                throw new IllegalStateException("Malformed config: Missing value after key.");
            }

            Token startValueToken = tokens.get(scanIndex);
            Token endValueToken = startValueToken;

            while (scanIndex < tokens.size()) {
                TokenType nextType = tokens.get(scanIndex).type();
                if (nextType == TokenType.IDENTIFIER ||
                        nextType == TokenType.RIGHT_BRACE ||
                        nextType == TokenType.EOF) {
                    break;
                }
                endValueToken = tokens.get(scanIndex);
                scanIndex++;
            }

            int startOffset = startValueToken.position().offset();
            int endOffset = endValueToken.position().offset() + endValueToken.lexeme().length();

            StringBuilder builder = new StringBuilder(source.text());
            builder.replace(startOffset, endOffset, " " + newValue.toText() + "\n");

            InySource newSource = new InySource(source.name(), builder.toString());
            return context.parse(newSource);
        }

        private int getScanIndex(String path, List<String> segments) {
            int currentSegmentIndex = 0;

            int keyTokenIndex = -1;
            int currentDepth = 0;

            for (int i = 0; i < tokens.size(); i++) {
                Token token = tokens.get(i);

                if (token.type() == TokenType.LEFT_BRACE) {
                    currentDepth++;
                } else if (token.type() == TokenType.RIGHT_BRACE) {
                    currentDepth--;
                }

                if (token.type() == TokenType.IDENTIFIER &&
                        token.lexeme().equals(segments.get(currentSegmentIndex)) &&
                        currentDepth == currentSegmentIndex) {

                    currentSegmentIndex++;

                    if (currentSegmentIndex == segments.size()) {
                        keyTokenIndex = i;
                        break;
                    }
                }
            }

            if (keyTokenIndex == -1) {
                throw new IllegalArgumentException("Path not found: " + path);
            }

            return keyTokenIndex + 1;
        }

    }

    private static final class DefaultInySection extends AbstractInySection {

        private DefaultInySection(InyValueAccess values, InySectionValue section, String path) {
            super(values, section, path);
        }
    }
}
