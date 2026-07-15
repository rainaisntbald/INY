package net.iridiummc.iny.exception;

import net.iridiummc.iny.api.InyIdentifier;

import java.util.Objects;

/** Raised when a parsed call is resolved without a matching registration. */
public final class InyUnknownFactoryException extends InyFactoryException {

    /** Java type requested by the caller. */
    private final Class<?> requestedType;

    /**
     * Creates an unknown-factory failure.
     *
     * @param path configuration path containing the call
     * @param identifier unregistered factory identifier
     * @param requestedType requested Java type
     */
    public InyUnknownFactoryException(String path, InyIdentifier identifier, Class<?> requestedType) {
        super("No INY factory is registered for " + identifier + " at path '" + path
                + "' while requesting " + requestedType.getTypeName(), path, identifier);
        this.requestedType = Objects.requireNonNull(requestedType, "requestedType");
    }

    /**
     * Returns the type requested by the caller.
     *
     * @return requested Java type
     */
    public Class<?> requestedType() {
        return requestedType;
    }
}
