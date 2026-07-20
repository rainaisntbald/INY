package net.iridiummc.iny.value;

import net.iridiummc.iny.exception.InyInvalidPathException;
import net.iridiummc.iny.exception.InyMissingValueException;
import net.iridiummc.iny.exception.InyPathTraversalException;
import net.iridiummc.iny.exception.InyInvalidProviderResultException;
import net.iridiummc.iny.exception.InyNotProviderException;
import net.iridiummc.iny.runtime.InyProvider;
import net.iridiummc.iny.runtime.InyRunnable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
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

    /**
     * Returns an immutable insertion-ordered view of this section's entries.
     *
     * @return immutable entries keyed by their local names
     */
    Map<String, Object> entries();

    /**
     * Returns a required relative dotted path as its ordinary Java representation.
     *
     * @param path dotted path relative to this section
     * @return the resolved Java value
     */
    default Object get(String path) {
        return resolve(this, path, true, null);
    }

    /**
     * Returns and decodes a required relative dotted path.
     *
     * @param path dotted path relative to this section
     * @param type requested Java type
     * @param <T> requested Java type
     * @return the decoded value
     */
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
     *
     * @param path dotted path relative to this section
     * @return the resolved Java value, or empty when missing or explicitly null
     */
    default Optional<Object> find(String path) {
        Object missing = new Object();
        Object value = resolve(this, path, false, missing);
        return value == missing ? Optional.empty() : Optional.ofNullable(value);
    }

    /**
     * Returns and decodes a relative dotted path, or empty when missing or explicitly null.
     *
     * @param path dotted path relative to this section
     * @param type requested Java type
     * @param <T> requested Java type
     * @return the decoded value, or empty when missing or explicitly null
     */
    default <T> Optional<T> find(String path, Class<T> type) {
        Objects.requireNonNull(type, "type");
        return find(path).map(value -> cast(path, value, type));
    }

    /**
     * Tests whether a valid relative dotted path exists, including paths whose value is null.
     *
     * @param path dotted path relative to this section
     * @return {@code true} when the path exists
     */
    default boolean contains(String path) {
        Object missing = new Object();
        return resolve(this, path, false, missing) != missing;
    }

    /**
     * Returns a required nested section at a relative dotted path.
     *
     * @param path dotted path relative to this section
     * @return the nested section
     */
    default InySection getSection(String path) {
        return get(path, InySection.class);
    }

    /**
     * Returns a required deferred action without executing it.
     *
     * @param path dotted path relative to this section
     * @return deferred action
     */
    default InyRunnable getRunnable(String path) {
        return get(path, InyRunnable.class);
    }

    /**
     * Returns a checked deferred provider. Plain values become constant providers; actions are rejected.
     *
     * @param path dotted path relative to this section
     * @param targetType requested provider result type
     * @param <T> provider result type
     * @return checked deferred or constant provider
     */
    default <T> InyProvider<T> getProvider(String path, Class<T> targetType) {
        Objects.requireNonNull(targetType, "targetType");
        Object value = get(path);
        if (value instanceof InyProvider<?> provider) {
            return runtime -> checkedProviderResult(
                    path, provider.resolve(Objects.requireNonNull(runtime, "runtime context")), targetType);
        }
        if (value instanceof InyRunnable runnable) {
            throw new InyNotProviderException(path, targetType, runnable.getClass());
        }
        T constant = get(path, targetType);
        if (constant == null) {
            throw new InyInvalidProviderResultException(path, targetType, null);
        }
        return runtime -> {
            Objects.requireNonNull(runtime, "runtime context");
            return constant;
        };
    }

    /**
     * Returns a required immutable list of ordinary Java values at a relative dotted path.
     *
     * @param path dotted path relative to this section
     * @return an immutable list of Java values
     */
    default List<Object> getList(String path) {
        Object value = get(path);
        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException("Section value '" + path + "' is not a list");
        }
        ArrayList<Object> copy = new ArrayList<>(list.size());
        copy.addAll(list);
        return Collections.unmodifiableList(copy);
    }

    /**
     * Returns a required immutable list whose elements are decoded as the requested type.
     *
     * @param path dotted path relative to this section
     * @param type requested element type
     * @param <T> requested element type
     * @return an immutable list of decoded values
     */
    default <T> List<T> getList(String path, Class<T> type) {
        Objects.requireNonNull(type, "type");
        List<Object> values = getList(path);
        ArrayList<T> decoded = new ArrayList<>(values.size());
        for (int index = 0; index < values.size(); index++) {
            decoded.add(cast(path + "[" + index + "]", values.get(index), type));
        }
        return Collections.unmodifiableList(decoded);
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

    private static <T> T checkedProviderResult(String path, Object result, Class<T> type) {
        Class<?> boxedType = boxed(type);
        if (result == null || !boxedType.isInstance(result)) {
            throw new InyInvalidProviderResultException(path, type, result);
        }
        @SuppressWarnings("unchecked")
        T cast = (T) result;
        return cast;
    }

    private static Object resolve(InySection root, String path, boolean required, Object missing) {
        List<String> segments = segments(path);
        InySection current = root;

        for (int index = 0; index < segments.size(); index++) {
            String segment = segments.get(index);
            Map<String, Object> entries = current.entries();
            boolean finalSegment = index == segments.size() - 1;
            if (!entries.containsKey(segment)) {
                if (required) {
                    throw new InyMissingValueException(path, segment, index, finalSegment);
                }
                return missing;
            }

            Object value = entries.get(segment);
            if (finalSegment) {
                return value;
            }
            if (!(value instanceof InySection childSection)) {
                throw new InyPathTraversalException(path, segment, actualType(value));
            }
            current = childSection;
        }
        throw new AssertionError("Validated paths always contain at least one segment");
    }

    private static List<String> segments(String path) {
        if (path == null) {
            throw new InyInvalidPathException("null", "path must not be null");
        }
        if (path.isEmpty()) {
            throw new InyInvalidPathException(path, "path must contain at least one segment");
        }
        if (!path.equals(path.trim())) {
            throw new InyInvalidPathException(path, "leading or trailing whitespace is not allowed");
        }

        List<String> segments = List.of(path.split("\\.", -1));
        for (int index = 0; index < segments.size(); index++) {
            String segment = segments.get(index);
            if (!segment.matches("[A-Za-z_][A-Za-z0-9_-]*")) {
                throw new InyInvalidPathException(path,
                        "segment " + (index + 1) + " ('" + segment + "') is not a valid bare key");
            }
        }
        return segments;
    }

    private static String actualType(Object value) {
        if (value == null) return "null";
        if (value instanceof String) return "string";
        if (value instanceof Boolean) return "boolean";
        if (value instanceof Byte || value instanceof Short || value instanceof Integer
                || value instanceof Long || value instanceof BigInteger) return "integer";
        if (value instanceof Float || value instanceof Double || value instanceof BigDecimal) return "decimal";
        if (value instanceof Number) return "number";
        if (value instanceof List<?>) return "list";
        return value.getClass().getTypeName();
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
