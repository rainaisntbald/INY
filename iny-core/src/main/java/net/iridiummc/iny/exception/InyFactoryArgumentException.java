package net.iridiummc.iny.exception;

import net.iridiummc.iny.api.InyIdentifier;
import net.iridiummc.iny.value.InyValueType;

import java.util.Objects;

/** Raised when a positional argument is absent or cannot be resolved as requested. */
public final class InyFactoryArgumentException extends InyFactoryException {

    private final int argumentIndex;
    private final Class<?> requestedType;
    private final InyValueType actualValueType;

    public InyFactoryArgumentException(
            String path,
            InyIdentifier identifier,
            int argumentIndex,
            Class<?> requestedType,
            InyValueType actualValueType,
            String explanation,
            Throwable cause
    ) {
        super("Invalid argument " + argumentIndex + " for " + identifier + " at path '" + path
                + "': requested " + requestedType.getTypeName() + "; actual INY type is "
                + (actualValueType == null ? "missing" : actualValueType) + "; " + explanation,
                path, identifier, cause);
        this.argumentIndex = argumentIndex;
        this.requestedType = Objects.requireNonNull(requestedType, "requestedType");
        this.actualValueType = actualValueType;
    }

    public int argumentIndex() {
        return argumentIndex;
    }

    public Class<?> requestedType() {
        return requestedType;
    }

    public InyValueType actualValueType() {
        return actualValueType;
    }
}
