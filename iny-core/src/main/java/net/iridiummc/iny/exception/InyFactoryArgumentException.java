package net.iridiummc.iny.exception;

import net.iridiummc.iny.api.InyIdentifier;

import java.util.Objects;

/** Raised when a positional argument is absent or cannot be resolved as requested. */
public final class InyFactoryArgumentException extends InyFactoryException {

    private final int argumentIndex;
    private final Class<?> requestedType;
    private final String actualType;

    public InyFactoryArgumentException(
            String path,
            InyIdentifier identifier,
            int argumentIndex,
            Class<?> requestedType,
            String actualType,
            String explanation,
            Throwable cause
    ) {
        super("Invalid argument " + argumentIndex + " for " + identifier + " at path '" + path
                + "': requested " + requestedType.getTypeName() + "; actual type is "
                + actualType + "; " + explanation,
                path, identifier, cause);
        this.argumentIndex = argumentIndex;
        this.requestedType = Objects.requireNonNull(requestedType, "requestedType");
        this.actualType = Objects.requireNonNull(actualType, "actualType");
    }

    public int argumentIndex() {
        return argumentIndex;
    }

    public Class<?> requestedType() {
        return requestedType;
    }

    public String actualType() {
        return actualType;
    }
}
