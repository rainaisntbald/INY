package net.iridiummc.iny.exception;

import net.iridiummc.iny.api.InyIdentifier;

import java.util.Objects;

/** Wraps an exception thrown by a registered third-party factory. */
public final class InyFactoryExecutionException extends InyFactoryException {

    private final Class<?> requestedType;
    private final Class<?> registeredResultType;

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

    public Class<?> requestedType() {
        return requestedType;
    }

    public Class<?> registeredResultType() {
        return registeredResultType;
    }
}
