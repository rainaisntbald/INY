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

/** Internal construction boundary for public configuration and section views. */
public final class InyConfigs {

    private InyConfigs() {
    }

    /** Creates an immutable root configuration view. */
    public static InyConfig create(InyValueAccess values, InySectionValue root) {
        return new DefaultInyConfig(values, root);
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

        private DefaultInyConfig(InyValueAccess values, InySectionValue root) {
            super(values, root, "");
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
    }

    private static final class DefaultInySection extends AbstractInySection {

        private DefaultInySection(InyValueAccess values, InySectionValue section, String path) {
            super(values, section, path);
        }
    }
}
