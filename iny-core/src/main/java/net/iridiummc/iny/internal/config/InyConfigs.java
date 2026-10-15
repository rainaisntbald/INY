package net.iridiummc.iny.internal.config;

import net.iridiummc.iny.api.Iny;
import net.iridiummc.iny.api.InyConfig;
import net.iridiummc.iny.exception.InyMissingValueException;
import net.iridiummc.iny.exception.InyPathTraversalException;
import net.iridiummc.iny.internal.codec.InyValueAccess;
import net.iridiummc.iny.internal.lexer.Token;
import net.iridiummc.iny.internal.path.InyPath;
import net.iridiummc.iny.internal.value.InyList;
import net.iridiummc.iny.internal.value.InySectionValue;
import net.iridiummc.iny.internal.value.InyValue;
import net.iridiummc.iny.runtime.InyProvider;
import net.iridiummc.iny.source.InySource;
import net.iridiummc.iny.value.InySection;

import java.util.*;

/** Internal construction boundary for public configuration and section views. */
public final class InyConfigs {

    private InyConfigs() {
    }

    /** Creates an immutable root configuration view. */
    public static InyConfig create(InyValueAccess values, InySectionValue root, List<Token> tokens, InySource source, Iny context) {
        return new DefaultInyConfig(values, root, source, context);
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
            section.entries().forEach((key, entry) -> {
                InyValue value = entry.value();
                entries.put(key, values.resolve(value, Object.class, childPath(key)));
            });
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

        protected Optional<InyValue> resolve(String relativePath, String fullPath, boolean required) {
            Optional<InyEntry> entry = resolveEntry(relativePath, fullPath, required);
            return entry.map(InyEntry::value);
        }

        protected Optional<InyEntry> resolveEntry(String relativePath, String fullPath, boolean required) {
            InyPath path = InyPath.parse(relativePath);
            InySectionValue current = section;

            for (int index = 0; index < path.segments().size(); index++) {
                String segment = path.segments().get(index);
                InyEntry entry = current.entries().get(segment);
                boolean finalSegment = index == path.segments().size() - 1;
                if (entry == null) {
                    if (required) {
                        throw new InyMissingValueException(fullPath, segment, index, finalSegment);
                    }
                    return Optional.empty();
                }
                if (finalSegment) {
                    return Optional.of(entry);
                }
                if (!(entry.value() instanceof InySectionValue childSection)) {
                    throw new InyPathTraversalException(fullPath, segment, entry.value().actualType());
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
        public String toString() {
            return "InySection" + entries();
        }

        @Override
        public boolean equals(Object other) {
            if(!(other instanceof InySection otherSection)) return false;
            if(this == other) return true;
            return this.entries().equals(otherSection.entries());
        }

        @Override
        public int hashCode() {
            return this.entries().hashCode();
        }
    }

    private static final class DefaultInyConfig extends AbstractInySection implements InyConfig {

        private final InySource source;
        private final Iny context;

        private DefaultInyConfig(InyValueAccess values, InySectionValue root, InySource source, Iny context) {
            super(values, root, "");
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

        /*@Override
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
        public InyConfig setStringList(String path, List<String> newValue) {
            List<InyValue> values = newValue.stream().map(raw -> (InyValue) new InyString(raw)).toList();
            InyList list = new InyList(values);
            return set(path, list);
        }

        @Override
        public InyConfig setBooleanList(String path, List<Boolean> newValue) {
            List<InyValue> values = newValue.stream().map(raw -> (InyValue) new InyBoolean(raw)).toList();
            InyList list = new InyList(values);
            return set(path, list);
        }

        @Override
        public InyConfig setBigIntegerList(String path, List<BigInteger> newValue) {
            List<InyValue> values = newValue.stream().map(raw -> (InyValue) new InyInteger(raw)).toList();
            InyList list = new InyList(values);
            return set(path, list);
        }

        @Override
        public InyConfig setBigDecimalList(String path, List<BigDecimal> newValue) {
            List<InyValue> values = newValue.stream().map(raw -> (InyValue) new InyDecimal(raw)).toList();
            InyList list = new InyList(values);
            return set(path, list);
        }

        @Override
        public void save(Path path) throws IOException {
            Files.writeString(path, source.text());
        }

        private InyConfig set(String path, InyValue newValue) {
            InyEntry entry = resolveEntry(path, path, true).orElseThrow();
            int startOffset = entry.start().offset();
            int endOffset = startOffset + entry.length();

            StringBuilder builder = new StringBuilder(source.text());
            builder.replace(startOffset, endOffset, ": " + newValue.toText());

            InySource newSource = new InySource(source.name(), builder.toString());
            return context.parse(newSource);
        }*/
    }

    private static final class DefaultInySection extends AbstractInySection {

        private DefaultInySection(InyValueAccess values, InySectionValue section, String path) {
            super(values, section, path);
        }

    }
}
