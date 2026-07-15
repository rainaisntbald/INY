package net.iridiummc.iny.value;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * An insertion-ordered, immutable, navigable view of an INY section.
 * Scalar values are exposed as their ordinary Java equivalents, nested sections as
 * {@code InySection}, and lists as immutable {@code List<Object>} values.
 */
public interface InySection {

    /** Returns an immutable insertion-ordered view of this section's entries. */
    Map<String, Object> entries();

    /** Returns a required relative dotted path as its ordinary Java representation. */
    default Object get(String path) {
        Objects.requireNonNull(path, "path");
        Map<String, Object> entries = entries();
        if (!entries.containsKey(path)) {
            throw new IllegalArgumentException("Section has no key '" + path + "'");
        }
        return entries.get(path);
    }

    /** Returns and decodes a required relative dotted path. */
    default <T> T get(String path, Class<T> type) {
        Objects.requireNonNull(type, "type");
        Object value = get(path);
        if (value == null) {
            if (type.isPrimitive()) {
                throw new IllegalArgumentException("Section value '" + path + "' is null");
            }
            return null;
        }
        if (!boxed(type).isInstance(value)) {
            throw new IllegalArgumentException(
                    "Section value '" + path + "' is not a " + type.getTypeName());
        }
        @SuppressWarnings("unchecked")
        T cast = (T) value;
        return cast;
    }

    /**
     * Returns a relative dotted path when present and non-null.
     * Use {@link #contains(String)} when an explicit null must be distinguished from a missing path.
     */
    default Optional<Object> find(String path) {
        Objects.requireNonNull(path, "path");
        return Optional.ofNullable(entries().get(path));
    }

    /** Returns and decodes a relative dotted path, or empty when missing or explicitly null. */
    default <T> Optional<T> find(String path, Class<T> type) {
        Objects.requireNonNull(type, "type");
        return find(path).map(value -> cast(path, value, type));
    }

    /** Tests whether a valid relative dotted path exists, including paths whose value is null. */
    default boolean contains(String path) {
        Objects.requireNonNull(path, "path");
        return entries().containsKey(path);
    }

    /** Returns a required nested section at a relative dotted path. */
    default InySection getSection(String path) {
        return get(path, InySection.class);
    }

    /** Returns a required immutable list of ordinary Java values at a relative dotted path. */
    default List<Object> getList(String path) {
        Object value = get(path);
        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException("Section value '" + path + "' is not a list");
        }
        @SuppressWarnings("unchecked")
        List<Object> cast = (List<Object>) list;
        return cast;
    }

    /** Returns a required list whose elements are decoded as the requested type. */
    default <T> List<T> getList(String path, Class<T> type) {
        Objects.requireNonNull(type, "type");
        List<Object> values = getList(path);
        ArrayList<T> decoded = new ArrayList<>(values.size());
        for (int index = 0; index < values.size(); index++) {
            decoded.add(cast(path + "[" + index + "]", values.get(index), type));
        }
        return decoded;
    }

    private static <T> T cast(String path, Object value, Class<T> type) {
        if (value == null) {
            if (type.isPrimitive()) {
                throw new IllegalArgumentException("Section value '" + path + "' is null");
            }
            return null;
        }
        if (!boxed(type).isInstance(value)) {
            throw new IllegalArgumentException(
                    "Section value '" + path + "' is not a " + type.getTypeName());
        }
        @SuppressWarnings("unchecked")
        T cast = (T) value;
        return cast;
    }

    private static Class<?> boxed(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == boolean.class) return Boolean.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        if (type == char.class) return Character.class;
        return type;
    }
}
