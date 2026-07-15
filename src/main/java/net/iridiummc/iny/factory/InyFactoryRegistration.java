package net.iridiummc.iny.factory;

import net.iridiummc.iny.api.InyIdentifier;

import java.util.Objects;

/** Immutable association between a namespaced identifier, declared type, and factory. */
public record InyFactoryRegistration<T>(
        InyIdentifier identifier,
        Class<T> resultType,
        InyFactory<T> factory
) {

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
