package net.iridiummc.iny.exception;

import net.iridiummc.iny.api.InyIdentifier;

import java.util.Objects;

/** Raised when third-party factory output violates its registration contract. */
public final class InyInvalidFactoryResultException extends InyFactoryException {

    /** Java type requested by the caller. */
    private final Class<?> requestedType;
    /** Result type declared by the factory registration. */
    private final Class<?> registeredResultType;
    /** Actual factory result type, or null for a null result. */
    private final Class<?> actualResultType;

    /**
     * Creates a failure for a factory result that violates its registration contract.
     *
     * @param path configuration path containing the call
     * @param identifier factory identifier
     * @param requestedType requested Java type
     * @param registeredResultType factory's declared result type
     * @param actualResult value returned by the factory
     */
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

    /**
     * Returns the actual result type, when the factory did not return null.
     *
     * @return actual result type, or {@code null} for a null factory result
     */
    public Class<?> actualResultType() {
        return actualResultType;
    }
}
