package net.iridiummc.iny.runtime;

import net.iridiummc.iny.api.InyIdentifier;

import java.util.Objects;

/**
 * A typed namespaced key for a value supplied when runtime configuration is executed.
 *
 * @param <T> context value type
 * @param identifier namespaced key identity
 * @param type context value class
 */
public record InyContextKey<T>(InyIdentifier identifier, Class<T> type) {

    /** Validates a context key. Context keys always use reference types. */
    public InyContextKey {
        Objects.requireNonNull(identifier, "identifier");
        Objects.requireNonNull(type, "type");
        if (type.isPrimitive()) {
            throw new IllegalArgumentException("Context key type must be a reference type: "
                    + type.getTypeName());
        }
    }

    /**
     * Creates a typed key from a canonical {@code namespace:name} identifier.
     *
     * @param identifier canonical key identifier
     * @param type context value class
     * @param <T> context value type
     * @return the typed context key
     */
    public static <T> InyContextKey<T> of(String identifier, Class<T> type) {
        return new InyContextKey<>(InyIdentifier.parse(identifier), type);
    }

    /** Context key identity is its namespaced identifier; registries enforce type consistency. */
    @Override
    public boolean equals(Object object) {
        return this == object || object instanceof InyContextKey<?> other
                && identifier.equals(other.identifier);
    }

    /** Context key identity is its namespaced identifier; registries enforce type consistency. */
    @Override
    public int hashCode() {
        return identifier.hashCode();
    }
}
