package net.iridiummc.iny.exception;

import net.iridiummc.iny.api.InyIdentifier;

import java.util.Objects;

/** Raised when the requested Java type cannot receive a factory's declared result type. */
public final class InyFactoryTypeException extends InyFactoryException {

    /** Java type requested by the caller. */
    private final Class<?> requestedType;
    /** Result type declared by the factory registration. */
    private final Class<?> registeredResultType;

    /**
     * Creates an incompatible factory-type failure.
     *
     * @param path configuration path containing the call
     * @param identifier factory identifier
     * @param requestedType requested Java type
     * @param registeredResultType factory's declared result type
     */
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

    /**
     * Returns the type requested by the caller.
     *
     * @return requested Java type
     */
    public Class<?> requestedType() {
        return requestedType;
    }

    /**
     * Returns the factory's declared result type.
     *
     * @return registered result type
     */
    public Class<?> registeredResultType() {
        return registeredResultType;
    }
}
