package net.iridiummc.iny.internal.config;

import net.iridiummc.iny.api.InyConfig;
import net.iridiummc.iny.exception.InyMissingValueException;
import net.iridiummc.iny.exception.InyPathTraversalException;
import net.iridiummc.iny.internal.codec.InyValueAccess;
import net.iridiummc.iny.internal.path.InyPath;
import net.iridiummc.iny.internal.value.InyList;
import net.iridiummc.iny.internal.value.InySectionValue;
import net.iridiummc.iny.internal.value.InyValue;
import net.iridiummc.iny.value.InySection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Internal construction boundary for public configuration views. */
public final class InyConfigs {

    private InyConfigs() {
    }

    /** Creates an immutable configuration view backed by the supplied service and root section. */
    public static InyConfig create(InyValueAccess values, InySectionValue root) {
        return new DefaultInyConfig(values, root);
    }

    /** Creates a public structural view over an internal section node. */
    public static InySection view(InyValueAccess values, InySectionValue section, String path) {
        return new DefaultInySection(values, section, path);
    }

    private static final class DefaultInyConfig implements InyConfig {

        private final InyValueAccess values;
        private final InySectionValue root;
        private final InySection rootView;

        private DefaultInyConfig(InyValueAccess values, InySectionValue root) {
            this.values = Objects.requireNonNull(values, "values");
            this.root = Objects.requireNonNull(root, "root");
            this.rootView = view(values, root, "");
        }

        @Override
        public InySection root() {
            return rootView;
        }

        @Override
        public <T> T get(String path, Class<T> type) {
            Objects.requireNonNull(type, "type");
            return values.resolve(resolve(path, true).orElseThrow(), type, path);
        }

        @Override
        public <T> Optional<T> find(String path, Class<T> type) {
            Objects.requireNonNull(type, "type");
            return resolve(path, false).map(value -> values.resolve(value, type, path));
        }

        @Override
        public boolean contains(String path) {
            return resolve(path, false).isPresent();
        }

        @Override
        public Object getValue(String path) {
            return get(path, Object.class);
        }

        @Override
        public Optional<Object> findValue(String path) {
            return find(path, Object.class);
        }

        @Override
        public InySection getSection(String path) {
            return get(path, InySection.class);
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<Object> getList(String path) {
            return (List<Object>) get(path, List.class);
        }

        @Override
        public <T> List<T> getList(String path, Class<T> type) {
            Objects.requireNonNull(type, "type");
            InyValue value = resolve(path, true).orElseThrow();
            if (!(value instanceof InyList list)) {
                values.resolve(value, List.class, path);
                throw new AssertionError("List decoder accepted a non-list value");
            }
            ArrayList<T> decoded = new ArrayList<>(list.values().size());
            for (int index = 0; index < list.values().size(); index++) {
                decoded.add(values.resolve(list.values().get(index), type, path + "[" + index + "]"));
            }
            return decoded;
        }

        private Optional<InyValue> resolve(String pathText, boolean required) {
            InyPath path = InyPath.parse(pathText);
            InySectionValue section = root;

            for (int index = 0; index < path.segments().size(); index++) {
                String segment = path.segments().get(index);
                InyValue value = section.entries().get(segment);
                boolean finalSegment = index == path.segments().size() - 1;
                if (value == null) {
                    if (required) {
                        throw new InyMissingValueException(pathText, segment, index, finalSegment);
                    }
                    return Optional.empty();
                }
                if (finalSegment) {
                    return Optional.of(value);
                }
                if (!(value instanceof InySectionValue childSection)) {
                    throw new InyPathTraversalException(pathText, segment, value.type());
                }
                section = childSection;
            }
            throw new AssertionError("Validated paths always contain at least one segment");
        }
    }

    private static final class DefaultInySection implements InySection {

        private final InyValueAccess values;
        private final InySectionValue section;
        private final String path;

        private DefaultInySection(InyValueAccess values, InySectionValue section, String path) {
            this.values = Objects.requireNonNull(values, "values");
            this.section = Objects.requireNonNull(section, "section");
            this.path = Objects.requireNonNull(path, "path");
        }

        @Override
        public Map<String, Object> entries() {
            LinkedHashMap<String, Object> entries = new LinkedHashMap<>();
            section.entries().forEach((key, value) ->
                    entries.put(key, values.resolve(value, Object.class, childPath(key))));
            return Collections.unmodifiableMap(entries);
        }

        @Override
        public Optional<Object> find(String key) {
            Objects.requireNonNull(key, "key");
            InyValue value = section.entries().get(key);
            return value == null
                    ? Optional.empty()
                    : Optional.ofNullable(values.resolve(value, Object.class, childPath(key)));
        }

        @Override
        public Object get(String key) {
            Objects.requireNonNull(key, "key");
            InyValue value = section.entries().get(key);
            if (value == null) {
                throw new IllegalArgumentException("Section has no key '" + key + "'");
            }
            return values.resolve(value, Object.class, childPath(key));
        }

        @Override
        public boolean contains(String key) {
            Objects.requireNonNull(key, "key");
            return section.entries().containsKey(key);
        }

        private String childPath(String key) {
            return path.isEmpty() ? key : path + "." + key;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (object instanceof DefaultInySection other) {
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
}
