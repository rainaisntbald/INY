package net.iridiummc.iny.exception;

import net.iridiummc.iny.api.InyIdentifier;

import java.util.Objects;

/** Wraps an exception thrown by a registered third-party factory. */
public final class InyFactoryExecutionException extends InyFactoryException {

    /** Java type requested by the caller. */
    private final Class<?> requestedType;
    /** Result type declared by the factory registration. */
    private final Class<?> registeredResultType;

    /**
     * Creates a failure wrapping an exception thrown by a factory.
     *
     * @param path configuration path containing the call
     * @param identifier factory identifier
     * @param requestedType requested Java type
     * @param registeredResultType factory's declared result type
     * @param cause exception thrown by the factory
     */
    public InyFactoryExecutionException(
            String path,
            InyIdentifier identifier,
            Class<?> requestedType,
            Class<?> registeredResultType,
            Throwable cause
    ) {
        super("Factory " + identifier + " failed at path '" + path + "' while requesting "
                        + requestedType.getTypeName() + " from a factory declaring "
                        + registeredResultType.getTypeName() + ": " + describe(cause),
                path, identifier, cause);
        this.requestedType = Objects.requireNonNull(requestedType, "requestedType");
        this.registeredResultType = Objects.requireNonNull(registeredResultType, "registeredResultType");
    }

    private static String describe(Throwable cause) {
        String message = cause.getMessage();
        return message == null || message.isBlank() ? cause.getClass().getTypeName() : message;
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
