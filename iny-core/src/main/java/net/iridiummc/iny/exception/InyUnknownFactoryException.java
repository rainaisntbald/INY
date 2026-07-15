package net.iridiummc.iny.exception;

import net.iridiummc.iny.api.InyIdentifier;

import java.util.Objects;

/** Raised when a parsed call is resolved without a matching registration. */
public final class InyUnknownFactoryException extends InyFactoryException {

    private final Class<?> requestedType;

    public InyUnknownFactoryException(String path, InyIdentifier identifier, Class<?> requestedType) {
        super("No INY factory is registered for " + identifier + " at path '" + path
                + "' while requesting " + requestedType.getTypeName(), path, identifier);
        this.requestedType = Objects.requireNonNull(requestedType, "requestedType");
    }

    public Class<?> requestedType() {
        return requestedType;
    }
}
