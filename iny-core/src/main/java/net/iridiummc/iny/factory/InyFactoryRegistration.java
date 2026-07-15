package net.iridiummc.iny.factory;

import net.iridiummc.iny.api.InyIdentifier;

import java.util.Objects;

/**
 * Immutable association between a namespaced identifier, declared type, and factory.
 * Lifecycle ownership is adapter-specific and is tracked separately by the shared Bukkit registry.
 *
 * @param <T> object type produced by the factory
 * @param identifier namespaced identifier used by INY calls
 * @param resultType declared result type
 * @param factory factory implementation
 */
public record InyFactoryRegistration<T>(
        InyIdentifier identifier,
        Class<T> resultType,
        InyFactory<T> factory
) {

    /** Validates and creates an immutable factory registration. */
    public InyFactoryRegistration {
        Objects.requireNonNull(identifier, "identifier");
        Objects.requireNonNull(resultType, "resultType");
        Objects.requireNonNull(factory, "factory");
        if (resultType.isPrimitive()) {
            throw new IllegalArgumentException("Factory result type must be a reference type: "
                    + resultType.getTypeName());
        }
    }
}
