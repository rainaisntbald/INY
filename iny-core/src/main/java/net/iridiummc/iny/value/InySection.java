package net.iridiummc.iny.value;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * An insertion-ordered, immutable structural view of an INY section.
 * Scalar values are exposed as their ordinary Java equivalents, nested sections as
 * {@code InySection}, and lists as immutable {@code List<Object>} values.
 */
public interface InySection {

    /** Returns an immutable insertion-ordered view of this section's entries. */
    Map<String, Object> entries();

    /**
     * Returns a value when the key exists and its value is not INY {@code null}.
     * Use {@link #contains(String)} when an explicit null must be distinguished from a missing key.
     */
    default Optional<Object> find(String key) {
        Objects.requireNonNull(key, "key");
        return Optional.ofNullable(entries().get(key));
    }

    /** Returns the value for a required key, including Java {@code null} for INY {@code null}. */
    default Object get(String key) {
        Objects.requireNonNull(key, "key");
        Map<String, Object> entries = entries();
        if (!entries.containsKey(key)) {
            throw new IllegalArgumentException("Section has no key '" + key + "'");
        }
        return entries.get(key);
    }

    /** Tests whether this section contains the supplied direct child key. */
    default boolean contains(String key) {
        Objects.requireNonNull(key, "key");
        return entries().containsKey(key);
    }
}
