package net.iridiummc.iny.exception;

import net.iridiummc.iny.api.InyIdentifier;

import java.util.Objects;

/** Raised when the requested Java type cannot receive a factory's declared result type. */
public final class InyFactoryTypeException extends InyFactoryException {

    private final Class<?> requestedType;
    private final Class<?> registeredResultType;

    public InyFactoryTypeException(
            String path,
            InyIdentifier identifier,
            Class<?> requestedType,
            Class<?> registeredResultType
    ) {
        super("Cannot resolve " + identifier + " at path '" + path + "' as "
                + requestedType.getTypeName() + ": factory declares " + registeredResultType.getTypeName(),
                path, identifier);
        this.requestedType = Objects.requireNonNull(requestedType, "requestedType");
        this.registeredResultType = Objects.requireNonNull(registeredResultType, "registeredResultType");
    }

    public Class<?> requestedType() {
        return requestedType;
    }

    public Class<?> registeredResultType() {
        return registeredResultType;
    }
}
