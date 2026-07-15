package net.iridiummc.iny.exception;

import net.iridiummc.iny.api.InyIdentifier;

import java.util.Objects;

/** Raised when a positional argument is absent or cannot be resolved as requested. */
public final class InyFactoryArgumentException extends InyFactoryException {

    /** Zero-based failing argument index. */
    private final int argumentIndex;
    /** Requested Java argument type. */
    private final Class<?> requestedType;
    /** Human-readable argument value type. */
    private final String actualType;

    /**
     * Creates a factory-argument resolution failure.
     *
     * @param path configuration path containing the call
     * @param identifier factory identifier
     * @param argumentIndex zero-based failing argument index
     * @param requestedType requested Java type
     * @param actualType human-readable argument value type
     * @param explanation explanation of the failure
     * @param cause underlying failure
     */
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

    /**
     * Returns the failing argument index.
     *
     * @return zero-based argument index
     */
    public int argumentIndex() {
        return argumentIndex;
    }

    /**
     * Returns the requested argument type.
     *
     * @return requested Java type
     */
    public Class<?> requestedType() {
        return requestedType;
    }

    /**
     * Returns the human-readable argument value type.
     *
     * @return actual argument value type
     */
    public String actualType() {
        return actualType;
    }
}
