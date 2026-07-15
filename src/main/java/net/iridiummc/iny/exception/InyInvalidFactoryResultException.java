package net.iridiummc.iny.exception;

import net.iridiummc.iny.api.InyIdentifier;

import java.util.Objects;

/** Raised when third-party factory output violates its registration contract. */
public final class InyInvalidFactoryResultException extends InyFactoryException {

    private final Class<?> requestedType;
    private final Class<?> registeredResultType;
    private final Class<?> actualResultType;

    public InyInvalidFactoryResultException(
            String path,
            InyIdentifier identifier,
            Class<?> requestedType,
            Class<?> registeredResultType,
            Object actualResult
    ) {
        super(message(path, identifier, requestedType, registeredResultType, actualResult), path, identifier);
        this.requestedType = Objects.requireNonNull(requestedType, "requestedType");
        this.registeredResultType = Objects.requireNonNull(registeredResultType, "registeredResultType");
        this.actualResultType = actualResult == null ? null : actualResult.getClass();
    }

    private static String message(
            String path,
            InyIdentifier identifier,
            Class<?> requestedType,
            Class<?> registeredResultType,
            Object result
    ) {
        String actual = result == null ? "Java null" : result.getClass().getTypeName();
        return "Failed to resolve " + identifier + " at path '" + path + "': factory declared "
                + registeredResultType.getTypeName() + " but returned " + actual
                + " while " + requestedType.getTypeName() + " was requested";
    }

    public Class<?> requestedType() {
        return requestedType;
    }

    public Class<?> registeredResultType() {
        return registeredResultType;
    }

    public Class<?> actualResultType() {
        return actualResultType;
    }
}
